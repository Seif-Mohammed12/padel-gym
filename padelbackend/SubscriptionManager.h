#pragma once
#include <string>
#include <vector>
#include <queue>
#include <unordered_map>
#include <ctime>
#include "include/json.hpp"

using json = nlohmann::json;

// ==================== Forward Declarations ====================
class Subscription;
class Notification;

// ==================== SubscriptionHistoryNode ====================
struct SubscriptionHistoryNode {
    std::string planName;
    std::string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    SubscriptionHistoryNode* next;

    SubscriptionHistoryNode(std::string plan, std::string dur, time_t start, time_t expiry, double p,
                            SubscriptionHistoryNode *pNode);

    json toJson() const;
    static SubscriptionHistoryNode* fromJson(const json& j);
};

// ==================== Subscription Class ====================
class Subscription {
private:
    std::string memberId;
    std::string planName;
    std::string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    bool isActive;
    SubscriptionHistoryNode* historyHead;

    static int durationToMonths(const std::string& duration);

public:
    Subscription(const std::string& id, std::string plan,
                 std::string dur, double p);
    Subscription(const std::string& id, std::string plan,
                 std::string dur, time_t start, time_t expiry,
                 double p, bool active);
    ~Subscription();

    // Accessors
    const std::string& getMemberId() const { return memberId; }
    const std::string& getPlanName() const { return planName; }
    const std::string& getDuration() const { return duration; }
    time_t getStartDate() const { return startDate; }
    time_t getExpiryDate() const { return expiryDate; }
    double getPrice() const { return price; }
    bool getIsActive() const { return isActive; }
    const SubscriptionHistoryNode* getHistoryHead() const { return historyHead; }

    // Operations
    bool isExpired() const;
    void addToHistory(std::string plan, std::string dur,
                      time_t start, time_t expiry, double p);
    void renew(std::string newPlan, std::string newDuration, double newPrice);

    // Serialization
    json toJson() const;
    static Subscription fromJson(const json& j);
};

// ==================== Notification Class ====================
class Notification {
private:
    std::string memberId;
    std::string message;
    time_t expiryDate;

public:
    Notification(std::string id, std::string msg, time_t expiry);

    // Accessors
    const std::string& getMemberId() const { return memberId; }
    const std::string& getMessage() const { return message; }
    time_t getExpiryDate() const { return expiryDate; }

    // Serialization
    json toJson() const;
    static Notification fromJson(const json& j);
};

// ==================== SubscriptionManager Class ====================
class SubscriptionManager {
private:
    std::vector<Subscription> subscriptions;
    std::unordered_map<std::string, size_t> memberIndexMap;
    std::queue<Notification> notificationQueue;
    const std::string subscriptionsFile = "active-subscriptions.json";

    void updateMemberIndex();

public:
    // Core Operations
    void addSubscription(const Subscription& sub);
    bool removeSubscription(const std::string& memberId);
    bool cancelSubscription(const std::string& memberId);

    // Lookup
    Subscription* findSubscription(const std::string& memberId);
    const Subscription* findSubscription(const std::string& memberId) const;

    // Notification Handling
    void addNotification(const Notification& notif);
    void processNotifications();
    void checkRenewalReminders();

    // Data Persistence
    void saveToFile();
    void loadFromFile();
    json getSubscriptionsAsJson() const;
    void loadSubscriptionsFromJson(const json& data);

    // Queries
    std::vector<Subscription> getExpiringSubscriptions(int days) const;
    std::vector<Subscription> getAllSubscriptions() const;
    std::vector<SubscriptionHistoryNode> getSubscriptionHistory(const std::string& memberId) const;
};