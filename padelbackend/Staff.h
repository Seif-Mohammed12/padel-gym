// Staff.h
#ifndef STAFF_H
#define STAFF_H

#include <string>
#include <iostream>
#include "include/json.hpp"

using json = nlohmann::json;

class Staff {
public:
    // Define roles as an enum for type safety
    enum class Role {
        Manager,
        Trainer,
        Receptionist,
        Unknown // For invalid roles
    };

    std::string name;
    Role role;

    Staff() : name(""), role(Role::Unknown) {}

    // Add a staff member with role validation
    void addStaff(const std::string& n, const std::string& r) {
        name = n;
        role = stringToRole(r);
        if (role == Role::Unknown) {
            throw std::invalid_argument("Invalid role: " + r);
        }
    }

    // Display staff information
    void showStaff() const {
        std::cout << "Name: " << name << ", Role: " << roleToString(role) << std::endl;
    }

    // Role-specific methods to demonstrate logic
    bool canManageStaff() const {
        return role == Role::Manager;
    }

    bool canTrainMembers() const {
        return role == Role::Trainer;
    }

    bool canHandleBookings() const {
        return role == Role::Receptionist || role == Role::Manager;
    }

    // Serialization to JSON
    json toJson() const {
        return {
                {"name", name},
                {"role", roleToString(role)}
        };
    }

    // Deserialization from JSON
    static Staff fromJson(const json& j) {
        Staff s;
        s.name = j.at("name").get<std::string>();
        s.role = stringToRole(j.at("role").get<std::string>());
        return s;
    }

private:
    // Convert string to Role enum
    static Role stringToRole(const std::string& roleStr) {
        if (roleStr == "manager") return Role::Manager;
        if (roleStr == "trainer") return Role::Trainer;
        if (roleStr == "receptionist") return Role::Receptionist;
        return Role::Unknown;
    }

    // Convert Role enum to string for display or serialization
    static std::string roleToString(Role r) {
        switch (r) {
            case Role::Manager: return "manager";
            case Role::Trainer: return "trainer";
            case Role::Receptionist: return "receptionist";
            default: return "unknown";
        }
    }
};

#endif // STAFF_H