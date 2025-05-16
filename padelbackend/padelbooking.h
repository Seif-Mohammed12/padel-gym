#ifndef padelbooking
#define padelbooking

#include <iostream>
#include <vector>
#include <map>
#include <string>
using namespace std;

class VIP {
public:
    string name;
    int bookingCount;
    bool isVIP;

    VIP(string n);
    void incrementBooking();
    bool checkVIPStatus() const;

};

class Court {
public:
    string name;
    string location;
    vector<string> availableTimes;
    map<string, string> bookedTimes;

    Court(string n, string loc, vector<string> times);
    bool bookTimeSlot(string time, string memberId);
    bool cancelTimeSlot(string time, string memberId);

};

class CourtSelection {

public:
    CourtSelection();
    Court* selectCourt(string name);
    vector<Court> courts;


};

class BookingSystem {
public:
    void bookCourt(string memberId, Court* court, string time);
    void cancelBooking(string memberId, Court* court, string time);
    void incrementBookingCount(string memberId, string memberName);

};

#endif