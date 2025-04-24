package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HomeController {
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox centerSearchField;
    @FXML
    private ImageView dropdownicon, calendaricon;
    @FXML
    private HBox navbar;
    private boolean isComboBoxOpen = false;
    private boolean isDatePickerOpen = false;

    @FXML
    private void openCalendar() {

        if (isDatePickerOpen) {
            datePicker.hide();
        } else {
            datePicker.show();
        }
        isDatePickerOpen = !isDatePickerOpen;
    }
    @FXML
    private void showCombobox() {
        if (isComboBoxOpen) {
            centerSearchField.hide();
        } else {
            centerSearchField.show();
        }
        isComboBoxOpen = !isComboBoxOpen;
    }


    @FXML
    private void initialize() {
        configureDatePicker(datePicker, LocalDate.now());
        dropdownicon.setCursor(Cursor.HAND);
        calendaricon.setCursor(Cursor.HAND);
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button && button.getStyleClass().contains("nav-button")) {
                ScaleTransition grow = new ScaleTransition(Duration.millis(200), button);
                grow.setToX(1.1);
                grow.setToY(1.1);

                ScaleTransition shrink = new ScaleTransition(Duration.millis(200), button);
                shrink.setToX(1.0);
                shrink.setToY(1.0);

                button.setOnMouseEntered(e -> grow.playFromStart());
                button.setOnMouseExited(e -> shrink.playFromStart());
            }
        }

    }

    private void configureDatePicker(DatePicker datePicker, LocalDate today) {
        if (datePicker.getValue() != null && datePicker.getValue().isBefore(today)) {
            datePicker.setValue(today);
        }

        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isBefore(today)) {
                datePicker.setValue(today);
            }
        });
        datePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return (date == null) ? "" : date.format(formatter);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(string, formatter);
            }
        });
    }

    @FXML
    private void gotoLogin() {
        try {
            // Load the Login page FXML
            Parent loginPage = FXMLLoader.load(getClass().getResource("LoginPage.fxml"));

            // Get the current Stage directly from the Scene
            Stage stage = (Stage) datePicker.getScene().getWindow();

            Scene currentScene = stage.getScene();

            // Fade out transition for the current scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                // Set the new scene root after fade-out completes
                currentScene.setRoot(loginPage);

                // Fade in transition for the new scene root (Login page)
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginPage);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
