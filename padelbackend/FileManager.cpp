#include "FileManager.h"
#include <fstream>
#include <sstream>
#include <iostream>

bool FileManager::writeCSV(const std::string& filePath, const std::vector<std::vector<std::string>>& rows, bool append) {
    std::ofstream file(filePath, append ? std::ios::app : std::ios::trunc);
    if (!file.is_open()) return false;

    for (const auto& row : rows) {
        for (size_t i = 0; i < row.size(); ++i) {
            file << row[i];
            if (i != row.size() - 1) file << ",";
        }
        file << "\n";
    }

    file.close();
    return true;
}

std::vector<std::vector<std::string>> FileManager::readCSV(const std::string& filePath) {
    std::vector<std::vector<std::string>> data;
    std::ifstream file(filePath);
    std::string line;

    while (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string field;
        std::vector<std::string> row;

        while (std::getline(ss, field, ',')) {
            row.push_back(field);
        }

        if (!row.empty()) {
            data.push_back(row);
        }
    }

    return data;
}
