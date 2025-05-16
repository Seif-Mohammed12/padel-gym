#ifndef REGISTRATION_H
#define REGISTRATION_H

#include <string>
#include <unordered_map>
#include <vector>
#include <fstream>
#include "include/json.hpp"
#include <regex>
#include <random>
#include <chrono>
#include <mutex>

using namespace std;
using json = nlohmann::json;

//=============================================================================
// Registration Class
// Manages user registration and account management functionality
//=============================================================================

class Registration
{
private:
    //=============================================================================
    // User Class Definition
    // Internal class for managing individual user data
    //=============================================================================

    class User
    {
    private:
        // Core user information
        string memberId;
        string firstName;
        string lastName;
        string email;
        string phoneNumber;
        string username;
        string password;
        string role;

    public:
        // Constructors
        User();
        User(const string &id, const string &fn, const string &ln,
             const string &em, const string &pn,
             const string &un, const string &pw, const string &r);

        // Data access methods
        json toJson() const;
        string getMemberId() const;
        string getUsername() const;
        string getPassword() const;
        string getRole() const;
    };

    //=============================================================================
    // Member Variables
    // Core data storage and synchronization
    //=============================================================================

    unordered_map<string, User> users;
    const string dataFile = "data.json";
    mutable std::mutex usersMutex;

    //=============================================================================
    // Private Helper Methods
    // Internal utility functions for user management
    //=============================================================================

    // ID Management
    string generateUniqueMemberId() const;
    bool memberIdExists(const string &id) const;

    // Validation Methods
    bool isValidName(const string &name) const;
    bool isValidPhoneNumber(const string &phone) const;
    bool isValidEmail(const string &email) const;
    bool isValidUsername(const string &un) const;
    bool isValidPassword(const string &pw) const;
    bool isValidRole(const string &role) const;

    // Data Persistence
    void saveToJsonFile() const;
    void loadFromJsonFile();

    // User Management Helpers
    bool isUsernameTaken(const string &un) const;

public:
    //=============================================================================
    // Public Interface
    // External API for user registration
    //=============================================================================

    // Constructor
    Registration();

    // User Registration
    bool registerUser(const string &firstName, const string &lastName,
                      const string &email, const string &phoneNumber,
                      const string &username, const string &password,
                      const string &role, string &memberId, string &errorMsg);
};

#endif // REGISTRATION_H