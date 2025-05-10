#ifndef MEMBER_H
#define MEMBER_H

#include <string>
#include <vector>
#include <iostream>
#include "include/json.hpp"
#include "WorkoutHistory.h" // For Workout struct

using json = nlohmann::json;

class Member {
public:
    std::string id;
    std::string firstName;
    std::string lastName;
    std::string email;
    std::string phoneNumber;
    std::string username;
    std::string dob;
    json subscription;
    bool isActive;
    std::vector<Workout> workouts;

    Member() : isActive(true) {}

    void addMember(const std::string& i, const std::string& fname, const std::string& lname,
                   const std::string& email, const std::string& phone, const std::string& username,
                   const std::string& d, const json& s) {
        id = i; // Store as string
        firstName = fname;
        lastName = lname;
        this->email = email;
        phoneNumber = phone;
        this->username = username;
        dob = d;
        subscription = s;
        isActive = true;
    }

    void showMember() {
        std::cout << "ID: " << id
                  << ", First Name: " << firstName
                  << ", Last Name: " << lastName
                  << ", Email: " << email
                  << ", Phone: " << phoneNumber
                  << ", Username: " << username
                  << ", DOB: " << dob
                  << ", Subscription: " << subscription.dump()
                  << ", Active: " << (isActive ? "Yes" : "No")
                  << ", Workouts: " << workouts.size() << std::endl;
    }

    json toJson() const {
        json j = {
                {"id", id},
                {"firstName", firstName},
                {"lastName", lastName},
                {"email", email},
                {"phoneNumber", phoneNumber},
                {"username", username},
                {"dob", dob},
                {"subscription", subscription},
                {"isActive", isActive}
        };
        if (!workouts.empty()) {
            json workoutsJson = json::array();
            for (const auto& workout : workouts) {
                workoutsJson.push_back(workout.toJson());
            }
            j["workouts"] = workoutsJson;
        }
        return j;
    }

    static Member fromJson(const json& j) {
        Member m;
        m.id = j.at("id").get<std::string>();
        m.firstName = j.at("firstName").get<std::string>();
        m.lastName = j.at("lastName").get<std::string>();
        m.email = j.value("email", "");
        m.phoneNumber = j.value("phoneNumber", "");
        m.username = j.value("username", "");
        m.dob = j.value("dob", "");
        m.subscription = j.at("subscription");
        m.isActive = j.value("isActive", true);
        if (j.contains("workouts") && j["workouts"].is_array()) {
            for (const auto& workoutJson : j["workouts"]) {
                m.workouts.push_back(Workout::fromJson(workoutJson));
            }
        }
        return m;
    }
};

#endif // MEMBER_H