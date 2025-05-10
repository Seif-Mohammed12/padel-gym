#pragma once
#include <string>
#include <map>
#include <vector>
#include "include/json.hpp"

using json = nlohmann::json;

struct Workout {
    std::string id;
    std::string className;
    std::string date;
    std::string instructor;

    json toJson() const {
        return {
                {"id", id},
                {"className", className},
                {"date", date},
                {"instructor", instructor}
        };
    }

    static Workout fromJson(const json& j) {
        Workout w;
        w.id = j.at("id").get<std::string>();
        w.className = j.at("className").get<std::string>();
        w.date = j.at("date").get<std::string>();
        w.instructor = j.at("instructor").get<std::string>();
        return w;
    }
};

class WorkoutHistory {
private:
    std::map<std::string, std::vector<Workout>> memberWorkouts; // Changed key to string
    json loadMembers();
    void saveMembers(const json& members);

public:
    void addWorkout(const std::string& memberId, const std::string& className, const std::string& date, const std::string& instructor); // Updated to string
    json getAllWorkouts(const std::string& memberId); // Updated to string
    void clearHistory(const std::string& memberId); // Updated to string
    void loadCache();
};