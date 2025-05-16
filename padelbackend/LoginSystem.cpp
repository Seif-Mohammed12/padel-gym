#include "LoginSystem.h"
#include "FileManager.h"
#include <iostream>
#include <chrono>

//=============================================================================
// Data Access Methods
// File operations and data loading functionality
//=============================================================================

json LoginSystem::loadUserData() const
{
    try
    {
        return FileManager::load("data.json");
    }
    catch (const std::exception &e)
    {
        std::cerr << "Error loading user data: " << e.what() << std::endl;
        return json::array();
    }
}

//=============================================================================
// User Verification Methods
// Functions to check user existence and credentials
//=============================================================================

bool LoginSystem::userExists(const string &username) const
{
    json data = loadUserData();
    for (const auto &user : data)
    {
        if (user.contains("username") && user["username"] == username)
        {
            return true;
        }
    }
    return false;
}

bool LoginSystem::passwordMatches(const string &username, const string &password) const
{
    json data = loadUserData();
    for (const auto &user : data)
    {
        if (user.contains("username") && user["username"] == username)
        {
            return user["password"] == password;
        }
    }
    return false;
}

//=============================================================================
// Authentication Methods
// Core login verification functionality
//=============================================================================

bool LoginSystem::authenticate(const string &username, const string &password) const
{
    json data = loadUserData();
    for (const auto &user : data)
    {
        if (user.contains("username") && user["username"] == username)
        {
            return user["password"] == password;
        }
    }
    return false;
}

//=============================================================================
// Password Recovery Methods
// Password retrieval and reset functionality
//=============================================================================

string LoginSystem::recoverPassword(const string &username) const
{
    json data = loadUserData();
    for (const auto &user : data)
    {
        if (user.contains("username") && user["username"] == username)
        {
            return user["password"].get<string>();
        }
    }
    return "";
}

string LoginSystem::handleForgotPassword(const string &username) const
{
    return recoverPassword(username);
}

