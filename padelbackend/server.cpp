#include <winsock2.h>
#include <ws2tcpip.h>
#include <iostream>
#include <string>
#include <thread>
#include <sstream>
#include <vector>
#include "include/json.hpp"
#include "FileManager.h"
#include "GymClass.h"

#pragma comment(lib, "ws2_32.lib")

using json = nlohmann::json;

const int PORT = 8080;

// --- Utility Functions ---

/**
 * Splits a string by a delimiter into a vector of trimmed strings.
 * @param str The string to split.
 * @param delimiter The character to split on.
 * @return A vector of trimmed substrings.
 */
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

/**
 * Sends a JSON response to the client.
 * @param clientSocket The socket to send the response to.
 * @param response The JSON response to send.
 */
void sendResponse(SOCKET clientSocket, const json& response) {
    std::string responseStr = response.dump();
    std::cout << "Sending response: " << responseStr << std::endl;
    int sendResult = send(clientSocket, responseStr.c_str(), responseStr.size(), 0);
    if (sendResult == SOCKET_ERROR) {
        std::cerr << "Error sending response: " << WSAGetLastError() << std::endl;
    }
}

// --- Data Management Functions ---

/**
 * Loads gym classes from a JSON file into a vector of GymClass objects.
 * @param filename The name of the file to load from.
 * @return A vector of GymClass objects.
 */
std::vector<GymClass> loadGymClasses(const std::string& filename) {
    std::vector<GymClass> classes;
    json jsonClasses = FileManager::load(filename);
    for (const auto& j : jsonClasses) {
        classes.push_back(GymClass::fromJson(j));
    }
    return classes;
}

/**
 * Saves a vector of GymClass objects to a JSON file.
 * @param classes The vector of GymClass objects to save.
 * @param filename The name of the file to save to.
 */
void saveGymClasses(const std::vector<GymClass>& classes, const std::string& filename) {
    json jsonClasses = json::array();
    for (const auto& gymClass : classes) {
        jsonClasses.push_back(gymClass.toJson());
    }
    FileManager::save(jsonClasses, filename);
}

// --- Request Handlers ---

/**
 * Handles a signup request by adding a new user to data.json.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
json handleSignup(const json& receivedJson) {
    json users = FileManager::load("data.json");
    json newUser = {
            {"firstName", receivedJson.value("firstName", "")},
            {"lastName", receivedJson.value("lastName", "")},
            {"username", receivedJson.value("username", "")},
            {"password", receivedJson.value("password", "")}
    };

    if (newUser["firstName"].get<std::string>().empty() ||
        newUser["lastName"].get<std::string>().empty() ||
        newUser["username"].get<std::string>().empty() ||
        newUser["password"].get<std::string>().empty()) {
        return {
                {"status", "error"},
                {"message", "All signup fields (firstName, lastName, username, password) are required"}
        };
    }

    users.push_back(newUser);
    FileManager::save(users, "data.json");
    return {
            {"status", "success"},
            {"message", "Data saved"}
    };
}

/**
 * Handles a login request by returning the user data.
 * @return The JSON response containing user data.
 */
json handleLogin() {
    return FileManager::load("data.json");
}

/**
 * Handles a request to get all gym classes.
 * @return The JSON response containing an array of gym classes.
 */
json handleGetClasses() {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json jsonClasses = json::array();
    for (const auto& gymClass : classes) {
        jsonClasses.push_back(gymClass.toJson());
    }
    return jsonClasses;
}

/**
 * Handles a request to save a new gym class.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
json handleSaveGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json classData = receivedJson["data"];

    std::cout << "Received class data: " << classData.dump() << std::endl;

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

    std::cout << "Saved class data: " << newClass.toJson().dump() << std::endl;

    json responseData = newClass.toJson();
    responseData["image"] = responseData["imagePath"];
    responseData.erase("imagePath");

    return {
            {"status", "success"},
            {"message", "Gym class saved"},
            {"data", responseData}
    };
}

/**
 * Handles a request to book a gym class.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
json handleBookGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json data = receivedJson["data"];
    std::string className = data["className"].get<std::string>();
    int memberId = data["memberId"].get<int>();

    bool classFound = false;
    bool booked = false;
    for (auto& gymClass : classes) {
        if (gymClass.getName() == className) {
            classFound = true;
            booked = gymClass.bookClass(memberId);
            break;
        }
    }

    if (!classFound) {
        return {{"status", "error"}, {"message", "Gym class not found"}};
    }

    saveGymClasses(classes, "gym-classes.json");
    return {
            {"status", "success"},
            {"message", booked ? "Successfully booked class" : "Added to waitlist"}
    };
}

/**
 * Handles a request to cancel a gym class booking.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
json handleCancelGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json data = receivedJson["data"];
    std::string className = data["className"].get<std::string>();
    int memberId = data["memberId"].get<int>();

    bool classFound = false;
    bool removed = false;
    for (auto& gymClass : classes) {
        if (gymClass.getName() == className) {
            classFound = true;
            removed = gymClass.removeMember(memberId);
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

/**
 * Handles a request to save a new padel center.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
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

/**
 * Handles a request to update an existing padel center.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
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

/**
 * Handles a request to update an existing gym class.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
json handleUpdateGymClass(const json& receivedJson) {
    std::vector<GymClass> classes = loadGymClasses("gym-classes.json");
    json oldData = receivedJson["oldData"];
    json newData = receivedJson["newData"];

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

    GymClass updatedClass(
            newData["name"].get<std::string>(),
            newData["instructor"].get<std::string>(),
            newData["time"].get<std::string>(),
            newData["capacity"].get<int>(),
            newData["image"].get<std::string>()
    );

    std::vector<int> participants = newData.value("participants", std::vector<int>{});
    for (int id : participants) {
        updatedClass.bookClass(id);
    }
    std::vector<int> waitlistVec = newData.value("waitlist", std::vector<int>{});
    for (int id : waitlistVec) {
        updatedClass.bookClass(id);
    }

    std::vector<GymClass> newClasses;
    bool found = false;
    for (const auto& gymClass : classes) {
        if (gymClass.toJson() == oldData) {
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

/**
 * Handles a request to get all padel centers.
 * @return The JSON response containing an array of padel centers.
 */
json handleGetPadelCenters() {
    return FileManager::load("padel-classes.json");
}

/**
 * Handles a request to delete a padel center.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
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

/**
 * Handles a request to delete a gym class.
 * @param receivedJson The JSON request data.
 * @return The JSON response.
 */
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

/**
 * Processes a client request and returns the appropriate response.
 * @param receivedJson The parsed JSON request.
 * @return The JSON response.
 */
json processRequest(const json& receivedJson) {
    std::string action = receivedJson["action"].get<std::string>();
    std::cout << "Processing action: " << action << std::endl;

    if (action == "signup") {
        return handleSignup(receivedJson);
    } else if (action == "login") {
        return handleLogin();
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
    }

    return {
            {"status", "error"},
            {"message", "Unknown action: " + action}
    };
}

/**
 * Handles communication with a connected client.
 * @param clientSocket The socket for the client connection.
 */
void handleClient(SOCKET clientSocket) {
    std::string request;
    char buffer[4096];
    int bytesReceived;

    // Receive request from client
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
        std::cout << "Successfully parsed JSON" << std::endl;

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

/**
 * Main function to start the server and handle incoming connections.
 */
int main() {
    // Initialize Winsock
    WSADATA wsData;
    if (WSAStartup(MAKEWORD(2, 2), &wsData) != 0) {
        std::cerr << "Winsock initialization failed!" << std::endl;
        return 1;
    }

    // Create listening socket
    SOCKET listening = socket(AF_INET, SOCK_STREAM, 0);
    if (listening == INVALID_SOCKET) {
        std::cerr << "Socket creation failed!" << std::endl;
        WSACleanup();
        return 1;
    }

    // Bind socket
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

    // Start listening
    if (listen(listening, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "Listening failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    std::cout << "Server is running on port " << PORT << "...\n";

    // Accept client connections
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