#include "FileManager.h"
#include <fstream>
#include <filesystem>
#include <iostream>

using namespace std;

// Relative paths to the root directory (one level up from cmake-build-debug)
const string DATA_FILE = "../files/data.json";
const string GYM_CLASSES_FILE = "../files/gym-classes.json";
const string PADEL_CLASSES_FILE = "../files/padel-classes.json";
const string SUBSCRIPTIONS_FILE = "../files/subscriptions.json";
const string ACTIVE_SUBSCRIPTIONS_FILE = "../files/active-subscriptions.json"; // New file for active subscriptions
const string MEMBERS_FILE = "../files/members.json";
const string STAFF_FILE = "../files/staff.json";

void FileManager::save(const json& data, const string& filename) {
    if (!data.is_array()) {
        std::cerr << "Data to save is not an array for " << filename << ". Skipping save." << std::endl;
        return;
    }

    string filepath;
    if (filename == "data.json") {
        filepath = DATA_FILE;
    } else if (filename == "gym-classes.json") {
        filepath = GYM_CLASSES_FILE;
    } else if (filename == "padel-classes.json") {
        filepath = PADEL_CLASSES_FILE;
    } else if (filename == "subscriptions.json") {
        filepath = SUBSCRIPTIONS_FILE;
    } else if (filename == "active-subscriptions.json") { // Added new file
        filepath = ACTIVE_SUBSCRIPTIONS_FILE;
    } else if (filename == "members.json") {
        filepath = MEMBERS_FILE;
    } else if (filename == "staff.json") {
        filepath = STAFF_FILE;
    } else {
        std::cerr << "Unknown filename: " << filename << std::endl;
        return;
    }

    ofstream outFile(filepath, ios::trunc);
    if (outFile.is_open()) {
        outFile << data.dump(4);
        outFile.close();
        std::cout << "Successfully saved to " << filepath << std::endl;
    } else {
        std::cerr << "Failed to open " << filepath << " for writing" << std::endl;
    }
}

json FileManager::load(const string& filename) {
    string filepath;
    if (filename == "data.json") {
        filepath = DATA_FILE;
    } else if (filename == "gym-classes.json") {
        filepath = GYM_CLASSES_FILE;
    } else if (filename == "padel-classes.json") {
        filepath = PADEL_CLASSES_FILE;
    } else if (filename == "subscriptions.json") {
        filepath = SUBSCRIPTIONS_FILE;
    } else if (filename == "active-subscriptions.json") { // Added new file
        filepath = ACTIVE_SUBSCRIPTIONS_FILE;
    } else if (filename == "members.json") {
        filepath = MEMBERS_FILE;
    } else if (filename == "staff.json") {
        filepath = STAFF_FILE;
    } else {
        std::cerr << "Unknown filename: " << filename << std::endl;
        return json::array();
    }

    if (!filesystem::exists(filepath)) {
        std::cout << "File " << filepath << " does not exist. Returning empty array." << std::endl;
        return json::array();
    }

    ifstream file(filepath);
    if (!file.is_open()) {
        std::cerr << "Failed to open " << filepath << " for reading" << std::endl;
        return json::array();
    }

    string content((istreambuf_iterator<char>(file)), istreambuf_iterator<char>());
    file.close();

    if (content.empty()) {
        std::cout << "File " << filepath << " is empty. Returning empty array." << std::endl;
        return json::array();
    }

    try {
        json data = json::parse(content);
        if (!data.is_array()) {
            std::cerr << "Loaded data from " << filepath << " is not an array. Returning empty array." << std::endl;
            return json::array();
        }
        std::cout << "Loaded JSON from " << filepath << ": " << data.dump() << std::endl;
        return data;
    } catch (const json::parse_error& e) {
        std::cerr << "Error parsing " << filepath << ": " << e.what() << std::endl;
        return json::array();
    }
}