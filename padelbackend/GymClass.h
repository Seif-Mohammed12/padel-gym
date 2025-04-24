#ifndef GYM_CLASS_H
#define GYM_CLASS_H

#include <string>
#include <vector>
#include <queue>
#include "include/json.hpp"

using json = nlohmann::json;

class GymClass {
private:
    std::string name;
    std::string instructor;
    std::string time;
    int capacity;
    std::vector<int> participants;
    std::queue<int> waitlist;
    std::string imagePath; // New field for image path

public:
    // Constructor with imagePath
    GymClass(const std::string& name, const std::string& instructor, const std::string& time, int capacity, const std::string& imagePath)
            : name(name), instructor(instructor), time(time), capacity(capacity), imagePath(imagePath) {}

    // Default constructor (for JSON deserialization)
    GymClass() : capacity(0) {}

    // Book a member into the class or waitlist
    bool bookClass(int memberId) {

        if (participants.size() < static_cast<size_t>(capacity)) {
            participants.push_back(memberId);
            return true; // Successfully booked
        } else {
            waitlist.push(memberId);
            return false; // Added to waitlist
        }
    }

    // Remove a member from the class or waitlist
    bool removeMember(int memberId) {
        // Check if member is in participants
        auto it = std::find(participants.begin(), participants.end(), memberId);
        if (it != participants.end()) {
            participants.erase(it);
            // If there's someone in the waitlist, add them to the class
            if (!waitlist.empty()) {
                int nextMember = waitlist.front();
                waitlist.pop();
                participants.push_back(nextMember);
            }
            return true;
        }

        // Check if member is in waitlist
        std::queue<int> temp;
        bool found = false;
        while (!waitlist.empty()) {
            int id = waitlist.front();
            waitlist.pop();
            if (id == memberId) {
                found = true;
            } else {
                temp.push(id);
            }
        }
        waitlist = temp;
        return found;
    }

    // Convert waitlist queue to vector for JSON serialization
    std::vector<int> queueToVector(const std::queue<int>& q) const {
        std::vector<int> vec;
        std::queue<int> temp = q;
        while (!temp.empty()) {
            vec.push_back(temp.front());
            temp.pop();
        }
        return vec;
    }

    // Convert to JSON
    json toJson() const {
        json j;
        j["name"] = name;
        j["instructor"] = instructor;
        j["time"] = time;
        j["capacity"] = capacity;
        j["currentParticipants"] = participants.size();
        j["participants"] = participants;
        j["waitlist"] = queueToVector(waitlist);
        j["waitlistSize"] = waitlist.size();
        j["imagePath"] = imagePath; // Include imagePath in JSON
        return j;
    }

    // Create from JSON
    static GymClass fromJson(const json& j) {
        GymClass gymClass;
        gymClass.name = j.at("name").get<std::string>();
        gymClass.instructor = j.at("instructor").get<std::string>();
        gymClass.time = j.at("time").get<std::string>();
        gymClass.capacity = j.at("capacity").get<int>();
        gymClass.participants = j.at("participants").get<std::vector<int>>();
        std::vector<int> waitlistVec = j.at("waitlist").get<std::vector<int>>();
        for (int id : waitlistVec) {
            gymClass.waitlist.push(id);
        }
        gymClass.imagePath = j.value("imagePath", ""); // Get imagePath, default to empty string if not present
        return gymClass;
    }

    // Getters (for use in server if needed)
    std::string getName() const { return name; }
    int getCapacity() const { return capacity; }
    int getCurrentParticipants() const { return static_cast<int>(participants.size()); }
    int getWaitlistSize() const { return static_cast<int>(waitlist.size()); }
};

#endif