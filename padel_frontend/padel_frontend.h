#include <iostream>
#include <vector>
#include <map>
#include <string>
using namespace std;

#ifndef padel_frontend
#define padel_frontend

// VIP Class
class VIP {
public:
    string name;
    int bookingCount;
    bool isVIP;

    VIP(string n);
    void incrementBooking();
    bool checkVIPStatus() const;
};

// Court Class
class Court {
public:
    string name;
    string location;
    vector<string> availableTimes;

    Court(string n, string loc, vector<string> times);
    void showCourtInfo() const;
};

// Court Selection Class
class CourtSelection {
private:
    vector<Court> courts;

public:
    CourtSelection();
    void showAvailableCourts() const;
    Court* selectCourt(string name);
};

// Booking System Class
class BookingSystem {
private:
    map<string, VIP*> members;

public:
    void bookCourt(VIP* member, Court* court, string time);
    void cancelBooking(VIP* member, Court* court, string time);
};



#endif // padel_frontend



