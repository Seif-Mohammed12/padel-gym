package com.example.padelfrontend;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GymController {

    @FXML
    private FlowPane classesContainer;
    @FXML
    private Button bookingButton;
    @FXML
    private Button subscriptionButton;
    @FXML
    private Button gymButton;
    @FXML
    private Button homeButton;
    @FXML
    private HBox navbar;

    @FXML
    public void initialize() {
        gymButton.getStyleClass().add("active");

        // Load gym classes
        try {
            String jsonFilePath = "gym-classes.json";
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONArray classesArray = new JSONArray(content);
            loadGymClasses(classesArray);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add hover effects to navbar buttons
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

    @FXML
    public void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));
            Parent homePage = loader.load();
            Stage stage = (Stage) classesContainer.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();
            StackPane transitionPane = new StackPane(currentPage, homePage);
            homePage.translateXProperty().set(-currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentPage);
            slideOut.setToX(currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), homePage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(homePage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToBooking() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));
            Parent bookingPage = loader.load();
            Stage stage = (Stage) classesContainer.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();
            StackPane transitionPane = new StackPane(currentPage, bookingPage);
            bookingPage.translateXProperty().set(currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentPage);
            slideOut.setToX(-currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), bookingPage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(bookingPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToSubscription() {
        System.out.println("Subscription page navigation not implemented yet.");
    }

    @FXML
    private void goToGym() {
        // Already on the gym page, no action needed
    }

    // Method to add a gym class card to the grid dynamically
    public void loadGymClasses(JSONArray classesArray) {
        classesContainer.getChildren().clear();

        for (int i = 0; i < classesArray.length(); i++) {
            JSONObject gymClass = classesArray.getJSONObject(i);
            String name = gymClass.getString("name");
            String instructor = gymClass.getString("instructor");
            String time = gymClass.getString("time");
            String imagePath = gymClass.getString("image");
            int capacity = gymClass.getInt("capacity");
            int currentParticipants = gymClass.getInt("currentParticipants");
            int waitlistSize = gymClass.getInt("waitlistSize");
            boolean isFull = currentParticipants >= capacity;

            // Create the card container using HBox
            HBox card = new HBox(15);
            card.getStyleClass().add("gym-class-card");
            card.setPadding(new Insets(15));
            card.setPrefWidth(350);
            card.setMinHeight(150);

            // Left side: image
            ImageView imageView = new ImageView(new Image(imagePath));
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);
            Rectangle clip = new Rectangle(80, 80);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imageView.setClip(clip);

            // Center: class details
            VBox details = new VBox(5);
            details.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("gym-class-name");

            Label instructorLabel = new Label("Instructor: " + instructor);
            instructorLabel.getStyleClass().add("gym-class-detail");

            Label timeLabel = new Label("Time: " + time);
            timeLabel.getStyleClass().add("gym-class-detail");

            Label capacityLabel = new Label("Capacity: " + currentParticipants + "/" + capacity);
            capacityLabel.getStyleClass().add("gym-class-detail");
            if (isFull) {
                capacityLabel.setStyle("-fx-text-fill: #EF5350;"); // Red if full
            }

            Label waitlistLabel = new Label(isFull ? "Waitlist: " + waitlistSize : "Waitlist: 0");
            waitlistLabel.getStyleClass().add("gym-class-detail");

            details.getChildren().addAll(nameLabel, instructorLabel, timeLabel, capacityLabel, waitlistLabel);

            // Right side: action button
            Button actionButton = new Button(isFull ? "Join Waitlist" : "Book");
            actionButton.getStyleClass().add("gym-action-button");

            // Spacer to push the button to the right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add all elements to the card
            card.getChildren().addAll(imageView, details, spacer, actionButton);

            // Add the card to the grid
            classesContainer.getChildren().add(card);

            // Update the action button click handler
            actionButton.setOnAction(e -> {
                try {
                    // Create JSON request
                    JSONObject request = new JSONObject();
                    request.put("action", isFull ? "join_waitlist" : "book_class");
                    request.put("memberId", 1); // Replace with actual member ID
                    request.put("className", name);

                    // Send request to server
                    String response = sendRequestToServer(request.toString());
                    JSONObject responseJson = new JSONObject(response);

                    if (responseJson.getString("status").equals("success")) {
                        if (responseJson.getBoolean("waitlisted")) {
                            showAlert("Success", "You have been added to the waitlist!");
                        } else {
                            showAlert("Success", "Class booked successfully!");
                        }
                        // Refresh the classes display
                        refreshClasses();
                    }
                } catch (Exception ex) {
                    showAlert("Error", "Failed to process booking: " + ex.getMessage());
                }
            });
        }
    }

    private String sendRequestToServer(String request) throws IOException {
        // Implement your HTTP client logic here
        // This is a placeholder - you'll need to implement actual HTTP communication
        return ""; // Return server response
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshClasses() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_classes");
            String response = sendRequestToServer(request.toString());
            JSONArray classesArray = new JSONArray(response);
            loadGymClasses(classesArray);
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh classes: " + e.getMessage());
        }
    }
}