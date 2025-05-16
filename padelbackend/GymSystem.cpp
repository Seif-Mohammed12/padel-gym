#include "GymSystem.h"
#include "FileManager.h"
#include <iostream>

//=============================================================================
// System Initialization
// Constructor and setup functionality
//=============================================================================

GymSystem::GymSystem()
{
    loadMembers();
    loadStaff();
}

//=============================================================================
// Member Management
// Core member operations and data handling
//=============================================================================

void GymSystem::addMember(const Member &m)
{
    members.push_back(m);
    saveMembers();
}

json GymSystem::getMembers() const
{
    json jsonMembers = json::array();
    for (const auto &m : members)
    {
        jsonMembers.push_back(m.toJson());
    }
    return jsonMembers;
}

void GymSystem::renewSubscription(std::string id, const json &newSub)
{
    loadMembers();
    for (auto &m : members)
    {
        if (m.id == id)
        {
            m.subscription = newSub;
            saveMembers();
            return;
        }
    }
    throw std::runtime_error("Member not found");
}

void GymSystem::cancelMembership(std::string id)
{
    for (auto it = members.begin(); it != members.end(); ++it)
    {
        if (it->id == id)
        {
            members.erase(it);
            saveMembers();
            return;
        }
    }
    throw std::runtime_error("Member not found");
}

//=============================================================================
// Staff Management
// Core staff operations and data handling
//=============================================================================

void GymSystem::addStaff(const Staff &s)
{
    staff.push_back(s);
    saveStaff();
}

json GymSystem::getStaff() const
{
    json jsonStaff = json::array();
    for (const auto &s : staff)
    {
        jsonStaff.push_back(s.toJson());
    }
    return jsonStaff;
}

//=============================================================================
// Data Persistence - Members
// File operations for member data
//=============================================================================

void GymSystem::saveMembers()
{
    FileManager::save(getMembers(), MEMBERS_FILE);
}

void GymSystem::loadMembers()
{
    json data = FileManager::load(MEMBERS_FILE);
    members.clear();
    for (const auto &m : data)
    {
        members.push_back(Member::fromJson(m));
    }
}

//=============================================================================
// Data Persistence - Staff
// File operations for staff data
//=============================================================================

void GymSystem::saveStaff()
{
    FileManager::save(getStaff(), STAFF_FILE);
}

void GymSystem::loadStaff()
{
    json data = FileManager::load(STAFF_FILE);
    staff.clear();
    for (const auto &s : data)
    {
        staff.push_back(Staff::fromJson(s));
    }
}