#ifndef GYMSYSTEM_H
#define GYMSYSTEM_H

#include <vector>
#include <string>
#include "include/json.hpp"
#include "Member.h"
#include "Staff.h"

using json = nlohmann::json;

class GymSystem {
private:
    std::vector<Member> members;
    std::vector<Staff> staff;
    const std::string MEMBERS_FILE = "members.json";
    const std::string STAFF_FILE = "staff.json";

public:
    GymSystem();
    void addMember(const Member& m);
    void addStaff(const Staff& s);
    json getMembers() const;
    json getStaff() const;
    void renewSubscription(std::string id, const json& newSub); // Changed parameter to json
    void cancelMembership(std::string id);

private:
    void saveMembers();
    void loadMembers();
    void saveStaff();
    void loadStaff();
};

#endif // GYMSYSTEM_H