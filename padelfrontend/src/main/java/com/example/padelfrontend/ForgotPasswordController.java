package com.example.padelfrontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;

public class ForgotPasswordController {

    @FXML private TextField usernameField;
    @FXML private TextField recoveredPasswordField;
    @FXML private Label responseLabel;
    @FXML private Button submitButton;
    @FXML private Hyperlink loginLink;
    private static final Duration TRANSITION_DURATION = Duration.millis(400);

    @FXML
    public void initialize() {
        // Ensure recovered password field is uneditable
        recoveredPasswordField.setEditable(false);
    }

    @FXML
    private void handleSubmitButton() {
        String username = usernameField.getText().trim();

        // Client-side validation
        if (username.isEmpty()) {
            responseLabel.setText("Username is required");
            responseLabel.getStyleClass().remove("success");
            return;
        }

        sendForgotPasswordRequest(username);
    }

    private void sendForgotPasswordRequest(String username) {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Build JSON request
                JSONObject request = new JSONObject();
                request.put("action", "forgotPassword");
                request.put("username", username);

                // Send request
                out.print(request.toString() + "\n");
                out.flush();

                // Read response
                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                // Parse response
                JSONObject jsonResponse = new JSONObject(response);

                // Update UI
                Platform.runLater(() -> {
                    try {
                        String status = jsonResponse.optString("status", "error");
                        String message = jsonResponse.optString("message", "No message provided");
                        String password = jsonResponse.optString("password", "");

                        responseLabel.setText(message);
                        if ("success".equals(status) && !password.isEmpty()) {
                            recoveredPasswordField.setText(password);
                            responseLabel.getStyleClass().add("success");
                        } else {
                            recoveredPasswordField.setText("");
                            responseLabel.getStyleClass().remove("success");
                        }
                    } catch (Exception e) {
                        recoveredPasswordField.setText("");
                        responseLabel.setText("Error parsing server response: " + e.getMessage());
                        responseLabel.getStyleClass().remove("success");
                    }
                });

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    recoveredPasswordField.setText("");
                    responseLabel.setText("Error: " + ex.getMessage());
                    responseLabel.getStyleClass().remove("success");
                });
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        navigateToPage("LoginPage.fxml", 1);
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