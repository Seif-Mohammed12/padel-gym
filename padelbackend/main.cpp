#include "SubscriptionManager.h"
#include <iostream>

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
        std::cout << "\nFound subscription for member " << found->getMemberId() << std::endl;
        std::cout << "Plan: " << found->getPlanName() << std::endl;
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