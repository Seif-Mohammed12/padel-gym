package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

public class SignUpController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visiblePasswordField;
    @FXML private ImageView togglePasswordIcon;
    @FXML private Label responseLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML private TextField visibleConfirmPasswordField;


    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Set the initial icon to 'hidden' for both fields
        togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/hidden.png")));
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
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = isPasswordVisible
                ? visiblePasswordField.getText().trim()
                : passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            responseLabel.setText("Please fill all fields");
            responseLabel.getStyleClass().remove("success");
            return;
        }

        if (!password.equals(confirmPassword)) {
            responseLabel.setText("Passwords do not match");
            responseLabel.getStyleClass().remove("success");
            return;
        }

        sendSignUpRequest(firstName, lastName, username, password);
    }

    private void sendSignUpRequest(String firstName, String lastName, String username, String password) {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Build JSON request
                JSONObject request = new JSONObject();
                request.put("action", "signUp");
                request.put("firstName", firstName);
                request.put("lastName", lastName);
                request.put("username", username);
                request.put("password", password);

                // Send request with newline
                out.println(request.toString());

                // Read response
                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }
                JSONObject jsonResponse = new JSONObject(response);

                // Update UI
                javafx.application.Platform.runLater(() -> {
                    String status = jsonResponse.getString("status");
                    String message = jsonResponse.getString("message");
                    responseLabel.setText(message);
                    if (status.equals("success")) {
                        responseLabel.getStyleClass().add("success");
                        goToLogin(new ActionEvent());
                    } else {
                        responseLabel.getStyleClass().remove("success");
                    }
                });

                socket.close();
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    responseLabel.setText("Error: " + ex.getMessage());
                    responseLabel.getStyleClass().remove("success");
                });
            }
        }).start();
    }
    @FXML
    private void goToLogin(ActionEvent event) {
        try {
            Parent loginPage = FXMLLoader.load(getClass().getResource("LoginPage.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();

            // Fade out transition for the current scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                // Set the new scene root after fade out completes
                currentScene.setRoot(loginPage);

                // Fade in transition for the new scene root (login page)
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


}
