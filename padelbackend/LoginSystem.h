#ifndef LOGIN_SYSTEM_H
#define LOGIN_SYSTEM_H

#include <string>
#include "include/json.hpp"

using json = nlohmann::json;
using namespace std;

class LoginSystem {
public:
    // Load user data from file
    json loadUserData() const;

    // Check for user existence
    bool userExists(const string& username) const;

    // Verify that the provided password matches the stored one
    bool passwordMatches(const string& username, const string& password) const;

    // Recover the password for a given username
    string recoverPassword(const string& username) const;

    // Authenticate the user credentials
    bool authenticate(const string& username, const string& password) const;

    // Handle forgot password: takes the username and returns the corresponding password (or empty string if not found)
    string handleForgotPassword(const string& username) const;
};

#endif
