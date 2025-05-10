#include "GymClass.h"
#include <algorithm>

GymClass::GymClass(const std::string& name, const std::string& instructor, const std::string& time, int capacity, const std::string& imagePath)
        : name(name), instructor(instructor), time(time), capacity(capacity), imagePath(imagePath) {}

GymClass::GymClass() : capacity(0) {}

bool GymClass::bookClass(const std::string& memberId) {
    if (participants.size() < static_cast<size_t>(capacity)) {
        participants.push_back(memberId);
        return true;
    } else {
        waitlist.push(memberId);
        return false;
    }
}

bool GymClass::removeMember(const std::string& memberId) {
    // Check if member is in participants
    auto it = std::find(participants.begin(), participants.end(), memberId);
    if (it != participants.end()) {
        participants.erase(it);
        if (!waitlist.empty()) {
            std::string nextMember = waitlist.front();
            waitlist.pop();
            participants.push_back(nextMember);
        }
        return true;
    }

    std::queue<std::string> temp;
    bool found = false;
    while (!waitlist.empty()) {
        std::string id = waitlist.front();
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

std::vector<std::string> GymClass::queueToVector(const std::queue<std::string>& q) const {
    std::vector<std::string> vec;
    std::queue<std::string> temp = q;
    while (!temp.empty()) {
        vec.push_back(temp.front());
        temp.pop();
    }
    return vec;
}

json GymClass::toJson() const {
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

GymClass GymClass::fromJson(const json& j) {
    GymClass gymClass;
    gymClass.name = j.at("name").get<std::string>();
    gymClass.instructor = j.at("instructor").get<std::string>();
    gymClass.time = j.at("time").get<std::string>();
    gymClass.capacity = j.at("capacity").get<int>();
    gymClass.participants = j.value("participants", std::vector<std::string>{});
    std::vector<std::string> waitlistVec = j.value("waitlist", std::vector<std::string>{});
    for (const std::string& id : waitlistVec) {
        gymClass.waitlist.push(id);
    }
    gymClass.imagePath = j.value("imagePath", "");
    return gymClass;
}

std::string GymClass::getName() const { return name; }
int GymClass::getCapacity() const { return capacity; }
int GymClass::getCurrentParticipants() const { return static_cast<int>(participants.size()); }
int GymClass::getWaitlistSize() const { return static_cast<int>(waitlist.size()); }