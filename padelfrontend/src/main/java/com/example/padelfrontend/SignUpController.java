package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.function.UnaryOperator;

public class SignUpController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private ImageView togglePasswordIcon;
    @FXML
    private Label responseLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField visibleConfirmPasswordField;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Set the initial icon to 'hidden' for both fields
        togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/hidden.png")));

        phoneNumberField.setText("+20");
        enforcePhoneNumberPrefix();
    }

    private void enforcePhoneNumberPrefix() {
        // TextFormatter to enforce +20 prefix
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // Allow empty field or +20 followed by up to 10 digits
            if (newText.isEmpty() || newText.equals("+20") || newText.matches("\\+20\\d{0,10}")) {
                return change;
            }
            // Block changes that remove or alter +20 prefix
            if (!newText.startsWith("+20")) {
                return null;
            }
            return change;
        };
        TextFormatter<String> formatter = new TextFormatter<>(filter);
        phoneNumberField.setTextFormatter(formatter);

        // Listener to revert to +20 if field is cleared
        phoneNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.startsWith("+20")) {
                phoneNumberField.setText("+20");
            }
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // Make both fields visible in plain text
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            visibleConfirmPasswordField.setText(confirmPasswordField.getText());
            visibleConfirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);

            togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/show.png")));
        } else {
            // Revert to password fields (masked)
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            confirmPasswordField.setText(visibleConfirmPasswordField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            visibleConfirmPasswordField.setVisible(false);
            visibleConfirmPasswordField.setManaged(false);

            togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/hidden.png")));
        }
    }

    @FXML
    private void handleSignUpButton() {
        String firstName = firstNameField.getText().trim();
        firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
        String lastName = lastNameField.getText().trim();
        lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1);
        String username = usernameField.getText().trim();
        String phoneNumber = phoneNumberField.getText().trim();
        String password = isPasswordVisible ? visiblePasswordField.getText().trim() : passwordField.getText().trim();
        String confirmPassword = isPasswordVisible ? visibleConfirmPasswordField.getText().trim() : confirmPasswordField.getText().trim();

        // Client-side validation
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            responseLabel.setText("Please fill all required fields (phone number is optional)");
            responseLabel.getStyleClass().remove("success");
            return;
        }
        if (!password.equals(confirmPassword)) {
            responseLabel.setText("Passwords do not match");
            responseLabel.getStyleClass().remove("success");
            return;
        }

        // Send empty phoneNumber if only +20 is present
        if (phoneNumber.equals("+20")) {
            phoneNumber = "";
        }

        sendSignUpRequest(firstName, lastName, username, phoneNumber, password);
    }

    private void sendSignUpRequest(String firstName, String lastName, String username, String phoneNumber, String password) {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Build JSON request
                JSONObject request = new JSONObject();
                request.put("action", "signup");
                request.put("firstName", firstName);
                request.put("lastName", lastName);
                request.put("username", username);
                request.put("phoneNumber", phoneNumber);
                request.put("password", password);
                request.put("role", "user");

                // Convert to string and send
                String requestString = request.toString();
                System.out.println("Sending request: " + requestString);

                // Send request with explicit newline
                out.print(requestString + "\n");
                out.flush();

                // Read response from the C++ server
                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                // Debug: Print the response
                System.out.println("Received response: " + response);

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response);

                // Update UI based on server response
                Platform.runLater(() -> {
                    try {
                        String status = jsonResponse.optString("status", "error");
                        String message = jsonResponse.optString("message", "No message provided");

                        responseLabel.setText(message);
                        if ("success".equals(status)) {
                            responseLabel.getStyleClass().add("success");
                            goToLogin(); // Move to the login page if successful
                        } else {
                            responseLabel.getStyleClass().remove("success");
                        }
                    } catch (Exception e) {
                        responseLabel.setText("Error parsing server response: " + e.getMessage());
                        responseLabel.getStyleClass().remove("success");
                    }
                });

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    responseLabel.setText("Error: " + ex.getMessage());
                    System.out.println("Error in SignUpController.sendSignUpRequest(): " + ex.getMessage());
                    responseLabel.getStyleClass().remove("success");
                });
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            // Load the LoginPage FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
            Parent loginPage = loader.load();

            Scene scene = usernameField.getScene();
            Parent currentPage = scene.getRoot(); // Current root (which is the SignUpPage)

            // Create a StackPane to manage the transition
            StackPane rootContainer = new StackPane(currentPage, loginPage);
            loginPage.translateXProperty().set(-scene.getWidth()); // Position the new page off-screen to the left
            scene.setRoot(rootContainer); // Set root to transition container first

            // Create transitions for sliding out the current page and sliding in the new
            // one
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), currentPage);
            slideOut.setToX(scene.getWidth()); // Move current page out to the right

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), loginPage);
            slideIn.setToX(0); // Move the new page in from the left

            // Play both transitions
            slideOut.play();
            slideIn.play();

            // Once the slide-in transition finishes, set the new page as the root
            slideIn.setOnFinished(event -> {
                rootContainer.getChildren().clear(); // Remove both pages from the transition container
                scene.setRoot(loginPage); // Set the new root to the loginPage
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
