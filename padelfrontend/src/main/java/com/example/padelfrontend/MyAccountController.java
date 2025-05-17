package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Controller for the My Account page, displaying user info and active subscriptions.
 */
public class MyAccountController {

    @FXML private BorderPane accountRoot;
    @FXML private HBox navbar;
    @FXML private Button homeButton;
    @FXML private Button bookingButton;
    @FXML private Button subscriptionButton;
    @FXML private Button gymButton;
    @FXML private Button loginButton;
    @FXML private HBox nameBox;
    @FXML private Label nameLabel;
    @FXML private HBox lastNameBox;
    @FXML private Label lastNameLabel;
    @FXML private HBox emailBox;
    @FXML private Label emailLabel;
    @FXML private HBox memberIdBox;
    @FXML private Label memberIdLabel;
    @FXML private HBox phoneBox;
    @FXML private Label phoneNumberLabel;
    @FXML private HBox usernameBox;
    @FXML private Label usernameLabel;
    @FXML private VBox subscriptionsBox;
    @FXML private Button editButton;
    @FXML private Button saveButton;

    private TextField nameField;
    private TextField lastNameField;
    private TextField emailField;
    private TextField phoneNumberField;
    private TextField usernameField;
    private boolean isEditing = false;

    private static final Duration TRANSITION_DURATION = Duration.millis(400);
    private boolean isNavigating = false;

    @FXML
    public void initialize() {
        setupNavbar();
        loadUserInfo();
        loadSubscriptions();
    }

    /**
     * Loads user information from AppContext and updates the UI.
     */
    private void loadUserInfo() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            String firstName = context.getFirstName();
            String lastName = context.getLastName();
            String email = context.getEmail();
            String memberId = context.getMemberId();
            String phoneNumber = context.getPhoneNumber();
            String username = context.getUsername();

            nameLabel.setText(firstName != null ? firstName : "Not provided");
            lastNameLabel.setText(lastName != null ? lastName : "Not provided");
            emailLabel.setText(email != null ? (email.isEmpty() ? "Not provided" : email) : "Not provided");
            memberIdLabel.setText(memberId != null ? memberId : "Not provided");
            phoneNumberLabel.setText(phoneNumber != null ? phoneNumber : "Not provided");
            usernameLabel.setText(username != null ? username : "Not provided");
        } else {
            nameLabel.setText("Not logged in");
            lastNameLabel.setText("Not logged in");
            emailLabel.setText("Not logged in");
            memberIdLabel.setText("Not logged in");
            phoneNumberLabel.setText("Not logged in");
            usernameLabel.setText("Not logged in");
        }
    }

    /**
     * Fetches active subscriptions from the server and populates the subscriptionsBox.
     */
    private void loadSubscriptions() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject request = new JSONObject();
                request.put("action", "get_active_subscriptions");
                request.put("memberId", AppContext.getInstance().getMemberId());
                out.print(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                if ("success".equals(status)) {
                    JSONArray subscriptions = jsonResponse.getJSONArray("data");
                    Platform.runLater(() -> populateSubscriptions(subscriptions));
                    AppContext context = AppContext.getInstance();
                    context.setSubscribedPlanName(!subscriptions.isEmpty() ? subscriptions.getJSONObject(0).optString("planName", "Unknown Plan") : "No active subscriptions");
                    context.setSubscribedDuration(!subscriptions.isEmpty() ? subscriptions.getJSONObject(0).optString("duration", "Unknown Duration") : "Unknown Duration");
                } else {
                    throw new Exception(jsonResponse.optString("message", "Failed to fetch subscriptions"));
                }

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Failed to load subscriptions: " + ex.getMessage());
                    errorLabel.getStyleClass().add("error-label");
                    subscriptionsBox.getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    /**
     * Populates the subscriptionsBox with active subscription cards.
     */
    private void populateSubscriptions(JSONArray subscriptions) {
        subscriptionsBox.getChildren().clear();
        if (subscriptions.length() == 0) {
            Label noSubsLabel = new Label("No active subscriptions.");
            noSubsLabel.getStyleClass().add("account-info");
            subscriptionsBox.getChildren().add(noSubsLabel);
            return;
        }

        for (int i = 0; i < subscriptions.length(); i++) {
            JSONObject sub = subscriptions.getJSONObject(i);
            String planName = sub.optString("planName", "Unknown Plan");
            String duration = sub.optString("duration", "Unknown Duration");
            String startDate = sub.optString("startDate", "N/A");
            String expiryDate = sub.optString("expiryDate", "N/A");

            VBox subCard = new VBox(5);
            subCard.getStyleClass().add("account-card");
            subCard.setAlignment(javafx.geometry.Pos.CENTER);

            Label planLabel = new Label("Plan: " + planName);
            planLabel.getStyleClass().add("account-info");
            Label durationLabel = new Label("Duration: " + duration);
            durationLabel.getStyleClass().add("account-info");
            Label dateLabel1 = new Label("Start Date: " + startDate);
            dateLabel1.getStyleClass().add("account-info");
            Label dateLabel2 = new Label("Expiry Date: " + expiryDate);
            dateLabel2.getStyleClass().add("account-info");

            subCard.getChildren().addAll(planLabel, durationLabel, dateLabel1, dateLabel2);
            subscriptionsBox.getChildren().add(subCard);
        }
    }

    /**
     * Sets up the navbar with hover effects and active state handling.
     */
    private void setupNavbar() {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button) {
                addButtonHoverEffects(button);
                var existingHandler = button.getOnAction();
                button.setOnAction(event -> {
                    setInactiveButtons();
                    button.getStyleClass().add("active");
                    if (existingHandler != null) {
                        existingHandler.handle(event);
                    }
                });
            }
        }
    }

    /**
     * Adds hover scale effects to a button.
     */
    private void addButtonHoverEffects(Button button) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(200), button);
        grow.setToX(1.1);
        grow.setToY(1.1);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), button);
        shrink.setToX(1.0);
        shrink.setToY(1.0);
        button.setOnMouseEntered(e -> grow.playFromStart());
        button.setOnMouseExited(e -> shrink.playFromStart());
    }

    @FXML
    private void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

    @FXML
    private void goToHome() {
        fadeToPage("home.fxml");
    }

    @FXML
    private void goToBooking() {
        fadeToPage("BookingPage.fxml");
    }

    @FXML
    private void goToSubscription() {
        fadeToPage("subscription.fxml");
    }

    @FXML
    private void goToGym() {
        fadeToPage("gym.fxml");
    }

    @FXML
    private void goToLogin() {
        AppContext.getInstance().clear();
        fadeToPage("home.fxml");
    }

    @FXML
    private void editUserInfo() {
        if (isEditing) return;

        isEditing = true;
        editButton.setVisible(false);
        saveButton.setVisible(true);

        // Convert labels to text fields
        nameField = new TextField(nameLabel.getText());
        lastNameField = new TextField(lastNameLabel.getText());
        emailField = new TextField(emailLabel.getText().equals("Not provided") ? "" : emailLabel.getText());
        phoneNumberField = new TextField(phoneNumberLabel.getText());
        usernameField = new TextField(usernameLabel.getText());

        nameField.getStyleClass().add("account-field");
        lastNameField.getStyleClass().add("account-field");
        emailField.getStyleClass().add("account-field");
        phoneNumberField.getStyleClass().add("account-field");
        usernameField.getStyleClass().add("account-field");

        nameBox.getChildren().set(1, nameField);
        lastNameBox.getChildren().set(1, lastNameField);
        emailBox.getChildren().set(1, emailField);
        phoneBox.getChildren().set(1, phoneNumberField);
        usernameBox.getChildren().set(1, usernameField);
    }

    @FXML
    private void saveUserInfo() {
        if (!isEditing) return;

        AppContext context = AppContext.getInstance();
        String newFirstName = nameField.getText().trim();
        String newLastName = lastNameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPhoneNumber = phoneNumberField.getText().trim();
        String newUsername = usernameField.getText().trim();

        // Validate input (basic example)
        if (newFirstName.isEmpty() || newLastName.isEmpty() || newPhoneNumber.isEmpty() || newUsername.isEmpty()) {
            showAlert("All fields except email must be filled.", getStage());
            return;
        }

        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject request = new JSONObject();
                request.put("action", "update_user_info");
                request.put("memberId", context.getMemberId());
                request.put("firstName", newFirstName);
                request.put("lastName", newLastName);
                request.put("email", newEmail);
                request.put("phoneNumber", newPhoneNumber);
                request.put("username", newUsername);
                out.print(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                if ("success".equals(status)) {
                    Platform.runLater(() -> {
                        // Update AppContext
                        context.setFirstName(newFirstName);
                        context.setLastName(newLastName);
                        context.setEmail(newEmail);
                        context.setPhoneNumber(newPhoneNumber);
                        context.setUsername(newUsername);

                        // Revert to labels
                        nameLabel.setText(newFirstName);
                        lastNameLabel.setText(newLastName);
                        emailLabel.setText(newEmail.isEmpty() ? "Not provided" : newEmail);
                        phoneNumberLabel.setText(newPhoneNumber);
                        usernameLabel.setText(newUsername);

                        nameBox.getChildren().set(1, nameLabel);
                        lastNameBox.getChildren().set(1, lastNameLabel);
                        emailBox.getChildren().set(1, emailLabel);
                        phoneBox.getChildren().set(1, phoneNumberLabel);
                        usernameBox.getChildren().set(1, usernameLabel);

                        isEditing = false;
                        editButton.setVisible(true);
                        saveButton.setVisible(false);
                        showAlert( "User information updated successfully.", getStage());
                    });
                } else {
                    throw new Exception(jsonResponse.optString("message", "Failed to update user info"));
                }

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showAlert("Failed to update user info: " + ex.getMessage(), getStage());
                    isEditing = false;
                    editButton.setVisible(true);
                    saveButton.setVisible(false);
                });
            }
        }).start();
    }

    private void navigateToPage(String fxmlFile, int direction) {
        if (isNavigating) return;

        try {
            if (getClass().getResource(fxmlFile) == null) {
                throw new IOException("FXML file not found: " + fxmlFile);
            }

            isNavigating = true;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newPage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
                isNavigating = false;
            });

            slideOut.play();
            slideIn.play();
        } catch (Exception e) {
            isNavigating = false;
            System.err.println("Failed to navigate to page " + fxmlFile + ": " + e.getMessage());
            showAlert("Unable to load page: " + e.getMessage(), getStage());
        }
    }

    private void fadeToPage(String fxmlFile) {
        try {
            Parent newPage = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene currentScene = accountRoot.getScene();
            Parent currentPage = currentScene.getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentPage);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                currentScene.setRoot(newPage);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), newPage);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } catch (IOException e) {
            System.err.println("Error loading " + fxmlFile + ": " + e.getMessage());
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
        return (Stage) accountRoot.getScene().getWindow();
    }
}