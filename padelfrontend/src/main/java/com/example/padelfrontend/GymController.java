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
import javafx.scene.shape.Circle;
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
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder.jpg";
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
     * Creates a UI card for a gym class with image, details, and action button.
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
        VBox card = new VBox(10);
        card.getStyleClass().add("gym-class-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinHeight(200);
        card.setAlignment(Pos.TOP_CENTER);

        // Add the class image
        ImageView imageView = createClassImage(imagePath);
        imageView.getStyleClass().add("gym-class-image");

        // Add class details
        VBox details = createClassDetails(name, instructor, time, capacity, currentParticipants, waitlistSize, isFull);

        // Add action button
        Button actionButton = createActionButton(isFull, name);

        // Assemble the card
        card.getChildren().addAll(imageView, details, actionButton);

        // Add hover effect to the card
        addCardHoverEffect(card);

        return card;
    }

    /**
     * Creates an ImageView for the gym class with a circular clip.
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
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        Circle clip = new Circle(40, 40, 40); // Circular image
        imageView.setClip(clip);
        return imageView;
    }

    /**
     * Creates a VBox containing the gym class details.
     *
     * @param name              The class name.
     * @param instructor        The instructor's name.
     * @param time              The class time.
     * @param capacity          The class capacity.
     * @param currentParticipants The number of current participants.
     * @param waitlistSize      The size of the waitlist.
     * @param isFull            Whether the class is full.
     * @return A VBox with the class details.
     */
    private VBox createClassDetails(String name, String instructor, String time, int capacity,
                                    int currentParticipants, int waitlistSize, boolean isFull) {
        VBox details = new VBox(5);
        details.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("gym-class-name");

        Label instructorLabel = new Label("Instructor: " + instructor);
        instructorLabel.getStyleClass().add("gym-class-detail");

        Label timeLabel = new Label("Time: " + time);
        timeLabel.getStyleClass().add("gym-class-detail");

        Label capacityLabel = new Label("Capacity: " + currentParticipants + "/" + capacity);
        capacityLabel.getStyleClass().add("gym-class-detail");
        if (isFull) {
            capacityLabel.getStyleClass().add("capacity-full");
        }

        Label waitlistLabel = new Label(isFull ? "Waitlist: " + waitlistSize : "Waitlist: 0");
        waitlistLabel.getStyleClass().add("gym-class-detail");

        details.getChildren().addAll(nameLabel, instructorLabel, timeLabel, capacityLabel, waitlistLabel);
        return details;
    }

    /**
     * Creates the action button ("Book" or "Join Waitlist") for the gym class.
     *
     * @param isFull Whether the class is full.
     * @param className The name of the class.
     * @return A Button with the appropriate action.
     */
    private Button createActionButton(boolean isFull, String className) {
        Button actionButton = new Button(isFull ? "Join Waitlist" : "Book");
        actionButton.getStyleClass().add("gym-action-button");

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
     * @param isFull Whether the class is full.
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
     * @param title The title of the alert.
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
        navigateToPage("home.fxml", 1);
    }

    /**
     * Navigates to the Subscription page (not implemented).
     */
    @FXML
    private void goToSubscription() {
        System.out.println("Subscription page navigation not implemented yet.");
    }

    /**
     * Already on the Gym page, no navigation needed.
     */
    @FXML
    private void goToGym() {
        // No action needed
    }

    /**
     * Navigates to the specified page with a slide transition.
     *
     * @param fxmlFile The FXML file of the target page.
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