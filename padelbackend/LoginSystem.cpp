#include "LoginSystem.h"
#include "FileManager.h"

json LoginSystem::loadUserData() const {
    return FileManager::load("data.json");
}

bool LoginSystem::userExists(const string& username) const {
    json data = loadUserData();
    for (const auto& user : data) {
        if (user.contains("username") && user["username"] == username) {
            return true;
        }
    }
    return false;
}

bool LoginSystem::passwordMatches(const string& username, const string& password) const {
    json data = loadUserData();
    for (const auto& user : data) {
        if (user.contains("username") && user["username"] == username) {
            return user["password"] == password;
        }
    }
    return false;
}

string LoginSystem::recoverPassword(const string& username) const {
    json data = loadUserData();
    for (const auto& user : data) {
        if (user.contains("username") && user["username"] == username) {
            return user["password"].get<string>();
        }
    }
    return "";
}

bool LoginSystem::authenticate(const string& username, const string& password) const {
    json data = loadUserData();
    for (const auto& user : data) {
        if (user.contains("username") && user["username"] == username) {
            return user["password"] == password;
        }
    }
    return false;
}

string LoginSystem::handleForgotPassword(const string& username) const {
    return recoverPassword(username);
}
