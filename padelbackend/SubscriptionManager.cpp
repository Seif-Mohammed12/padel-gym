#include "SubscriptionManager.h"
#include "FileManager.h"
#include <iomanip>
#include <sstream>
#include <iostream>

std::string formatTime(time_t timestamp) {
    struct tm* timeinfo = localtime(&timestamp);
    char buffer[80];
    strftime(buffer, sizeof(buffer), "%d-%m-%Y", timeinfo);
    return std::string(buffer);
}


// Subscription class implementations
Subscription::Subscription(const std::string& id, std::string plan, std::string dur, double p) {
    memberId = id;
    planName = plan;
    duration = dur;
    price = p;
    startDate = time(nullptr);
    isActive = true;
    historyHead = nullptr;

    int months = 1;
    if (dur == "3_months") months = 3;
    else if (dur == "6_months") months = 6;
    else if (dur == "1_year") months = 12;
    expiryDate = startDate + (months * 30 * 24 * 60 * 60);
}

Subscription::Subscription(const std::string& id, std::string plan, std::string dur, time_t start, time_t expiry, double p, bool active) {
    memberId = id;
    planName = plan;
    duration = dur;
    startDate = start;
    expiryDate = expiry;
    price = p;
    isActive = active;
    historyHead = nullptr;
}

bool Subscription::isExpired() const {
    return time(nullptr) > expiryDate;
}

void Subscription::addToHistory(std::string plan, std::string dur, time_t start, time_t expiry, double p) {
    auto* newNode = new SubscriptionHistoryNode(plan, dur, start, expiry, p);
    newNode->next = historyHead;
    historyHead = newNode;
}

void Subscription::renew(std::string newPlan, std::string newDuration, double newPrice) {
    addToHistory(planName, duration, startDate, expiryDate, price);
    planName = newPlan;
    duration = newDuration;
    price = newPrice;
    startDate = time(nullptr);

    int months = 1;
    if (newDuration == "3_months") months = 3;
    else if (newDuration == "6_months") months = 6;
    else if (newDuration == "1_year") months = 12;
    expiryDate = startDate + (months * 30 * 24 * 60 * 60);
}

json Subscription::toJson() const {
    json history = json::array();
    SubscriptionHistoryNode* current = historyHead;
    while (current) {
        history.push_back(current->toJson());
        current = current->next;
    }

    // Convert time_t to human-readable date strings
    std::string startDateStr = formatTime(startDate);
    std::string expiryDateStr = formatTime(expiryDate);

    return {
            {"memberId", memberId},
            {"planName", planName},
            {"duration", duration},
            {"startDate", startDateStr},  // Format date as string
            {"expiryDate", expiryDateStr}, // Format date as string
            {"price", price},
            {"isActive", isActive},
            {"history", history}
    };
}




Subscription Subscription::fromJson(const json& j) {
    // Parse the startDate and expiryDate from the string using std::istringstream
    std::istringstream startDateStream(j.at("startDate").get<std::string>());
    std::istringstream expiryDateStream(j.at("expiryDate").get<std::string>());

    std::tm startTm = {};
    std::tm expiryTm = {};

    // Read the date using the specified format (same as in strptime)
    startDateStream >> std::get_time(&startTm, "%d-%m-%Y");
    expiryDateStream >> std::get_time(&expiryTm, "%d-%m-%Y");

    // Convert std::tm to time_t
    time_t startDate = mktime(&startTm);
    time_t expiryDate = mktime(&expiryTm);

    Subscription sub(
            j.at("memberId").get<std::string>(),
            j.at("planName").get<std::string>(),
            j.at("duration").get<std::string>(),
            startDate,
            expiryDate,
            j.at("price").get<double>(),
            j.at("isActive").get<bool>()
    );

    if (j.contains("history")) {
        for (const auto& historyJson : j.at("history")) {
            auto* node = SubscriptionHistoryNode::fromJson(historyJson);
            node->next = sub.historyHead;
            sub.historyHead = node;
        }
    }

    return sub;
}



Notification::Notification(const std::string& id, std::string msg, time_t expiry)
        : memberId(id), message(msg), expiryDate(expiry) {}

json Notification::toJson() const {
    return {
            {"memberId", memberId}, // Already a string
            {"message", message},
            {"expiryDate", static_cast<long long>(expiryDate)}
    };
}

Notification Notification::fromJson(const json& j) {
    return Notification(
            j.at("memberId").get<std::string>(), // Changed to string
            j.at("message").get<std::string>(),
            static_cast<time_t>(j.at("expiryDate").get<long long>())
    );
}

// SubscriptionManager class implementations
void SubscriptionManager::addSubscription(const Subscription& sub) {
    subscriptions.push_back(sub);
    updateMemberIndex();
    saveToFile();
}

void SubscriptionManager::removeSubscription(const std::string& memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        subscriptions.erase(subscriptions.begin() + it->second);
        updateMemberIndex();
        saveToFile();
        std::cout << "Subscription removed successfully!" << std::endl;
    } else {
        std::cout << "Member not found." << std::endl;
    }
}

void SubscriptionManager::updateMemberIndex() {
    memberIndexMap.clear();
    for (int i = 0; i < subscriptions.size(); ++i) {
        memberIndexMap[subscriptions[i].getMemberId()] = i;
    }
}

Subscription* SubscriptionManager::findSubscription(const std::string& memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        return &subscriptions[it->second];
    }
    return nullptr;
}

void SubscriptionManager::addNotification(const Notification& notif) {
    notificationQueue.push(notif);
}

void SubscriptionManager::processNotifications() {
    std::cout << "\nProcessing " << notificationQueue.size() << " notifications...\n";
    while (!notificationQueue.empty()) {
        Notification notif = notificationQueue.front();
        notificationQueue.pop();

        std::cout << "\n=== Notification ===\n";
        std::cout << "Member ID: " << notif.getMemberId() << std::endl;
        std::cout << "Message: " << notif.getMessage() << std::endl;
        std::cout << "Expiry Date: " << formatTime(notif.getExpiryDate()) << std::endl;
    }
}

void SubscriptionManager::checkRenewalReminders() {
    time_t now = time(nullptr);
    for (const auto& sub : subscriptions) {
        time_t expiry = sub.getExpiryDate();

        if (expiry - now <= 7 * 24 * 60 * 60 && !sub.isExpired()) {
            double discount = (expiry - now <= 14 * 24 * 60 * 60) ? 0.1 : 0.0;
            std::string message = "Your subscription expires in " +
                                  std::to_string((expiry - now) / (24 * 60 * 60)) +
                                  " days. Early renewal discount: " +
                                  std::to_string(discount * 100) + "%";

            notificationQueue.push(Notification(
                    sub.getMemberId(), message, expiry
            ));
        }
    }
    processNotifications();
}

void SubscriptionManager::printAllSubscriptions() {
    std::cout << "\n=== All Subscriptions ===\n";
    for (const auto& sub : subscriptions) {
        std::cout << "Member ID: " << sub.getMemberId() << std::endl;
        std::cout << "Plan: " << sub.getPlanName() << std::endl;
        std::cout << "Duration: " << sub.getDuration() << std::endl;
        std::cout << "Price: $" << std::fixed << std::setprecision(2) << sub.getPrice() << std::endl;
        std::cout << "Start Date: " << formatTime(sub.getStartDate()) << std::endl;
        std::cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << std::endl;
        std::cout << "-------------------\n";
    }
}

void SubscriptionManager::printSubscriptionHistory(const std::string& memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        const Subscription& sub = subscriptions[it->second];

        std::cout << "\n=== Subscription History for Member " << memberId << " ===\n";
        std::cout << "Current Plan: " << sub.getPlanName() << std::endl;
        std::cout << "Current Duration: " << sub.getDuration() << std::endl;
        std::cout << "Current Price: $" << std::fixed << std::setprecision(2) << sub.getPrice() << std::endl;
        std::cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << std::endl;

        std::cout << "\nHistory:\n";
        SubscriptionHistoryNode* current = sub.getHistoryHead();
        while (current) {
            std::cout << "Plan: " << current->planName << std::endl;
            std::cout << "Duration: " << current->duration << std::endl;
            std::cout << "Price: $" << std::fixed << std::setprecision(2) << current->price << std::endl;
            std::cout << "Start Date: " << formatTime(current->startDate) << std::endl;
            std::cout << "Expiry Date: " << formatTime(current->expiryDate) << std::endl;
            std::cout << "-------------------\n";
            current = current->next;
        }
    } else {
        std::cout << "Member not found." << std::endl;
    }
}

std::vector<Subscription> SubscriptionManager::getExpiringSubscriptions(int days) {
    std::vector<Subscription> expiring;
    time_t now = time(nullptr);
    time_t threshold = now + (days * 24 * 60 * 60);

    for (const auto& sub : subscriptions) {
        if (sub.getExpiryDate() <= threshold && !sub.isExpired()) {
            expiring.push_back(sub);
        }
    }
    return expiring;
}

void SubscriptionManager::printExpiringSubscriptions(int days) {
    std::vector<Subscription> expiring = getExpiringSubscriptions(days);

    std::cout << "\n=== Subscriptions Expiring in " << days << " Days ===\n";
    for (const auto& sub : expiring) {
        std::cout << "Member ID: " << sub.getMemberId() << std::endl;
        std::cout << "Plan: " << sub.getPlanName() << std::endl;
        std::cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << std::endl;
        std::cout << "-------------------\n";
    }
}

void SubscriptionManager::cancelSubscription(const std::string& memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        subscriptions[it->second].addToHistory(
                subscriptions[it->second].getPlanName(),
                subscriptions[it->second].getDuration(),
                subscriptions[it->second].getStartDate(),
                subscriptions[it->second].getExpiryDate(),
                subscriptions[it->second].getPrice()
        );
        subscriptions.erase(subscriptions.begin() + it->second);
        updateMemberIndex();
        saveToFile();
        std::cout << "Subscription cancelled successfully!" << std::endl;
    } else {
        std::cout << "Member not found." << std::endl;
    }
}

json SubscriptionManager::getSubscriptionsAsJson() const {
    json jsonSubscriptions = json::array();
    for (const auto& sub : subscriptions) {
        jsonSubscriptions.push_back(sub.toJson());
    }
    return jsonSubscriptions;
}

void SubscriptionManager::loadSubscriptionsFromJson(const json& data) {
    subscriptions.clear();
    for (const auto& subJson : data) {
        subscriptions.push_back(Subscription::fromJson(subJson));
    }
    updateMemberIndex();
}

void SubscriptionManager::saveToFile() {
    FileManager::save(getSubscriptionsAsJson(), subscriptionsFile);
}

void SubscriptionManager::loadFromFile() {
    json data = FileManager::load(subscriptionsFile);
    loadSubscriptionsFromJson(data);
}