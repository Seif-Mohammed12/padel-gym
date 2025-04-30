#include "SubscriptionManager.h"
#include <fstream>
#include <iomanip>
#include <sstream>
#include <ctime>
#include <iostream>

// Helper function to format time_t to string
string formatTime(time_t timestamp) {
    struct tm* timeinfo = localtime(&timestamp);
    char buffer[80];
    strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", timeinfo);
    return string(buffer);
}

// Vector operations
void SubscriptionManager::addSubscription(const Subscription& sub) {
    subscriptions.push_back(sub);
    updateMemberIndex();
    saveToFile();
}

void SubscriptionManager::removeSubscription(int memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        subscriptions.erase(subscriptions.begin() + it->second);
        updateMemberIndex();
        saveToFile();
        cout << "Subscription removed successfully!" << endl;
    } else {
        cout << "Member not found." << endl;
    }
}

// Hash table operations
void SubscriptionManager::updateMemberIndex() {
    memberIndexMap.clear();
    for (int i = 0; i < subscriptions.size(); ++i) {
        memberIndexMap[subscriptions[i].getMemberId()] = i;
    }
}

Subscription* SubscriptionManager::findSubscription(int memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        return &subscriptions[it->second];
    }
    return nullptr;
}

// Queue operations
void SubscriptionManager::addNotification(const Notification& notif) {
    notificationQueue.push(notif);
}

void SubscriptionManager::processNotifications() {
    cout << "\nProcessing " << notificationQueue.size() << " notifications...\n";
    while (!notificationQueue.empty()) {
        Notification notif = notificationQueue.front();
        notificationQueue.pop();
        
        cout << "\n=== Notification ===\n";
        cout << "Member ID: " << notif.getMemberId() << endl;
        cout << "Message: " << notif.getMessage() << endl;
        cout << "Expiry Date: " << formatTime(notif.getExpiryDate()) << endl;
    }
}

// Check for expiring subscriptions
void SubscriptionManager::checkRenewalReminders() {
    time_t now = time(nullptr);
    for (const auto& sub : subscriptions) {
        time_t expiry = sub.getExpiryDate();
        
        // Check if subscription expires in 7 days
        if (expiry - now <= 7 * 24 * 60 * 60 && !sub.isExpired()) {
            double discount = (expiry - now <= 14 * 24 * 60 * 60) ? 0.1 : 0.0;
            string message = "Your subscription expires in " + 
                           to_string((expiry - now) / (24 * 60 * 60)) + 
                           " days. Early renewal discount: " + 
                           to_string(discount * 100) + "%";
            
            notificationQueue.push(Notification(
                sub.getMemberId(), message, expiry
            ));
        }
    }
    processNotifications();
}

// Print all subscriptions
void SubscriptionManager::printAllSubscriptions() {
    cout << "\n=== All Subscriptions ===\n";
    for (const auto& sub : subscriptions) {
        cout << "Member ID: " << sub.getMemberId() << endl;
        cout << "Plan: " << sub.getPlanName() << endl;
        cout << "Duration: " << sub.getDuration() << endl;
        cout << "Price: $" << fixed << setprecision(2) << sub.getPrice() << endl;
        cout << "Start Date: " << formatTime(sub.getStartDate()) << endl;
        cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << endl;
        cout << "-------------------\n";
    }
}

void SubscriptionManager::saveToFile() {
    ofstream file(subscriptionsFile);
    if (file.is_open()) {
        // Save number of subscriptions
        file << subscriptions.size() << "\n";
        
        // Save each subscription
        for (const auto& sub : subscriptions) {
            file << sub.getMemberId() << "\n";
            file << sub.getPlanName() << "\n";
            file << sub.getDuration() << "\n";
            file << sub.getStartDate() << "\n";
            file << sub.getExpiryDate() << "\n";
            file << sub.getPrice() << "\n";
            file << sub.getIsActive() << "\n";

            // Save history
            SubscriptionHistoryNode* current = sub.getHistoryHead();
            int historyCount = 0;
            while (current) {
                historyCount++;
                current = current->next;
            }
            file << historyCount << "\n";

            current = sub.getHistoryHead();
            while (current) {
                file << current->planName << "\n";
                file << current->duration << "\n";
                file << current->startDate << "\n";
                file << current->expiryDate << "\n";
                file << current->price << "\n";
                current = current->next;
            }
        }
        file.close();
        cout << "Subscriptions saved to file successfully!" << endl;
    } else {
        cout << "Error: Could not open file for writing." << endl;
    }
}

void SubscriptionManager::loadFromFile() {
    ifstream file(subscriptionsFile);
    if (file.is_open()) {
        subscriptions.clear();
        
        // Read number of subscriptions
        int count;
        file >> count;
        file.ignore();  // Skip newline
        
        // Read each subscription
        for (int i = 0; i < count; i++) {
            int memberId;
            string planName, duration;
            time_t startDate, expiryDate;
            double price;
            bool isActive;

            file >> memberId;
            file.ignore();
            getline(file, planName);
            getline(file, duration);
            file >> startDate >> expiryDate >> price >> isActive;
            file.ignore();

            // Create subscription
            Subscription sub(memberId, planName, duration, price);

            // Load history
            int historyCount;
            file >> historyCount;
            file.ignore();

            for (int j = 0; j < historyCount; j++) {
                string plan, dur;
                time_t start, expiry;
                double p;

                getline(file, plan);
                getline(file, dur);
                file >> start >> expiry >> p;
                file.ignore();

                sub.addToHistory(plan, dur, start, expiry, p);
            }

            subscriptions.push_back(sub);
        }
        
        updateMemberIndex();
        file.close();
        cout << "Subscriptions loaded from file successfully!" << endl;
    } else {
        cout << "No existing subscription file found. Starting with empty list." << endl;
    }
}

void SubscriptionManager::printSubscriptionHistory(int memberId) {
    auto it = memberIndexMap.find(memberId);
    if (it != memberIndexMap.end()) {
        const Subscription& sub = subscriptions[it->second];
        
        cout << "\n=== Subscription History for Member " << memberId << " ===\n";
        cout << "Current Plan: " << sub.getPlanName() << endl;
        cout << "Current Duration: " << sub.getDuration() << endl;
        cout << "Current Price: $" << fixed << setprecision(2) << sub.getPrice() << endl;
        cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << endl;
        
        cout << "\nHistory:\n";
        SubscriptionHistoryNode* current = sub.getHistoryHead();
        while (current) {
            cout << "Plan: " << current->planName << endl;
            cout << "Duration: " << current->duration << endl;
            cout << "Price: $" << fixed << setprecision(2) << current->price << endl;
            cout << "Start Date: " << formatTime(current->startDate) << endl;
            cout << "Expiry Date: " << formatTime(current->expiryDate) << endl;
            cout << "-------------------\n";
            current = current->next;
        }
    } else {
        cout << "Member not found." << endl;
    }
}

vector<Subscription> SubscriptionManager::getExpiringSubscriptions(int days) {
    vector<Subscription> expiring;
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
    vector<Subscription> expiring = getExpiringSubscriptions(days);
    
    cout << "\n=== Subscriptions Expiring in " << days << " Days ===\n";
    for (const auto& sub : expiring) {
        cout << "Member ID: " << sub.getMemberId() << endl;
        cout << "Plan: " << sub.getPlanName() << endl;
        cout << "Expiry Date: " << formatTime(sub.getExpiryDate()) << endl;
        cout << "-------------------\n";
    }
}

void SubscriptionManager::cancelSubscription(int memberId) {
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
        cout << "Subscription cancelled successfully!" << endl;
    } else {
        cout << "Member not found." << endl;
    }
}

// Example usage
int main() {
    SubscriptionManager manager;
    
    // Load existing subscriptions from file
    manager.loadFromFile();

    // Add some subscriptions (Vector operation)
    Subscription sub1(1, "Basic Plan", "1_month", 29.99);
    Subscription sub2(2, "Pro Plan", "3_months", 84.99);
    Subscription sub3(3, "Elite Plan", "6_months", 161.99);

    manager.addSubscription(sub1);
    manager.addSubscription(sub2);
    manager.addSubscription(sub3);

    // Print all subscriptions
    manager.printAllSubscriptions();

    // Find a subscription (Hash table operation)
    Subscription* found = manager.findSubscription(2);
    if (found) {
        cout << "\nFound subscription for member " << found->getMemberId() << endl;
        cout << "Plan: " << found->getPlanName() << endl;
    }

    // Check for expiring subscriptions and process notifications (Queue operation)
    manager.checkRenewalReminders();

    // Remove a subscription
    manager.removeSubscription(1);
    manager.printAllSubscriptions();

    // Save all changes to file
    manager.saveToFile();

    return 0;
} 