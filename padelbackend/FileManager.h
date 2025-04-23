#ifndef FILEMANAGER_H
#define FILEMANAGER_H

#include <string>
#include <vector>

class FileManager {
public:
    static bool writeCSV(const std::string& filePath, const std::vector<std::vector<std::string>>& rows, bool append = false);
    static std::vector<std::vector<std::string>> readCSV(const std::string& filePath);
};

#endif
