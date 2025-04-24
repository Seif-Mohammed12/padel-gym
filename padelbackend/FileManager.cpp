#include "FileManager.h"
#include <fstream>
#include <filesystem>
#include <iostream>

using namespace std;

// Relative paths to the root directory (one level up from cmake-build-debug)
const string DATA_FILE = "../data.json";
const string CLASSES_FILE = "../gym-classes.json";

void FileManager::save(const json& data, const string& filename) {
    string filepath = (filename == "data.json") ? DATA_FILE : CLASSES_FILE;
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
    string filepath = (filename == "data.json") ? DATA_FILE : CLASSES_FILE;
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
        return json::parse(content);
    } catch (const json::parse_error& e) {
        std::cerr << "Error parsing " << filepath << ": " << e.what() << std::endl;
        return json::array();
    }
}