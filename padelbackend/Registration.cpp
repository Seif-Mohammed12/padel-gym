#include "Registration.h"
#include "FileManager.h"
#include <algorithm>
#include <regex>
#include <random>
#include <chrono>
#include <set>

// Default constructor for User
Registration::User::User()
        : memberId(""), firstName(""), lastName(""), email(""),
          phoneNumber(""), username(""), password(""), role("user") {}

// Parameterized constructor for User
Registration::User::User(const string& id, const string& fn, const string& ln,
                         const string& em, const string& pn,
                         const string& un, const string& pw, const string& r)
        : memberId(id), firstName(fn), lastName(ln), email(em),
          phoneNumber(pn), username(un), password(pw), role(r) {}

// Convert User to JSON
json Registration::User::toJson() const {
    return {
            {"memberId", memberId},
            {"firstName", firstName},
            {"lastName", lastName},
            {"email", email},
            {"phoneNumber", phoneNumber},
            {"username", username},
            {"password", password},
            {"role", role}
    };
}

// User getters
string Registration::User::getMemberId() const { return memberId; }
string Registration::User::getUsername() const { return username; }
string Registration::User::getPassword() const { return password; }
string Registration::User::getRole() const { return role; }

// Registration constructor: loads users from JSON file
Registration::Registration() {
    loadFromJsonFile();
}

// Generate a unique member ID (format: MEMxxxxxx)
string Registration::generateUniqueMemberId() const {
    static mt19937 rng(chrono::system_clock::now().time_since_epoch().count());
    uniform_int_distribution<int> dist(100000, 999999);

    string newId;
    do {
        newId = "MEM" + to_string(dist(rng));
    } while (memberIdExists(newId));

    return newId;
}

// Check if a member ID already exists
bool Registration::memberIdExists(const string& id) const {
    std::lock_guard<std::mutex> lock(usersMutex);
    for (const auto& [username, user] : users) {
        if (user.getMemberId() == id) {
            return true;
        }
    }
    return false;
}

// Validate name (letters and optional spaces)
bool Registration::isValidName(const string& name) const {
    static const regex pattern("^[a-zA-Z]+(?: [a-zA-Z]+)*$");
    return regex_match(name, pattern);
}

// Validate phone number (Egyptian format: +20 followed by 10 digits)
bool Registration::isValidPhoneNumber(const string& phone) const {
    static const regex pattern("^\\+20\\d{10}$");
    return regex_match(phone, pattern);
}

// Validate email format
bool Registration::isValidEmail(const string& email) const {
    static const regex pattern("^[\\w-\\.]+@([\\w-]+\\.)+[a-zA-Z]{2,4}$");
    return regex_match(email, pattern);
}

// Validate username (6+ alphanumeric characters or underscores)
bool Registration::isValidUsername(const string& un) const {
    static const regex pattern("^[a-zA-Z0-9_]{6,}$");
    return regex_match(un, pattern);
}

// Validate password (8+ characters with upper, lower, number, special)
bool Registration::isValidPassword(const string& pw) const {
    return pw.length() >= 8 &&
           regex_search(pw, regex("[A-Z]")) &&
           regex_search(pw, regex("[a-z]")) &&
           regex_search(pw, regex("[0-9]")) &&
           regex_search(pw, regex("[!@#$%^&*()_+]"));
}

// Validate role (must be user, admin, or moderator)
bool Registration::isValidRole(const string& role) const {
    vector<string> validRoles = {"user", "admin", "moderator"};
    return find(validRoles.begin(), validRoles.end(), role) != validRoles.end();
}

// Check if username is taken
bool Registration::isUsernameTaken(const string& un) const {
    std::lock_guard<std::mutex> lock(usersMutex);
    return users.find(un) != users.end();
}

// Save users to JSON file using FileManager
// Save users to JSON file using FileManager (append instead of overwrite)
void Registration::saveToJsonFile() const {
    std::lock_guard<std::mutex> lock(usersMutex);

    json j = FileManager::load("data.json");

    if (!j.is_array()) {
        j = json::array();
    }
    // Create a set of existing usernames in the JSON to avoid duplicates
    std::set<std::string> existingUsernames;
    for (const auto& userJson : j) {
        if (userJson.contains("username")) {
            existingUsernames.insert(userJson["username"].get<std::string>());
        }
    }
    // Add or update users from the in-memory users map
    for (const auto& [username, user] : users) {
        if (existingUsernames.find(username) == existingUsernames.end()) {
            j.push_back(user.toJson());
        } else {
            // Optionally update existing user data (if you want to allow updates)
            for (auto& userJson : j) {
                if (userJson["username"] == username) {
                    userJson = user.toJson(); // Update existing user
                    break;
                }
            }
        }
    }

    // Save the updated JSON back to the file
    FileManager::save(j, "data.json");
}

// Load users from JSON file using FileManager
void Registration::loadFromJsonFile() {
    std::lock_guard<std::mutex> lock(usersMutex);
    json j = FileManager::load("data.json");
    users.clear();
    for (const auto& userJson : j) {
        if (userJson.contains("username")) {
            string un = userJson["username"];
            users[un] = User(
                    userJson["memberId"],
                    userJson["firstName"],
                    userJson["lastName"],
                    userJson["email"],
                    userJson["phoneNumber"],
                    un,
                    userJson["password"],
                    userJson.value("role", "user")
            );
        }
    }
}

// Programmatic user registration
bool Registration::registerUser(const string& firstName, const string& lastName,
                                const string& email, const string& phoneNumber,
                                const string& username, const string& password,
                                const string& role, string& memberId, string& errorMsg) {
    // Validate inputs
    if (!isValidName(firstName)) {
        errorMsg = "Invalid first name format. Use letters only.";
        return false;
    }
    if (!isValidName(lastName)) {
        errorMsg = "Invalid last name format. Use letters only.";
        return false;
    }
    if (!email.empty() && !isValidEmail(email)) {
        errorMsg = "Invalid email format.";
        return false;
    }
    if (!phoneNumber.empty() && !isValidPhoneNumber(phoneNumber)) {
        errorMsg = "Invalid phone number format. Must be +20 followed by 10 digits.";
        return false;
    }
    if (!isValidUsername(username)) {
        errorMsg = "Invalid username format. Must be 6+ alphanumeric characters.";
        return false;
    }
    if (isUsernameTaken(username)) {
        errorMsg = "Username already exists.";
        return false;
    }
    if (!isValidPassword(password)) {
        errorMsg = "Password must be 8+ characters with upper, lower, number, and special character.";
        return false;
    }
    if (!isValidRole(role)) {
        errorMsg = "Invalid role. Must be user, admin, or moderator.";
        return false;
    }

    // Generate unique member ID
    memberId = generateUniqueMemberId();

    // Create and store user
    User newUser(memberId, firstName, lastName, email, phoneNumber, username, password, role);
    {
        std::lock_guard<std::mutex> lock(usersMutex);
        users[username] = newUser;
    }
    saveToJsonFile();

    return true;
}