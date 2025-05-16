package com.example.padelfrontend;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    private String subscriptionStartDate;
    private String subscriptionExpiryDate;

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
    public String getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(String subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }
    public String getSubscriptionExpiryDate() { return subscriptionExpiryDate; }
    public void setSubscriptionExpiryDate(String subscriptionExpiryDate) { this.subscriptionExpiryDate = subscriptionExpiryDate; }

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

    /**
     * Loads active subscriptions for the current user from the server and updates AppContext.
     * @param showAlerts Whether to show UI alerts for errors (true for controllers with UI, false for background tasks).
     * @return JSONArray of active subscriptions, or null if none exist or an error occurs.
     */
    public JSONArray loadActiveSubscriptions(boolean showAlerts) {
        try {
            String memberId = getMemberId();
            if (memberId == null) {
                if (showAlerts) {

                } else {
                    System.out.println("Member ID is missing.");
                }
                return null;
            }

            JSONObject request = new JSONObject();
            request.put("action", "get_active_subscriptions");
            request.put("memberId", memberId);

            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            System.out.println("Server response: " + response);

            if (!responseJson.getString("status").equals("success")) {
                String message = responseJson.getString("message");
                if (showAlerts) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText(message);
                        alert.showAndWait();
                    });
                } else {
                    System.out.println(message);
                }
                return null;
            }

            JSONArray activeSubs = responseJson.getJSONArray("data");
            if (activeSubs.length() > 0) {
                JSONObject sub = activeSubs.getJSONObject(0);
                setSubscribedPlanName(sub.optString("planName", "Unknown Plan"));
                setSubscribedDuration(sub.optString("duration", "Unknown Duration"));
                setDob(sub.optString("dob", null));
                setSubscriptionStartDate(sub.optString("startDate", null));
                setSubscriptionExpiryDate(sub.optString("expiryDate", null));
                setActive(true);
            } else {
                setSubscribedPlanName(null);
                setSubscribedDuration(null);
                setDob(null);
                setSubscriptionStartDate(null);
                setSubscriptionExpiryDate(null);
                setActive(false);
            }

            return activeSubs;
        } catch (Exception e) {
            if (showAlerts) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to load active subscriptions: " + e.getMessage());
                    alert.showAndWait();
                });
            } else {
                e.printStackTrace();
            }
            return null;
        }
    }

    private String sendRequestToServer(String request) throws IOException {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println(request);
            String response = in.readLine();
            return response != null ? response : "{}";
        }
    }
}