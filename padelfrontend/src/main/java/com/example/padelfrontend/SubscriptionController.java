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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

/**
 * Controller for the Subscription page, handling plan display and subscription
 * logic.
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

    // State
    private String selectedDuration = "1_month"; // Default duration
    private JSONArray currentPlansArray; // Cached plans for refreshing
    private boolean isFormCanceled = false; // Tracks form cancellation

    // --------------------- Initialization ---------------------

    @FXML
    public void initialize() {
        setupNavigation();
        setupDurationSelector();
        loadSubscriptionPlans();
        updateLoginButton();
    }

    // --------------------- UI Setup ---------------------

    private void setupNavigation() {
        setActiveButton(subscriptionButton);
        addNavButtonHoverEffects();
    }

    private void setupDurationSelector() {
        setActiveDurationButton(oneMonthButton);
        addDurationButtonHoverEffects();
    }

    private void updateLoginButton() {
        AppContext context = AppContext.getInstance();
        loginButton.setText(context.isLoggedIn() ? "Welcome, " + context.getUsername() : "Login");
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
                addScaleHoverEffect(button);
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
                addScaleHoverEffect(button);
            }
        }
    }

    private void addScaleHoverEffect(Button button) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(200), button);
        grow.setToX(1.1);
        grow.setToY(1.1);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), button);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        button.setOnMouseEntered(e -> grow.playFromStart());
        button.setOnMouseExited(e -> shrink.playFromStart());
    }

    // --------------------- Plan Rendering ---------------------

    private void loadSubscriptionPlans() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_subscription_plans");
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                currentPlansArray = responseJson.getJSONArray("data");
                if (currentPlansArray == null || currentPlansArray.length() == 0) {
                    showAlert("No subscription plans available.", getStage());
                    return;
                }
                displaySubscriptionPlans(currentPlansArray);
            } else {
                showAlert(responseJson.getString("message"), getStage());
            }
        } catch (Exception e) {
            showAlert("Unable to load subscription plans: " + e.getMessage(), getStage());
        }
    }

    private void displaySubscriptionPlans(JSONArray plansArray) {
        plansContainer.getChildren().clear();
        plansContainer.setHgap(20);
        plansContainer.setVgap(20);
        plansContainer.setPadding(new Insets(20));

        for (int i = 0; i < plansArray.length(); i++) {
            JSONObject plan = plansArray.getJSONObject(i);
            System.out.println("Rendering plan: " + plan.getString("name"));
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

        // Main card container
        VBox card = new VBox();
        card.getStyleClass().add("subscription-plan-card");
        card.getStyleClass().add("plan-" + name.toLowerCase());
        card.setPrefWidth(300);
        card.setMinHeight(350);

        // Header
        HBox header = createPlanHeader(name);

        // Content area (will expand to push button down)
        VBox content = new VBox();
        content.getStyleClass().add("card-content");

        // Pricing and features
        VBox vpricing = createPricingSection(price, durationText);
        VBox features = createFeaturesSection(featuresArray);

        // Container for the button (will stay at bottom)
        VBox buttonContainer = new VBox();
        buttonContainer.getStyleClass().add("button-container");
        Button actionButton = createActionButton(name);
        buttonContainer.getChildren().add(actionButton);

        // Add all components to content
        content.getChildren().addAll(vpricing, features);

        // Use a spacer to push content up and button down
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Add all to main card
        card.getChildren().addAll(header, content, spacer, buttonContainer);

        addCardHoverEffect(card);
        return card;
    }

    private HBox createPlanHeader(String name) {
        HBox header = new HBox();
        header.getStyleClass().add("plan-header");
        header.getStyleClass().add("plan-" + name.toLowerCase());
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15, 0, 0, 0)); // Top padding for badge space

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("plan-name");

        if (name.equalsIgnoreCase("Elite")) {
            // Create a stack pane to overlay the badge
            StackPane headerStack = new StackPane();
            headerStack.setAlignment(Pos.TOP_CENTER);

            Label badge = new Label("Best Value");
            badge.getStyleClass().add("plan-badge");

            // Add both label and badge to stack
            headerStack.getChildren().addAll(nameLabel, badge);
            header.getChildren().add(headerStack);
        } else {
            header.getChildren().add(nameLabel);
        }

        return header;
    }

    private VBox createPricingSection(double price, String duration) {
        VBox pricing = new VBox(5);
        pricing.setAlignment(Pos.CENTER);

        Label priceLabel = new Label(String.format("$%.2f", price));
        priceLabel.getStyleClass().add("plan-price");

        Label durationLabel = new Label("/ " + duration);
        durationLabel.getStyleClass().add("plan-duration");

        pricing.getChildren().addAll(priceLabel, durationLabel);
        return pricing;
    }

    private VBox createFeaturesSection(JSONArray featuresArray) {
        VBox featuresBox = new VBox(8);
        featuresBox.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < featuresArray.length(); i++) {
            HBox featureRow = new HBox(8);
            Label checkmark = new Label("✓");
            checkmark.getStyleClass().add("feature-checkmark");
            Label featureLabel = new Label(featuresArray.getString(i));
            featureLabel.getStyleClass().add("plan-detail");
            featureRow.getChildren().addAll(checkmark, featureLabel);
            featuresBox.getChildren().add(featureRow);
        }

        return featuresBox;
    }

    private Button createActionButton(String planName) {
        AppContext context = AppContext.getInstance();
        String subscribedPlanName = context.getSubscribedPlanName();
        String subscribedDuration = context.getSubscribedDuration();

        boolean isSubscribed = context.isLoggedIn() && subscribedPlanName != null &&
                subscribedPlanName.equals(planName) && subscribedDuration != null &&
                subscribedDuration.equals(selectedDuration);

        Button actionButton = new Button();
        if (isSubscribed) {
            actionButton.setText("Subscribed");
            actionButton.getStyleClass().add("subscribed-button");
            actionButton.setDisable(true);
        } else {
            actionButton
                    .setText(context.isLoggedIn() && subscribedPlanName != null ? "Change Subscription" : "Subscribe");
            actionButton.getStyleClass()
                    .add(context.isLoggedIn() && subscribedPlanName != null ? "change-subscription-button"
                            : "subscribe-button");
            actionButton.setOnAction(e -> handleSubscribeAction(planName));
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

    // --------------------- Subscription Logic ---------------------

    private void handleSubscribeAction(String planName) {
        AppContext context = AppContext.getInstance();

        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to subscribe. Please log in or sign up.", getStage());
            navigateToPage("LoginPage.fxml", -1);
            return;
        }

        String subscribedPlanName = context.getSubscribedPlanName();
        String subscribedDuration = context.getSubscribedDuration();

        if (subscribedPlanName != null && !subscribedPlanName.isEmpty() &&
                subscribedDuration != null && !subscribedDuration.isEmpty()) {
            showChangeSubscriptionConfirmation(planName);
        } else {
            openSubscriptionForm(planName);
        }
    }

    private void showChangeSubscriptionConfirmation(String planName) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Change Subscription");
        confirmationAlert.setHeaderText("You already have an active subscription.");

        TextFlow textFlow = new TextFlow();
        Text text = new Text("Are you sure you want to change to the " + planName + " plan?");
        text.getStyleClass().add("dialog-text");
        textFlow.getChildren().add(text);

        Stage currentStage = (Stage) plansContainer.getScene().getWindow();
        configureConfirmationDialog(confirmationAlert, textFlow, currentStage);

        ButtonType confirmButton = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmationAlert.getButtonTypes().setAll(confirmButton, cancelButton);

        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == confirmButton) {
                changeSubscription(planName);
            }
        });
    }

    private void openSubscriptionForm(String planName) {
        AppContext context = AppContext.getInstance();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SubscriptionForm.fxml"));
            Parent formOverlay = loader.load();
            SubscriptionFormController formController = loader.getController();

            Scene currentScene = plansContainer.getScene();
            Parent currentRoot = currentScene.getRoot();
            StackPane rootContainer = currentRoot instanceof StackPane ? (StackPane) currentRoot
                    : new StackPane(currentRoot);
            currentScene.setRoot(rootContainer);

            rootContainer.getChildren().add(formOverlay);
            Parent subscriptionPage = (Parent) rootContainer.getChildren().get(0);
            formController.setSubscriptionPage(subscriptionPage);

            formController.setOnCancelCallback(() -> isFormCanceled = true);

            formOverlay.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent == null && !isFormCanceled) {
                    processSubscriptionForm(planName, context);
                } else if (isFormCanceled) {
                    isFormCanceled = false;
                }
            });

            formOverlay.toFront();
        } catch (Exception e) {
            showAlert("Failed to open subscription form: " + e.getMessage(), getStage());
            e.printStackTrace();
        }
    }

    private void processSubscriptionForm(String planName, AppContext context) {
        try {
            String firstName = context.getFirstName();
            String lastName = context.getLastName();
            String phoneNumber = context.getPhoneNumber();
            String dob = context.getDob();
            String memberId = context.getMemberId();
            String duration = selectedDuration;

            if (firstName == null || lastName == null || phoneNumber == null || dob == null ||
                    memberId == null || planName == null || duration == null) {
                showAlert("Incomplete user data. Please fill out the subscription form.", getStage());
                return;
            }

            double price = getPlanPrice(planName, duration);
            if (price == -1) {
                showAlert("Plan '" + planName + "' or duration '" + duration + "' not found.", getStage());
                return;
            }

            boolean isMemberActive = checkMemberStatus(memberId, dob, context);
            if (!isMemberActive) {
                addNewMember(memberId, firstName, lastName, phoneNumber, dob, planName, duration, price);
            }

            subscribeMember(memberId, firstName, lastName, phoneNumber, dob, planName, duration, price, context);
        } catch (Exception e) {
            showAlert("Failed to subscribe: " + e.getMessage(), getStage());
            e.printStackTrace();
        }
    }

    private double getPlanPrice(String planName, String duration) {
        for (int i = 0; i < currentPlansArray.length(); i++) {
            JSONObject plan = currentPlansArray.getJSONObject(i);
            if (plan.getString("name").equals(planName)) {
                JSONObject pricing = plan.getJSONObject("pricing");
                if (pricing.has(duration)) {
                    return pricing.getDouble(duration);
                }
            }
        }
        return -1;
    }

    private boolean checkMemberStatus(String memberId, String dob, AppContext context) throws IOException {
        JSONObject checkMemberRequest = new JSONObject();
        checkMemberRequest.put("action", "check_member");
        JSONObject checkData = new JSONObject();
        checkData.put("memberId", memberId);
        checkData.put("dob", dob);
        checkMemberRequest.put("data", checkData);
        context.setDob(dob);

        String response = sendRequestToServer(checkMemberRequest.toString());
        JSONObject json = new JSONObject(response);
        System.out.println("Check member response: " + response);

        if (json.getString("status").equals("success")) {
            JSONObject result = json.getJSONObject("data");
            return result.getBoolean("isActive");
        }
        return false;
    }

    private void addNewMember(String memberId, String firstName, String lastName, String phoneNumber,
            String dob, String planName, String duration, double price) throws IOException {
        JSONObject addMemberRequest = new JSONObject();
        addMemberRequest.put("action", "add_member");
        JSONObject memberData = new JSONObject();
        memberData.put("memberId", memberId);
        memberData.put("name", firstName + " " + lastName);
        memberData.put("phoneNumber", phoneNumber);
        memberData.put("dob", dob);
        JSONObject subscriptionData = new JSONObject();
        subscriptionData.put("planName", planName);
        subscriptionData.put("duration", duration);
        subscriptionData.put("price", price);
        memberData.put("subscription", subscriptionData);
        addMemberRequest.put("data", memberData);

        System.out.println("Sending add_member request: " + addMemberRequest.toString());
        String response = sendRequestToServer(addMemberRequest.toString());
        JSONObject json = new JSONObject(response);
        System.out.println("Add member response: " + response);

        if (!json.getString("status").equals("success")) {
            throw new IOException("Failed to add member: " + json.getString("message"));
        }
        try {
            Thread.sleep(100); // Ensure server saves members.json
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void subscribeMember(String memberId, String firstName, String lastName, String phoneNumber,
            String dob, String planName, String duration, double price, AppContext context) throws IOException {
        JSONObject subscribeRequest = new JSONObject();
        subscribeRequest.put("action", "subscribe");
        JSONObject subscribeData = new JSONObject();
        subscribeData.put("memberId", memberId);
        subscribeData.put("firstName", firstName);
        subscribeData.put("lastName", lastName);
        subscribeData.put("phoneNumber", phoneNumber);
        subscribeData.put("dob", dob);
        subscribeData.put("planName", planName);
        subscribeData.put("duration", duration);
        subscribeData.put("price", price);
        subscribeRequest.put("data", subscribeData);

        System.out.println("Sending subscribe request: " + subscribeRequest.toString());
        String response = sendRequestToServer(subscribeRequest.toString());
        JSONObject json = new JSONObject(response);
        System.out.println("Subscribe response: " + response);

        if (json.getString("status").equals("success")) {
            context.setSubscribedPlanName(planName);
            context.setSubscribedDuration(duration);
            refreshPlans();
        } else {
            throw new IOException(json.getString("message"));
        }
    }

    private void changeSubscription(String planName) {
        AppContext context = AppContext.getInstance();
        try {
            String firstName = context.getFirstName();
            String lastName = context.getLastName();
            String phoneNumber = context.getPhoneNumber();
            String dob = context.getDob();
            String memberId = context.getMemberId();
            String duration = selectedDuration;

            if (firstName == null || lastName == null || phoneNumber == null || dob == null ||
                    memberId == null || planName == null || duration == null) {
                showAlert("Incomplete user data. Please update your profile.", getStage());
                return;
            }

            double price = getPlanPrice(planName, duration);
            if (price == -1) {
                showAlert("Plan '" + planName + "' or duration '" + duration + "' not found.", getStage());
                return;
            }

            subscribeMember(memberId, firstName, lastName, phoneNumber, dob, planName, duration, price, context);
        } catch (Exception e) {
            showAlert("Failed to change subscription: " + e.getMessage(), getStage());
            e.printStackTrace();
        }
    }

    // --------------------- Navigation ---------------------

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

    @FXML
    private void handleLoginButton() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            context.clear();
            loginButton.setText("Login");
            navigateToPage("home.fxml", -1);
        } else {
            navigateToPage("LoginPage.fxml", -1);
        }
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
            showAlert("Unable to load page: " + e.getMessage(), getStage());
        }
    }

    // --------------------- Utilities ---------------------

    private void configureConfirmationDialog(Alert alert, TextFlow textFlow, Stage parentStage) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(textFlow);

        // Use only CSS for styling
        dialogPane.getStylesheets().clear();
        dialogPane.getStylesheets().add(getClass().getResource("home.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        dialogPane.setGraphic(null);

        // Style the content text via CSS class
        textFlow.getStyleClass().add("dialog-text");

        // Configure window behavior
        alert.initOwner(parentStage);
        alert.initModality(Modality.WINDOW_MODAL);

        // Add shadow effect (optional, since CSS already has it)
        dialogPane.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.75)));

        // Remove default window decorations
        alert.setOnShowing(event -> {
            Stage alertStage = (Stage) dialogPane.getScene().getWindow();
            alertStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = dialogPane.getScene();
            scene.setFill(null);

        });
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

    private void refreshPlans() {
        loadSubscriptionPlans();
    }

    private Stage getStage() {
        return (Stage) plansContainer.getScene().getWindow();
    }

    private void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("home.css").toExternalForm());
        dialogPane.getStyleClass().add("error-dialog");
        dialogPane.setHeaderText(null);
        dialogPane.setGraphic(null);

        dialogPane.setStyle("-fx-background-color: #f8d7da; -fx-background-radius: 20; -fx-border-radius: 20;");

        dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        dialogPane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        Stage alertStage = (Stage) dialogPane.getScene().getWindow();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.getScene().setFill(null);

        alert.showAndWait();
    }
}