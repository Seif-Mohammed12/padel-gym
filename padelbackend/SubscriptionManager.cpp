#include "SubscriptionManager.h"
#include "FileManager.h"
#include <iomanip>
#include <sstream>
#include <algorithm>
#include <iostream>

//=============================================================================
// Helper Functions
// Utility functions for date/time formatting and parsing
//=============================================================================

namespace
{
    std::string formatTime(time_t timestamp)
    {
        struct tm *timeinfo = localtime(&timestamp);
        char buffer[80];
        strftime(buffer, sizeof(buffer), "%d-%m-%Y", timeinfo);
        return std::string(buffer);
    }

    time_t parseTime(const std::string &timeStr)
    {
        std::istringstream timeStream(timeStr);
        std::tm tm = {};
        timeStream >> std::get_time(&tm, "%d-%m-%Y");
        return mktime(&tm);
    }
}

//=============================================================================
// SubscriptionHistoryNode Implementation
// Represents a single node in the subscription history linked list
//=============================================================================

SubscriptionHistoryNode::SubscriptionHistoryNode(std::string plan, std::string dur,
                                                 time_t start, time_t expiry,
                                                 double p,
                                                 SubscriptionHistoryNode *pNode)
    : planName(std::move(plan)), duration(std::move(dur)),
      startDate(start), expiryDate(expiry), price(p), next(nullptr) {}

json SubscriptionHistoryNode::toJson() const
{
    return {
        {"planName", planName},
        {"duration", duration},
        {"startDate", static_cast<long long>(startDate)},
        {"expiryDate", static_cast<long long>(expiryDate)},
        {"price", price}};
}

SubscriptionHistoryNode *SubscriptionHistoryNode::fromJson(const json &j)
{
    return new SubscriptionHistoryNode(
        j.at("planName").get<std::string>(),
        j.at("duration").get<std::string>(),
        static_cast<time_t>(j.at("startDate").get<long long>()),
        static_cast<time_t>(j.at("expiryDate").get<long long>()),
        j.at("price").get<double>(), nullptr);
}

//=============================================================================
// Subscription Class Implementation
// Core subscription functionality including creation, renewal, and serialization
//=============================================================================

int Subscription::durationToMonths(const std::string &duration)
{
    static const std::unordered_map<std::string, int> durationMap = {
        {"1_month", 1},
        {"3_months", 3},
        {"6_months", 6},
        {"1_year", 12}};
    auto it = durationMap.find(duration);
    return it != durationMap.end() ? it->second : 1;
}

Subscription::Subscription(const std::string &id, std::string plan, std::string dur, double p)
    : memberId(id), planName(std::move(plan)), duration(std::move(dur)), price(p),
      startDate(time(nullptr)), isActive(true), historyHead(nullptr)
{
    expiryDate = startDate + (durationToMonths(duration) * 30 * 24 * 60 * 60);
}

Subscription::Subscription(const std::string &id, std::string plan, std::string dur,
                           time_t start, time_t expiry, double p, bool active)
    : memberId(id), planName(std::move(plan)), duration(std::move(dur)),
      startDate(start), expiryDate(expiry), price(p), isActive(active), historyHead(nullptr) {}

Subscription::~Subscription()
{
    while (historyHead)
    {
        SubscriptionHistoryNode *temp = historyHead;
        historyHead = historyHead->next;
        delete temp;
    }
}

bool Subscription::isExpired() const
{
    return time(nullptr) > expiryDate;
}

void Subscription::addToHistory(std::string plan, std::string dur,
                                time_t start, time_t expiry, double p)
{
    historyHead = new SubscriptionHistoryNode(
        std::move(plan), std::move(dur), start, expiry, p, historyHead);
}

void Subscription::renew(std::string newPlan, std::string newDuration, double newPrice)
{
    addToHistory(planName, duration, startDate, expiryDate, price);
    planName = std::move(newPlan);
    duration = std::move(newDuration);
    price = newPrice;
    startDate = time(nullptr);
    expiryDate = startDate + (durationToMonths(duration) * 30 * 24 * 60 * 60);
}

json Subscription::toJson() const
{
    json history = json::array();
    for (auto *current = historyHead; current; current = current->next)
    {
        history.push_back(current->toJson());
    }

    return {
        {"memberId", memberId},
        {"planName", planName},
        {"duration", duration},
        {"startDate", formatTime(startDate)},
        {"expiryDate", formatTime(expiryDate)},
        {"price", price},
        {"isActive", isActive},
        {"history", history}};
}

Subscription Subscription::fromJson(const json &j)
{
    Subscription sub(
        j.at("memberId").get<std::string>(),
        j.at("planName").get<std::string>(),
        j.at("duration").get<std::string>(),
        parseTime(j.at("startDate").get<std::string>()),
        parseTime(j.at("expiryDate").get<std::string>()),
        j.at("price").get<double>(),
        j.at("isActive").get<bool>());

    if (j.contains("history"))
    {
        for (const auto &historyJson : j.at("history"))
        {
            sub.addToHistory(
                historyJson.at("planName").get<std::string>(),
                historyJson.at("duration").get<std::string>(),
                parseTime(historyJson.at("startDate").get<std::string>()),
                parseTime(historyJson.at("expiryDate").get<std::string>()),
                historyJson.at("price").get<double>());
        }
    }
    return sub;
}

//=============================================================================
// Notification System Implementation
// Handles subscription notifications and reminders
//=============================================================================

Notification::Notification(std::string id, std::string msg, time_t expiry)
    : memberId(std::move(id)), message(std::move(msg)), expiryDate(expiry) {}

json Notification::toJson() const
{
    return {
        {"memberId", memberId},
        {"message", message},
        {"expiryDate", static_cast<long long>(expiryDate)}};
}

Notification Notification::fromJson(const json &j)
{
    return Notification(
        j.at("memberId").get<std::string>(),
        j.at("message").get<std::string>(),
        static_cast<time_t>(j.at("expiryDate").get<long long>()));
}

//=============================================================================
// SubscriptionManager Core Implementation
// Main subscription management functionality
//=============================================================================

void SubscriptionManager::addSubscription(const Subscription &sub)
{
    subscriptions.push_back(sub);
    updateMemberIndex();
    saveToFile();
}

bool SubscriptionManager::removeSubscription(const std::string &memberId)
{
    auto it = memberIndexMap.find(memberId);
    if (it == memberIndexMap.end())
        return false;

    subscriptions.erase(subscriptions.begin() + it->second);
    updateMemberIndex();
    saveToFile();
    return true;
}

bool SubscriptionManager::cancelSubscription(const std::string &memberId)
{
    auto sub = findSubscription(memberId);
    if (!sub)
        return false;

    sub->addToHistory(sub->getPlanName(), sub->getDuration(),
                      sub->getStartDate(), sub->getExpiryDate(), sub->getPrice());
    return removeSubscription(memberId);
}

Subscription *SubscriptionManager::findSubscription(const std::string &memberId)
{
    auto it = memberIndexMap.find(memberId);
    return it != memberIndexMap.end() ? &subscriptions[it->second] : nullptr;
}

const Subscription *SubscriptionManager::findSubscription(const std::string &memberId) const
{
    auto it = memberIndexMap.find(memberId);
    return it != memberIndexMap.end() ? &subscriptions[it->second] : nullptr;
}

void SubscriptionManager::updateMemberIndex()
{
    memberIndexMap.clear();
    for (size_t i = 0; i < subscriptions.size(); ++i)
    {
        memberIndexMap[subscriptions[i].getMemberId()] = i;
    }
}

void SubscriptionManager::addNotification(const Notification &notif)
{
    notificationQueue.push(notif);
}

void SubscriptionManager::processNotifications()
{
    while (!notificationQueue.empty())
    {
        notificationQueue.pop();
    }
}

void SubscriptionManager::checkRenewalReminders()
{
    time_t now = time(nullptr);
    for (const auto &sub : subscriptions)
    {
        time_t expiry = sub.getExpiryDate();
        if (expiry - now <= 7 * 24 * 60 * 60 && !sub.isExpired())
        {
            double discount = (expiry - now <= 14 * 24 * 60 * 60) ? 0.1 : 0.0;
            std::string message = "Your subscription expires in " +
                                  std::to_string((expiry - now) / (24 * 60 * 60)) +
                                  " days. Early renewal discount: " +
                                  std::to_string(discount * 100) + "%";
            notificationQueue.push(Notification(sub.getMemberId(), message, expiry));
        }
    }
}

//=============================================================================
// Data Persistence
// Handles saving and loading subscription data
//=============================================================================

void SubscriptionManager::saveToFile()
{
    FileManager::save(getSubscriptionsAsJson(), subscriptionsFile);
}

void SubscriptionManager::loadFromFile()
{
    loadSubscriptionsFromJson(FileManager::load(subscriptionsFile));
}

json SubscriptionManager::getSubscriptionsAsJson() const
{
    json subscriptionsJson = json::array();
    for (const auto &sub : subscriptions)
    {
        subscriptionsJson.push_back(sub.toJson());
    }
    return subscriptionsJson;
}

void SubscriptionManager::loadSubscriptionsFromJson(const json &data)
{
    subscriptions.clear();
    memberIndexMap.clear();

    if (!data.is_array())
    {
        std::cerr << "Invalid subscription data format" << std::endl;
        return;
    }

    for (const auto &subJson : data)
    {
        try
        {
            subscriptions.push_back(Subscription::fromJson(subJson));
        }
        catch (const json::exception &e)
        {
            std::cerr << "Error parsing subscription: " << e.what() << std::endl;
        }
    }

    updateMemberIndex();
}

//=============================================================================
// Query Operations
// Methods for retrieving subscription information
//=============================================================================

std::vector<Subscription> SubscriptionManager::getExpiringSubscriptions(int days) const
{
    std::vector<Subscription> expiring;
    time_t threshold = time(nullptr) + (days * 24 * 60 * 60);

    for (const auto &sub : subscriptions)
    {
        if (sub.getExpiryDate() <= threshold && !sub.isExpired())
        {
            expiring.push_back(sub);
        }
    }
    return expiring;
}

std::vector<Subscription> SubscriptionManager::getAllSubscriptions() const
{
    return subscriptions;
}

std::vector<SubscriptionHistoryNode> SubscriptionManager::getSubscriptionHistory(
    const std::string &memberId) const
{
    std::vector<SubscriptionHistoryNode> history;
    const Subscription *sub = findSubscription(memberId);
    if (sub)
    {
        for (const SubscriptionHistoryNode *node = sub->getHistoryHead(); node; node = node->next)
        {
            history.push_back(*node);
        }
    }
    return history;
}