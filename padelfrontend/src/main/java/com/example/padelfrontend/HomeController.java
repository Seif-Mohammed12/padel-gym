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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
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
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";

    @FXML
    public void initialize() {
        setupNavbar();
        homeButton.getStyleClass().add("active");
        updateLoginButton();
        updateJoinButton();
        fetchPadelCenters();
        fetchGymClasses();
        loadActiveSubscriptions();
        Platform.runLater(() -> {
            loginButton.requestLayout();
            navbar.requestLayout();
        });
    }

    /**
     * Updates the login button text based on the user's login state.
     */
    void updateLoginButton() {
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
    void updateJoinButton() {
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
            Image image = loadImage(imageUrl).getImage();
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
     * Loads active subscriptions for the current user from the server and updates AppContext.
     * @return JSONArray of active subscriptions, or null if none exist or an error occurs.
     */
    /**
     * Loads active subscriptions for the current user from the server and updates AppContext.
     * @return JSONArray of active subscriptions, or null if none exist or an error occurs.
     */
    private JSONArray loadActiveSubscriptions() {
        AppContext context = AppContext.getInstance();
        try {
            String memberId = context.getMemberId();
            if (memberId == null) {
                //showAlert("Error", "Member ID is missing. Please update your profile.");
                return null;
            }

            JSONObject request = new JSONObject();
            request.put("action", "get_active_subscriptions");
            request.put("memberId", memberId);

            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            System.out.println("Server response: " + response);

            if (!responseJson.getString("status").equals("success")) {
                //showAlert("Error", responseJson.getString("message"));
                return null;
            }

            JSONArray activeSubs = responseJson.getJSONArray("data");
            // Update AppContext with subscription details
            if (activeSubs.length() > 0) {
                JSONObject sub = activeSubs.getJSONObject(0);
                context.setSubscribedPlanName(sub.optString("planName", "Unknown Plan"));
                context.setSubscribedDuration(sub.optString("duration", "Unknown Duration"));
                context.setDob(sub.optString("dob", null)); // Set dob from server response
                System.out.println(context.getDob());
            } else {
                context.setSubscribedPlanName(null);
                context.setSubscribedDuration(null);
                context.setDob(null); // Clear dob if no active subscriptions
            }

            return activeSubs;
        } catch (Exception e) {
            //showAlert("Error", "Failed to load active subscriptions: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String sendRequestToServer(String request) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sending request: " + request);
            out.println(request);
            String response = in.readLine();
            System.out.println("Server response: " + response);
            return response != null ? response : "{}";
        }
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
            Image image = loadImage(imageUrl).getImage();
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
     * Creates an image view for a class card using a classpath resource.
     *
     * @param imagePath The path to the image resource (e.g., /images/yoga.jpg).
     * @return An ImageView with the loaded image or a fallback.
     */
    private ImageView loadImage(String imagePath) {
        Image image;
        try {
            if (imagePath == null || imagePath.trim().isEmpty() || !imagePath.startsWith("/images/")) {
                System.err.println("Invalid image path: '" + imagePath + "', using placeholder.");
                image = loadResourceImage(PLACEHOLDER_IMAGE_PATH);
            } else {
                System.out.println("Loading image: " + imagePath);
                image = loadResourceImage(imagePath);
            }
        } catch (Exception e) {
            System.err.println("Failed to load image: '" + imagePath + "', using placeholder. Error: " + e.getMessage());
            image = loadResourceImage(PLACEHOLDER_IMAGE_PATH);
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(150);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    /**
     * Loads an image from a classpath resource, falling back to a transparent image if loading fails.
     *
     * @param resourcePath The classpath resource path (e.g., /images/yoga.jpg).
     * @return The loaded Image or a transparent 1x1 image.
     */
    private Image loadResourceImage(String resourcePath) {
        try {
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Image image = new Image(stream);
            if (image.isError()) {
                throw new IOException("Image failed to load: " + resourcePath);
            }
            return image;
        } catch (Exception e) {
            System.err.println("Failed to load resource: '" + resourcePath + "'. Error: " + e.getMessage());
            // Fallback to a transparent 1x1 image
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
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
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            context.clear();
            loginButton.setText("Login");
            loginButton.setOnMouseEntered(null);
            loginButton.setOnMouseExited(null);
            updateJoinButton();
            homeButton.getStyleClass().add("active");
            loginButton.setMinWidth(Region.USE_COMPUTED_SIZE);
        } else {
            fadeToPage("LoginPage.fxml");
        }
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