#include <winsock2.h>
#include <ws2tcpip.h>
#include <iostream>
#include <string>
#include <thread>
#include <sstream>
#include <vector>
#include <random>
#include "include/json.hpp"
#include "FileManager.h"
#include "GymClass.h"
#include "SubscriptionManager.h"
#include "GymSystem.h"
#include "WorkoutHistory.h"
#include "Registration.h"
#include "LoginSystem.h"

#pragma comment(lib, "ws2_32.lib")

using json = nlohmann::json;

const int PORT = 8080;

// SECTION: Global Instances
GymSystem gymSystem;
SubscriptionManager subscriptionManager;
WorkoutHistory workoutHistory;
Registration registration;
LoginSystem loginSystem;

// SECTION: Utility Functions
// Splits a string by a delimiter and trims whitespace
std::vector<std::string> split(const std::string &str, char delimiter)
{
    std::vector<std::string> tokens;
    std::string token;
    std::istringstream tokenStream(str);
    while (std::getline(tokenStream, token, delimiter))
    {
        token.erase(0, token.find_first_not_of(" \t"));
        token.erase(token.find_last_not_of(" \t") + 1);
        if (!token.empty())
        {
            tokens.push_back(token);
        }
    }
    return tokens;
}

// Sends a JSON response to the client socket
void sendResponse(SOCKET clientSocket, const json &response)
{
    std::string responseStr = response.dump();
    std::cout << "Sending response: " << responseStr << std::endl;
    int sendResult = send(clientSocket, responseStr.c_str(), responseStr.size(), 0);
    if (sendResult == SOCKET_ERROR)
    {
        std::cerr << "Error sending response: " << WSAGetLastError() << std::endl;
    }
    send(clientSocket, "\n", 1, 0);
}

// SECTION: Data Management Functions
// Loads gym classes from a file
std::vector<GymClass> loadGymClasses(const std::string &filename)
{
    json data = FileManager::load(filename);
    std::vector<GymClass> classes;
    if (data.is_array())
    {
        for (const auto &classJson : data)
        {
            classes.push_back(GymClass::fromJson(classJson));
        }
    }
    return classes;
}

// Saves gym classes to a file
void saveGymClasses(const std::vector<GymClass> &classes, const std::string &filename)
{
    json data = json::array();
    for (const auto &gymClass : classes)
    {
        data.push_back(gymClass.toJson());
    }
    FileManager::save(data, filename);
}

// SECTION: User Management Handlers
// Handles user signup requests
json handleSignup(const json &receivedJson)
{
    try
    {
        std::string firstName = receivedJson.value("firstName", "");
        std::string lastName = receivedJson.value("lastName", "");
        std::string username = receivedJson.value("username", "");
        std::string phone = receivedJson.value("phoneNumber", "");
        std::string password = receivedJson.value("password", "");
        std::string role = receivedJson.value("role", "user");
        if (firstName.empty() || lastName.empty() || username.empty() || password.empty())
        {
            return {{"status", "error"}, {"message", "First name, last name, username, phone and password are required"}};
        }
        std::string memberId, errorMsg;
        bool success = registration.registerUser(
            firstName, lastName, "", phone, username, password, role, memberId, errorMsg);
        if (!success)
        {
            return {{"status", "error"}, {"message", errorMsg}};
        }
        return {
            {"status", "success"},
            {"message", "User registered successfully"},
            {"data", {{"memberId", memberId}}}};
    }
    catch (const json::parse_error &e)
    {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    }
    catch (const std::exception &e)
    {
        std::cerr << "Signup Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Failed to register user: " + std::string(e.what())}};
    }
}

// Handles user login requests
json handleLogin(const json& receivedJson) {
    std::string username = receivedJson.value("username", "");
    std::string password = receivedJson.value("password", "");

    if (username.empty() || password.empty()) {
        return {{"status", "error"}, {"message", "Username and password are required"}};
    }

    json users = loginSystem.loadUserData();
    json loggedInUser;
    bool userFound = false;
    for (const auto& user : users) {
        if (user["username"] == username && user["password"] == password) {
            loggedInUser = user;
            userFound = true;
            break;
        }
    }

    if (userFound) {
        std::string role = loggedInUser.value("role", "user");
        if (role != "user") {
            return {{"status", "error"}, {"message", "Invalid role in user data: " + role}};
        }
        return {
                {"status", "success"},
                {"message", "Login successful"},
                {"data", {
                                   {"username", loggedInUser["username"]},
                                   {"memberId", loggedInUser["memberId"]},
                                   {"firstName", loggedInUser["firstName"]},
                                   {"lastName", loggedInUser["lastName"]},
                                   {"role", "user"},
                                   {"phoneNumber", loggedInUser["phoneNumber"]},
                                   {"email", loggedInUser["email"]}
                           }}
        };
    }

    json staffData = FileManager::load("staff.json");
    if (!staffData.is_array()) staffData = json::array();
    for (const auto& staff : staffData) {
        if (staff["username"] == username && staff["password"] == password && staff["role"] == "manager") {
            return {
                    {"status", "success"},
                    {"message", "Login successful"},
                    {"data", {
                                       {"username", username},
                                       {"memberId", ""},
                                       {"firstName", ""},
                                       {"lastName", ""},
                                       {"role", "manager"},
                                       {"phoneNumber", ""},
                                       {"email", ""}
                               }}
            };
        }
    }

    return {{"status", "error"}, {"message", "Invalid username or password"}};
}

// Handles password recovery requests
json handleForgotPassword(const json &receivedJson, LoginSystem &loginSystem)
{
    try
    {
        string username = receivedJson.value("username", "");
        if (username.empty())
        {
            return {{"status", "error"}, {"message", "Username is required"}};
        }
        string password = loginSystem.handleForgotPassword(username);
        if (password.empty())
        {
            return {{"status", "error"}, {"message", "Username not found"}};
        }
        return {
            {"status", "success"},
            {"message", "Password recovered successfully"},
            {"password", password}};
    }
    catch (const json::parse_error &e)
    {
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    }
    catch (const exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to process request: " + string(e.what())}};
    }
}

// Handles user info update requests
json handleUpdateUserInfo(const json &receivedJson)
{
    try
    {
        std::string memberId = receivedJson.value("memberId", "");
        std::string firstName = receivedJson.value("firstName", "");
        std::string lastName = receivedJson.value("lastName", "");
        std::string email = receivedJson.value("email", "");
        std::string phoneNumber = receivedJson.value("phoneNumber", "");
        std::string username = receivedJson.value("username", "");
        if (memberId.empty())
        {
            return {{"status", "error"}, {"message", "memberId is required"}};
        }
        if (firstName.empty() || lastName.empty() || phoneNumber.empty() || username.empty())
        {
            return {{"status", "error"}, {"message", "First name, last name, phone number, and username are required"}};
        }
        json users = FileManager::load("data.json");
        bool userFound = false;
        for (auto &user : users)
        {
            if (user["memberId"].get<std::string>() == memberId)
            {
                user["firstName"] = firstName;
                user["lastName"] = lastName;
                user["email"] = email;
                user["phoneNumber"] = phoneNumber;
                user["username"] = username;
                userFound = true;
                break;
            }
        }
        if (!userFound)
        {
            return {{"status", "error"}, {"message", "User with memberId " + memberId + " not found"}};
        }
        FileManager::save(users, "data.json");
        return {
            {"status", "success"},
            {"message", "User information updated successfully"},
            {"data", {{"memberId", memberId}, {"firstName", firstName}, {"lastName", lastName}, {"email", email}, {"phoneNumber", phoneNumber}, {"username", username}}}};
    }
    catch (const json::parse_error &e)
    {
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to update user info: " + std::string(e.what())}};
    }
}

json handleGetClasses()
{
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json jsonClasses = json::array();
    for (const auto &gymClass : classes)
    {
        jsonClasses.push_back(gymClass.toJson());
    }
    return {{"status", "success"}, {"data", jsonClasses}};
}

// Handles saving a new gym class or updating an existing one
json handleSaveGymClass(const json &receivedJson)
{
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json classData = receivedJson["data"];
    if (!classData.contains("name") || classData["name"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Class name is required"}};
    }
    if (!classData.contains("instructor") || classData["instructor"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Instructor is required"}};
    }
    if (!classData.contains("time") || classData["time"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Time is required"}};
    }
    if (!classData.contains("image") || classData["image"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!classData.contains("capacity") || !classData["capacity"].is_number_integer() || classData["capacity"].get<int>() <= 0)
    {
        return {{"status", "error"}, {"message", "Capacity must be a positive integer"}};
    }
    std::string className = classData["name"].get<std::string>();
    std::string imagePath = classData["image"].get<std::string>();
    if (imagePath.find("/images/") != 0 || (imagePath.find(".jpg") == std::string::npos && imagePath.find(".png") == std::string::npos))
    {
        imagePath = "/images/placeholder.jpg";
    }
    for (const auto &gymClass : classes)
    {
        if (gymClass.getName() == className)
        {
            return {{"status", "error"}, {"message", "Class with name " + className + " already exists"}};
        }
    }
    GymClass newClass(
        className,
        classData["instructor"].get<std::string>(),
        classData["time"].get<std::string>(),
        classData["capacity"].get<int>(),
        imagePath);
    classes.push_back(newClass);
    try
    {
        saveGymClasses(classes, "gym-classes.json");
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save gym classes: " + std::string(e.what())}};
    }
    json responseData = newClass.toJson();
    responseData["image"] = responseData["imagePath"];
    responseData.erase("imagePath");
    return {
        {"status", "success"},
        {"message", "Gym class saved"},
        {"data", responseData}};
}

json handleBookGymClass(const json &receivedJson)
{
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];
    if (!data.contains("className"))
    {
        return {{"status", "error"}, {"message", "className is required"}};
    }
    std::string className = data["className"].get<std::string>();
    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string())
    {
        memberId = data["memberId"].get<std::string>();
    }
    else if (data.contains("username"))
    {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto &user : users)
        {
            if (user["username"] == username)
            {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    }
    if (memberId.empty())
    {
        return {{"status", "error"}, {"message", "memberId or username required"}};
    }
    json members = FileManager::load("members.json");
    if (!members.is_array())
        members = json::array();
    bool isActive = true;
    bool memberFound = false;
    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId)
        {
            memberFound = true;
            isActive = member.value("isActive", true);
            break;
        }
    }
    if (!memberFound)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive)
    {
        return {{"status", "error"}, {"message", "Cannot book class for inactive member"}};
    }
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    bool classFound = false;
    bool booked = false;
    bool movedFromWaitlist = false;
    std::string instructor;
    for (auto &gymClass : classes)
    {
        if (gymClass.getName() == className)
        {
            classFound = true;
            booked = gymClass.bookClass(memberId, &movedFromWaitlist);
            instructor = gymClass.toJson().value("instructor", "Unknown");
            break;
        }
    }
    if (!classFound)
    {
        return {{"status", "error"}, {"message", "Gym class not found"}};
    }
    try
    {
        saveGymClasses(classes, "gym-classes.json");
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save gym classes: " + std::string(e.what())}};
    }
    if (booked || movedFromWaitlist)
    {
        try
        {
            auto now = std::chrono::system_clock::now();
            auto in_time_t = std::chrono::system_clock::to_time_t(now);
            std::stringstream ss;
            ss << std::put_time(std::localtime(&in_time_t), "%Y-%m-%d");
            std::string currentDate = ss.str();
            workoutHistory.addWorkout(memberId, className, currentDate, instructor);
            for (auto &member : members)
            {
                std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
                if (member[memberIdKey].get<std::string>() == memberId)
                {
                    if (!member.contains("workouts") || !member["workouts"].is_array())
                    {
                        member["workouts"] = json::array();
                    }
                    json workout = {
                        {"className", className},
                        {"date", currentDate},
                        {"instructor", instructor}};
                    member["workouts"].push_back(workout);
                    break;
                }
            }
            try
            {
                FileManager::save(members, "members.json");
            }
            catch (const std::exception &e)
            {
                return {{"status", "error"}, {"message", "Failed to save members.json: " + std::string(e.what())}};
            }
            std::cout << "Added workout to history and members.json for member " << memberId << ", class " << className << std::endl;
        }
        catch (const std::exception &e)
        {
            std::cerr << "Failed to add workout: " << e.what() << std::endl;
            return {{"status", "error"}, {"message", "Failed to add workout: " + std::string(e.what())}};
        }
    }

    return {
        {"status", "success"},
        {"message", booked || movedFromWaitlist ? "Successfully booked class" : "Added to waitlist"},
        {"waitlisted", !(booked || movedFromWaitlist)}};
}

json handleCancelGymClass(const json &receivedJson)
{
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];
    if (!data.contains("className") || !data.contains("memberId"))
    {
        return {{"status", "error"}, {"message", "className and memberId are required"}};
    }
    std::string className = data["className"].get<std::string>();
    std::string memberId = data["memberId"].get<std::string>();

    // Verify member exists and is active
    json members = FileManager::load("members.json");
    if (!members.is_array())
        members = json::array();
    bool memberFound = false;
    bool isActive = true;
    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId)
        {
            memberFound = true;
            isActive = member.value("isActive", true);
            break;
        }
    }
    if (!memberFound)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive)
    {
        return {{"status", "error"}, {"message", "Cannot cancel booking for inactive member"}};
    }

    // Load and update gym classes
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    bool classFound = false;
    std::string movedMemberId;
    std::string instructor;
    bool memberWasInClassOrWaitlist = false;
    for (auto &gymClass : classes)
    {
        if (gymClass.getName() == className)
        {
            classFound = true;
            std::cout << "Canceling booking: Class=" << className
                      << ", Member=" << memberId
                      << ", Capacity=" << gymClass.getCapacity()
                      << ", Participants=" << gymClass.getCurrentParticipants()
                      << ", WaitlistSize=" << gymClass.getWaitlistSize() << std::endl;
            // Check if member is in participants or waitlist before removal
            auto participants = gymClass.toJson()["participants"].get<std::vector<std::string>>();
            auto waitlist = gymClass.queueToVector(gymClass.waitlist);
            memberWasInClassOrWaitlist = std::find(participants.begin(), participants.end(), memberId) != participants.end() ||
                                         std::find(waitlist.begin(), waitlist.end(), memberId) != waitlist.end();
            movedMemberId = gymClass.removeMember(memberId);
            instructor = gymClass.toJson().value("instructor", "Unknown");
            std::cout << "Result: MovedMember=" << (movedMemberId.empty() ? "None" : movedMemberId)
                      << ", Participants=" << gymClass.getCurrentParticipants()
                      << ", WaitlistSize=" << gymClass.getWaitlistSize() << std::endl;
            break;
        }
    }

    if (!classFound)
    {
        return {{"status", "error"}, {"message", "Gym class not found"}};
    }
    if (!memberWasInClassOrWaitlist && movedMemberId.empty())
    {
        return {{"status", "error"}, {"message", "Member not found in class or waitlist"}};
    }

    // Save updated classes
    try
    {
        saveGymClasses(classes, "gym-classes.json");
        std::cout << "Successfully saved gym classes after cancellation" << std::endl;
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save gym classes: " + std::string(e.what())}};
    }

    // If a member was moved from waitlist, update their workout history
    if (!movedMemberId.empty())
    {
        try
        {
            auto now = std::chrono::system_clock::now();
            auto in_time_t = std::chrono::system_clock::to_time_t(now);
            std::stringstream ss;
            ss << std::put_time(std::localtime(&in_time_t), "%Y-%m-%d");
            std::string currentDate = ss.str();
            workoutHistory.addWorkout(movedMemberId, className, currentDate, instructor);

            // Update members.json workouts field
            for (auto &member : members)
            {
                std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
                if (member[memberIdKey].get<std::string>() == movedMemberId)
                {
                    if (!member.contains("workouts") || !member["workouts"].is_array())
                    {
                        member["workouts"] = json::array();
                    }
                    json workout = {
                        {"className", className},
                        {"date", currentDate},
                        {"instructor", instructor}};
                    member["workouts"].push_back(workout);
                    std::cout << "Added workout to members.json for moved member " << movedMemberId
                              << ", class " << className << std::endl;
                    break;
                }
            }
            try
            {
                FileManager::save(members, "members.json");
                std::cout << "Successfully saved members.json after updating workout" << std::endl;
            }
            catch (const std::exception &e)
            {
                return {{"status", "error"}, {"message", "Failed to save members.json: " + std::string(e.what())}};
            }
        }
        catch (const std::exception &e)
        {
            return {{"status", "error"}, {"message", "Failed to add workout for moved member: " + std::string(e.what())}};
        }
    }

    return {
        {"status", "success"},
        {"message", "Successfully canceled booking" + (movedMemberId.empty() ? "" : " and moved member " + movedMemberId + " from waitlist")},
        {"movedMemberId", movedMemberId}};
}

// Handles saving a new padel center
json handleSavePadelCenter(const json &receivedJson)
{
    json centers = FileManager::load("padel-classes.json");
    json centerData = receivedJson["data"];
    if (!centerData.contains("name") || centerData["name"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Center name is required"}};
    }
    if (!centerData.contains("times") || centerData["times"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Available times are required"}};
    }
    if (!centerData.contains("image") || centerData["image"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!centerData.contains("location") || centerData["location"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Location is required"}};
    }
    std::string timesStr = centerData["times"].get<std::string>();
    std::vector<std::string> timesList = split(timesStr, ',');
    if (timesList.empty())
    {
        return {{"status", "error"}, {"message", "At least one available time is required"}};
    }
    json timesArray = json::array();
    for (const auto &time : timesList)
    {
        timesArray.push_back(time);
    }
    centerData.erase("times");
    centerData["availableTimes"] = timesArray;
    centers.push_back(centerData);
    FileManager::save(centers, "padel-classes.json");
    return {
        {"status", "success"},
        {"message", "Padel center saved"},
        {"data", centerData}};
}

// Handles updating an existing padel center
json handleUpdatePadelCenter(const json &receivedJson)
{
    json centers = FileManager::load("padel-classes.json");
    json oldData = receivedJson["oldData"];
    json newData = receivedJson["newData"];
    if (!newData.contains("name") || newData["name"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Center name is required"}};
    }
    if (!newData.contains("times") || newData["times"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Available times are required"}};
    }
    if (!newData.contains("image") || newData["image"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!newData.contains("location") || newData["location"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Location is required"}};
    }
    std::string timesStr = newData["times"].get<std::string>();
    std::vector<std::string> timesList = split(timesStr, ',');
    if (timesList.empty())
    {
        return {{"status", "error"}, {"message", "At least one available time is required"}};
    }
    json timesArray = json::array();
    for (const auto &time : timesList)
    {
        timesArray.push_back(time);
    }
    newData.erase("times");
    newData["availableTimes"] = timesArray;
    json newCenters = json::array();
    bool found = false;
    for (const auto &center : centers)
    {
        if (center == oldData)
        {
            found = true;
        }
        else
        {
            newCenters.push_back(center);
        }
    }
    if (!found)
    {
        return {{"status", "error"}, {"message", "Padel center to update not found"}};
    }
    newCenters.push_back(newData);
    FileManager::save(newCenters, "padel-classes.json");
    return {
        {"status", "success"},
        {"message", "Padel center updated"},
        {"data", newData}};
}

// Handles updating an existing gym class
json handleUpdateGymClass(const json &receivedJson)
{
    if (!receivedJson.contains("oldData") || !receivedJson.contains("newData") ||
        receivedJson["oldData"].is_null() || receivedJson["newData"].is_null())
    {
        return {{"status", "error"}, {"message", "oldData and newData are required"}};
    }
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json oldData = receivedJson["oldData"];
    json newData = receivedJson["newData"];
    if (!newData.contains("name") || newData["name"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Class name is required"}};
    }
    if (!newData.contains("instructor") || newData["instructor"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Instructor is required"}};
    }
    if (!newData.contains("time") || newData["time"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Time is required"}};
    }
    if (!newData.contains("image") || newData["image"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!newData.contains("capacity") || !newData["capacity"].is_number_integer() || newData["capacity"].get<int>() <= 0)
    {
        return {{"status", "error"}, {"message", "Capacity must be a positive integer"}};
    }
    std::string oldClassName = oldData.value("name", "");
    if (oldClassName.empty())
    {
        return {{"status", "error"}, {"message", "Old class name is required in oldData"}};
    }
    std::vector<std::string> oldParticipants, oldWaitlist;
    bool found = false;
    for (const auto &gymClass : classes)
    {
        if (gymClass.getName() == oldClassName)
        {
            found = true;
            oldParticipants = gymClass.toJson()["participants"].get<std::vector<std::string>>();
            oldWaitlist = gymClass.queueToVector(gymClass.waitlist);
            break;
        }
    }
    if (!found)
    {
        return {{"status", "error"}, {"message", "Gym class to update not found"}};
    }

    // Create updated class
    GymClass updatedClass(
        newData["name"].get<std::string>(),
        newData["instructor"].get<std::string>(),
        newData["time"].get<std::string>(),
        newData["capacity"].get<int>(),
        newData["image"].get<std::string>());

    // Update participants and waitlist
    std::vector<std::string> newParticipants = newData.value("participants", std::vector<std::string>{});
    std::vector<std::string> newWaitlist = newData.value("waitlist", std::vector<std::string>{});
    std::vector<std::string> movedMembers;
    for (const std::string &id : newParticipants)
    {
        bool wasWaitlisted = std::find(oldWaitlist.begin(), oldWaitlist.end(), id) != oldWaitlist.end();
        bool wasParticipant = std::find(oldParticipants.begin(), oldParticipants.end(), id) != oldParticipants.end();
        bool moved = false;
        updatedClass.bookClass(id, &moved);
        if (moved || (!wasParticipant && wasWaitlisted))
        {
            movedMembers.push_back(id);
        }
    }
    for (const std::string &id : newWaitlist)
    {
        updatedClass.bookClass(id);
    }

    // Replace old class with updated class
    std::vector<GymClass> newClasses;
    for (const auto &gymClass : classes)
    {
        if (gymClass.getName() != oldClassName)
        {
            newClasses.push_back(gymClass);
        }
    }
    newClasses.push_back(updatedClass);

    // Save updated classes
    try
    {
        saveGymClasses(newClasses, "gym-classes.json");
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save gym classes: " + std::string(e.what())}};
    }

    // Update workouts for moved members
    if (!movedMembers.empty())
    {
        json members = FileManager::load("members.json");
        if (!members.is_array())
            members = json::array();
        std::string instructor = newData["instructor"].get<std::string>();
        auto now = std::chrono::system_clock::now();
        auto in_time_t = std::chrono::system_clock::to_time_t(now);
        std::stringstream ss;
        ss << std::put_time(std::localtime(&in_time_t), "%Y-%m-%d");
        std::string currentDate = ss.str();

        for (const std::string &movedMemberId : movedMembers)
        {
            try
            {
                workoutHistory.addWorkout(movedMemberId, newData["name"].get<std::string>(), currentDate, instructor);
                for (auto &member : members)
                {
                    std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
                    if (member[memberIdKey].get<std::string>() == movedMemberId)
                    {
                        if (!member.contains("workouts") || !member["workouts"].is_array())
                        {
                            member["workouts"] = json::array();
                        }
                        json workout = {
                            {"className", newData["name"].get<std::string>()},
                            {"date", currentDate},
                            {"instructor", instructor}};
                        member["workouts"].push_back(workout);
                        std::cout << "Added workout to members.json for moved member " << movedMemberId
                                  << ", class " << newData["name"].get<std::string>() << std::endl;
                        break;
                    }
                }
            }
            catch (const std::exception &e)
            {
                std::cerr << "Failed to add workout for moved member " << movedMemberId << ": " << e.what() << std::endl;
            }
        }
        try
        {
            FileManager::save(members, "members.json");
            std::cout << "Successfully saved members.json after updating workouts" << std::endl;
        }
        catch (const std::exception &e)
        {
            return {{"status", "error"}, {"message", "Failed to save members.json: " + std::string(e.what())}};
        }
    }

    json responseData = updatedClass.toJson();
    responseData["image"] = responseData["imagePath"];
    responseData.erase("imagePath");

    return {
        {"status", "success"},
        {"message", "Gym class updated"},
        {"data", responseData},
        {"movedMembers", movedMembers}};
}

json handleGetPadelCenters()
{
    json centers = FileManager::load("padel-classes.json");
    return {{"status", "success"}, {"data", centers}};
}

json handleDeletePadelCenter(const json &receivedJson)
{
    json centers = FileManager::load("padel-classes.json");
    json centerToDelete = receivedJson["data"];
    json newCenters = json::array();

    for (const auto &center : centers)
    {
        if (center != centerToDelete)
        {
            newCenters.push_back(center);
        }
    }

    FileManager::save(newCenters, "padel-classes.json");
    return {{"status", "success"}, {"message", "Padel center deleted"}};
}

json handleDeleteGymClass(const json &receivedJson)
{
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json classToDelete = receivedJson["data"];
    std::vector<GymClass> newClasses;

    for (const auto &gymClass : classes)
    {
        if (gymClass.toJson() != classToDelete)
        {
            newClasses.push_back(gymClass);
        }
    }

    saveGymClasses(newClasses, "gym-classes.json");
    return {{"status", "success"}, {"message", "Gym class deleted"}};
}

json handleGetActiveSubscription(const json &receivedJson)
{
    // Validate that memberId exists
    if (!receivedJson.contains("memberId"))
    {
        return {{"status", "error"}, {"message", "memberId is required"}};
    }

    // Extract memberId as string (handle both int and string types)
    std::string memberId;
    try
    {
        if (receivedJson["memberId"].is_number())
        {
            memberId = std::to_string(receivedJson["memberId"].get<int>());
        }
        else if (receivedJson["memberId"].is_string())
        {
            memberId = receivedJson["memberId"].get<std::string>();
        }
        else
        {
            return {{"status", "error"}, {"message", "Invalid memberId format"}};
        }
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Error parsing memberId: " + std::string(e.what())}};
    }

    // Load subscriptions file
    json allSubs;
    try
    {
        allSubs = FileManager::load("active-subscriptions.json");
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Could not load subscriptions file: " + std::string(e.what())}};
    }

    // Get member's DOB from members.json
    json members = FileManager::load("members.json");
    std::string memberDob;
    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId)
        {
            memberDob = member["dob"].get<std::string>();
            break;
        }
    }

    // Filter subscriptions for the given memberId and isActive
    json activeSubs = json::array();
    for (const auto &sub : allSubs)
    {
        try
        {
            std::string subMemberId;
            if (sub["memberId"].is_number())
            {
                subMemberId = std::to_string(sub["memberId"].get<int>());
            }
            else
            {
                subMemberId = sub["memberId"].get<std::string>();
            }

            if (subMemberId == memberId && sub.value("isActive", true))
            {
                json subWithDob = sub;
                subWithDob["dob"] = memberDob;
                subWithDob["startDate"] = sub.value("startDate", "N/A");
                subWithDob["expiryDate"] = sub.value("expiryDate", "N/A");
                activeSubs.push_back(subWithDob);
            }
        }
        catch (...)
        {
            // Skip malformed entries
            continue;
        }
    }

    return {{"status", "success"}, {"data", activeSubs}};
}

json handleGetSubscriptionPlans()
{
    json plans = FileManager::load("subscriptions.json");
    return {{"status", "success"}, {"data", plans}};
}

json handleSubscribePlan(const json &receivedJson)
{
    std::cout << "Processing handleSubscribePlan with request: " << receivedJson.dump() << std::endl;
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    if (!data.contains("planName") || data["planName"].get<std::string>().empty() ||
        !data.contains("duration") || data["duration"].get<std::string>().empty())
    {
        return {{"status", "error"}, {"message", "planName and duration are required"}};
    }

    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string())
    {
        memberId = data["memberId"].get<std::string>();
    }
    else if (data.contains("username"))
    {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto &user : users)
        {
            if (user["username"] == username)
            {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    }
    else
    {
        return {{"status", "error"}, {"message", "memberId or username required"}};
    }

    if (memberId.empty())
    {
        return {{"status", "error"}, {"message", "Invalid memberId or username: user not found"}};
    }

    json members = FileManager::load("members.json");
    if (!members.is_array())
        members = json::array();

    bool memberExists = false;
    json existingMember;
    for (auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId)
        {
            if (!memberExists)
            {
                existingMember = member;
                memberExists = true;
                // Update the member in-place
                std::string planName = data["planName"].get<std::string>();
                std::string duration = data["duration"].get<std::string>();

                json plans = FileManager::load("subscriptions.json");
                json selectedPlan;
                bool planFound = false;
                for (const auto &plan : plans)
                {
                    if (plan["name"] == planName)
                    {
                        selectedPlan = plan;
                        planFound = true;
                        break;
                    }
                }

                if (!planFound)
                {
                    return {{"status", "error"}, {"message", "Plan '" + planName + "' not found"}};
                }

                if (!selectedPlan["pricing"].contains(duration) || selectedPlan["pricing"][duration].is_null())
                {
                    return {{"status", "error"}, {"message", "Invalid duration '" + duration + "' for plan '" + planName + "'"}};
                }
                double price = selectedPlan["pricing"][duration].get<double>();

                // Create subscription JSON
                json subscriptionJson = {
                    {"planName", planName},
                    {"duration", duration},
                    {"price", price}};

                // Update member's subscription
                member["subscription"] = subscriptionJson;
                if (!member.contains("workouts"))
                {
                    member["workouts"] = json::array();
                }
            }
            else
            {
                std::cout << "Found duplicate member with id: " << memberId << ", merging data" << std::endl;
                // Merge duplicate: keep existingMember's subscription, append unique fields
                if (member.contains("workouts") && !existingMember.contains("workouts"))
                {
                    existingMember["workouts"] = member["workouts"];
                    member["workouts"] = existingMember["workouts"];
                }
            }
        }
    }

    if (!memberExists)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!existingMember.value("isActive", true))
    {
        return {{"status", "error"}, {"message", "Cannot subscribe: member is inactive"}};
    }

    // Save updated members.json
    try
    {
        std::cout << "Saving subscription for member " << memberId << ": " << existingMember["subscription"].dump() << std::endl;
        FileManager::save(members, "members.json");
        std::cout << "Successfully saved members.json: " << members.dump(2) << std::endl;
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save members.json: " + std::string(e.what())}};
    }

    // Update subscription manager and create Subscription object
    std::string planName = data["planName"].get<std::string>();
    std::string duration = data["duration"].get<std::string>();
    double price = existingMember["subscription"]["price"].get<double>();

    Subscription *existingSub = subscriptionManager.findSubscription(memberId);
    Subscription newSub(memberId, planName, duration, price); // Creates with startDate and expiryDate
    if (existingSub)
    {
        std::string oldPlanName = existingSub->getPlanName();
        json plans = FileManager::load("subscriptions.json");
        for (auto &plan : plans)
        {
            if (plan["name"] == oldPlanName)
            {
                json subscribers = plan.value("subscribers", json::array());
                auto it = std::find(subscribers.begin(), subscribers.end(), memberId);
                if (it != subscribers.end())
                {
                    subscribers.erase(it);
                    plan["subscribers"] = subscribers;
                }
            }
        }
        existingSub->renew(planName, duration, price);
    }
    else
    {
        subscriptionManager.addSubscription(newSub);
    }

    // Update subscribers in plans
    json plans = FileManager::load("subscriptions.json");
    for (auto &plan : plans)
    {
        if (plan["name"] == planName)
        {
            json subscribers = plan.value("subscribers", json::array());
            if (std::find(subscribers.begin(), subscribers.end(), memberId) == subscribers.end())
            {
                subscribers.push_back(memberId);
                plan["subscribers"] = subscribers;
            }
        }
    }

    try
    {
        FileManager::save(plans, "subscriptions.json");
        std::cout << "Successfully saved subscriptions.json: " << plans.dump(2) << std::endl;
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save subscription plans: " + std::string(e.what())}};
    }

    // Update active-subscriptions.json with full subscription details
    json activeSubs = FileManager::load("active-subscriptions.json");
    if (!activeSubs.is_array())
        activeSubs = json::array();

    for (auto it = activeSubs.begin(); it != activeSubs.end();)
    {
        if ((*it)["memberId"].get<std::string>() == memberId)
        {
            it = activeSubs.erase(it);
        }
        else
        {
            ++it;
        }
    }

    // Get member's DOB from members.json
    std::string memberDob;
    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId)
        {
            memberDob = member["dob"].get<std::string>();
            break;
        }
    }

    // Use Subscription::toJson() to include startDate, expiryDate, etc.
    json newSubJson = newSub.toJson();
    newSubJson["dob"] = memberDob;
    activeSubs.push_back(newSubJson);

    try
    {
        FileManager::save(activeSubs, "active-subscriptions.json");
        std::cout << "Successfully saved active-subscriptions.json: " << activeSubs.dump(2) << std::endl;
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to save active-subscriptions.json: " + std::string(e.what())}};
    }

    // Call renewSubscription with structured subscription JSON
    json subscriptionJson = {
        {"planName", planName},
        {"duration", duration},
        {"price", price}};
    try
    {
        std::cout << "Calling renewSubscription with subscription: " << subscriptionJson.dump() << std::endl;
        gymSystem.renewSubscription(memberId, subscriptionJson);
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to update member subscription: " + std::string(e.what())}};
    }

    // Verify members.json after renewSubscription
    members = FileManager::load("members.json");
    std::cout << "Members.json after renewSubscription: " << members.dump(2) << std::endl;

    return {{"status", "success"}, {"message", "Subscribed successfully"}};
}

json handleAddMember(const json &receivedJson)
{
    std::cout << "Processing handleAddMember with request: " << receivedJson.dump() << std::endl;
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];
    Member m;

    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string())
    {
        memberId = data["memberId"].get<std::string>();
    }
    else if (data.contains("username"))
    {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto &user : users)
        {
            if (user["username"] == username)
            {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    }

    if (memberId.empty())
    {
        return {{"status", "error"}, {"message", "memberId or username required and must correspond to a valid user"}};
    }

    json users = FileManager::load("data.json");
    std::string email, username;
    bool userFound = false;
    for (const auto &user : users)
    {
        if (user["memberId"].get<std::string>() == memberId)
        {
            email = user.value("email", "");
            username = user.value("username", "");
            userFound = true;
            break;
        }
    }

    if (!userFound)
    {
        return {{"status", "error"}, {"message", "User with memberId " + memberId + " not found in user data"}};
    }

    std::string fullName = data["name"].get<std::string>();
    std::vector<std::string> nameParts = split(fullName, ' ');
    std::string firstName = nameParts.empty() ? "" : nameParts[0];
    std::string lastName = nameParts.size() > 1 ? nameParts[1] : "";
    std::string dob = data["dob"].get<std::string>();
    std::string phone = data.value("phoneNumber", "");

    // Extract subscription details
    if (!data.contains("subscription") || !data["subscription"].is_object())
    {
        return {{"status", "error"}, {"message", "Subscription must be an object with planName, duration, and price"}};
    }
    json subscriptionData = data["subscription"];
    std::string planName = subscriptionData["planName"].get<std::string>();
    std::string duration = subscriptionData["duration"].get<std::string>();
    double price = subscriptionData["price"].get<double>();

    if (planName.empty() || duration.empty())
    {
        return {{"status", "error"}, {"message", "Missing or invalid subscription fields (planName, duration)"}};
    }

    // Validate plan
    json plans = FileManager::load("subscriptions.json");
    bool planFound = false;
    for (const auto &plan : plans)
    {
        if (plan["name"] == planName)
        {
            if (plan["pricing"].contains(duration))
            {
                double expectedPrice = plan["pricing"][duration].get<double>();
                if (price != expectedPrice)
                {
                    std::cout << "Warning: Provided price (" << price << ") differs from expected (" << expectedPrice << "). Using expected price." << std::endl;
                    price = expectedPrice;
                }
                planFound = true;
            }
            break;
        }
    }

    if (!planFound)
    {
        return {{"status", "error"}, {"message", "Plan '" + planName + "' or duration '" + duration + "' not found"}};
    }

    json subscriptionJson = {
        {"planName", planName},
        {"duration", duration},
        {"price", price}};

    m.addMember(memberId, firstName, lastName, email, phone, username, dob, subscriptionJson);
    gymSystem.addMember(m);

    json members = FileManager::load("members.json");
    if (!members.is_array())
        members = json::array();

    // Remove existing entries for this memberId
    json newMembers = json::array();
    bool found = false;
    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() != memberId)
        {
            newMembers.push_back(member);
        }
        else
        {
            std::cout << "Removing existing member with id: " << memberId << " to update." << std::endl;
            found = true;
        }
    }

    // Add updated member
    json newMember = m.toJson();
    newMember["id"] = memberId;
    newMember["isActive"] = true;
    newMember["workouts"] = json::array();
    newMembers.push_back(newMember);

    try
    {
        FileManager::save(newMembers, "members.json");
        std::cout << "Successfully saved members.json: " << newMembers.dump(2) << std::endl;
    }
    catch (const std::exception &e)
    {
        std::cerr << "Failed to save members.json: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Failed to save member to members.json: " + std::string(e.what())}};
    }

    return {{"status", "success"}, {"message", "Member added successfully"}};
}

json handleCheckMember(const json &receivedJson)
{
    std::cout << "Processing handleCheckMember with request: " << receivedJson.dump() << std::endl;
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string())
    {
        memberId = data["memberId"].get<std::string>();
    }
    else
    {
        return {{"status", "error"}, {"message", "memberId required"}};
    }

    std::string dob;
    if (data.contains("dob") && data["dob"].is_string())
    {
        dob = data["dob"].get<std::string>();
    }
    else
    {
        return {{"status", "error"}, {"message", "dob required"}};
    }

    json members = FileManager::load("members.json");
    if (!members.is_array())
    {
        return {{"status", "error"}, {"message", "No members found"}};
    }

    for (const auto &member : members)
    {
        std::string memberIdKey = member.contains("memberId") ? "memberId" : "id";
        if (member[memberIdKey].get<std::string>() == memberId && member["dob"].get<std::string>() == dob)
        {
            bool isActive = member.value("isActive", false);
            return {
                {"status", "success"},
                {"message", "Member found"},
                {"data", {{"memberId", memberId}, {"isActive", isActive}, {"dob", dob}}}};
        }
    }

    return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
}

json handleAddStaff(const json& receivedJson) {
    try {
        json data = receivedJson["data"];
        Staff s;
        s.addStaff(
                data["username"].get<std::string>(),
                data["password"].get<std::string>(),
                data["role"].get<std::string>()
        );
        gymSystem.addStaff(s);

        // Save to staff.json
        json staffData = FileManager::load("staff.json");
        if (!staffData.is_array()) staffData = json::array();
        staffData.push_back(s.toJson());
        FileManager::save(staffData, "staff.json");

        return {{"status", "success"}, {"message", "Staff added successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to add staff: " + std::string(e.what())}};
    }
}

json handleGetMembers()
{
    return {{"status", "success"}, {"data", gymSystem.getMembers()}};
}

json handleGetStaff()
{
    return {{"status", "success"}, {"data", gymSystem.getStaff()}};
}

json handleRenewSubscription(const json &receivedJson)
{
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    // Extract required fields
    std::string memberId = data.value("memberId", "");
    std::string planName = data.value("planName", "");
    std::string duration = data.value("duration", "");

    if (memberId.empty() || planName.empty() || duration.empty())
    {
        return {{"status", "error"}, {"message", "memberId, planName, and duration are required"}};
    }

    try
    {
        json members = FileManager::load("members.json");
        json activeSubs = FileManager::load("active-subscriptions.json");

        // Get plan price
        json plans = FileManager::load("subscriptions.json");
        double price = 0.0;
        bool planFound = false;

        for (const auto &plan : plans)
        {
            if (plan["name"] == planName && plan["pricing"].contains(duration))
            {
                price = plan["pricing"][duration].get<double>();
                planFound = true;
                break;
            }
        }

        if (!planFound)
        {
            return {{"status", "error"}, {"message", "Invalid plan or duration"}};
        }

        // Find member - explicitly check both id and memberId fields
        bool memberFound = false;
        std::string memberDob;

        std::cout << "Looking for member with ID: " << memberId << std::endl;

        for (auto &member : members)
        {
            // Debug output
            std::cout << "Checking member: " << member.dump(2) << std::endl;

            bool idMatch = false;
            // Check all possible ID field variations
            if (member.contains("memberId"))
            {
                idMatch = (member["memberId"].get<std::string>() == memberId);
            }
            if (!idMatch && member.contains("id"))
            {
                idMatch = (member["id"].get<std::string>() == memberId);
            }

            if (idMatch)
            {
                memberFound = true;
                memberDob = member["dob"].get<std::string>();

                // Update subscription details
                member["subscription"] = {
                    {"planName", planName},
                    {"duration", duration},
                    {"price", price}};
                member["isActive"] = true;

                std::cout << "Found and updated member: " << member.dump(2) << std::endl;
                break;
            }
        }

        if (!memberFound)
        {
            std::cout << "Member not found. Members data: " << members.dump(2) << std::endl;
            return {{"status", "error"}, {"message", "Member not found"}};
        }

        // Update active-subscriptions.json
        Subscription newSub(memberId, planName, duration, price);
        json newSubJson = newSub.toJson();
        newSubJson["dob"] = memberDob;

        // Remove old subscription
        bool hadPreviousSub = false;
        for (auto it = activeSubs.begin(); it != activeSubs.end();)
        {
            if ((*it)["memberId"].get<std::string>() == memberId)
            {
                it = activeSubs.erase(it);
                hadPreviousSub = true;
            }
            else
            {
                ++it;
            }
        }

        // Add new subscription
        activeSubs.push_back(newSubJson);

        // Save both files
        FileManager::save(members, "members.json");
        FileManager::save(activeSubs, "active-subscriptions.json");

        // Use GymSystem to update subscription
        json subscriptionJson = {
            {"planName", planName},
            {"duration", duration},
            {"price", price}};
        gymSystem.renewSubscription(memberId, subscriptionJson);

        return {
            {"status", "success"},
            {"message", hadPreviousSub ? "Subscription renewed successfully" : "New subscription created successfully"},
            {"data", newSubJson}};
    }
    catch (const std::exception &e)
    {
        std::cerr << "Error in handleRenewSubscription: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Failed to renew subscription: " + std::string(e.what())}};
    }
}

json handleCancelMembership(const json &receivedJson)
{
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    if (!data.contains("memberId") || !data["memberId"].is_string())
    {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = data["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto &member : members)
    {
        if (member["id"].get<std::string>() == memberId)
        {
            memberExists = true;
            break;
        }
    }

    if (!memberExists)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try
    {
        gymSystem.cancelMembership(memberId);
        subscriptionManager.cancelSubscription(memberId);
        workoutHistory.clearHistory(memberId);

        // Update isActive in members.json
        for (auto &member : members)
        {
            if (member["id"].get<std::string>() == memberId)
            {
                member["isActive"] = false;
                member["workouts"] = json::array();
                break;
            }
        }
        FileManager::save(members, "members.json");

        // Remove memberId from all subscription plans
        json subs = FileManager::load("subscriptions.json");
        for (auto &plan : subs)
        {
            if (plan.contains("subscribers") && plan["subscribers"].is_array())
            {
                auto &subscribers = plan["subscribers"];
                subscribers.erase(
                    std::remove(subscribers.begin(), subscribers.end(), memberId),
                    subscribers.end());
            }
        }
        FileManager::save(subs, "subscriptions.json");

        return {{"status", "success"}, {"message", "Membership canceled successfully"}};
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to cancel membership: " + std::string(e.what())}};
    }
}

// New Workout History Handlers
json handleGetWorkoutHistory(const json &receivedJson)
{
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string())
    {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = receivedJson["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto &member : members)
    {
        if (member["id"].get<std::string>() == memberId)
        {
            memberExists = true;
            break;
        }
    }

    if (!memberExists)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try
    {
        json workouts = workoutHistory.getAllWorkouts(memberId);
        return {{"status", "success"}, {"data", workouts}};
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to get workout history: " + std::string(e.what())}};
    }
}

json handleAddWorkout(const json &receivedJson)
{
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string())
    {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }
    if (!receivedJson.contains("data") || receivedJson["data"].is_null())
    {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }

    json data = receivedJson["data"];
    std::string memberId = receivedJson["memberId"].get<std::string>();
    std::string className = data.value("className", "");
    std::string date = data.value("date", "");
    std::string instructor = data.value("instructor", "");

    // Validate required fields
    if (className.empty() || date.empty() || instructor.empty())
    {
        return {{"status", "error"}, {"message", "className, date, and instructor are required"}};
    }

    // Validate member exists and is active
    json members = FileManager::load("members.json");
    bool memberExists = false;
    bool isActive = false;
    for (const auto &member : members)
    {
        if (member["id"].get<std::string>() == memberId)
        {
            memberExists = true;
            isActive = member.value("isActive", true);
            break;
        }
    }

    if (!memberExists)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive)
    {
        return {{"status", "error"}, {"message", "Cannot add workout: member is inactive"}};
    }

    try
    {
        workoutHistory.addWorkout(memberId, className, date, instructor);
        return {{"status", "success"}, {"message", "Workout added successfully"}};
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to add workout: " + std::string(e.what())}};
    }
}

json handleClearWorkoutHistory(const json &receivedJson)
{
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string())
    {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = receivedJson["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto &member : members)
    {
        if (member["id"].get<std::string>() == memberId)
        {
            memberExists = true;
            break;
        }
    }

    if (!memberExists)
    {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try
    {
        workoutHistory.clearHistory(memberId);
        return {{"status", "success"}, {"message", "Workout history cleared successfully"}};
    }
    catch (const std::exception &e)
    {
        return {{"status", "error"}, {"message", "Failed to clear workout history: " + std::string(e.what())}};
    }
}

json processRequest(const json &receivedJson)
{
    std::string action = receivedJson.value("action", "");
    std::cout << "Processing action: " << action << std::endl;

    if (action == "signup")
    {
        return handleSignup(receivedJson);
    }
    else if (action == "login")
    {
        return handleLogin(receivedJson);
    }
    else if (action == "get_classes")
    {
        return handleGetClasses();
    }
    else if (action == "save_gym_class")
    {
        return handleSaveGymClass(receivedJson);
    }
    else if (action == "book_gym_class")
    {
        return handleBookGymClass(receivedJson);
    }
    else if (action == "cancel_gym_class")
    {
        return handleCancelGymClass(receivedJson);
    }
    else if (action == "save_padel_center")
    {
        return handleSavePadelCenter(receivedJson);
    }
    else if (action == "update_padel_center")
    {
        return handleUpdatePadelCenter(receivedJson);
    }
    else if (action == "update_gym_class")
    {
        return handleUpdateGymClass(receivedJson);
    }
    else if (action == "get_padel_centers")
    {
        return handleGetPadelCenters();
    }
    else if (action == "delete_padel_center")
    {
        return handleDeletePadelCenter(receivedJson);
    }
    else if (action == "delete_gym_class")
    {
        return handleDeleteGymClass(receivedJson);
    }
    else if (action == "get_subscription_plans")
    {
        return handleGetSubscriptionPlans();
    }
    else if (action == "subscribe")
    {
        return handleSubscribePlan(receivedJson);
    }
    else if (action == "add_member")
    {
        return handleAddMember(receivedJson);
    }
    else if (action == "add_staff")
    {
        return handleAddStaff(receivedJson);
    }
    else if (action == "get_members")
    {
        return handleGetMembers();
    }
    else if (action == "get_staff")
    {
        return handleGetStaff();
    }
    else if (action == "renew_subscription")
    {
        return handleRenewSubscription(receivedJson);
    }
    else if (action == "cancel_membership")
    {
        return handleCancelMembership(receivedJson);
    }
    else if (action == "get_workout_history")
    {
        return handleGetWorkoutHistory(receivedJson);
    }
    else if (action == "add_workout")
    {
        return handleAddWorkout(receivedJson);
    }
    else if (action == "clear_workout_history")
    {
        return handleClearWorkoutHistory(receivedJson);
    }
    else if (action == "forgotPassword")
    {
        return handleForgotPassword(receivedJson, loginSystem);
    }
    else if (action == "update_user_info")
    {
        return handleUpdateUserInfo(receivedJson);
    }
    else if (action == "get_active_subscriptions")
    {
        return handleGetActiveSubscription(receivedJson);
    }
    else if (action == "check_member")
    {
        return handleCheckMember(receivedJson);
    }

    return {{"status", "error"}, {"message", "Unknown action: " + action}};
}

void handleClient(SOCKET clientSocket)
{
    std::string request;
    char buffer[4096];
    int bytesReceived;

    while ((bytesReceived = recv(clientSocket, buffer, sizeof(buffer) - 1, 0)) > 0)
    {
        buffer[bytesReceived] = '\0';
        request += buffer;
        size_t newlinePos = request.find('\n');
        if (newlinePos != std::string::npos)
        {
            request = request.substr(0, newlinePos);
            break;
        }
    }

    if (request.empty())
    {
        std::cerr << "Error: No data received from client" << std::endl;
        closesocket(clientSocket);
        return;
    }

    try
    {
        std::cout << "Received request: " << request << std::endl;
        json receivedJson = json::parse(request);
        json response = processRequest(receivedJson);
        sendResponse(clientSocket, response);
    }
    catch (const json::parse_error &e)
    {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        sendResponse(clientSocket, {{"status", "error"}, {"message", "Invalid JSON"}});
    }
    catch (const std::exception &e)
    {
        std::cerr << "General Error: " << e.what() << std::endl;
        sendResponse(clientSocket, {{"status", "error"}, {"message", "Server error"}});
    }

    closesocket(clientSocket);
}

int main()
{
    subscriptionManager.loadFromFile();

    WSADATA wsData;
    if (WSAStartup(MAKEWORD(2, 2), &wsData) != 0)
    {
        std::cerr << "Winsock initialization failed!" << std::endl;
        return 1;
    }

    SOCKET listening = socket(AF_INET, SOCK_STREAM, 0);
    if (listening == INVALID_SOCKET)
    {
        std::cerr << "Socket creation failed!" << std::endl;
        WSACleanup();
        return 1;
    }

    sockaddr_in hint{};
    hint.sin_family = AF_INET;
    hint.sin_port = htons(PORT);
    hint.sin_addr.S_un.S_addr = INADDR_ANY;
    if (bind(listening, (sockaddr *)&hint, sizeof(hint)) == SOCKET_ERROR)
    {
        std::cerr << "Binding failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    if (listen(listening, SOMAXCONN) == SOCKET_ERROR)
    {
        std::cerr << "Listening failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    std::cout << "Server is running on port " << PORT << "...\n";

    while (true)
    {
        SOCKET client = accept(listening, nullptr, nullptr);
        if (client == INVALID_SOCKET)
        {
            std::cerr << "Client connection failed!" << std::endl;
            continue;
        }
        std::thread(handleClient, client).detach();
    }

    closesocket(listening);
    WSACleanup();
    return 0;
}