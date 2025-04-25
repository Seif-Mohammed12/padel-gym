#include <iostream>
#include <vector>
#include <string>
using namespace std;

// Member Class
class Member {
public:
    int id;
    string name;
    string dob;
    string subscription; 

    void addMember(int i, string n, string d, string s) {
        id = i;
        name = n;
        dob = d;
        subscription = s;
    }

    void showMember() {
        cout << "ID: " << id << ", Name: " << name
            << ", DOB: " << dob << ", Subscription: " << subscription << endl;
    }
};

// Staff Class
class Staff {
public:
    string name;
    string role;

    void addStaff(string n, string r) {
        name = n;
        role = r;
    }

    void showStaff() {
        cout << "Name: " << name << ", Role: " << role << endl;
    }
};

// Management Class
class GymSystem {
public:
    vector<Member> members;
    vector<Staff> staff;

    void addNewMember() {
        Member m;
        int id;
        string name, dob, sub;

        cout << "Enter Member ID: ";
        cin >> id;
        cin.ignore();
        cout << "Enter Name: ";
        getline(cin, name);
        cout << "Enter Date of Birth: ";
        getline(cin, dob);
        cout << "Enter Subscription (Monthly/3Months/6Months/Yearly): ";
        getline(cin, sub);

        m.addMember(id, name, dob, sub);
        members.push_back(m);

        cout << "Member added!\n";
    }

    void addNewStaff() {
        Staff s;
        string name, role;

        cout << "Enter Staff Name: ";
        cin.ignore();
        getline(cin, name);
        cout << "Enter Staff Role (Receptionist/Coach/Manager): ";
        getline(cin, role);

        s.addStaff(name, role);
        staff.push_back(s);

        cout << "Staff added!\n";
    }

    void showAllMembers() {
        cout << "\nAll Members:\n";
        for (int i = 0; i < members.size(); i++) {
            members[i].showMember();
        }
    }

    void showAllStaff() {
        cout << "\nAll Staff:\n";
        for (int i = 0; i < staff.size(); i++) {
            staff[i].showStaff();
        }
    }

    void renewSubscription() {
        int id;
        cout << "Enter Member ID to renew subscription: ";
        cin >> id;
        cin.ignore();

        for (int i = 0; i < members.size(); i++) {
            if (members[i].id == id) {
                cout << "Enter new Subscription: ";
                string newSub;
                getline(cin, newSub);
                members[i].subscription = newSub;
                cout << "Subscription updated!\n";
                return;
            }
        }
        cout << "Member not found.\n";
    }

    void cancelMembership() {
        int id;
        cout << "Enter Member ID to cancel: ";
        cin >> id;

        for (int i = 0; i < members.size(); i++) {
            if (members[i].id == id) {
                members.erase(members.begin() + i);
                cout << "Membership cancelled!\n";
                return;
            }
        }
        cout << "Member not found.\n";
    }
};

int main() {
    GymSystem gym;
    int choice;

    do {
        cout << "\n--- Gym Management System ---\n";
        cout << "1. Add Member\n";
        cout << "2. Add Staff\n";
        cout << "3. Show All Members\n";
        cout << "4. Show All Staff\n";
        cout << "5. Renew Member Subscription\n";
        cout << "6. Cancel Membership\n";
        cout << "7. Exit\n";
        cout << "Enter your choice: ";
        cin >> choice;

        switch (choice) {
        case 1: gym.addNewMember(); break;
        case 2: gym.addNewStaff(); break;
        case 3: gym.showAllMembers(); break;
        case 4: gym.showAllStaff(); break;
        case 5: gym.renewSubscription(); break;
        case 6: gym.cancelMembership(); break;
        case 7: cout << "Goodbye!\n"; break;
        default: cout << "Invalid choice!\n";
        }

    } while (choice != 7);

    return 0;
}
