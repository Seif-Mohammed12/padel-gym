#include <winsock2.h>
#include <ws2tcpip.h>
#include <iostream>
#include <string>
#include <thread>
#include "include/json.hpp"
#include "FileManager.h" // Make sure you have this file to handle the saving

#pragma comment(lib, "ws2_32.lib")

using json = nlohmann::json;

const int PORT = 8080;

void handleClient(SOCKET clientSocket) {
    char buffer[4096];
    int bytesReceived = recv(clientSocket, buffer, sizeof(buffer), 0);

    if (bytesReceived > 0) {
        std::string request(buffer, bytesReceived);  // Convert buffer to string
        try {
            // Parse the received JSON request
            json receivedJson = json::parse(request);

            // Save the received JSON data using FileManager (no additional logic here)
            FileManager::save(receivedJson, "data.json");

            // Respond back with a success message
            std::string successResponse = R"({"status":"success","message":"User  created successfully"})";
            send(clientSocket, successResponse.c_str(), successResponse.size(), 0);
        } catch (const json::parse_error& e) {
            // If JSON parsing fails, send error response
            std::string errorResponse = R"({"status":"error","message":"Invalid JSON"})";
            send(clientSocket, errorResponse.c_str(), errorResponse.size(), 0);
        }
    }

    // Close the client socket after processing
    closesocket(clientSocket);
}

int main() {
    // Initialize Winsock
    WSADATA wsData;
    if (WSAStartup(MAKEWORD(2, 2), &wsData) != 0) {
        std::cerr << "Winsock initialization failed!" << std::endl;
        return 1;
    }

    // Create socket for listening
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

    // Bind socket to a port
    if (bind(listening, (sockaddr*)&hint, sizeof(hint)) == SOCKET_ERROR) {
        std::cerr << "Binding failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    // Start listening for incoming connections
    if (listen(listening, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "Listening failed!" << std::endl;
        closesocket(listening);
        WSACleanup();
        return 1;
    }

    std::cout << "Server is running on port " << PORT << "...\n";

    // Accept and handle client connections
    while (true) {
        SOCKET client = accept(listening, nullptr, nullptr);
        if (client == INVALID_SOCKET) {
            std::cerr << "Client connection failed!" << std::endl;
            continue;  // Try accepting next client
        }

        // Create a new thread to handle the client
        std::thread(handleClient, client).detach();
    }

    // Cleanup and close the listening socket
    closesocket(listening);
    WSACleanup();
    return 0;
}