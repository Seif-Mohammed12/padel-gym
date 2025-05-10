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

// --- Global Instances ---
GymSystem gymSystem;
SubscriptionManager subscriptionManager;
WorkoutHistory workoutHistory;
Registration registration;
LoginSystem loginSystem;

// --- Utility Functions ---
std::vector<std::string> split(const std::string& str, char delimiter) {
    std::vector<std::string> tokens;
    std::string token;
    std::istringstream tokenStream(str);
    while (std::getline(tokenStream, token, delimiter)) {
        token.erase(0, token.find_first_not_of(" \t"));
        token.erase(token.find_last_not_of(" \t") + 1);
        if (!token.empty()) {
            tokens.push_back(token);
        }
    }
    return tokens;
}

void sendResponse(SOCKET clientSocket, const json& response) {
    std::string responseStr = response.dump();
    std::cout << "Sending response: " << responseStr << std::endl;
    int sendResult = send(clientSocket, responseStr.c_str(), responseStr.size(), 0);
    if (sendResult == SOCKET_ERROR) {
        std::cerr << "Error sending response: " << WSAGetLastError() << std::endl;
    }
    send(clientSocket, "\n", 1, 0);
}

// --- Data Management Functions ---
std::vector<GymClass> loadGymClasses(const std::string& filename) {
    json data = FileManager::load(filename);
    std::vector<GymClass> classes;
    if (data.is_array()) {
        for (const auto& classJson : data) {
            classes.push_back(GymClass::fromJson(classJson));
        }
    }
    return classes;
}

void saveGymClasses(const std::vector<GymClass>& classes, const std::string& filename) {
    json data = json::array();
    for (const auto& gymClass : classes) {
        data.push_back(gymClass.toJson());
    }
    FileManager::save(data, filename);
}

// --- Request Handlers ---
json handleSignup(const json& receivedJson) {
    try {
        std::string firstName = receivedJson.value("firstName", "");
        std::string lastName = receivedJson.value("lastName", "");
        std::string username = receivedJson.value("username", "");
        std::string phone = receivedJson.value("phoneNumber", "");
        std::string password = receivedJson.value("password", "");
        std::string role = receivedJson.value("role", "user");

        if (firstName.empty() || lastName.empty() || username.empty() || password.empty()) {
            return {{"status", "error"}, {"message", "First name, last name, username, phone and password are required"}};
        }

        std::string memberId, errorMsg;
        bool success = registration.registerUser(
                firstName, lastName, "", phone, username, password, role, memberId, errorMsg
        );

        if (!success) {
            return {{"status", "error"}, {"message", errorMsg}};
        }

        return {
                {"status", "success"},
                {"message", "User registered successfully"},
                {"data", {{"memberId", memberId}}}
        };
    } catch (const json::parse_error& e) {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    } catch (const std::exception& e) {
        std::cerr << "Signup Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Failed to register user: " + std::string(e.what())}};
    }
}


json handleLogin(const json& receivedJson) {
    std::string username = receivedJson.value("username", "");
    std::string password = receivedJson.value("password", "");

    if (username.empty() || password.empty()) {
        return {{"status", "error"}, {"message", "Username and password are required"}};
    }

    LoginSystem loginSystem;

    // Authenticate user using the LoginSystem class
    if (!loginSystem.authenticate(username, password)) {
        return {{"status", "error"}, {"message", "Invalid username or password"}};
    }

    // Retrieve user data after successful authentication
    json users = loginSystem.loadUserData();
    json loggedInUser;

    for (const auto& user : users) {
        if (user["username"] == username) {
            loggedInUser = user;
            break;
        }
    }

    return {
            {"status", "success"},
            {"message", "Login successful"},
            {"data", {
                               {"username", loggedInUser["username"]},
                               {"memberId", loggedInUser["memberId"]},
                               {"firstName", loggedInUser["firstName"]},
                               {"lastName", loggedInUser["lastName"]},
                               {"role", loggedInUser["role"]},
                               {"phoneNumber", loggedInUser["phoneNumber"]},
                               {"email", loggedInUser["email"]}
                       }}
    };
}

json handleForgotPassword(const json& receivedJson, LoginSystem& loginSystem) {
    try {
        string username = receivedJson.value("username", "");

        if (username.empty()) {
            return {{"status", "error"}, {"message", "Username is required"}};
        }

        string password = loginSystem.handleForgotPassword(username);
        if (password.empty()) {
            return {{"status", "error"}, {"message", "Username not found"}};
        }

        return {
                {"status", "success"},
                {"message", "Password recovered successfully"},
                {"password", password}
        };
    } catch (const json::parse_error& e) {
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    } catch (const exception& e) {
        return {{"status", "error"}, {"message", "Failed to process request: " + string(e.what())}};
    }
}

// New Handler for Updating User Info
json handleUpdateUserInfo(const json& receivedJson) {
    try {
        // Extract required fields from the request
        std::string memberId = receivedJson.value("memberId", "");
        std::string firstName = receivedJson.value("firstName", "");
        std::string lastName = receivedJson.value("lastName", "");
        std::string email = receivedJson.value("email", "");
        std::string phoneNumber = receivedJson.value("phoneNumber", "");
        std::string username = receivedJson.value("username", "");

        // Validate required fields
        if (memberId.empty()) {
            return {{"status", "error"}, {"message", "memberId is required"}};
        }
        if (firstName.empty() || lastName.empty() || phoneNumber.empty() || username.empty()) {
            return {{"status", "error"}, {"message", "First name, last name, phone number, and username are required"}};
        }

        // Load user data from data.json (assuming this is where user info is stored)
        json users = FileManager::load("data.json");
        bool userFound = false;

        // Update the user in data.json
        for (auto& user : users) {
            if (user["memberId"].get<std::string>() == memberId) {
                user["firstName"] = firstName;
                user["lastName"] = lastName;
                user["email"] = email;
                user["phoneNumber"] = phoneNumber;
                user["username"] = username;
                userFound = true;
                break;
            }
        }

        if (!userFound) {
            return {{"status", "error"}, {"message", "User with memberId " + memberId + " not found"}};
        }

        // Save the updated user data
        FileManager::save(users, "data.json");

        return {
                {"status", "success"},
                {"message", "User information updated successfully"},
                {"data", {
                                   {"memberId", memberId},
                                   {"firstName", firstName},
                                   {"lastName", lastName},
                                   {"email", email},
                                   {"phoneNumber", phoneNumber},
                                   {"username", username}
                           }}
        };
    } catch (const json::parse_error& e) {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Invalid JSON format"}};
    } catch (const std::exception& e) {
        std::cerr << "Update User Info Error: " << e.what() << std::endl;
        return {{"status", "error"}, {"message", "Failed to update user info: " + std::string(e.what())}};
    }
}


json handleGetClasses() {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json jsonClasses = json::array();
    for (const auto& gymClass : classes) {
        jsonClasses.push_back(gymClass.toJson());
    }
    return {{"status", "success"}, {"data", jsonClasses}};
}

json handleSaveGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json classData = receivedJson["data"];

    if (!classData.contains("name") || classData["name"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Class name is required"}};
    }
    if (!classData.contains("instructor") || classData["instructor"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Instructor is required"}};
    }
    if (!classData.contains("time") || classData["time"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Time is required"}};
    }
    if (!classData.contains("image") || classData["image"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!classData.contains("capacity") || !classData["capacity"].is_number_integer() || classData["capacity"].get<int>() <= 0) {
        return {{"status", "error"}, {"message", "Capacity must be a positive integer"}};
    }

    GymClass newClass(
            classData["name"].get<std::string>(),
            classData["instructor"].get<std::string>(),
            classData["time"].get<std::string>(),
            classData["capacity"].get<int>(),
            classData["image"].get<std::string>()
    );
    classes.push_back(newClass);
    saveGymClasses(classes, "gym-classes.json");

    json responseData = newClass.toJson();
    responseData["image"] = responseData["imagePath"];
    responseData.erase("imagePath");

    return {
            {"status", "success"},
            {"message", "Gym class saved"},
            {"data", responseData}
    };
}

json handleBookGymClass(const json& receivedJson) {
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];
    std::string className = data["className"].get<std::string>();
    std::string memberId;

    if (data.contains("memberId") && data["memberId"].is_string()) {
        memberId = data["memberId"].get<std::string>();
    } else if (data.contains("username")) {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto& user : users) {
            if (user["username"] == username) {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    }

    if (memberId.empty()) {
        return {{"status", "error"}, {"message", "memberId or username required"}};
    }

    // Check if member is active
    json members = FileManager::load("members.json");
    bool isActive = true;
    bool memberFound = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberFound = true;
            isActive = member.value("isActive", true);
            break;
        }
    }
    if (!memberFound) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive) {
        return {{"status", "error"}, {"message", "Cannot book class for inactive member"}};
    }

    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    bool classFound = false;
    bool booked = false;
    std::string instructor;
    for (auto& gymClass : classes) {
        if (gymClass.getName() == className) {
            classFound = true;
            booked = gymClass.bookClass(memberId); // Updated to string
            instructor = gymClass.toJson().value("instructor", "Unknown");
            break;
        }
    }

    if (!classFound) {
        return {{"status", "error"}, {"message", "Gym class not found"}};
    }

    saveGymClasses(classes, "gym-classes.json");

    if (booked) {
        try {
            workoutHistory.addWorkout(memberId, className, "2025-05-05", instructor); // Updated to string
        } catch (const std::exception& e) {
            return {{"status", "error"}, {"message", "Failed to add workout: " + std::string(e.what())}};
        }
    }

    return {
            {"status", "success"},
            {"message", booked ? "Successfully booked class" : "Added to waitlist"},
            {"waitlisted", !booked}
    };
}

json handleCancelGymClass(const json& receivedJson) {
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];
    std::string className = data["className"].get<std::string>();
    std::string memberId = data["memberId"].get<std::string>(); // Updated to string

    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    bool classFound = false;
    bool removed = false;
    for (auto& gymClass : classes) {
        if (gymClass.getName() == className) {
            classFound = true;
            removed = gymClass.removeMember(memberId); // Updated to string
            break;
        }
    }

    if (!classFound) {
        return {{"status", "error"}, {"message", "Gym class not found"}};
    }
    if (!removed) {
        return {{"status", "error"}, {"message", "Member not found in class or waitlist"}};
    }

    saveGymClasses(classes, "gym-classes.json");
    return {{"status", "success"}, {"message", "Successfully canceled booking"}};
}

json handleSavePadelCenter(const json& receivedJson) {
    json centers = FileManager::load("padel-classes.json");
    json centerData = receivedJson["data"];

    if (!centerData.contains("name") || centerData["name"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Center name is required"}};
    }
    if (!centerData.contains("times") || centerData["times"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Available times are required"}};
    }
    if (!centerData.contains("image") || centerData["image"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!centerData.contains("location") || centerData["location"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Location is required"}};
    }

    std::string timesStr = centerData["times"].get<std::string>();
    std::vector<std::string> timesList = split(timesStr, ',');
    if (timesList.empty()) {
        return {{"status", "error"}, {"message", "At least one available time is required"}};
    }

    json timesArray = json::array();
    for (const auto& time : timesList) {
        timesArray.push_back(time);
    }
    centerData.erase("times");
    centerData["availableTimes"] = timesArray;

    centers.push_back(centerData);
    FileManager::save(centers, "padel-classes.json");
    return {
            {"status", "success"},
            {"message", "Padel center saved"},
            {"data", centerData}
    };
}

json handleUpdatePadelCenter(const json& receivedJson) {
    json centers = FileManager::load("padel-classes.json");
    json oldData = receivedJson["oldData"];
    json newData = receivedJson["newData"];

    if (!newData.contains("name") || newData["name"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Center name is required"}};
    }
    if (!newData.contains("times") || newData["times"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Available times are required"}};
    }
    if (!newData.contains("image") || newData["image"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!newData.contains("location") || newData["location"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Location is required"}};
    }

    std::string timesStr = newData["times"].get<std::string>();
    std::vector<std::string> timesList = split(timesStr, ',');
    if (timesList.empty()) {
        return {{"status", "error"}, {"message", "At least one available time is required"}};
    }

    json timesArray = json::array();
    for (const auto& time : timesList) {
        timesArray.push_back(time);
    }
    newData.erase("times");
    newData["availableTimes"] = timesArray;

    json newCenters = json::array();
    bool found = false;
    for (const auto& center : centers) {
        if (center == oldData) {
            found = true;
        } else {
            newCenters.push_back(center);
        }
    }

    if (!found) {
        return {{"status", "error"}, {"message", "Padel center to update not found"}};
    }

    newCenters.push_back(newData);
    FileManager::save(newCenters, "padel-classes.json");
    return {
            {"status", "success"},
            {"message", "Padel center updated"},
            {"data", newData}
    };
}

json handleUpdateGymClass(const json& receivedJson) {
    if (!receivedJson.contains("oldData") || !receivedJson.contains("newData") ||
        receivedJson["oldData"].is_null() || receivedJson["newData"].is_null()) {
        return {{"status", "error"}, {"message", "oldData and newData are required"}};
    }

    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json oldData = receivedJson["oldData"];
    json newData = receivedJson["newData"];

    // Validate required fields in newData
    if (!newData.contains("name") || newData["name"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Class name is required"}};
    }
    if (!newData.contains("instructor") || newData["instructor"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Instructor is required"}};
    }
    if (!newData.contains("time") || newData["time"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Time is required"}};
    }
    if (!newData.contains("image") || newData["image"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "Image is required"}};
    }
    if (!newData.contains("capacity") || !newData["capacity"].is_number_integer() || newData["capacity"].get<int>() <= 0) {
        return {{"status", "error"}, {"message", "Capacity must be a positive integer"}};
    }

    // Create updated class
    GymClass updatedClass(
            newData["name"].get<std::string>(),
            newData["instructor"].get<std::string>(),
            newData["time"].get<std::string>(),
            newData["capacity"].get<int>(),
            newData["image"].get<std::string>()
    );

    // Update participants and waitlist with string member IDs
    std::vector<std::string> participants = newData.value("participants", std::vector<std::string>{});
    for (const std::string& id : participants) {
        updatedClass.bookClass(id); // Updated to string
    }
    std::vector<std::string> waitlistVec = newData.value("waitlist", std::vector<std::string>{});
    for (const std::string& id : waitlistVec) {
        updatedClass.bookClass(id); // Updated to string
    }

    std::vector<GymClass> newClasses;
    bool found = false;
    std::string oldClassName = oldData.value("name", "");
    if (oldClassName.empty()) {
        return {{"status", "error"}, {"message", "Old class name is required in oldData"}};
    }

    for (const auto& gymClass : classes) {
        if (gymClass.getName() == oldClassName) {
            found = true;
        } else {
            newClasses.push_back(gymClass);
        }
    }

    if (!found) {
        return {{"status", "error"}, {"message", "Gym class to update not found"}};
    }

    newClasses.push_back(updatedClass);
    saveGymClasses(newClasses, "gym-classes.json");

    json responseData = updatedClass.toJson();
    responseData["image"] = responseData["imagePath"];
    responseData.erase("imagePath");

    return {
            {"status", "success"},
            {"message", "Gym class updated"},
            {"data", responseData}
    };
}

json handleGetPadelCenters() {
    json centers = FileManager::load("padel-classes.json");
    return {{"status", "success"},{"data", centers}};
}

json handleDeletePadelCenter(const json& receivedJson) {
    json centers = FileManager::load("padel-classes.json");
    json centerToDelete = receivedJson["data"];
    json newCenters = json::array();

    for (const auto& center : centers) {
        if (center != centerToDelete) {
            newCenters.push_back(center);
        }
    }

    FileManager::save(newCenters, "padel-classes.json");
    return {{"status", "success"}, {"message", "Padel center deleted"}};
}

json handleDeleteGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json classToDelete = receivedJson["data"];
    std::vector<GymClass> newClasses;

    for (const auto& gymClass : classes) {
        if (gymClass.toJson() != classToDelete) {
            newClasses.push_back(gymClass);
        }
    }

    saveGymClasses(newClasses, "gym-classes.json");
    return {{"status", "success"}, {"message", "Gym class deleted"}};
}

json handleGetActiveSubscription(const json& receivedJson) {
    // Validate that memberId exists
    if (!receivedJson.contains("memberId")) {
        return {{"status", "error"}, {"message", "memberId is required"}};
    }

    // Extract memberId as string (handle both int and string types)
    std::string memberId;
    try {
        if (receivedJson["memberId"].is_number()) {
            memberId = std::to_string(receivedJson["memberId"].get<int>());
        } else if (receivedJson["memberId"].is_string()) {
            memberId = receivedJson["memberId"].get<std::string>();
        } else {
            return {{"status", "error"}, {"message", "Invalid memberId format"}};
        }
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Error parsing memberId: " + std::string(e.what())}};
    }

    // Load subscriptions file
    json allSubs;
    try {
        allSubs = FileManager::load("active-subscriptions.json");
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Could not load subscriptions file: " + std::string(e.what())}};
    }

    // Filter subscriptions for the given memberId and isActive
    json activeSubs = json::array();
    for (const auto& sub : allSubs) {
        try {
            std::string subMemberId;
            if (sub["memberId"].is_number()) {
                subMemberId = std::to_string(sub["memberId"].get<int>());
            } else {
                subMemberId = sub["memberId"].get<std::string>();
            }

            if (subMemberId == memberId && sub.value("isActive", true)) {
                activeSubs.push_back(sub);
            }
        } catch (...) {
            // Skip malformed entries
            continue;
        }
    }

    return {{"status", "success"}, {"data", activeSubs}};
}


json handleGetSubscriptionPlans() {
    json plans = FileManager::load("subscriptions.json");
    return {{"status", "success"}, {"data", plans}};
}

json handleSubscribePlan(const json& receivedJson) {
    // Validate the presence of the "data" object
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    // Validate required fields: planName and duration
    if (!data.contains("planName") || data["planName"].get<std::string>().empty() ||
        !data.contains("duration") || data["duration"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "planName and duration are required"}};
    }

    // Retrieve memberId (as a string)
    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string()) {
        memberId = data["memberId"].get<std::string>();
    } else if (data.contains("username")) {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto& user : users) {
            if (user["username"] == username) {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    } else {
        return {{"status", "error"}, {"message", "memberId or username required"}};
    }

    // Validate memberId
    if (memberId.empty()) {
        return {{"status", "error"}, {"message", "Invalid memberId or username: user not found"}};
    }

    // Check if the member exists in members.json and is active
    json members = FileManager::load("members.json");
    bool memberExists = false;
    bool isActive = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            isActive = member.value("isActive", true);
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive) {
        return {{"status", "error"}, {"message", "Cannot subscribe: member is inactive"}};
    }

    // Extract plan details
    std::string planName = data["planName"].get<std::string>();
    std::string duration = data["duration"].get<std::string>();

    // Load subscription plans
    json plans = FileManager::load("subscriptions.json");
    json selectedPlan;
    bool planFound = false;
    for (const auto& plan : plans) {
        if (plan["name"] == planName) {
            selectedPlan = plan;
            planFound = true;
            break;
        }
    }

    if (!planFound) {
        return {{"status", "error"}, {"message", "Plan '" + planName + "' not found"}};
    }

    // Validate duration and get price
    if (!selectedPlan["pricing"].contains(duration) || selectedPlan["pricing"][duration].is_null()) {
        return {{"status", "error"}, {"message", "Invalid duration '" + duration + "' for plan '" + planName + "'"}};
    }
    double price = selectedPlan["pricing"][duration].get<double>();

    // Manage subscription via SubscriptionManager
    Subscription* existingSub = subscriptionManager.findSubscription(memberId);
    if (existingSub) {
        existingSub->renew(planName, duration, price);
    } else {
        Subscription newSub(memberId, planName, duration, price);
        subscriptionManager.addSubscription(newSub);
    }

    // Update subscribers list in subscriptions.json
    for (auto& plan : plans) {
        if (plan["name"] == planName) {
            json subscribers = plan.value("subscribers", json::array());
            // Ensure memberId is stored as a string
            if (std::find(subscribers.begin(), subscribers.end(), memberId) == subscribers.end()) {
                subscribers.push_back(memberId);
                plan["subscribers"] = subscribers;
            }
        }
    }
    FileManager::save(plans, "subscriptions.json");

    // Update member's subscription in GymSystem
    try {
        gymSystem.renewSubscription(memberId, duration);
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to update member subscription: " + std::string(e.what())}};
    }

    return {{"status", "success"}, {"message", "Subscribed successfully"}};
}

json handleAddMember(const json& receivedJson) {
    json data = receivedJson["data"];
    Member m;

    std::string memberId;
    if (data.contains("memberId") && data["memberId"].is_string()) {
        memberId = data["memberId"].get<std::string>();
    } else if (data.contains("username")) {
        json users = FileManager::load("data.json");
        std::string username = data["username"].get<std::string>();
        for (const auto& user : users) {
            if (user["username"] == username) {
                memberId = user["memberId"].get<std::string>();
                break;
            }
        }
    }

    if (memberId.empty()) {
        return {{"status", "error"}, {"message", "memberId or username required and must correspond to a valid user"}};
    }

    // Fetch user info from data.json to populate email and username
    json users = FileManager::load("data.json");
    std::string email, username;
    bool userFound = false;
    for (const auto& user : users) {
        if (user["memberId"].get<std::string>() == memberId) {
            email = user.value("email", "");
            username = user.value("username", "");
            userFound = true;
            break;
        }
    }

    if (!userFound) {
        return {{"status", "error"}, {"message", "User with memberId " + memberId + " not found in user data"}};
    }

    std::string fullName = data["name"].get<std::string>();
    std::vector<std::string> nameParts = split(fullName, ' ');
    std::string firstName = nameParts.empty() ? "" : nameParts[0];
    std::string lastName = nameParts.size() > 1 ? nameParts[1] : "";

    std::string dob = data["dob"].get<std::string>();
    std::string phone = data.value("phoneNumber", "");
    std::string subscriptionStr = data["subscription"].get<std::string>();

    json subscriptionJson = {{"planName", subscriptionStr}};

    if (firstName.empty() || dob.empty() || subscriptionStr.empty()) {
        return {{"status", "error"}, {"message", "Missing or invalid required fields (name, dob, subscription)"}};
    }

    m.addMember(memberId, firstName, lastName, email, phone, username, dob, subscriptionJson);
    gymSystem.addMember(m);

    // Ensure members.json includes isActive and workouts
    json members = FileManager::load("members.json");
    if (!members.is_array()) members = json::array();
    bool found = false;
    for (auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            member["isActive"] = true;
            if (!member.contains("workouts")) member["workouts"] = json::array();
            found = true;
            break;
        }
    }
    if (!found) {
        json newMember = m.toJson();
        newMember["workouts"] = json::array();
        members.push_back(newMember);
    }
    FileManager::save(members, "members.json");

    return {{"status", "success"}, {"message", "Member added successfully"}};
}

json handleAddStaff(const json& receivedJson) {
    try {
        json data = receivedJson["data"];
        Staff s;
        s.addStaff(data["name"].get<std::string>(), data["role"].get<std::string>());
        gymSystem.addStaff(s);
        return {{"status", "success"}, {"message", "Staff added successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to add staff: " + std::string(e.what())}};
    }
}

json handleGetMembers() {
    return {{"status", "success"}, {"data", gymSystem.getMembers()}};
}

json handleGetStaff() {
    return {{"status", "success"}, {"data", gymSystem.getStaff()}};
}

json handleRenewSubscription(const json& receivedJson) {
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    if (!data.contains("memberId") || !data["memberId"].is_string()) {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }
    if (!data.contains("newSubscription") || data["newSubscription"].get<std::string>().empty()) {
        return {{"status", "error"}, {"message", "newSubscription is required"}};
    }

    std::string memberId = data["memberId"].get<std::string>();
    std::string newSub = data["newSubscription"].get<std::string>();

    // Validate member exists and is active
    json members = FileManager::load("members.json");
    bool memberExists = false;
    bool isActive = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            isActive = member.value("isActive", true);
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive) {
        return {{"status", "error"}, {"message", "Cannot renew subscription: member is inactive"}};
    }

    try {
        gymSystem.renewSubscription(memberId, newSub);
        return {{"status", "success"}, {"message", "Subscription renewed successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to renew subscription: " + std::string(e.what())}};
    }
}

json handleCancelMembership(const json& receivedJson) {
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }
    json data = receivedJson["data"];

    if (!data.contains("memberId") || !data["memberId"].is_string()) {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = data["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try {
        gymSystem.cancelMembership(memberId);
        subscriptionManager.cancelSubscription(memberId);
        workoutHistory.clearHistory(memberId);

        // Update isActive in members.json
        for (auto& member : members) {
            if (member["id"].get<std::string>() == memberId) {
                member["isActive"] = false;
                member["workouts"] = json::array();
                break;
            }
        }
        FileManager::save(members, "members.json");

        return {{"status", "success"}, {"message", "Membership canceled successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to cancel membership: " + std::string(e.what())}};
    }
}

// New Workout History Handlers
json handleGetWorkoutHistory(const json& receivedJson) {
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string()) {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = receivedJson["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try {
        json workouts = workoutHistory.getAllWorkouts(memberId);
        return {{"status", "success"}, {"data", workouts}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to get workout history: " + std::string(e.what())}};
    }
}

json handleAddWorkout(const json& receivedJson) {
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string()) {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }
    if (!receivedJson.contains("data") || receivedJson["data"].is_null()) {
        return {{"status", "error"}, {"message", "Data object is required"}};
    }

    json data = receivedJson["data"];
    std::string memberId = receivedJson["memberId"].get<std::string>();
    std::string className = data.value("className", "");
    std::string date = data.value("date", "");
    std::string instructor = data.value("instructor", "");

    // Validate required fields
    if (className.empty() || date.empty() || instructor.empty()) {
        return {{"status", "error"}, {"message", "className, date, and instructor are required"}};
    }

    // Validate member exists and is active
    json members = FileManager::load("members.json");
    bool memberExists = false;
    bool isActive = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            isActive = member.value("isActive", true);
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }
    if (!isActive) {
        return {{"status", "error"}, {"message", "Cannot add workout: member is inactive"}};
    }

    try {
        workoutHistory.addWorkout(memberId, className, date, instructor);
        return {{"status", "success"}, {"message", "Workout added successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to add workout: " + std::string(e.what())}};
    }
}

json handleClearWorkoutHistory(const json& receivedJson) {
    if (!receivedJson.contains("memberId") || !receivedJson["memberId"].is_string()) {
        return {{"status", "error"}, {"message", "Valid memberId (string) is required"}};
    }

    std::string memberId = receivedJson["memberId"].get<std::string>();

    // Validate member exists
    json members = FileManager::load("members.json");
    bool memberExists = false;
    for (const auto& member : members) {
        if (member["id"].get<std::string>() == memberId) {
            memberExists = true;
            break;
        }
    }

    if (!memberExists) {
        return {{"status", "error"}, {"message", "Member with memberId " + memberId + " not found"}};
    }

    try {
        workoutHistory.clearHistory(memberId);
        return {{"status", "success"}, {"message", "Workout history cleared successfully"}};
    } catch (const std::exception& e) {
        return {{"status", "error"}, {"message", "Failed to clear workout history: " + std::string(e.what())}};
    }
}

json processRequest(const json& receivedJson) {
    std::string action = receivedJson.value("action", "");
    std::cout << "Processing action: " << action << std::endl;

    if (action == "signup") {
        return handleSignup(receivedJson);
    } else if (action == "login") {
        return handleLogin(receivedJson);
    } else if (action == "get_classes") {
        return handleGetClasses();
    } else if (action == "save_gym_class") {
        return handleSaveGymClass(receivedJson);
    } else if (action == "book_gym_class") {
        return handleBookGymClass(receivedJson);
    } else if (action == "cancel_gym_class") {
        return handleCancelGymClass(receivedJson);
    } else if (action == "save_padel_center") {
        return handleSavePadelCenter(receivedJson);
    } else if (action == "update_padel_center") {
        return handleUpdatePadelCenter(receivedJson);
    } else if (action == "update_gym_class") {
        return handleUpdateGymClass(receivedJson);
    } else if (action == "get_padel_centers") {
        return handleGetPadelCenters();
    } else if (action == "delete_padel_center") {
        return handleDeletePadelCenter(receivedJson);
    } else if (action == "delete_gym_class") {
        return handleDeleteGymClass(receivedJson);
    } else if (action == "get_subscription_plans") {
        return handleGetSubscriptionPlans();
    } else if (action == "subscribe") {
        return handleSubscribePlan(receivedJson);
    } else if (action == "add_member") {
        return handleAddMember(receivedJson);
    } else if (action == "add_staff") {
        return handleAddStaff(receivedJson);
    } else if (action == "get_members") {
        return handleGetMembers();
    } else if (action == "get_staff") {
        return handleGetStaff();
    } else if (action == "renew_subscription") {
        return handleRenewSubscription(receivedJson);
    } else if (action == "cancel_membership") {
        return handleCancelMembership(receivedJson);
    } else if (action == "get_workout_history") {
        return handleGetWorkoutHistory(receivedJson);
    } else if (action == "add_workout") {
        return handleAddWorkout(receivedJson);
    } else if (action == "clear_workout_history") {
        return handleClearWorkoutHistory(receivedJson);
    } else if (action == "forgotPassword"){
        return handleForgotPassword(receivedJson, loginSystem);
    } else if (action == "update_user_info") {
        return handleUpdateUserInfo(receivedJson);
    } else if (action == "get_active_subscriptions"){
        return handleGetActiveSubscription(receivedJson);
    }

    return {{"status", "error"}, {"message", "Unknown action: " + action}};
}

void handleClient(SOCKET clientSocket) {
    std::string request;
    char buffer[4096];
    int bytesReceived;

    while ((bytesReceived = recv(clientSocket, buffer, sizeof(buffer) - 1, 0)) > 0) {
        buffer[bytesReceived] = '\0';
        request += buffer;
        size_t newlinePos = request.find('\n');
        if (newlinePos != std::string::npos) {
            request = request.substr(0, newlinePos);
            break;
        }
    }

    if (request.empty()) {
        std::cerr << "Error: No data received from client" << std::endl;
        closesocket(clientSocket);
        return;
    }

    try {
        std::cout << "Received request: " << request << std::endl;
        json receivedJson = json::parse(request);
        json response = processRequest(receivedJson);
        sendResponse(clientSocket, response);
    } catch (const json::parse_error& e) {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        sendResponse(clientSocket, {{"status", "error"}, {"message", "Invalid JSON"}});
    } catch (const std::exception& e) {
        std::cerr << "General Error: " << e.what() << std::endl;
        sendResponse(clientSocket, {{"status", "error"}, {"message", "Server error"}});
    }

    closesocket(clientSocket);
}

int main() {
    subscriptionManager.loadFromFile();

    WSADATA wsData;
    if (WSAStartup(MAKEWORD(2, 2), &wsData) != 0) {
        std::cerr << "Winsock initialization failed!" << std::endl;
        return 1;
    }

    SOCKET listening = socket(AF_INET, SOCK_STREAM, 0);
    if (listening == INVALID_SOCKET) {
        std::cerr << "Socket creation failed!" << std::endl;
        WSACleanup();
        return 1;
    }

    sockaddr_in hint{};
    hint.sin_family = AF_INET;
    hint.sin_port = htons(PORT);
    hint.sin_addr.S_un.S_addr = INADDR_ANY;
    if (bind(listening, (sockaddr*)&hint, sizeof(hint)) == SOCKET_ERROR) {
        std::cerr << "Binding failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    if (listen(listening, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "Listening failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    std::cout << "Server is running on port " << PORT << "...\n";

    while (true) {
        SOCKET client = accept(listening, nullptr, nullptr);
        if (client == INVALID_SOCKET) {
            std::cerr << "Client connection failed!" << std::endl;
            continue;
        }
        std::thread(handleClient, client).detach();
    }

    closesocket(listening);
    WSACleanup();
    return 0;
}