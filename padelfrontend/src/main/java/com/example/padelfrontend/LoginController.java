package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label responseLabel;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private ImageView togglePasswordIcon;

    private boolean isPasswordVisible = false;
    private static final Duration TRANSITION_DURATION = Duration.millis(400);

    @FXML
    public void initialize() {
        togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/hidden.png")));
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/hidden.png")));
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            togglePasswordIcon.setImage(new Image(getClass().getResourceAsStream("/icons/show.png")));
        }
    }

    @FXML
    private void handleLoginButton() {
        String username = usernameField.getText().trim();
        String password = isPasswordVisible
                ? visiblePasswordField.getText().trim()
                : passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            responseLabel.setText("Please enter both username and password");
            responseLabel.getStyleClass().remove("success");
            return;
        }

        sendLoginRequest(username, password);
    }

    private void sendLoginRequest(String username, String password) {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Build JSON request
                JSONObject request = new JSONObject();
                request.put("action", "login");
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
                    try {
                        String status = jsonResponse.getString("status");
                        String message = jsonResponse.optString("message", "No message provided");
                        responseLabel.setText(message);

                        if (status.equals("success")) {
                            responseLabel.getStyleClass().add("success");

                            // Extract user data and store in AppContext
                            JSONObject userData = jsonResponse.getJSONObject("data");
                            String loggedInUsername = userData.getString("username");
                            String memberId = userData.getString("memberId");
                            String firstName = userData.getString("firstName");
                            String lastName = userData.getString("lastName");
                            String role = userData.getString("role");
                            String phoneNumber = userData.optString("phoneNumber", "");
                            String email = userData.optString("email", "");

                            // Store data in AppContext
                            AppContext context = AppContext.getInstance();
                            context.setUsername(loggedInUsername);
                            context.setMemberId(memberId);
                            context.setFirstName(firstName);
                            context.setLastName(lastName);
                            context.setRole(role);
                            context.setPhoneNumber(phoneNumber);
                            context.setEmail(email);

                            try {
                                // Load home page
                                Parent homePage = FXMLLoader.load(getClass().getResource("home.fxml"));
                                Scene currentScene = usernameField.getScene();
                                Parent currentPage = currentScene.getRoot();

                                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentPage);
                                fadeOut.setFromValue(1.0);
                                fadeOut.setToValue(0.0);

                                fadeOut.setOnFinished(e -> {
                                    currentScene.setRoot(homePage);
                                    FadeTransition fadeIn = new FadeTransition(Duration.millis(500), homePage);
                                    fadeIn.setFromValue(0.0);
                                    fadeIn.setToValue(1.0);
                                    fadeIn.play();
                                });

                                fadeOut.play();
                            } catch (Exception e) {
                                responseLabel.setText("Error loading home page: " + e.getMessage());
                                responseLabel.getStyleClass().remove("success");
                            }
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
                javafx.application.Platform.runLater(() -> {
                    responseLabel.setText("Error: " + ex.getMessage());
                    responseLabel.getStyleClass().remove("success");
                });
            }
        }).start();
    }

    @FXML
    private void goToSignUp() {
        navigateToPage("SignUpPage.fxml", 1);
    }
    @FXML
    private void goToForgotPassword() {
        navigateToPage("ForgotPasswordPage.fxml", -1);
    }

    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(TRANSITION_DURATION, currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());

            TranslateTransition slideIn = new TranslateTransition(TRANSITION_DURATION, newPage);
            slideIn.setToX(0);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}