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
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controller for the Gym page, responsible for displaying gym classes and handling navigation.
 */
public class GymController {

    // FXML injected UI elements
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

    // Constants
    private static final String GYM_CLASSES_FILE = "padelbackend/gym-classes.json";
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/yoga.jpg";
    private static final String CLASS_ICON_PATH = "/icons/dumbbell.png"; // Add this icon to your resources
    private static final String BOOKING_ICON_PATH = "/icons/booking.png";
    private static final String WAITLIST_ICON_PATH = "/icons/hourglass.png";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";
    private static final int MEMBER_ID = 1; // Placeholder for member ID

    /**
     * Initializes the controller after FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        // Set the gym button as active
        setActiveButton(gymButton);

        // Load gym classes from file
        loadGymClassesFromFile();

        // Add hover effects to navigation bar buttons
        addNavButtonHoverEffects();
    }

    /**
     * Sets the specified button as active and deactivates others in the navbar.
     *
     * @param activeButton The button to set as active.
     */
    @FXML
    private void setActiveButton(Button activeButton) {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("active");
            }
        }
        activeButton.getStyleClass().add("active");
    }

    /**
     * Adds hover effects to navigation bar buttons using scale transitions.
     */
    private void addNavButtonHoverEffects() {
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

    /**
     * Loads gym classes from the JSON file and displays them.
     */
    private void loadGymClassesFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(GYM_CLASSES_FILE)));
            JSONArray classesArray = new JSONArray(content);
            displayGymClasses(classesArray);
        } catch (IOException e) {
            System.err.println("Failed to load gym classes from file: " + e.getMessage());
            showAlert("Error", "Unable to load gym classes: " + e.getMessage());
        }
    }

    /**
     * Displays gym classes in the UI by creating cards for each class.
     *
     * @param classesArray The JSON array of gym classes.
     */
    private void displayGymClasses(JSONArray classesArray) {
        classesContainer.getChildren().clear();
        classesContainer.setHgap(20);
        classesContainer.setVgap(20);
        classesContainer.setPadding(new Insets(20));

        for (int i = 0; i < classesArray.length(); i++) {
            JSONObject gymClass = classesArray.getJSONObject(i);
            VBox card = createGymClassCard(gymClass);
            classesContainer.getChildren().add(card);
        }
    }

    /**
     * Creates a visually appealing UI card for a gym class with image, details, and action button.
     *
     * @param gymClass The JSON object representing the gym class.
     * @return A VBox containing the gym class card.
     */
    private VBox createGymClassCard(JSONObject gymClass) {
        // Extract class details
        String name = gymClass.getString("name");
        String instructor = gymClass.getString("instructor");
        String time = gymClass.getString("time");
        String imagePath = gymClass.getString("imagePath");
        int capacity = gymClass.getInt("capacity");
        int currentParticipants = gymClass.getInt("currentParticipants");
        int waitlistSize = gymClass.getInt("waitlistSize");
        boolean isFull = currentParticipants >= capacity;

        // Create the card container
        VBox card = new VBox(15);
        card.getStyleClass().add("gym-class-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setMinHeight(350);
        card.setAlignment(Pos.TOP_CENTER);

        // Add the class image
        ImageView imageView = createClassImage(imagePath);
        imageView.getStyleClass().add("gym-class-image");

        // Header with icon and name
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(CLASS_ICON_PATH)));
        icon.setFitWidth(24);
        icon.setFitHeight(24);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("gym-class-name");

        header.getChildren().addAll(icon, nameLabel);

        // Add class details
        VBox details = createClassDetails(instructor, time);

        // Add capacity indicator
        HBox capacityIndicator = createCapacityIndicator(capacity, currentParticipants);

        // Add waitlist info
        Label waitlistLabel = new Label(isFull ? "Waitlist: " + waitlistSize : "Waitlist: 0");
        waitlistLabel.getStyleClass().add("gym-class-detail");

        // Add action button
        Button actionButton = createActionButton(isFull, name);

        // Assemble the card
        card.getChildren().addAll(imageView, header, details, capacityIndicator, waitlistLabel, actionButton);

        // Add hover effect to the card
        addCardHoverEffect(card);

        return card;
    }

    /**
     * Creates an ImageView for the gym class with a rounded clip.
     *
     * @param imagePath The path to the class image.
     * @return An ImageView with the class image or a placeholder.
     */
    private ImageView createClassImage(String imagePath) {
        ImageView imageView = new ImageView();
        try {
            Image image = new Image(imagePath);
            if (image.isError()) {
                throw new IllegalArgumentException("Image failed to load");
            }
            imageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Failed to load image: " + imagePath);
            Image placeholder = new Image(getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH));
            imageView.setImage(placeholder);
        }
        imageView.setFitWidth(260);
        imageView.setFitHeight(150);
        Rectangle clip = new Rectangle(260, 150);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);
        return imageView;
    }

    /**
     * Creates a VBox containing the gym class details.
     *
     * @param instructor The instructor's name.
     * @param time       The class time.
     * @return A VBox with the class details.
     */
    private VBox createClassDetails(String instructor, String time) {
        VBox details = new VBox(5);
        details.setAlignment(Pos.CENTER_LEFT);

        Label instructorLabel = new Label("Instructor: " + instructor);
        instructorLabel.getStyleClass().add("gym-class-detail");

        Label timeLabel = new Label("Time: " + time);
        timeLabel.getStyleClass().add("gym-class-detail");

        details.getChildren().addAll(instructorLabel, timeLabel);
        return details;
    }

    /**
     * Creates a horizontal capacity indicator with a circular percentage and a progress bar.
     *
     * @param capacity          The total capacity of the class.
     * @param currentParticipants The current number of participants.
     * @return An HBox containing the capacity indicator.
     */
    private HBox createCapacityIndicator(int capacity, int currentParticipants) {
        HBox capacityBox = new HBox(15);
        capacityBox.setAlignment(Pos.CENTER_LEFT);

        // Circular percentage indicator
        StackPane indicator = new StackPane();
        indicator.setAlignment(Pos.CENTER);

        Circle outerCircle = new Circle(24);
        outerCircle.getStyleClass().add("capacity-circle");

        Circle innerCircle = new Circle(20);
        innerCircle.getStyleClass().add("capacity-inner-circle");

        double percentage = (double) currentParticipants / capacity;
        Label percentageLabel = new Label(String.format("%.0f%%", percentage * 100));
        percentageLabel.getStyleClass().add("capacity-label");

        indicator.getChildren().addAll(outerCircle, innerCircle, percentageLabel);

        // Progress bar with numeric label
        VBox progressSection = new VBox(5);
        progressSection.setAlignment(Pos.CENTER_LEFT);

        Label capacityLabel = new Label(currentParticipants + "/" + capacity + " Participants");
        capacityLabel.getStyleClass().add("gym-class-detail");

        // Use JavaFX ProgressBar
        ProgressBar progressBar = new ProgressBar(percentage);
        progressBar.setPrefWidth(150);
        progressBar.setPrefHeight(12);
        progressBar.getStyleClass().add("capacity-progress");

        // Dynamic styling based on capacity
        if (percentage >= 1.0) {
            progressBar.getStyleClass().add("capacity-full");
        } else if (percentage >= 0.75) {
            progressBar.getStyleClass().add("capacity-warning");
        }

        progressSection.getChildren().addAll(capacityLabel, progressBar);

        capacityBox.getChildren().addAll(indicator, progressSection);
        return capacityBox;
    }

    /**
     * Creates the action button ("Book" or "Join Waitlist") for the gym class.
     *
     * @param isFull    Whether the class is full.
     * @param className The name of the class.
     * @return A Button with the appropriate action.
     */
    private Button createActionButton(boolean isFull, String className) {
        Button actionButton = new Button(isFull ? "Join Waitlist" : "Book");
        actionButton.getStyleClass().add("gym-action-button");

        if( isFull ) {
            ImageView arrowIcon = new ImageView(new Image(getClass().getResourceAsStream(WAITLIST_ICON_PATH)));
            arrowIcon.setFitWidth(16);
            arrowIcon.setFitHeight(16);
            actionButton.setGraphic(arrowIcon);
        }
        else{
            ImageView arrowIcon = new ImageView(new Image(getClass().getResourceAsStream(BOOKING_ICON_PATH)));
            arrowIcon.setFitWidth(16);
            arrowIcon.setFitHeight(16);
            actionButton.setGraphic(arrowIcon);
        }

        actionButton.setOnAction(e -> handleBookingAction(isFull, className));
        return actionButton;
    }

    /**
     * Adds a hover effect to the gym class card using scale transitions.
     *
     * @param card The card to apply the effect to.
     */
    private void addCardHoverEffect(VBox card) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(200), card);
        grow.setToX(1.05);
        grow.setToY(1.05);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), card);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        card.setOnMouseEntered(e -> grow.playFromStart());
        card.setOnMouseExited(e -> shrink.playFromStart());
    }

    /**
     * Handles the booking action (book or join waitlist) when the action button is clicked.
     *
     * @param isFull    Whether the class is full.
     * @param className The name of the class.
     */
    private void handleBookingAction(boolean isFull, String className) {
        try {
            JSONObject request = new JSONObject();
            request.put("action", isFull ? "join_waitlist" : "book_class");
            request.put("memberId", MEMBER_ID);
            request.put("className", className);

            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);

            if (responseJson.getString("status").equals("success")) {
                String message = responseJson.getBoolean("waitlisted")
                        ? "You have been added to the waitlist!"
                        : "Class booked successfully!";
                showAlert("Success", message);
                refreshClasses();
            } else {
                String errorMessage = responseJson.optString("message", "Unknown error");
                showAlert("Error", "Failed to process booking: " + errorMessage);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to process booking: " + e.getMessage());
        }
    }

    /**
     * Sends a request to the server and returns the response.
     *
     * @param request The JSON request string to send.
     * @return The server's JSON response string.
     * @throws IOException If a communication error occurs.
     */
    private String sendRequestToServer(String request) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);
            String response = in.readLine();
            System.out.println("Server response: " + response);
            return response != null ? response : "{}";
        }
    }

    /**
     * Refreshes the gym classes display by fetching the latest data from the server.
     */
    private void refreshClasses() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_classes");
            String response = sendRequestToServer(request.toString());
            JSONArray classesArray = new JSONArray(response);
            displayGymClasses(classesArray);
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh classes: " + e.getMessage());
        }
    }

    /**
     * Shows an alert dialog with the specified title and message.
     *
     * @param title   The title of the alert.
     * @param message The message to display.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Navigates to the Home page with a slide transition.
     */
    @FXML
    private void goToHome() {
        navigateToPage("home.fxml", -1);
    }

    /**
     * Navigates to the Booking page with a slide transition.
     */
    @FXML
    private void goToBooking() {
        navigateToPage("BookingPage.fxml", -1);
    }

    /**
     * Navigates to the Subscription page with a slide transition.
     */
    @FXML
    private void goToSubscription() {
        navigateToPage("subscription.fxml", -1);
    }

    /**
     * Navigates to the specified page with a slide transition.
     *
     * @param fxmlFile  The FXML file of the target page.
     * @param direction The direction of the slide (-1 for left, 1 for right).
     */
    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();
            Stage stage = (Stage) classesContainer.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newPage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            System.err.println("Failed to navigate to page " + fxmlFile + ": " + e.getMessage());
            showAlert("Error", "Unable to load page: " + e.getMessage());
        }
    }
}