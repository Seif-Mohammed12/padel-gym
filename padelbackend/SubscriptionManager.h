#pragma once
#include <iostream>
#include <string>
#include <vector>
#include <queue>
#include <unordered_map>
#include <ctime>
#include "include/json.hpp"

using json = nlohmann::json;

struct SubscriptionHistoryNode {
    std::string planName;
    std::string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    SubscriptionHistoryNode* next;

    SubscriptionHistoryNode(std::string plan, std::string dur, time_t start, time_t expiry, double p)
            : planName(plan), duration(dur), startDate(start), expiryDate(expiry), price(p), next(nullptr) {}

    json toJson() const {
        return {
                {"planName", planName},
                {"duration", duration},
                {"startDate", static_cast<long long>(startDate)},
                {"expiryDate", static_cast<long long>(expiryDate)},
                {"price", price}
        };
    }

    static SubscriptionHistoryNode* fromJson(const json& j) {
        auto* node = new SubscriptionHistoryNode(
                j.at("planName").get<std::string>(),
                j.at("duration").get<std::string>(),
                static_cast<time_t>(j.at("startDate").get<long long>()),
                static_cast<time_t>(j.at("expiryDate").get<long long>()),
                j.at("price").get<double>()
        );
        return node;
    }
};

class Subscription {
private:
    std::string memberId; // Changed from int to string
    std::string planName;
    std::string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    bool isActive;
    SubscriptionHistoryNode* historyHead;

public:
    Subscription(const std::string& id, std::string plan, std::string dur, double p);
    Subscription(const std::string& id, std::string plan, std::string dur, time_t start, time_t expiry, double p, bool active);

    std::string getMemberId() const { return memberId; }
    std::string getPlanName() const { return planName; }
    std::string getDuration() const { return duration; }
    time_t getStartDate() const { return startDate; }
    time_t getExpiryDate() const { return expiryDate; }
    double getPrice() const { return price; }
    bool getIsActive() const { return isActive; }
    SubscriptionHistoryNode* getHistoryHead() const { return historyHead; }

    bool isExpired() const;
    void addToHistory(std::string plan, std::string dur, time_t start, time_t expiry, double p);
    void renew(std::string newPlan, std::string newDuration, double newPrice);

    json toJson() const;
    static Subscription fromJson(const json& j);
};

class Notification {
private:
    std::string memberId; // Already string, but ensure consistency
    std::string message;
    time_t expiryDate;

public:
    Notification(const std::string& id, std::string msg, time_t expiry);

    std::string getMemberId() const { return memberId; }
    std::string getMessage() const { return message; }
    time_t getExpiryDate() const { return expiryDate; }

    json toJson() const;
    static Notification fromJson(const json& j);
};

class SubscriptionManager {
private:
    std::vector<Subscription> subscriptions;
    std::unordered_map<std::string, int> memberIndexMap; // Changed key to string
    std::queue<Notification> notificationQueue;
    const std::string subscriptionsFile = "active-subscriptions.json"; // Changed to separate file

    void updateMemberIndex();

public:
    void addSubscription(const Subscription& sub);
    void removeSubscription(const std::string& memberId); // Updated to string
    Subscription* findSubscription(const std::string& memberId); // Updated to string
    void addNotification(const Notification& notif);
    void processNotifications();
    void checkRenewalReminders();
    void printAllSubscriptions();
    void printSubscriptionHistory(const std::string& memberId);
    void printExpiringSubscriptions(int days);
    void saveToFile();
    void loadFromFile();
    std::vector<Subscription> getExpiringSubscriptions(int days);
    void cancelSubscription(const std::string& memberId); // Updated to string

    json getSubscriptionsAsJson() const;
    void loadSubscriptionsFromJson(const json& data);
};