#pragma once

#include "include/json.hpp"
#include <string>
using namespace std;

using json = nlohmann::json;

class FileManager
{
public:
    static void save(const json &data, const string &filepath);
    static json load(const string &filepath);
    static bool exists(const string &filepath);
    static void remove(const string &filepath);
};
