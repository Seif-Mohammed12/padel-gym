#include <iostream>
#include <string>
#include <thread>
#include <csignal>
#include <map>

#include "include/json.hpp"

using json = nlohmann::json;

// Platform-specific socket headers
#ifdef _WIN32
#include <winsock2.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#endif

// Global server file descriptor for signal handling
int server_fd = -1;

// Hardcoded user credentials (replace with file or database in production)
std::map<std::string, std::string> users = {
        {"admin", "admin123"},
        {"user", "pass123"}
};

// Signal handler for graceful shutdown
void signalHandler(int signum) {
    if (server_fd >= 0) {
#ifdef _WIN32
        closesocket(server_fd);
#else
        close(server_fd);
#endif
    }
#ifdef _WIN32
    WSACleanup();
#endif
    std::cout << "\nServer shutting down...\n";
    exit(signum);
}

// Read full request dynamically
std::string readFullRequest(int client_socket) {
    std::string buffer;
    char temp[1024];
    int bytes_read;
    while ((bytes_read = recv(client_socket, temp, sizeof(temp) - 1, 0)) > 0) {
        temp[bytes_read] = '\0';
        buffer.append(temp);
        if (buffer.find('\n') != std::string::npos || buffer.find('}') != std::string::npos) {
            break;
        }
    }
    if (bytes_read < 0) {
        std::cerr << "Error reading from socket\n";
    }
    if (buffer.empty()) {
        std::cerr << "Received empty buffer\n";
    } else {
        std::cout << "Raw buffer: '" << buffer << "'\n";
    }
    return buffer;
}

// Process client requests (login)
json processRequest(const json& request) {
    json response;
    if (!request.contains("action")) {
        response["status"] = "error";
        response["message"] = "Missing action field";
        return response;
    }

    std::string action = request["action"];
    if (action == "login") {
        if (!request.contains("username") || !request.contains("password")) {
            response["status"] = "error";
            response["message"] = "Missing username or password field";
            return response;
        }
        std::string username = request["username"];
        std::string password = request["password"];

        auto it = users.find(username);
        if (it != users.end() && it->second == password) {
            response["status"] = "success";
            response["message"] = "Login successful for " + username;
        } else {
            response["status"] = "error";
            response["message"] = "Invalid username or password";
        }
    } else {
        response["status"] = "error";
        response["message"] = "Unknown action";
    }
    return response;
}

// Handle individual client connection
void handleClient(int client_socket) {
    struct timeval timeout;
    timeout.tv_sec = 5;
    timeout.tv_usec = 0;
    setsockopt(client_socket, SOL_SOCKET, SO_RCVTIMEO, (char*)&timeout, sizeof(timeout));

    std::string buffer = readFullRequest(client_socket);
    if (buffer.empty()) {
        json error = {{"status", "error"}, {"message", "Empty request received"}};
        send(client_socket, error.dump().c_str(), error.dump().size(), 0);
    } else {
        try {
            buffer.erase(std::remove_if(buffer.begin(), buffer.end(), isspace), buffer.end());
            json request = json::parse(buffer);
            std::cout << "Parsed JSON: " << request.dump() << "\n";

            json response = processRequest(request);
            std::string response_str = response.dump();
            send(client_socket, response_str.c_str(), response_str.size(), 0);
        } catch (const std::exception& e) {
            std::cerr << "JSON error: " << e.what() << "\n";
            json error = {{"status", "error"}, {"message", "Invalid JSON: " + std::string(e.what())}};
            send(client_socket, error.dump().c_str(), error.dump().size(), 0);
        }
    }

#ifdef _WIN32
    closesocket(client_socket);
#else
    close(client_socket);
#endif
}

int main() {
    signal(SIGINT, signalHandler);

#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        std::cerr << "WSAStartup failed\n";
        return 1;
    }
#endif

    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        std::cerr << "Socket creation failed\n";
        return 1;
    }

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(8080);

    if (bind(server_fd, (sockaddr*)&address, sizeof(address)) < 0) {
        std::cerr << "Bind failed\n";
        return 1;
    }

    if (listen(server_fd, 10) < 0) {
        std::cerr << "Listen failed\n";
        return 1;
    }

    std::cout << "C++ Server running on port 8080...\n";

    while (true) {
        int client_socket;
#ifdef _WIN32
        int addrlen = sizeof(address);
        client_socket = accept(server_fd, (sockaddr*)&address, &addrlen);
#else
        client_socket = accept(server_fd, nullptr, nullptr);
#endif

        if (client_socket < 0) {
            std::cerr << "Accept failed\n";
            continue;
        }

        std::thread client_thread(handleClient, client_socket);
        client_thread.detach();
    }

    return 0;
}