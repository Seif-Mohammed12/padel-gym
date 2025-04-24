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

    @FXML private TextField visiblePasswordField;
    @FXML private ImageView togglePasswordIcon;

    private boolean isPasswordVisible = false;

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
                        String message = jsonResponse.optString("message", "No message provided"); // Use optString to avoid exception if "message" is missing
                        responseLabel.setText(message);

                        if (status.equals("success")) {
                            responseLabel.getStyleClass().add("success");
                            try {
                                // Load success page
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/padelfrontend/SuccessPage.fxml"));
                                Scene successScene = new Scene(loader.load());
                                Stage stage = (Stage) usernameField.getScene().getWindow();
                                stage.setScene(successScene);
                                stage.setTitle("Login Success");
                                stage.setMaximized(true); // Keep full-screen
                            } catch (Exception e) {
                                responseLabel.setText("Error loading success page: " + e.getMessage());
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
    private void goToSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SignUpPage.fxml"));
            Parent signUpPage = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent loginPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(loginPage, signUpPage);
            signUpPage.translateXProperty().set(currentScene.getWidth());
            currentScene.setRoot(transitionPane); // Set temp container first

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), loginPage);
            slideOut.setToX(-currentScene.getWidth());

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), signUpPage);
            slideIn.setToX(0);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear(); // remove both nodes from stackpane
                currentScene.setRoot(signUpPage); // now it's safe to set the new root
            });

            slideOut.play();
            slideIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}