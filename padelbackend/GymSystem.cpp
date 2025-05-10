#include "GymSystem.h"
#include "FileManager.h"
#include <iostream>

GymSystem::GymSystem() {
    loadMembers();
    loadStaff();
}

void GymSystem::addMember(const Member& m) {
    members.push_back(m);
    saveMembers();
}

void GymSystem::addStaff(const Staff& s) {
    staff.push_back(s);
    saveStaff();
}

json GymSystem::getMembers() const {
    json jsonMembers = json::array();
    for (const auto& m : members) {
        jsonMembers.push_back(m.toJson());
    }
    return jsonMembers;
}

json GymSystem::getStaff() const {
    json jsonStaff = json::array();
    for (const auto& s : staff) {
        jsonStaff.push_back(s.toJson());
    }
    return jsonStaff;
}

void GymSystem::renewSubscription(string id, const json& newSub) {
    for (auto& m : members) {
        if (m.id == id) {
            m.subscription = newSub;
            saveMembers();
            return;
        }
    }
    throw std::runtime_error("Member not found");
}

void GymSystem::cancelMembership(string id) {
    for (auto it = members.begin(); it != members.end(); ++it) {
        if (it->id == id) {
            members.erase(it);
            saveMembers();
            return;
        }
    }
    throw std::runtime_error("Member not found");
}

void GymSystem::saveMembers() {
    FileManager::save(getMembers(), MEMBERS_FILE);
}

void GymSystem::loadMembers() {
    json data = FileManager::load(MEMBERS_FILE);
    members.clear();
    for (const auto& m : data) {
        members.push_back(Member::fromJson(m));
    }
}

void GymSystem::saveStaff() {
    FileManager::save(getStaff(), STAFF_FILE);
}

void GymSystem::loadStaff() {
    json data = FileManager::load(STAFF_FILE);
    staff.clear();
    for (const auto& s : data) {
        staff.push_back(Staff::fromJson(s));
    }
}