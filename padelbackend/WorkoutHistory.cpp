#include "WorkoutHistory.h"
#include "FileManager.h"
#include <stdexcept>
#include <chrono>
#include <iostream>

json WorkoutHistory::loadMembers() {
    return FileManager::load("members.json");
}

void WorkoutHistory::saveMembers(const json& members) {
    FileManager::save(members, "members.json");
}

void WorkoutHistory::loadCache() {
    json members = loadMembers();
    if (!members.is_array()) return;

    memberWorkouts.clear();
    for (const auto& memberJson : members) {
        try {
            std::string memberId = memberJson.at("id").get<std::string>(); // Changed to string
            std::vector<Workout> workouts;
            if (memberJson.contains("workouts") && memberJson["workouts"].is_array()) {
                for (const auto& workoutJson : memberJson["workouts"]) {
                    workouts.push_back(Workout::fromJson(workoutJson));
                }
            }
            memberWorkouts[memberId] = workouts;
        } catch (const json::exception& e) {
            std::cerr << "Error parsing member JSON: " << e.what() << std::endl;
            // Skip invalid entries
        }
    }
}

void WorkoutHistory::addWorkout(const std::string& memberId, const std::string& className, const std::string& date, const std::string& instructor) {
    if (className.empty()) throw std::invalid_argument("Class name cannot be empty");
    if (date.empty()) throw std::invalid_argument("Date cannot be empty");
    if (instructor.empty()) throw std::invalid_argument("Instructor cannot be empty");

    // Check if member exists and is active
    json members = loadMembers();
    bool found = false;
    bool isActive = true;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) { // Changed to string
            found = true;
            isActive = member.value("isActive", true);
            break;
        }
    }
    if (!found) throw std::runtime_error("Member ID not found");
    if (!isActive) throw std::runtime_error("Cannot add workout for inactive member");

    Workout workout;
    // Generate a unique ID using timestamp
    auto now = std::chrono::system_clock::now();
    auto timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()).count();
    workout.id = std::to_string(timestamp);
    workout.className = className;
    workout.date = date;
    workout.instructor = instructor;

    memberWorkouts[memberId].push_back(workout);

    // Update members.json
    for (auto& member : members) {
        if (member["id"].get<std::string>() == memberId) { // Changed to string
            if (!member.contains("workouts")) member["workouts"] = json::array();
            member["workouts"].push_back(workout.toJson());
            break;
        }
    }
    saveMembers(members);
}

json WorkoutHistory::getAllWorkouts(const std::string& memberId) {
    json workoutsJson = json::array();
    auto it = memberWorkouts.find(memberId);
    if (it != memberWorkouts.end()) {
        for (const auto& workout : it->second) {
            workoutsJson.push_back(workout.toJson());
        }
    }
    return workoutsJson;
}

void WorkoutHistory::clearHistory(const std::string& memberId) {
    memberWorkouts.erase(memberId);

    json members = loadMembers();
    for (auto& member : members) {
        if (member["id"].get<std::string>() == memberId) { // Changed to string
            member["workouts"] = json::array();
            break;
        }
    }
    saveMembers(members);
}