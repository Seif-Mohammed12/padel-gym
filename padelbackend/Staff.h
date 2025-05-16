#ifndef STAFF_H
#define STAFF_H

#include <string>
#include <iostream>
#include "include/json.hpp"

using json = nlohmann::json;

//=============================================================================
// Staff Class Definition
// Manages staff member data and permissions in the system
//=============================================================================

class Staff
{
public:
    //=============================================================================
    // Types and Constants
    // Defines the available roles and their permissions
    //=============================================================================

    enum class Role
    {
        Manager
    };

    //=============================================================================
    // Member Variables
    // Core data fields for staff information
    //=============================================================================

    std::string username;
    std::string password;
    Role role;

    //=============================================================================
    // Constructors
    // Initialize staff members with default values
    //=============================================================================

    Staff() : username(""), password(""), role(Role::Manager) {}

    //=============================================================================
    // Staff Management Methods
    // Handle creation and display of staff members
    //=============================================================================

    void addStaff(const std::string &uname, const std::string &pwd, const std::string &r)
    {
        username = uname;
        password = pwd;
        if (r != "manager")
        {
            throw std::invalid_argument("Invalid role: " + r + ". Only 'manager' is allowed.");
        }
        role = Role::Manager;
    }

    void showStaff() const
    {
        std::cout << "Username: " << username << ", Role: manager" << std::endl;
    }

    //=============================================================================
    // Permission Methods
    // Handle access control and role-based permissions
    //=============================================================================

    bool canManageStaff() const
    {
        return true;
    }

    //=============================================================================
    // Serialization Methods
    // Convert staff data to/from JSON format
    //=============================================================================

    json toJson() const
    {
        return {
            {"username", username},
            {"password", password},
            {"role", "manager"}};
    }

    static Staff fromJson(const json &j)
    {
        Staff s;
        s.username = j.at("username").get<std::string>();
        s.password = j.at("password").get<std::string>();
        std::string roleStr = j.at("role").get<std::string>();
        if (roleStr != "manager")
        {
            throw std::invalid_argument("Invalid role in JSON: " + roleStr +
                                        ". Only 'manager' is allowed.");
        }
        s.role = Role::Manager;
        return s;
    }

private:
    //=============================================================================
    // Private Helper Methods
    // Internal utility functions
    //=============================================================================

    static std::string roleToString(Role r)
    {
        return "manager";
    }
};

#endif // STAFF_H