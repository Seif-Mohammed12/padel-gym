#include <winsock2.h>
#include <ws2tcpip.h>
#include <iostream>
#include <string>
#include <thread>
#include "include/json.hpp"
#include "FileManager.h"

#pragma comment(lib, "ws2_32.lib")

using json = nlohmann::json;

const int PORT = 8080;

void handleClient(SOCKET clientSocket) {
    std::string request;
    char buffer[4096];
    int bytesReceived;

    // Receive request until newline
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

        // Parse the incoming JSON request
        json receivedJson = json::parse(request);
        std::cout << "Successfully parsed JSON" << std::endl;

        std::string action = receivedJson["action"].get<std::string>();
        json response;

        if (action == "signup") {
            // For signup, append the entire receivedJson (minus the action) to data.json
            json users = FileManager::load("data.json");
            // Create a new user object with the fields directly from receivedJson
            json newUser = {
                    {"firstName", receivedJson.value("firstName", "")},
                    {"lastName", receivedJson.value("lastName", "")},
                    {"username", receivedJson.value("username", "")},
                    {"password", receivedJson.value("password", "")}
            };
            users.push_back(newUser);
            FileManager::save(users, "data.json");
            response = {
                    {"status", "success"},
                    {"message", "Data saved"}
            };
        } else if (action == "login") {
            // For login, send the contents of data.json
            response = FileManager::load("data.json");
        } else if (action == "get_classes") {
            // For get_classes, send the contents of gym-classes.json
            response = FileManager::load("gym-classes.json");
        } else {
            // For any other action, echo the request back to the client
            response = receivedJson;
        }

        // Send the response back to the client
        std::string responseStr = response.dump();
        std::cout << "Sending response: " << responseStr << std::endl;
        int sendResult = send(clientSocket, responseStr.c_str(), responseStr.size(), 0);
        if (sendResult == SOCKET_ERROR) {
            std::cerr << "Error sending response: " << WSAGetLastError() << std::endl;
        }
    } catch (const json::parse_error& e) {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        std::string errorResponse = R"({"status":"error","message":"Invalid JSON"})";
        send(clientSocket, errorResponse.c_str(), errorResponse.size(), 0);
    } catch (const std::exception& e) {
        std::cerr << "General Error: " << e.what() << std::endl;
        std::string errorResponse = R"({"status":"error","message":"Server error"})";
        send(clientSocket, errorResponse.c_str(), errorResponse.size(), 0);
    }

    closesocket(clientSocket);
}

int main() {
    // Initialize Winsock
    WSADATA wsData;
    if (WSAStartup(MAKEWORD(2, 2), &wsData) != 0) {
        std::cerr << "Winsock initialization failed!" << std::endl;
        return 1;
    }

    // Create socket
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

    // Listen for connections
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