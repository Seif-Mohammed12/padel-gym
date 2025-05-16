#include "padelbooking.h"
#include "FileManager.h"
#include <iostream>
#include <algorithm>

using namespace std;
using json = nlohmann::json;

//=============================================================================
// VIP Class Implementation
// Manages VIP status and booking count tracking
//=============================================================================

VIP::VIP(string n) : name(n), bookingCount(0), isVIP(false) {}

void VIP::incrementBooking()
{
    bookingCount++;
    if (bookingCount >= 20)
    {
        isVIP = true;
    }
}

bool VIP::checkVIPStatus() const
{
    return isVIP;
}

//=============================================================================
// Court Class Implementation
// Handles individual court booking operations
//=============================================================================

Court::Court(string n, string loc, vector<string> times)
    : name(n), location(loc), availableTimes(times) {}

bool Court::bookTimeSlot(string time, string memberId)
{
    auto it = find(availableTimes.begin(), availableTimes.end(), time);
    if (it == availableTimes.end())
    {
        cout << "Time slot not available: " << time << endl;
        return false;
    }
    if (bookedTimes.find(time) != bookedTimes.end())
    {
        cout << "Time slot already booked: " << time << endl;
        return false;
    }
    bookedTimes[time] = memberId;
    availableTimes.erase(it);
    cout << "Booked time slot: " << time << " for member: " << memberId << endl;
    return true;
}

bool Court::cancelTimeSlot(string time, string memberId)
{
    auto it = bookedTimes.find(time);
    if (it == bookedTimes.end() || it->second != memberId)
    {
        cout << "Cannot cancel: Time slot " << time << " not booked by member " << memberId << endl;
        return false;
    }
    bookedTimes.erase(it);
    availableTimes.push_back(time);
    sort(availableTimes.begin(), availableTimes.end());
    cout << "Cancelled time slot: " << time << endl;
    return true;
}

//=============================================================================
// Court Selection Implementation
// Manages court loading and selection functionality
//=============================================================================

CourtSelection::CourtSelection()
{
    json data = FileManager::load("padel-classes.json"); // Use correct path
    courts.clear();                                      // Ensure courts is empty
    if (data.is_array())
    {
        for (const auto &courtJson : data)
        {
            string name = courtJson.value("name", "Unknown");
            string location = courtJson.value("location", "Unknown");
            vector<string> times;
            if (courtJson.contains("availableTimes") && courtJson["availableTimes"].is_array())
            {
                for (const auto &time : courtJson["availableTimes"])
                {
                    times.push_back(time.get<string>());
                }
            }
            courts.emplace_back(name, location, times);
            if (courtJson.contains("bookedTimes") && courtJson["bookedTimes"].is_object())
            {
                for (auto it = courtJson["bookedTimes"].begin(); it != courtJson["bookedTimes"].end(); ++it)
                {
                    courts.back().bookedTimes[it.key()] = it.value().get<string>();
                }
            }
            cout << "Loaded court: " << name << " with " << times.size() << " available times" << endl;
        }
        cout << "Total courts loaded: " << courts.size() << endl;
    }
    else
    {
        cout << "Failed to load ../files/padel-classes.json or file is not an array." << endl;
    }
}

Court *CourtSelection::selectCourt(string name)
{
    if (courts.empty())
    {
        cout << "No courts loaded, attempting to reload..." << endl;
        CourtSelection temp; // Reload courts
        courts = temp.courts;
    }
    cout << "Searching for court: " << name << endl;
    cout << "Current courts in memory (" << courts.size() << "):" << endl;
    for (const auto &court : courts)
    {
        cout << "- " << court.name << " (" << court.location << ")" << endl;
    }
    for (auto &court : courts)
    {
        if (court.name == name)
        {
            cout << "Found court: " << court.name << endl;
            return &court;
        }
    }
    cout << "Court not found: " << name << endl;
    return nullptr;
}

//=============================================================================
// Booking System Implementation
// Core booking management functionality
//=============================================================================

void BookingSystem::bookCourt(string memberId, Court *court, string time)
{
    if (!court || memberId.empty())
    {
        cout << "Invalid court or memberId for booking" << endl;
        return;
    }
    if (!court->bookTimeSlot(time, memberId))
    {
        cout << "Failed to book time slot: " << time << endl;
        return;
    }
}

void BookingSystem::cancelBooking(string memberId, Court *court, string time)
{
    if (!court || memberId.empty())
    {
        cout << "Invalid court or memberId for cancellation" << endl;
        return;
    }
    if (!court->cancelTimeSlot(time, memberId))
    {
        cout << "Failed to cancel time slot: " << time << endl;
        return;
    }
}

//=============================================================================
// VIP Management
// Handles VIP status updates and booking counts
//=============================================================================

void BookingSystem::incrementBookingCount(string memberId, string memberName)
{
    json users = FileManager::load("data.json");
    if (!users.is_array())
        users = json::array();
    bool userFound = false;
    for (auto &user : users)
    {
        if (user["memberId"].get<string>() == memberId)
        {
            userFound = true;
            int currentCount = user.value("bookingCount", 0);
            currentCount++;
            user["bookingCount"] = currentCount;
            VIP tempVIP(memberName);
            tempVIP.bookingCount = currentCount;
            tempVIP.incrementBooking();
            user["isVIP"] = tempVIP.checkVIPStatus();
            cout << "Incremented booking count for member: " << memberId << " to " << currentCount << endl;
            break;
        }
    }
    if (!userFound)
    {
        cout << "User not found for incrementing booking count: " << memberId << endl;
        return;
    }
    try
    {
        FileManager::save(users, "data.json");
    }
    catch (const exception &e)
    {
        cout << "Failed to save data.json: " << e.what() << endl;
    }
}

//=============================================================================
// File Operations
// JSON data persistence and loading
//=============================================================================

json loadCourtData()
{
    return FileManager::load("padel-classes.json");
}

void saveBookingData(const json &data)
{
    FileManager::save(data, "data.json");
}