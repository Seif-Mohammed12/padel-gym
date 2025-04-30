#pragma once
#include <iostream>
#include <string>
#include <vector>
#include <queue>
#include <unordered_map>
#include <ctime>
#include <fstream>

using namespace std;

// History node for subscription history
struct SubscriptionHistoryNode {
    string planName;
    string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    SubscriptionHistoryNode* next;

    SubscriptionHistoryNode(string plan, string dur, time_t start, time_t expiry, double p)
        : planName(plan), duration(dur), startDate(start), expiryDate(expiry), price(p), next(nullptr) {}
};

// Simple Subscription class
class Subscription {
private:
    int memberId;
    string planName;
    string duration;
    time_t startDate;
    time_t expiryDate;
    double price;
    bool isActive;
    SubscriptionHistoryNode* historyHead;

public:
    Subscription(int id, string plan, string dur, double p) {
        memberId = id;
        planName = plan;
        duration = dur;
        price = p;
        startDate = time(nullptr);
        isActive = true;
        historyHead = nullptr;

        // Calculate expiry date based on duration
        int months = 1;
        if (dur == "3_months") months = 3;
        else if (dur == "6_months") months = 6;
        else if (dur == "1_year") months = 12;

        expiryDate = startDate + (months * 30 * 24 * 60 * 60);
    }

    // Getters
    int getMemberId() const { return memberId; }
    string getPlanName() const { return planName; }
    string getDuration() const { return duration; }
    time_t getStartDate() const { return startDate; }
    time_t getExpiryDate() const { return expiryDate; }
    double getPrice() const { return price; }
    bool getIsActive() const { return isActive; }
    SubscriptionHistoryNode* getHistoryHead() const { return historyHead; }

    bool isExpired() const {
        return time(nullptr) > expiryDate;
    }

    void addToHistory(string plan, string dur, time_t start, time_t expiry, double p) {
        SubscriptionHistoryNode* newNode = new SubscriptionHistoryNode(plan, dur, start, expiry, p);
        newNode->next = historyHead;
        historyHead = newNode;
    }

    void renew(string newPlan, string newDuration, double newPrice) {
        // Add current state to history
        addToHistory(planName, duration, startDate, expiryDate, price);

        // Update current state
        planName = newPlan;
        duration = newDuration;
        price = newPrice;
        startDate = time(nullptr);

        // Calculate new expiry date
        int months = 1;
        if (newDuration == "3_months") months = 3;
        else if (newDuration == "6_months") months = 6;
        else if (newDuration == "1_year") months = 12;

        expiryDate = startDate + (months * 30 * 24 * 60 * 60);
    }
};

// Notification class
class Notification {
private:
    int memberId;
    string message;
    time_t expiryDate;

public:
    Notification(int id, string msg, time_t expiry)
        : memberId(id), message(msg), expiryDate(expiry) {}

    int getMemberId() const { return memberId; }
    string getMessage() const { return message; }
    time_t getExpiryDate() const { return expiryDate; }
};

// Main Subscription Manager class
class SubscriptionManager {
private:
    vector<Subscription> subscriptions;
    unordered_map<int, int> memberIndexMap;
    queue<Notification> notificationQueue;
    string subscriptionsFile = "subscriptions.txt";

    void updateMemberIndex() {
        memberIndexMap.clear();
        for (int i = 0; i < subscriptions.size(); ++i) {
            memberIndexMap[subscriptions[i].getMemberId()] = i;
        }
    }

public:
    // Vector operations
    void addSubscription(const Subscription& sub);
    void removeSubscription(int memberId);

    // Hash table operations
    Subscription* findSubscription(int memberId);

    // Queue operations
    void addNotification(const Notification& notif);
    void processNotifications();
    void checkRenewalReminders();

    // Print operations
    void printAllSubscriptions();
    void printSubscriptionHistory(int memberId);
    void printExpiringSubscriptions(int days);

    // File operations
    void saveToFile();
    void loadFromFile();

    // Utility functions
    vector<Subscription> getExpiringSubscriptions(int days);
    void cancelSubscription(int memberId);
}; 