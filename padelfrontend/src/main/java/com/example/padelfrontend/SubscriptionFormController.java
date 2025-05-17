package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    private DatePicker dobPicker; // Reverted to DatePicker
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
    @FXML
    private VBox dobContainer;

    private TextField dobTextField; // For direct date entry
    private Label dobErrorLabel; // For validation feedback
    private Parent subscriptionPage;
    private Runnable onCancelCallback;

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

        // Set up phone number field with "+20" prefix
        setupPhoneNumberField();

        // Add a TextField and Error Label for DOB entry
        setupDobField();
        configureDatePicker(dobPicker, LocalDate.now());

        // Pre-fill fields with AppContext data if logged in
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            firstNameField.setText(context.getFirstName() != null ? context.getFirstName() : "");
            lastNameField.setText(context.getLastName() != null ? context.getLastName() : "");
            String phoneNumber = context.getPhoneNumber();
            phoneNumberField.setText(phoneNumber != null ? phoneNumber : "+20");

            String dob = context.getDob();
            if (dob != null && !dob.trim().isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    LocalDate dobDate = LocalDate.parse(dob, formatter);
                    dobPicker.setValue(dobDate);
                } catch (DateTimeParseException e) {
                    System.err.println("Failed to parse DOB from context: " + dob + ". Error: " + e.getMessage());
                    // Optionally clear the date picker or set a default value

                }
            } else {
                dobPicker.setValue(null); // Clear date picker if DOB is null or empty
            }

        }

        // Restrict DatePicker to a reasonable range (e.g., 1900 to current year)
        restrictDatePickerRange();
    }

    /**
     * Sets up the phone number field to enforce the "+20" prefix and allow only digits after it.
     */
    private void setupPhoneNumberField() {
        // Initialize with "+20"
        phoneNumberField.setText("+20");
        phoneNumberField.setPromptText("+201234567890");

        // Add listener to enforce "+20" prefix and restrict input to digits
        phoneNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                phoneNumberField.setText("+20");
                return;
            }

            // Ensure the "+20" prefix is always present
            if (!newValue.startsWith("+20")) {
                phoneNumberField.setText("+20" + newValue.replaceAll("[^\\d]", ""));
                return;
            }

            // Remove non-digits after "+20" and enforce length
            String digits = newValue.substring(3).replaceAll("[^\\d]", "");
            String result = "+20" + digits;

            // Limit to 13 characters total (3 for "+20", 10 for the number)
            if (result.length() > 13) {
                result = result.substring(0, 13);
            }

            if (!result.equals(newValue)) {
                int caretPosition = phoneNumberField.getCaretPosition();
                phoneNumberField.setText(result);
                phoneNumberField.positionCaret(Math.min(caretPosition, result.length()));
            }
        });

        // Prevent cursor from moving before "+20"
        phoneNumberField.caretPositionProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.intValue() < 3) {
                phoneNumberField.positionCaret(3);
            }
        });

        // Prevent selection of "+20" for deletion
        phoneNumberField.selectionProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection.getStart() < 3) {
                phoneNumberField.selectRange(3, Math.max(3, newSelection.getEnd()));
            }
        });
    }

    /**
     * Sets up the DOB field with a DatePicker, a TextField for direct entry, and an error label.
     */
    private void setupDobField() {
        // Create a TextField for direct date entry
        dobTextField = new TextField();
        dobTextField.setPromptText("dd-MM-yyyy");
        dobTextField.setPrefWidth(150);

        // Create an error label for validation feedback
        dobErrorLabel = new Label();
        dobErrorLabel.getStyleClass().add("error-label");
        dobErrorLabel.setVisible(false);

        // Sync TextField with DatePicker
        dobTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            // Prevent loop
            if (newValue == null || newValue.equals(oldValue)) return;

            int caretPos = dobTextField.getCaretPosition();

            // Remove non-digits
            String digits = newValue.replaceAll("[^\\d]", "");
            StringBuilder formatted = new StringBuilder();
            int addedDashes = 0;

            for (int i = 0; i < digits.length() && i < 8; i++) {
                if (i == 2 || i == 4) {
                    formatted.append("-");
                    if (i < caretPos) caretPos++;
                }
                formatted.append(digits.charAt(i));
            }

            String result = formatted.toString();

            // Prevent recursive setText
            if (!result.equals(newValue)) {
                dobTextField.setText(result);

                // Suppress caret error
                try {
                    dobTextField.positionCaret(Math.min(caretPos, result.length()));
                } catch (IllegalArgumentException ignored) {
                    // Prevents "start must be <= end" crash
                }
            }

            // Date validation
            if (result.length() == 10) {
                try {
                    LocalDate date = LocalDate.parse(result, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    dobPicker.setValue(date);
                    dobErrorLabel.setVisible(false);
                } catch (DateTimeParseException e) {
                    dobErrorLabel.setText("Invalid date. Please check the day, month, or year.");
                    dobErrorLabel.setVisible(true);
                }
            } else {
                dobPicker.setValue(null);
                dobErrorLabel.setText("Incomplete date. Format: dd-MM-yyyy.");
                dobErrorLabel.setVisible(true);
            }
        });

        // Sync DatePicker with TextField
        dobPicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                dobTextField.clear();
            } else {
                dobTextField.setText(newValue.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            }
            dobErrorLabel.setVisible(false);
        });

        // Add the TextField and Error Label to the form
        HBox dobInputContainer = new HBox(10, dobPicker, dobTextField);
        dobContainer.getChildren().addAll(dobInputContainer, dobErrorLabel);
    }

    /**
     * Restricts the DatePicker to a reasonable range for DOB (e.g., 1900 to current year).
     */
    private void restrictDatePickerRange() {
        LocalDate minDate = LocalDate.of(1900, 1, 1);
        LocalDate maxDate = LocalDate.now();

        // Disable dates outside the range
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(minDate) || item.isAfter(maxDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #C61F2B;");
                }
            }
        };
        dobPicker.setDayCellFactory(dayCellFactory);

        // Set the default visible date to a reasonable past date (e.g., 1990)
        dobPicker.setValue(null);
        dobPicker.setShowWeekNumbers(false);
    }

    /**
     * Sets up the date picker with a custom format and restricts past dates.
     */
    private void configureDatePicker(DatePicker datePicker, LocalDate today) {
        if (datePicker.getValue() != null && datePicker.getValue().isAfter(today)) {
            datePicker.setValue(today);
        }
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isAfter(today)) {
                datePicker.setValue(today);
            }
        });

        datePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(formatter);
            }

            @Override
            public LocalDate fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalDate.parse(string, formatter);
            }
        });
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
     * Sets the callback to invoke when the form is canceled.
     *
     * @param onCancelCallback The callback to run on cancellation.
     */
    public void setOnCancelCallback(Runnable onCancelCallback) {
        this.onCancelCallback = onCancelCallback;
    }

    /**
     * Handles the confirm action, validates input, and updates AppContext with the data.
     */
    @FXML
    private void handleConfirm() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phoneNumber = phoneNumberField.getText().trim(); // Includes "+20"
        LocalDate dob = dobPicker.getValue();

        // Validation
        if (firstName.isEmpty()) {
            showAlert("First name is required.", getStage());
            return;
        }
        if (lastName.isEmpty()) {
            showAlert("Last name is required.", getStage());
            return;
        }
        if (phoneNumber.length() <= 3 || !phoneNumber.startsWith("+20") || !phoneNumber.substring(3).matches("\\d{10,12}")) {
            showAlert( "Please enter a valid phone number (10-12 digits after +20).", getStage());
            return;
        }
        if (dob == null || dob.isAfter(LocalDate.now().minusYears(16))) {
            showAlert("Please select a valid date of birth (must be at least 16 years old).", getStage());
            return;
        }

        // Format DOB as dd-MM-yyyy
        String dobString = dob.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // Update AppContext with the validated data (store with "+20")
        AppContext context = AppContext.getInstance();
        context.setFirstName(firstName);
        context.setLastName(lastName);
        context.setPhoneNumber(phoneNumber); // Store with "+20"
        context.setDob(dobString);

        // Close the overlay
        closeOverlay();
    }

    /**
     * Handles the cancel action, closing the overlay and invoking the cancel callback.
     */
    @FXML
    private void handleCancel() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
        closeOverlay();
    }

    /**
     * Handles clicking the background to close the overlay and invoke the cancel callback.
     */
    @FXML
    private void handleBackgroundClick() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
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
     */
    private void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.NONE, "", ButtonType.OK);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // Content pane
        VBox contentPane = new VBox(15);
        contentPane.getStyleClass().add("custom-alert");
        contentPane.setPadding(new Insets(20));
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPrefSize(300, 200);

        // Message label
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(260);
        messageLabel.setAlignment(Pos.CENTER);

        // OK button
        Button okButton = new Button("OK");
        okButton.getStyleClass().add("alert-button");
        okButton.setPrefWidth(120);
        okButton.setPrefHeight(36);
        okButton.setOnAction(e -> alert.close());

        contentPane.getChildren().addAll(messageLabel, okButton);

        // Set content to DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(contentPane);
        dialogPane.getStylesheets().add(getClass().getResource("home.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: transparent;");
        dialogPane.setMinSize(300, 200);
        dialogPane.setMaxSize(300, 200);

        // Style the default OK button (hidden but ensures native closing)
        dialogPane.lookupButton(ButtonType.OK).setVisible(false);

        // Transparent stage
        Stage alertStage = (Stage) dialogPane.getScene().getWindow();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.getScene().setFill(null);

        // Allow closing with Escape key
        dialogPane.getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                alert.close();
            }
        });

        // Fade-in animation
        contentPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        alert.setOnShown(e -> fadeIn.play());

        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) confirmButton.getScene().getWindow();
    }
}