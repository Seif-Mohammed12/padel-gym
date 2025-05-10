package com.example.padelfrontend;

public class AppContext {
    private static final AppContext instance = new AppContext();

    private AppContext() {}

    public static AppContext getInstance() {
        return instance;
    }

    private String username;
    private String memberId;
    private String firstName;
    private String lastName;
    private String role;
    private String phoneNumber;
    private String dob;
    private String subscribedPlanName;
    private String subscribedDuration;
    private boolean isActive;
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getSubscribedPlanName() {
        return subscribedPlanName;
    }

    public void setSubscribedPlanName(String subscribedPlanName) {
        this.subscribedPlanName = subscribedPlanName;
    }

    public String getSubscribedDuration() {
        return subscribedDuration;
    }

    public void setSubscribedDuration(String subscribedDuration) {
        this.subscribedDuration = subscribedDuration;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setSubscription(String planName, String duration) {
        this.subscribedPlanName = planName;
        this.subscribedDuration = duration;
        this.isActive = true; // Activate membership on subscription
    }

    public void setLoginData(String username, String memberId, String firstName, String lastName,
                             String role, String phoneNumber, String dob, String planName,
                             String duration, boolean isActive) {
        this.username = username;
        this.memberId = memberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.dob = dob;
        this.subscribedPlanName = planName;
        this.subscribedDuration = duration;
        this.isActive = isActive;
    }

    public void clear() {
        username = null;
        memberId = null;
        firstName = null;
        lastName = null;
        role = null;
        phoneNumber = null;
        dob = null;
        subscribedPlanName = null;
        subscribedDuration = null;
        isActive = false;
    }

    public boolean isLoggedIn() {
        return username != null && memberId != null;
    }

    public boolean isSubscribedToPlan(String planName, String duration) {
        return isActive &&
                planName != null && duration != null &&
                planName.equals(subscribedPlanName) && duration.equals(subscribedDuration);
    }

    public void clearSubscription() {
        subscribedPlanName = null;
        subscribedDuration = null;
        isActive = false;
    }
}