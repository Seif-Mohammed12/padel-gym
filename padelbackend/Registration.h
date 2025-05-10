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

class Registration {
private:
    class User {
    private:
        string memberId;
        string firstName;
        string lastName;
        string email;
        string phoneNumber;
        string username;
        string password;
        string role;

    public:
        User();
        User(const string& id, const string& fn, const string& ln,
             const string& em, const string& pn,
             const string& un, const string& pw, const string& r);

        json toJson() const;
        string getMemberId() const;
        string getUsername() const;
        string getPassword() const;
        string getRole() const;
    };

    unordered_map<string, User> users;
    const string dataFile = "data.json";
    mutable std::mutex usersMutex;

    string generateUniqueMemberId() const;
    bool memberIdExists(const string& id) const;

    bool isValidName(const string& name) const;
    bool isValidPhoneNumber(const string& phone) const;
    bool isValidEmail(const string& email) const;
    bool isValidUsername(const string& un) const;
    bool isValidPassword(const string& pw) const;
    bool isValidRole(const string& role) const;

    void saveToJsonFile() const;
    void loadFromJsonFile();
    bool isUsernameTaken(const string& un) const;

public:
    Registration();
    bool registerUser(const string& firstName, const string& lastName,
                      const string& email, const string& phoneNumber,
                      const string& username, const string& password,
                      const string& role, string& memberId, string& errorMsg);
};

#endif