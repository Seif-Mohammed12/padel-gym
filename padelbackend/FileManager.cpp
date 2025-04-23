#include "FileManager.h"
#include <fstream>
#include <filesystem>
#include <iostream>

using namespace std;

void FileManager::save(const json& data, const string& filepath) {
    bool fileExists = exists(filepath);

    // If the file exists, open it in read mode
    if (fileExists) {
        // Read existing data
        ifstream inFile(filepath);
        json existingData;
        if (inFile.is_open()) {
            inFile >> existingData;
            inFile.close();
        }

        // Ensure the existing data is an array (even if it's empty)
        if (!existingData.is_array()) {
            existingData = json::array();
        }

        // Append the new data to the array
        existingData.push_back(data);

        // Now open the file in write mode to save the updated data
        ofstream outFile(filepath, ios::trunc);  // Overwrite the file
        if (outFile.is_open()) {
            outFile << existingData.dump(4);  // Pretty print the JSON with 4 spaces of indentation
            outFile.close();
        }
    } else {
        // If the file does not exist, create a new one with the data
        ofstream outFile(filepath);
        if (outFile.is_open()) {
            json newArray = json::array();
            newArray.push_back(data);  // Append the first entry
            outFile << newArray.dump(4);  // Pretty print the JSON with 4 spaces of indentation
            outFile.close();
        }
    }
}

json FileManager::load(const string& filepath) {
    ifstream file(filepath);
    json data;
    if (file.is_open()) {
        file >> data;
    }
    return data;
}

bool FileManager::exists(const string& filepath) {
    return filesystem::exists(filepath);
}

void FileManager::remove(const string& filepath) {
    filesystem::remove(filepath);
}
