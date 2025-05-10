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
    std::vector<std::string> participants;
    std::queue<std::string> waitlist;
    std::string imagePath;

public:

    GymClass(const std::string& name, const std::string& instructor, const std::string& time, int capacity, const std::string& imagePath);

    GymClass();

    bool bookClass(const std::string& memberId);

    bool removeMember(const std::string& memberId);

    std::vector<std::string> queueToVector(const std::queue<std::string>& q) const;

    json toJson() const;

    static GymClass fromJson(const json& j);

    std::string getName() const;
    int getCapacity() const;
    int getCurrentParticipants() const;
    int getWaitlistSize() const;
};

#endif