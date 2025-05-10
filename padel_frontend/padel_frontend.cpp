#include <iostream>
#include <map>
#include <vector>
#include <string>
#include "padel_frontend.h"
using namespace std;






// VIP Class Implementation
VIP::VIP(string n) : name(n), bookingCount(0), isVIP(false) {}

void VIP::incrementBooking() {
    bookingCount++;
    if (bookingCount >= 20) {
        isVIP = true;
        cout << "🎉 Congratulations " << name << "! You are now a VIP member!\n";
    }
}

bool VIP::checkVIPStatus() const {
    return isVIP;
}

// Court Class Implementation
Court::Court(string n, string loc, vector<string> times) : name(n), location(loc), availableTimes(times) {}

void Court::showCourtInfo() const {
    cout << "Court: " << name << " (" << location << ")\nAvailable Times: ";
    for (const auto& time : availableTimes) {
        cout << time << " ";
    }
    cout << "\n-------------------------\n";
}

// Court Selection Class Implementation
CourtSelection::CourtSelection() {
    courts = {
        Court("Padel Ace", "Zamalek, Cairo", {"10:00 AM", "12:00 PM", "02:00 PM"}),
        Court("Padel Hood", "Sheikh Zayed, Giza", {"11:00 AM", "01:00 PM", "03:00 PM"}),
        Court("SR Padel", "Maadi, Cairo", {"09:00 AM", "10:30 AM", "12:00 PM", "01:30 PM", "03:00 PM", "04:30 PM", "06:00 PM", "07:30 PM"}),
        Court("Go! Padel", "Downtown Cairo", {"08:00 AM", "09:30 AM", "11:00 AM", "12:30 PM", "02:00 PM", "03:30 PM", "05:00 PM", "06:30 PM", "08:00 PM"})
    };
}

void CourtSelection::showAvailableCourts() const {
    cout << "Available Padel Courts:\n";
    for (const auto& court : courts) {
        court.showCourtInfo();
    }
}

Court* CourtSelection::selectCourt(string name) {
    for (auto& court : courts) {
        if (court.name == name) return &court;
    }
    return nullptr;
}

// Booking System Class Implementation
void BookingSystem::bookCourt(VIP* member, Court* court, string time) {
    if (!court || !member) {
        cout << "Invalid court or member selection.\n";
        return;
    }

    for (const auto& availableTime : court->availableTimes) {
        if (availableTime == time) {
            cout << "✅ " << member->name << " booked " << court->name << " at " << time << " (" << court->location << ")!\n";
            member->incrementBooking();
            return;
        }
    }
    cout << "❌ Selected time is not available. Please choose another time.\n";
}

void BookingSystem::cancelBooking(VIP* member, Court* court, string time) {
    if (!court || !member) {
        cout << "Invalid court or member selection.\n";
        return;
    }

    cout << "❌ " << member->name << " canceled booking for " << time << " at " << court->name << " (" << court->location << ").\n";
}