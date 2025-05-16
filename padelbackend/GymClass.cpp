#include "GymClass.h"
#include <algorithm>
#include <iostream>

//=============================================================================
// Constructors
// Initialize gym class objects with provided or default values
//=============================================================================

GymClass::GymClass(const std::string &name, const std::string &instructor,
                   const std::string &time, int capacity, const std::string &imagePath)
    : name(name), instructor(instructor), time(time),
      capacity(capacity), imagePath(imagePath) {}

GymClass::GymClass() : capacity(0) {}

//=============================================================================
// Booking Management
// Handle class bookings and waitlist operations
//=============================================================================

bool GymClass::bookClass(const std::string &memberId, bool *movedFromWaitlist)
{
    if (movedFromWaitlist)
        *movedFromWaitlist = false;

    // Log initial state
    std::cout << "Attempting to book class: " << name
              << ", Member: " << memberId
              << ", Current Participants: " << participants.size()
              << ", Capacity: " << capacity
              << ", Waitlist Size: " << waitlist.size() << std::endl;

    if (std::find(participants.begin(), participants.end(), memberId) != participants.end())
    {
        return true;
    }

    // Check if member is on waitlist
    auto waitlistVec = queueToVector(waitlist);
    auto waitlistIt = std::find(waitlistVec.begin(), waitlistVec.end(), memberId);
    if (waitlistIt != waitlistVec.end())
    {
        // Move to participants if capacity allows
        if (participants.size() < static_cast<size_t>(capacity))
        {
            // Remove from waitlist
            std::queue<std::string> temp;
            while (!waitlist.empty())
            {
                std::string id = waitlist.front();
                waitlist.pop();
                if (id != memberId)
                {
                    temp.push(id);
                }
            }
            waitlist = temp;
            participants.push_back(memberId);
            if (movedFromWaitlist)
                *movedFromWaitlist = true;
            return true;
        }
        return false; // Still waitlisted
    }

    if (capacity <= 0)
    {
        waitlist.push(memberId);
        return false; // Invalid capacity
    }
    if (participants.size() >= static_cast<size_t>(capacity))
    {
        waitlist.push(memberId);
        return false; // Class full
    }
    participants.push_back(memberId);
    return true;
}

std::string GymClass::removeMember(const std::string &memberId)
{
    std::string movedMemberId;
    // Check if member is in participants
    auto it = std::find(participants.begin(), participants.end(), memberId);
    if (it != participants.end())
    {
        participants.erase(it);
        if (!waitlist.empty())
        {
            movedMemberId = waitlist.front();
            waitlist.pop();
            participants.push_back(movedMemberId);
            std::cout << "Moved member " << movedMemberId << " from waitlist to participants for class " << name << std::endl;
        }
        return movedMemberId;
    }

    // Check if member is in waitlist
    std::queue<std::string> temp;
    bool found = false;
    while (!waitlist.empty())
    {
        std::string id = waitlist.front();
        waitlist.pop();
        if (id == memberId)
        {
            found = true;
        }
        else
        {
            temp.push(id);
        }
    }
    waitlist = temp;
    return found ? "" : movedMemberId;
}

//=============================================================================
// Utility Functions
// Helper methods for internal operations
//=============================================================================

std::vector<std::string> GymClass::queueToVector(const std::queue<std::string> &q) const
{
    std::vector<std::string> vec;
    std::queue<std::string> temp = q;
    while (!temp.empty())
    {
        vec.push_back(temp.front());
        temp.pop();
    }
    return vec;
}

//=============================================================================
// Serialization
// JSON conversion for data persistence
//=============================================================================

json GymClass::toJson() const
{
    json j;
    j["name"] = name;
    j["instructor"] = instructor;
    j["time"] = time;
    j["capacity"] = capacity;
    j["currentParticipants"] = participants.size();
    j["participants"] = participants;
    j["waitlist"] = queueToVector(waitlist);
    j["waitlistSize"] = waitlist.size();
    j["imagePath"] = imagePath;
    return j;
}

GymClass GymClass::fromJson(const json &j)
{
    GymClass gymClass;
    gymClass.name = j.at("name").get<std::string>();
    gymClass.instructor = j.at("instructor").get<std::string>();
    gymClass.time = j.at("time").get<std::string>();
    gymClass.capacity = j.at("capacity").get<int>();
    gymClass.participants = j.value("participants", std::vector<std::string>{});
    std::vector<std::string> waitlistVec = j.value("waitlist", std::vector<std::string>{});
    for (const std::string &id : waitlistVec)
    {
        gymClass.waitlist.push(id);
    }
    gymClass.imagePath = j.value("imagePath", "");
    return gymClass;
}

//=============================================================================
// Getters
// Access methods for class properties
//=============================================================================

std::string GymClass::getName() const { return name; }
int GymClass::getCapacity() const { return capacity; }
int GymClass::getCurrentParticipants() const
{
    return static_cast<int>(participants.size());
}
int GymClass::getWaitlistSize() const
{
    return static_cast<int>(waitlist.size());
}