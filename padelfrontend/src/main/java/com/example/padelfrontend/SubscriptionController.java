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
 * Controller for the Subscription page, responsible for displaying subscription plans and handling subscriptions.
 */
public class SubscriptionController {

    // FXML injected UI elements
    @FXML
    private FlowPane plansContainer;
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
    private HBox durationSelector;
    @FXML
    private Button oneMonthButton;
    @FXML
    private Button threeMonthsButton;
    @FXML
    private Button sixMonthsButton;
    @FXML
    private Button oneYearButton;

    // Constants
    private static final String SUBSCRIPTIONS_FILE = "padelbackend/subscriptions.json";
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder.jpg";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";
    private static final int MEMBER_ID = 1; // Placeholder for member ID

    // State
    private String selectedDuration = "1_month"; // Default duration
    private JSONArray currentPlansArray; // Store the plans for refreshing

    /**
     * Initializes the controller after FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        // Set the subscription button as active
        setActiveButton(subscriptionButton);

        // Add hover effects to navigation bar buttons
        addNavButtonHoverEffects();

        // Set default duration and style
        setActiveDurationButton(oneMonthButton);

        // Add hover effects to duration buttons
        addDurationButtonHoverEffects();

        // Load subscription plans from file
        loadSubscriptionPlansFromFile();
    }

    /**
     * Sets the specified button as active and deactivates others in the navbar.
     *
     * @param activeButton The button to set as active.
     */
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
     * Sets the specified duration button as active and deactivates others.
     *
     * @param activeButton The duration button to set as active.
     */
    private void setActiveDurationButton(Button activeButton) {
        for (Node node : durationSelector.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("active-duration");
            }
        }
        activeButton.getStyleClass().add("active-duration");
    }

    /**
     * Adds hover effects to duration selection buttons using scale transitions.
     */
    private void addDurationButtonHoverEffects() {
        for (Node node : durationSelector.getChildren()) {
            if (node instanceof Button button && button.getStyleClass().contains("duration-button")) {
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
     * Loads subscription plans from the JSON file and displays them.
     */
    private void loadSubscriptionPlansFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(SUBSCRIPTIONS_FILE)));
            currentPlansArray = new JSONArray(content);
            displaySubscriptionPlans(currentPlansArray);
        } catch (IOException e) {
            System.err.println("Failed to load subscription plans from file: " + e.getMessage());
            showAlert("Error", "Unable to load subscription plans: " + e.getMessage());
        }
    }

    /**
     * Displays subscription plans in the UI by creating cards for each plan.
     *
     * @param plansArray The JSON array of subscription plans.
     */
    private void displaySubscriptionPlans(JSONArray plansArray) {
        plansContainer.getChildren().clear();
        plansContainer.setHgap(20);
        plansContainer.setVgap(20);
        plansContainer.setPadding(new Insets(20));

        for (int i = 0; i < plansArray.length(); i++) {
            JSONObject plan = plansArray.getJSONObject(i);
            VBox card = createSubscriptionPlanCard(plan);
            plansContainer.getChildren().add(card);
        }
    }

    /**
     * Creates a UI card for a subscription plan with image, details, and action button.
     *
     * @param plan The JSON object representing the subscription plan.
     * @return A VBox containing the subscription plan card.
     */
    private VBox createSubscriptionPlanCard(JSONObject plan) {
        // Extract plan details
        String name = plan.getString("name");
        JSONObject pricing = plan.getJSONObject("pricing");
        double price = pricing.getDouble(selectedDuration);
        String durationText = selectedDuration.replace("_", " ").replace("month", "Month").replace("year", "Year");
        JSONArray featuresArray = plan.getJSONArray("features");
        String imagePath = plan.getString("imagePath");
        JSONArray subscribersArray = plan.getJSONArray("subscribers");
        boolean isSubscribed = false;
        for (int i = 0; i < subscribersArray.length(); i++) {
            if (subscribersArray.getInt(i) == MEMBER_ID) {
                isSubscribed = true;
                break;
            }
        }

        // Create the card container
        VBox card = new VBox(10);
        card.getStyleClass().add("subscription-plan-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinHeight(250);
        card.setAlignment(Pos.TOP_CENTER);

        // Add the plan image
        ImageView imageView = createPlanImage(imagePath);
        imageView.getStyleClass().add("plan-image");

        // Add plan details
        VBox details = createPlanDetails(name, price, durationText, featuresArray);

        // Add action button
        Button actionButton = createActionButton(isSubscribed, name);

        // Assemble the card
        card.getChildren().addAll(imageView, details, actionButton);

        // Add hover effect to the card
        addCardHoverEffect(card);

        return card;
    }

    /**
     * Creates an ImageView for the subscription plan with a circular clip.
     *
     * @param imagePath The path to the plan image.
     * @return An ImageView with the plan image or a placeholder.
     */
    private ImageView createPlanImage(String imagePath) {
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
     * Creates a VBox containing the subscription plan details.
     *
     * @param name        The plan name.
     * @param price       The plan price for the selected duration.
     * @param duration    The selected duration text.
     * @param featuresArray The array of plan features.
     * @return A VBox with the plan details.
     */
    private VBox createPlanDetails(String name, double price, String duration, JSONArray featuresArray) {
        VBox details = new VBox(5);
        details.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("plan-name");

        Label priceLabel = new Label(String.format("$%.2f / %s", price, duration));
        priceLabel.getStyleClass().add("plan-detail");

        VBox featuresBox = new VBox(3);
        for (int i = 0; i < featuresArray.length(); i++) {
            Label featureLabel = new Label("• " + featuresArray.getString(i));
            featureLabel.getStyleClass().add("plan-detail");
            featuresBox.getChildren().add(featureLabel);
        }

        details.getChildren().addAll(nameLabel, priceLabel, featuresBox);
        return details;
    }

    /**
     * Creates the action button ("Subscribed" or "Subscribe") for the subscription plan.
     *
     * @param isSubscribed Whether the user is already subscribed to this plan.
     * @param planName     The name of the plan.
     * @return A Button with the appropriate action.
     */
    private Button createActionButton(boolean isSubscribed, String planName) {
        Button actionButton = new Button(isSubscribed ? "Subscribed" : "Subscribe");
        actionButton.getStyleClass().add("subscribe-button");
        if (isSubscribed) {
            actionButton.setDisable(true);
        }

        actionButton.setOnAction(e -> handleSubscribeAction(planName));
        return actionButton;
    }

    /**
     * Adds a hover effect to the subscription plan card using scale transitions.
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
     * Handles the subscription action when the "Subscribe" button is clicked.
     *
     * @param planName The name of the plan to subscribe to.
     */
    private void handleSubscribeAction(String planName) {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "subscribe_plan");
            request.put("memberId", MEMBER_ID);
            request.put("planName", planName);
            request.put("duration", selectedDuration);

            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);

            if (responseJson.getString("status").equals("success")) {
                showAlert("Success", "Successfully subscribed to " + planName + " for " + selectedDuration.replace("_", " ") + "!");
                refreshPlans();
            } else {
                String errorMessage = responseJson.optString("message", "Unknown error");
                showAlert("Error", "Failed to subscribe: " + errorMessage);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to subscribe: " + e.getMessage());
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
     * Refreshes the subscription plans display by fetching the latest data from the server.
     */
    private void refreshPlans() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_subscription_plans");
            String response = sendRequestToServer(request.toString());
            JSONArray plansArray = new JSONArray(response);
            currentPlansArray = plansArray;
            displaySubscriptionPlans(plansArray);
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh plans: " + e.getMessage());
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
     * Selects the 1-month duration and refreshes the plans display.
     */
    @FXML
    private void selectOneMonth() {
        selectedDuration = "1_month";
        setActiveDurationButton(oneMonthButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    /**
     * Selects the 3-month duration and refreshes the plans display.
     */
    @FXML
    private void selectThreeMonths() {
        selectedDuration = "3_months";
        setActiveDurationButton(threeMonthsButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    /**
     * Selects the 6-month duration and refreshes the plans display.
     */
    @FXML
    private void selectSixMonths() {
        selectedDuration = "6_months";
        setActiveDurationButton(sixMonthsButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    /**
     * Selects the 1-year duration and refreshes the plans display.
     */
    @FXML
    private void selectOneYear() {
        selectedDuration = "1_year";
        setActiveDurationButton(oneYearButton);
        displaySubscriptionPlans(currentPlansArray);
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
     * Navigates to the Gym page with a slide transition.
     */
    @FXML
    private void goToGym() {
        navigateToPage("gym.fxml", 1);
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
            Stage stage = (Stage) plansContainer.getScene().getWindow();
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
            showAlert("Error", "Unable to load page: " + e.getMessage());
        }
    }
}