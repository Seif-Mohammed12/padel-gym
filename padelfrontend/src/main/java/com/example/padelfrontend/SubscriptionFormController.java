package com.example.padelfrontend;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Controller for the SubscriptionForm overlay, handling user input for subscription details.
 */
public class SubscriptionFormController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private DatePicker dobPicker;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;
    @FXML
    private StackPane overlayPane;
    @FXML
    private StackPane backgroundOverlay;
    @FXML
    private VBox formCard;

    private Consumer<SubscriptionDetails> onConfirmCallback;
    private Parent subscriptionPage;

    /**
     * Initializes the controller after FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        // Ensure the form card is focusable
        formCard.setFocusTraversable(true);
        firstNameField.requestFocus();

        // Clear fields
        firstNameField.clear();
        lastNameField.clear();
        phoneNumberField.clear();
        dobPicker.setValue(null);

        // Pre-fill fields with AppContext data if logged in
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            firstNameField.setText(context.getFirstName() != null ? context.getFirstName() : "");
            lastNameField.setText(context.getLastName() != null ? context.getLastName() : "");
            // Optionally pre-fill phoneNumber and dob if stored in AppContext
            // phoneNumberField.setText(context.getPhoneNumber() != null ? context.getPhoneNumber() : "");
            // if (context.getDob() != null) {
            //     dobPicker.setValue(LocalDate.parse(context.getDob(), DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            // }
        }
    }

    /**
     * Sets the callback to handle the subscription details when confirmed.
     *
     * @param callback The callback to invoke with the subscription details.
     */
    public void setOnConfirmCallback(Consumer<SubscriptionDetails> callback) {
        this.onConfirmCallback = callback;
    }

    /**
     * Sets the parent subscription page node to restore interaction after closing the overlay.
     *
     * @param subscriptionPage The parent node of the subscription page.
     */
    public void setSubscriptionPage(Parent subscriptionPage) {
        this.subscriptionPage = subscriptionPage;
        // Disable interaction with the subscription page while the overlay is visible
        if (subscriptionPage != null) {
            subscriptionPage.setDisable(true);
        }
    }

    /**
     * Handles the confirm action, validates input, and passes the data back to the parent controller.
     */
    @FXML
    private void handleConfirm() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phoneNumber = phoneNumberField.getText().trim();
        LocalDate dob = dobPicker.getValue();

        // Validation
        if (firstName.isEmpty()) {
            showAlert("Error", "First name is required.");
            return;
        }
        if (lastName.isEmpty()) {
            showAlert("Error", "Last name is required.");
            return;
        }
        if (phoneNumber.isEmpty() || !phoneNumber.matches("\\d{10,15}")) {
            showAlert("Error", "Please enter a valid phone number (10-15 digits).");
            return;
        }
        if (dob == null || dob.isAfter(LocalDate.now().minusYears(16))) {
            showAlert("Error", "Please select a valid date of birth (must be at least 16 years old).");
            return;
        }

        // Format DOB as YYYY-MM-DD
        String dobString = dob.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Get memberId from AppContext if logged in
        AppContext context = AppContext.getInstance();
        String memberId = context.isLoggedIn() ? context.getMemberId() : null;

        // Create subscription details
        SubscriptionDetails details = new SubscriptionDetails(firstName, lastName, phoneNumber, dobString, memberId);

        // Pass the details to the callback
        if (onConfirmCallback != null) {
            onConfirmCallback.accept(details);
        }

        // Close the overlay
        closeOverlay();
    }

    /**
     * Handles the cancel action, closing the overlay.
     */
    @FXML
    private void handleCancel() {
        closeOverlay();
    }

    /**
     * Handles clicking the background to close the overlay.
     */
    @FXML
    private void handleBackgroundClick() {
        closeOverlay();
    }

    /**
     * Closes the overlay by removing it from the scene and re-enabling the subscription page.
     */
    private void closeOverlay() {
        // Remove the overlay from the scene
        StackPane parent = (StackPane) overlayPane.getParent();
        if (parent != null) {
            parent.getChildren().remove(overlayPane);
            // Re-enable interaction with the subscription page
            if (subscriptionPage != null) {
                subscriptionPage.setDisable(false);
            }
        }
    }

    /**
     * Shows an alert dialog with the specified title and message.
     *
     * @param title The title of the alert.
     * @param message The message to display.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

/**
 * Class to hold subscription details.
 */
class SubscriptionDetails {
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String dob;
    private final String memberId;

    public SubscriptionDetails(String firstName, String lastName, String phoneNumber, String dob, String memberId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.dob = dob;
        this.memberId = memberId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDob() {
        return dob;
    }

    public String getMemberId() {
        return memberId;
    }
}