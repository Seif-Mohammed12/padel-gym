package com.example.padelfrontend;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;

public class HomeController {
    @FXML
    private DatePicker datePicker;

    @FXML
    private void openCalendar() {
        datePicker.show();
    }

    public void handleLogout() {
        System.out.println("Logout clicked");
        // Logic to switch back to login scene
    }

    public void goToBookings() {
        System.out.println("Bookings clicked");
    }

    public void goToCoaches() {
        System.out.println("Coaches clicked");
    }

    public void goToMembers() {
        System.out.println("Members clicked");
    }

    public void goToCourts() {
        System.out.println("Courts clicked");
    }

    public void goToSettings() {
        System.out.println("Settings clicked");
    }
}
