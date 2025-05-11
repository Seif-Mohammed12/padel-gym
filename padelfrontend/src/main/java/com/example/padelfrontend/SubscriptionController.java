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
import javafx.scene.control.*;
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
    @FXML
    private Button loginButton;

    // Constants
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder.jpg";

    // State
    private String selectedDuration = "1_month"; // Default duration
    private JSONArray currentPlansArray; // Store the plans for refreshing
    private boolean isFormCanceled = false; // Flag to track cancellation

    @FXML
    public void initialize() {
        setActiveButton(subscriptionButton);
        addNavButtonHoverEffects();
        setActiveDurationButton(oneMonthButton);
        addDurationButtonHoverEffects();
        loadSubscriptionPlansFromServer();

        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            loginButton.setText("Welcome, " + context.getUsername());
        } else {
            loginButton.setText("Login");
        }
    }

    private void setActiveButton(Button activeButton) {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("active");
            }
        }
        activeButton.getStyleClass().add("active");
    }

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

    private void setActiveDurationButton(Button activeButton) {
        for (Node node : durationSelector.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("active-duration");
            }
        }
        activeButton.getStyleClass().add("active-duration");
    }

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

    private void loadSubscriptionPlansFromServer() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_subscription_plans");
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                currentPlansArray = responseJson.getJSONArray("data");
                if (currentPlansArray == null || currentPlansArray.length() == 0) {
                    showAlert("Error", "No subscription plans available.");
                    return;
                }
                displaySubscriptionPlans(currentPlansArray);
            } else {
                showAlert("Error", responseJson.getString("message"));
            }
        } catch (Exception e) {
            showAlert("Error", "Unable to load subscription plans: " + e.getMessage());
        }
    }

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

    private VBox createSubscriptionPlanCard(JSONObject plan) {
        String name = plan.getString("name");
        JSONObject pricing = plan.getJSONObject("pricing");
        double price = pricing.getDouble(selectedDuration);
        String durationText = selectedDuration.replace("_", " ").replace("month", "Month").replace("year", "Year");
        JSONArray featuresArray = plan.getJSONArray("features");
        String imagePath = plan.optString("imagePath", PLACEHOLDER_IMAGE_PATH);

        VBox card = new VBox(10);
        card.getStyleClass().add("subscription-plan-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinHeight(250);
        card.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = createPlanImage(imagePath);
        imageView.getStyleClass().add("plan-image");

        VBox details = createPlanDetails(name, price, durationText, featuresArray);

        Button actionButton = createActionButton(name);

        card.getChildren().addAll(imageView, details, actionButton);

        addCardHoverEffect(card);

        return card;
    }

    private ImageView createPlanImage(String imagePath) {
        ImageView imageView = new ImageView();
        try {
            Image image = new Image(imagePath);
            if (image.isError()) {
                throw new IllegalArgumentException("Image failed to load: " + imagePath);
            }
            imageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Failed to load image: " + imagePath + ". Using placeholder.");
            Image placeholder = new Image(getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH));
            imageView.setImage(placeholder.isError() ? new Image("file:images/default-placeholder.jpg") : placeholder);
        }
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        Circle clip = new Circle(40, 40, 40);
        imageView.setClip(clip);
        return imageView;
    }

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

    private Button createActionButton(String planName) {
        AppContext context = AppContext.getInstance();
        boolean isSubscribed = context.isLoggedIn() && context.isSubscribedToPlan(planName, selectedDuration);

        Button actionButton = new Button(isSubscribed ? "Subscribed" : "Subscribe");
        actionButton.getStyleClass().add(isSubscribed ? "subscribed-button" : "subscribe-button");

        if (!isSubscribed) {
            actionButton.setOnAction(e -> handleSubscribeAction(planName));
        } else {
            actionButton.setDisable(true); // Disable the button for the subscribed plan
        }

        return actionButton;
    }

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

    private void handleSubscribeAction(String planName) {
        AppContext context = AppContext.getInstance();

        if (!context.isLoggedIn()) {
            showAlert("Error", "You must be logged in to subscribe. Please log in or sign up.");
            navigateToPage("login.fxml", -1);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SubscriptionForm.fxml"));
            Parent formOverlay = loader.load();

            SubscriptionFormController formController = loader.getController();

            Scene currentScene = plansContainer.getScene();
            Parent currentRoot = currentScene.getRoot();
            StackPane rootContainer;
            if (!(currentRoot instanceof StackPane)) {
                rootContainer = new StackPane(currentRoot);
                currentScene.setRoot(rootContainer);
            } else {
                rootContainer = (StackPane) currentRoot;
            }

            rootContainer.getChildren().add(formOverlay);

            // Set the subscription page to disable interaction while the form is open
            Parent subscriptionPage = (Parent) rootContainer.getChildren().get(0);
            formController.setSubscriptionPage(subscriptionPage);

            // Set cancellation callback
            formController.setOnCancelCallback(() -> {
                isFormCanceled = true; // Mark as canceled
            });

            // Listen for the form overlay being removed (i.e., form closed)
            formOverlay.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent == null) { // Form was closed
                    if (isFormCanceled) {
                        isFormCanceled = false; // Reset flag for next use
                        return; // Skip subscription logic and alert
                    }

                    try {
                        // Retrieve data from AppContext
                        String firstName = context.getFirstName();
                        String lastName = context.getLastName();
                        String phoneNumber = context.getPhoneNumber();
                        String dob = context.getDob();
                        String memberId = context.getMemberId();

                        // Validate that required data is present
                        if (firstName == null || lastName == null || phoneNumber == null || dob == null || memberId == null) {
                            showAlert("Error", "Incomplete user data. Please fill out the subscription form.");
                            return;
                        }

                        // Step 1: Send add_member request
                        JSONObject addMemberRequest = new JSONObject();
                        addMemberRequest.put("action", "add_member");
                        JSONObject memberData = new JSONObject();
                        memberData.put("memberId", memberId);
                        memberData.put("name", firstName + " " + lastName);
                        memberData.put("phoneNumber", phoneNumber);
                        memberData.put("dob", dob);
                        memberData.put("subscription", planName);
                        addMemberRequest.put("data", memberData);

                        String addMemberResponse = sendRequestToServer(addMemberRequest.toString());
                        JSONObject addMemberJson = new JSONObject(addMemberResponse);

                        if (!addMemberJson.getString("status").equals("success")) {
                            showAlert("Error", "Failed to add member: " + addMemberJson.getString("message"));
                            return;
                        }

                        // Step 2: Send subscribe request
                        JSONObject subscribeRequest = new JSONObject();
                        subscribeRequest.put("action", "subscribe");
                        JSONObject subscribeData = new JSONObject();
                        subscribeData.put("memberId", memberId);
                        subscribeData.put("firstName", firstName);
                        subscribeData.put("lastName", lastName);
                        subscribeData.put("phoneNumber", phoneNumber);
                        subscribeData.put("dob", dob);
                        subscribeData.put("planName", planName);
                        subscribeData.put("duration", selectedDuration);
                        subscribeRequest.put("data", subscribeData);

                        String subscribeResponse = sendRequestToServer(subscribeRequest.toString());
                        JSONObject subscribeJson = new JSONObject(subscribeResponse);

                        if (subscribeJson.getString("status").equals("success")) {
                            // Store subscription in AppContext
                            context.setSubscribedPlanName(planName);
                            context.setSubscribedDuration(selectedDuration);

                            showAlert("Success", "Successfully subscribed to " + planName + "!");
                            refreshPlans(); // Refresh UI to update button
                        } else {
                            showAlert("Error", subscribeJson.getString("message"));
                        }
                    } catch (Exception e) {
                        showAlert("Error", "Failed to subscribe: " + e.getMessage());
                    }
                }
            });

            formOverlay.toFront();
        } catch (Exception e) {
            showAlert("Error", "Failed to open subscription form: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private void refreshPlans() {
        loadSubscriptionPlansFromServer();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void selectOneMonth() {
        selectedDuration = "1_month";
        setActiveDurationButton(oneMonthButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    @FXML
    private void selectThreeMonths() {
        selectedDuration = "3_months";
        setActiveDurationButton(threeMonthsButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    @FXML
    private void selectSixMonths() {
        selectedDuration = "6_months";
        setActiveDurationButton(sixMonthsButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    @FXML
    private void selectOneYear() {
        selectedDuration = "1_year";
        setActiveDurationButton(oneYearButton);
        displaySubscriptionPlans(currentPlansArray);
    }

    @FXML
    private void goToHome() {
        navigateToPage("home.fxml", -1);
    }

    @FXML
    private void goToBooking() {
        navigateToPage("BookingPage.fxml", -1);
    }

    @FXML
    private void goToGym() {
        navigateToPage("gym.fxml", 1);
    }

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

    @FXML
    private void handleLoginButton() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            context.clear();
            loginButton.setText("Login");
            navigateToPage("login.fxml", -1);
        } else {
            navigateToPage("login.fxml", -1);
        }
    }
}