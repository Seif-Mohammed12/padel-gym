package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Controller for the Home page, handling navigation and dynamic content loading.
 */
public class HomeController {

    @FXML private BorderPane homeRoot;
    @FXML private HBox navbar;
    @FXML private Button homeButton;
    @FXML private Button bookingButton;
    @FXML private Button subscriptionButton;
    @FXML private Button gymButton;
    @FXML private Button loginButton;
    @FXML private Button joinButton;
    @FXML private Button viewPlansButton;
    @FXML private HBox centersContainer;
    @FXML private HBox classesContainer;

    private static final Duration TRANSITION_DURATION = Duration.millis(400);
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder.png";

    @FXML
    public void initialize() {
        setupNavbar();
        homeButton.getStyleClass().add("active");
        updateLoginButton();
        updateJoinButton();
        fetchPadelCenters();
        fetchGymClasses();
    }

    /**
     * Updates the login button text based on the user's login state.
     */
    private void updateLoginButton() {
        AppContext context = AppContext.getInstance();
        String originalText;
        if (context.isLoggedIn()) {
            String firstName = context.getFirstName();
            originalText = "Welcome, " + firstName;
            loginButton.setText(originalText);
            loginButton.setMinWidth(150); // Set a minimum width to prevent layout shifts
            // Add hover effects to change text to "Logout" when signed in
            loginButton.setOnMouseEntered(e -> {
                loginButton.setText("Logout");
                loginButton.requestLayout(); // Force layout update to prevent text overlap
            });
            loginButton.setOnMouseExited(e -> {
                loginButton.setText(originalText);
                loginButton.requestLayout(); // Force layout update to ensure proper rendering
            });
        } else {
            originalText = "Login";
            loginButton.setText(originalText);
            // Remove hover effects when not signed in to avoid interference
            loginButton.setOnMouseEntered(null);
            loginButton.setOnMouseExited(null);
        }
    }

    /**
     * Updates the join button based on the user's login state.
     * If signed in, replaces "Join Now" with "My Account".
     */
    private void updateJoinButton() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            joinButton.setText("My Account");
            joinButton.setOnAction(event -> goToMyAccount());
            joinButton.getStyleClass().remove("join-button");
            joinButton.getStyleClass().add("my-account-button");
        } else {
            joinButton.setText("Join Now");
            joinButton.setOnAction(event -> goToSignUp());
            joinButton.getStyleClass().remove("my-account-button");
            joinButton.getStyleClass().add("join-button");
        }
    }

    /**
     * Fetches padel centers from the server and populates the centersContainer.
     */
    private void fetchPadelCenters() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send get_padel_centers request
                JSONObject request = new JSONObject();
                request.put("action", "get_padel_centers");
                out.print(request.toString() + "\n");
                out.flush();

                // Read response
                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                // Parse response
                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                if ("success".equals(status)) {
                    JSONArray centers = jsonResponse.getJSONArray("data");
                    Platform.runLater(() -> populatePadelCenters(centers));
                } else {
                    throw new Exception(jsonResponse.optString("message", "Failed to fetch padel centers"));
                }

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Failed to load padel centers: " + ex.getMessage());
                    errorLabel.getStyleClass().add("error-label");
                    centersContainer.getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    /**
     * Populates the centersContainer with padel center cards.
     */
    private void populatePadelCenters(JSONArray centers) {
        centersContainer.getChildren().clear();
        for (int i = 0; i < Math.min(centers.length(), 8); i++) { // Limit to 5 centers
            JSONObject center = centers.getJSONObject(i);
            String name = center.optString("name", "Unknown Center");
            String location = center.optString("location", "Unknown Location");
            String imageUrl = center.optString("image", "");

            VBox card = new VBox(10);
            card.getStyleClass().add("center-card");
            card.setAlignment(javafx.geometry.Pos.CENTER);

            ImageView imageView = new ImageView();
            Image image = loadImage(imageUrl);
            if (image != null) {
                imageView.setImage(image);
            } else {
                System.err.println("Failed to load image for: " + name + ". Using no image.");
            }
            imageView.getStyleClass().add("center-image");

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("center-name");

            Label locationLabel = new Label(location);
            locationLabel.getStyleClass().add("center-location");

            card.getChildren().addAll(imageView, nameLabel, locationLabel);
            centersContainer.getChildren().add(card);
        }
    }

    /**
     * Fetches gym classes from the server and populates the classesContainer.
     */
    private void fetchGymClasses() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send get_classes request
                JSONObject request = new JSONObject();
                request.put("action", "get_classes");
                out.print(request.toString() + "\n");
                out.flush();

                // Read response
                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                // Parse response
                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                if ("success".equals(status)) {
                    JSONArray classes = jsonResponse.getJSONArray("data");
                    Platform.runLater(() -> populateGymClasses(classes));
                } else {
                    throw new Exception(jsonResponse.optString("message", "Failed to fetch gym classes"));
                }

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Failed to load gym classes: " + ex.getMessage());
                    errorLabel.getStyleClass().add("error-label");
                    classesContainer.getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    /**
     * Populates the classesContainer with gym class cards.
     */
    private void populateGymClasses(JSONArray classes) {
        classesContainer.getChildren().clear();
        for (int i = 0; i < Math.min(classes.length(), 3); i++) { // Limit to 3 classes
            JSONObject gymClass = classes.getJSONObject(i);
            String name = gymClass.optString("name", "Unknown Class");
            String instructor = gymClass.optString("instructor", "Unknown Instructor");
            String time = gymClass.optString("time", "Unknown Time");
            String imageUrl = gymClass.optString("imagePath", "");

            VBox card = new VBox(10);
            card.getStyleClass().add("class-card");
            card.setAlignment(javafx.geometry.Pos.CENTER);

            ImageView imageView = new ImageView();
            Image image = loadImage(imageUrl);
            if (image != null) {
                imageView.setImage(image);
            } else {
                System.err.println("Failed to load image for: " + name + ". Using no image.");
            }
            imageView.getStyleClass().add("class-image");

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("class-name");

            Label infoLabel = new Label("Instructor: " + instructor + " | Time: " + time);
            infoLabel.getStyleClass().add("class-info");

            card.getChildren().addAll(imageView, nameLabel, infoLabel);
            classesContainer.getChildren().add(card);
        }
    }

    /**
     * Loads an image from the given URL or falls back to a placeholder.
     */
    private Image loadImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Try loading from URL or file
                return new Image(imageUrl, true); // true for background loading
            }
            // Fallback to placeholder
            return new Image(getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH));
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            // Return null or try placeholder again if the stream fails
            return getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH) != null
                    ? new Image(getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH))
                    : null;
        }
    }

    /**
     * Sets up the navbar with hover effects and removes active state from other buttons.
     */
    private void setupNavbar() {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button) {
                addButtonHoverEffects(button);

                // Preserve existing FXML onAction handler
                var existingHandler = button.getOnAction();
                button.setOnAction(event -> {
                    setInactiveButtons();
                    button.getStyleClass().add("active");

                    // Only trigger fallback navigation if no FXML handler exists
                    if (existingHandler == null) {
                        if (button == homeButton) {
                            // No navigation needed
                        } else if (button == bookingButton) {
                            goToBooking();
                        } else if (button == subscriptionButton) {
                            goToSubscription();
                        } else if (button == gymButton) {
                            goToGym();
                        }
                    }
                    // Let the FXML handler handle navigation if it exists
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

    /**
     * Removes the active style from all navbar buttons.
     */
    @FXML
    private void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

    @FXML
    private void goToBooking() {
        navigateToPage("BookingPage.fxml", 1);
    }

    @FXML
    private void goToSubscription() {
        navigateToPage("subscription.fxml", 1);
    }

    @FXML
    private void goToGym() {
        navigateToPage("gym.fxml", 1);
    }

    @FXML
    private void goToLogin() {
        fadeToPage("LoginPage.fxml");
    }

    @FXML
    private void goToSignUp() {
        fadeToPage("SignUpPage.fxml");
    }

    @FXML
    private void goToMyAccount() {
        fadeToPage("MyAccountPage.fxml");
    }

    /**
     * Navigates to a new page with a slide transition.
     */
    private void navigateToPage(String fxmlFile, int direction) {
        try {
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
            });

            slideOut.play();
            slideIn.play();
        } catch (Exception e) {
            System.err.println("Failed to navigate to page " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigates to a new page with a sequenced fade transition.
     * @param fxmlFile The FXML file of the target page.
     */
    private void fadeToPage(String fxmlFile) {
        try {
            // Load the new page
            Parent newPage = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene currentScene = homeRoot.getScene();
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
            e.printStackTrace();
        }
    }
}