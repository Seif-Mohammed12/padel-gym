package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controller for the Gym page, responsible for rendering UI and communicating
 * with the server.
 */
public class GymController {


    // FXML UI Components
    @FXML
    private FlowPane classesContainer;
    @FXML
    private Label tagline;
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
    private Button loginButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private TableView<JSONObject> workoutHistoryTable;
    @FXML
    private VBox subscriptionDetailsPanel;
    @FXML
    private Label subscriptionPlanLabel;
    @FXML
    private Label subscriptionDurationLabel;
    @FXML
    private Label subscriptionDatesLabel;
    @FXML
    private Button clearHistoryButton;
    @FXML
    private FlowPane workoutHistoryContainer;

    // Constants
    private static final String GYM_CLASSES_FILE = "padelbackend/files/gym-classes.json";
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/yoga.jpg";
    private static final String CLASS_ICON_PATH = "/icons/dumbbell.png";
    private static final String BOOKING_ICON_PATH = "/icons/booking.png";
    private static final String WAITLIST_ICON_PATH = "/icons/hourglass.png";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";

    // ----------------------- Initialization Methods -----------------------

    /**
     * Initializes the controller, setting up the UI and loading initial data.
     */
    @FXML
    public void initialize() {
        setActiveButton(gymButton);
        initializeAppContext();
        updateLoginButton();
        Platform.runLater(() -> {
            AppContext context = AppContext.getInstance();
            if (!context.isLoggedIn() || context.getSubscribedPlanName() == null) {
                showSubscriptionPrompt();
            } else {
                loadGymClassesFromFile();
                initializeWorkoutHistoryTable();
                initializeSubscriptionDetails();
                addNavButtonHoverEffects();
                initializeClearHistoryButton();
            }
        });
    }

    /**
     *If not subscribed then shows a detailed message and a subscribe button instead of the gym content
     */
    private void showSubscriptionPrompt() {
        // Hide original content
        classesContainer.getChildren().clear();
        if (workoutHistoryTable != null) {
            workoutHistoryTable.setVisible(false);
        }
        if (clearHistoryButton != null) {
            clearHistoryButton.setVisible(false);
        }
        if (subscriptionDetailsPanel != null) {
            subscriptionDetailsPanel.setVisible(false);
        }

        // Create subscription prompt
        for (Tab tab : tabPane.getTabs()) {
            VBox promptBox = new VBox(20);
            promptBox.setAlignment(Pos.CENTER);
            promptBox.setPadding(new Insets(50));
            promptBox.getStyleClass().add("subscription-prompt-container");
            promptBox.setMaxWidth(500);

            Label messageLabel = new Label("Subscribe to Access Gym Features");
            messageLabel.getStyleClass().add("subscription-prompt-title");

            Text descriptionText = new Text("You need an active subscription to access gym classes and features.");
            descriptionText.getStyleClass().add("subscription-prompt-text");
            descriptionText.setTextAlignment(TextAlignment.CENTER);

            Button subscribeButton = new Button("Subscribe Now");
            subscribeButton.getStyleClass().addAll("primary-button", "subscription-prompt-button");
            subscribeButton.setOnAction(e -> navigateToPage("subscription.fxml", -1));

            promptBox.getChildren().addAll(messageLabel, descriptionText, subscribeButton);

            VBox containerBox = new VBox(promptBox);
            containerBox.setAlignment(Pos.CENTER);
            containerBox.setStyle("-fx-background-color: #1a1a1a;");

            // Create a ScrollPane for each tab's content
            ScrollPane scrollPane = new ScrollPane(containerBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: #1a1a1a;");

            // Set the content for each tab
            tab.setContent(scrollPane);
        }

        // Keep tabs visible
        tabPane.setVisible(true);
    }

    /**
     * Initializes the AppContext and updates the login button text.
     */
    private void initializeAppContext() {
        AppContext context = AppContext.getInstance();
        context.loadActiveSubscriptions(true);
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
     * Sets the active state for the navigation button.
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
     * Adds hover effects to navigation buttons.
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
     * Initializes the clear history button visibility and action.
     */
    private void initializeClearHistoryButton() {
        if (clearHistoryButton != null) {
            clearHistoryButton.setOnAction(e -> handleClearWorkoutHistory());
            clearHistoryButton.setVisible(AppContext.getInstance().isLoggedIn());
        }
    }

    // ----------------------- UI Rendering Methods -----------------------

    /**
     * Loads gym classes from the JSON file.
     */
    private void loadGymClassesFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(GYM_CLASSES_FILE)));
            JSONArray classesArray = new JSONArray(content);
            displayGymClasses(classesArray);
        } catch (IOException e) {
            showAlert("Unable to load gym classes: " + e.getMessage(), getStage());
        }
    }

    /**
     * Displays gym classes in the FlowPane.
     */
    private void displayGymClasses(JSONArray classesArray) {
        classesContainer.getChildren().clear();
        classesContainer.setHgap(20);
        classesContainer.setVgap(20);
        classesContainer.setPadding(new Insets(20));
        for (int i = 0; i < classesArray.length(); i++) {
            try {
                JSONObject gymClass = classesArray.getJSONObject(i);
                String className = gymClass.optString("name", "Unknown");
                System.out.println("Rendering class: " + className);
                classesContainer.getChildren().add(createGymClassCard(gymClass));
            } catch (Exception e) {
                System.err.println("Failed to render class at index " + i + ": " + e.getMessage());
            }
        }
        System.out.println("Total classes rendered: " + classesContainer.getChildren().size());
    }

    /**
     * Creates a card for a gym class.
     */
    private VBox createGymClassCard(JSONObject gymClass) {
        String name = gymClass.getString("name");
        String instructor = gymClass.getString("instructor");
        String time = gymClass.getString("time");
        String imagePath = gymClass.getString("imagePath");
        int capacity = gymClass.getInt("capacity");
        int currentParticipants = gymClass.getInt("currentParticipants");
        int waitlistSize = gymClass.getInt("waitlistSize");
        boolean isFull = currentParticipants >= capacity;

        VBox card = new VBox(15);
        card.getStyleClass().add("gym-class-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setMinHeight(350);
        card.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = createClassImage(imagePath);
        imageView.getStyleClass().add("gym-class-image");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(CLASS_ICON_PATH)));
        icon.setFitWidth(24);
        icon.setFitHeight(24);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("gym-class-name");
        header.getChildren().addAll(icon, nameLabel);

        VBox details = createClassDetails(instructor, time);
        HBox capacityIndicator = createCapacityIndicator(capacity, currentParticipants);
        Label waitlistLabel = new Label(isFull ? "Waitlist: " + waitlistSize : "Waitlist: 0");
        waitlistLabel.getStyleClass().add("gym-class-detail");

        Button actionButton = createActionButton(isFull, name);

        card.getChildren().addAll(imageView, header, details, capacityIndicator, waitlistLabel, actionButton);
        addCardHoverEffect(card);
        return card;
    }

    /**
     * Creates an image view for a class card using a classpath resource.
     *
     * @param imagePath The path to the image resource (e.g., /images/yoga.jpg).
     * @return An ImageView with the loaded image or a fallback.
     */
    private ImageView createClassImage(String imagePath) {
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
            System.err
                    .println("Failed to load image: '" + imagePath + "', using placeholder. Error: " + e.getMessage());
            image = loadResourceImage(PLACEHOLDER_IMAGE_PATH);
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(150);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    /**
     * Loads an image from a classpath resource, falling back to a transparent image
     * if loading fails.
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
            return new Image(
                    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        }
    }

    /**
     * Creates details section for a class card.
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
     * Creates capacity indicator for a class card.
     */
    private HBox createCapacityIndicator(int capacity, int currentParticipants) {
        HBox capacityBox = new HBox(15);
        capacityBox.setAlignment(Pos.CENTER_LEFT);
        capacityBox.getStyleClass().add("capacity-indicator");

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

        VBox progressSection = new VBox(5);
        progressSection.setAlignment(Pos.CENTER_LEFT);
        Label capacityLabel = new Label(currentParticipants + "/" + capacity + " Participants");
        capacityLabel.getStyleClass().add("gym-class-detail");
        ProgressBar progressBar = new ProgressBar(percentage);
        progressBar.setPrefWidth(150);
        progressBar.setPrefHeight(12);
        progressBar.getStyleClass().add("capacity-progress");
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
     * Creates action button (Book, Join Waitlist, or Cancel) for a class card.
     */
    private Button createActionButton(boolean isFull, String className) {
        AppContext context = AppContext.getInstance();
        Button actionButton = new Button();
        actionButton.getStyleClass().add("gym-action-button");
        ImageView arrowIcon;

        boolean isBooked = false;
        boolean isWaitlisted = false;
        if (context.isLoggedIn()) {
            try {
                JSONObject request = new JSONObject();
                request.put("action", "get_classes");
                String response = sendRequestToServer(request.toString());
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.getString("status").equals("success")) {
                    JSONArray classesArray = responseJson.getJSONArray("data");
                    for (int i = 0; i < classesArray.length(); i++) {
                        JSONObject gymClass = classesArray.getJSONObject(i);
                        if (gymClass.getString("name").equals(className)) {
                            JSONArray participants = gymClass.getJSONArray("participants");
                            JSONArray waitlist = gymClass.getJSONArray("waitlist");
                            String memberId = context.getMemberId();
                            for (int j = 0; j < participants.length(); j++) {
                                if (participants.getString(j).equals(memberId)) {
                                    isBooked = true;
                                    break;
                                }
                            }
                            for (int j = 0; j < waitlist.length(); j++) {
                                if (waitlist.getString(j).equals(memberId)) {
                                    isWaitlisted = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to check booking status: " + e.getMessage());
            }
        }

        if (isBooked || isWaitlisted) {
            actionButton.setText("Cancel");
            arrowIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/booking.png")));
            actionButton.setOnAction(e -> handleCancelBooking(className));
        } else {
            actionButton.setText(isFull ? "Join Waitlist" : "Book");
            arrowIcon = new ImageView(
                    new Image(getClass().getResourceAsStream(isFull ? WAITLIST_ICON_PATH : BOOKING_ICON_PATH)));
            actionButton.setOnAction(e -> handleBookingAction(isFull, className));
        }

        arrowIcon.setFitWidth(16);
        arrowIcon.setFitHeight(16);
        actionButton.setGraphic(arrowIcon);
        return actionButton;
    }

    /**
     * Adds hover effect to a class card.
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

    // ----------------------- Booking and Subscription Handlers
    // -----------------------

    /**
     * Handles booking or joining waitlist for a class.
     */
    private void handleBookingAction(boolean isFull, String className) {
        AppContext context = AppContext.getInstance();
        Stage stage = getStage();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to book a class.", stage);
            navigateToPage("LoginPage.fxml", -1);
            return;
        }
        if (!context.isActive()) {
            showAlert("Your membership is inactive. Please renew or contact support.", stage);
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", "book_gym_class");
            request.put("data", new JSONObject().put("className", className).put("memberId", context.getMemberId()));
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                String message = responseJson.getBoolean("waitlisted")
                        ? "You have been added to the waitlist for " + className + "."
                        : "Successfully booked " + className + "!";
                showAlert(message, stage);
                updateClassCard(className, responseJson.getBoolean("waitlisted"));
                if (!responseJson.getBoolean("waitlisted")) {
                    initializeWorkoutHistoryTable();
                }
            } else {
                showAlert("Failed to process booking: " + responseJson.optString("message", "Unknown error"), stage);
            }
        } catch (Exception e) {
            showAlert("Failed to process booking: " + e.getMessage(), stage);
        }
    }

    /**
     * Updates a class card after booking or cancellation.
     */
    private void updateClassCard(String className, boolean isWaitlisted) {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_classes");
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (!responseJson.getString("status").equals("success")) {
                System.err.println(
                        "Failed to fetch classes for update: " + responseJson.optString("message", "Unknown error"));
                showAlert("Failed to update class details.", getStage());
                return;
            }

            JSONArray classesArray = responseJson.getJSONArray("data");
            JSONObject targetClass = null;
            for (int i = 0; i < classesArray.length(); i++) {
                JSONObject gymClass = classesArray.getJSONObject(i);
                if (gymClass.getString("name").equals(className)) {
                    targetClass = gymClass;
                    break;
                }
            }

            if (targetClass == null) {
                System.err.println("Class not found in server response: " + className);
                showAlert("Class " + className + " not found.", getStage());
                return;
            }

            int capacity = targetClass.getInt("capacity");
            int currentParticipants = targetClass.getInt("currentParticipants");
            int waitlistSize = targetClass.getInt("waitlistSize");
            System.out.println("Updating class card: " + className + ", capacity: " + capacity + ", participants: "
                    + currentParticipants + ", waitlist: " + waitlistSize);

            for (Node node : classesContainer.getChildren()) {
                if (node instanceof VBox card) {
                    boolean found = false;
                    int capacityIndex = -1;
                    int waitlistIndex = -1;
                    int buttonIndex = -1;

                    for (int i = 0; i < card.getChildren().size(); i++) {
                        Node child = card.getChildren().get(i);
                        if (child instanceof HBox header) {
                            for (Node headerChild : header.getChildren()) {
                                if (headerChild instanceof Label nameLabel && nameLabel.getText().equals(className)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (child instanceof HBox capacityBox
                                && capacityBox.getStyleClass().contains("capacity-indicator")) {
                            capacityIndex = i;
                        }
                        if (child instanceof Label waitlistLabel && waitlistLabel.getText().startsWith("Waitlist: ")) {
                            waitlistIndex = i;
                        }
                        if (child instanceof Button actionButton
                                && actionButton.getStyleClass().contains("gym-action-button")) {
                            buttonIndex = i;
                        }
                    }

                    if (found) {
                        if (buttonIndex != -1) {
                            Button actionButton = (Button) card.getChildren().get(buttonIndex);
                            actionButton.setText("Cancel");
                            ImageView cancelIcon = new ImageView(
                                    new Image(getClass().getResourceAsStream("/icons/booking.png")));
                            cancelIcon.setFitWidth(16);
                            cancelIcon.setFitHeight(16);
                            actionButton.setGraphic(cancelIcon);
                            actionButton.setOnAction(e -> handleCancelBooking(className));
                        }

                        if (capacityIndex != -1) {
                            HBox newCapacityIndicator = createCapacityIndicator(capacity, currentParticipants);
                            card.getChildren().set(capacityIndex, newCapacityIndicator);
                        }

                        if (waitlistIndex != -1) {
                            Label waitlistLabel = (Label) card.getChildren().get(waitlistIndex);
                            waitlistLabel.setText("Waitlist: " + waitlistSize);
                        }

                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to update class card for " + className + ": " + e.getMessage());
            showAlert("Failed to update class details: " + e.getMessage(), getStage());
        }
    }

    /**
     * Handles canceling a class booking.
     */
    private void handleCancelBooking(String className) {
        AppContext context = AppContext.getInstance();
        Stage stage = getStage();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to cancel a booking.", stage);
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", "cancel_gym_class");
            request.put("data", new JSONObject().put("className", className).put("memberId", context.getMemberId()));
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                showAlert("Booking canceled successfully!", stage);
                refreshClasses();
                initializeWorkoutHistoryTable();
            } else {
                showAlert("Failed to cancel booking: " + responseJson.optString("message", "Unknown error"), stage);
            }
        } catch (Exception e) {
            showAlert("Failed to cancel booking: " + e.getMessage(), stage);
        }
    }

    /**
     * Initializes the workout history table.
     */
    private void initializeWorkoutHistoryTable() {
        workoutHistoryContainer.getChildren().clear();
        workoutHistoryContainer.setAlignment(Pos.CENTER);

        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            Label noHistoryLabel = new Label("Please log in to view your workout history");
            noHistoryLabel.getStyleClass().add("workout-detail");
            workoutHistoryContainer.getChildren().add(noHistoryLabel);
            return;
        }

        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_workout_history");
            request.put("memberId", context.getMemberId());
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);

            if (responseJson.getString("status").equals("success")) {
                JSONArray workoutsArray = responseJson.getJSONArray("data");

                if (workoutsArray.length() == 0) {
                    Label noHistoryLabel = new Label("No workout history available");
                    noHistoryLabel.getStyleClass().add("workout-detail");
                    workoutHistoryContainer.getChildren().add(noHistoryLabel);
                    return;
                }

                for (int i = 0; i < workoutsArray.length(); i++) {
                    JSONObject workout = workoutsArray.getJSONObject(i);
                    VBox card = createWorkoutHistoryCard(
                            workout.getString("className"),
                            workout.getString("instructor"),
                            workout.getString("date")
                    );
                    workoutHistoryContainer.getChildren().add(card);
                }
            } else {
                showAlert("Failed to load workout history: " +
                        responseJson.optString("message", "Unknown error"), getStage());
            }
        } catch (Exception e) {
            showAlert("Failed to load workout history: " + e.getMessage(), getStage());
        }
    }

    private VBox createWorkoutHistoryCard(String className, String instructor, String date) {
        VBox card = new VBox(10);
        card.getStyleClass().add("workout-card");

        Label titleLabel = new Label(className);
        titleLabel.getStyleClass().add("workout-title");

        Label instructorLabel = new Label("Instructor: " + instructor);
        instructorLabel.getStyleClass().add("workout-detail");

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("workout-date");

        // Add a small icon or visual indicator
        ImageView icon = new ImageView(new Image(
                getClass().getResourceAsStream("/icons/dumbbell.png")));
        icon.setFitWidth(24);
        icon.setFitHeight(24);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(icon, titleLabel);

        card.getChildren().addAll(header, instructorLabel, dateLabel);

        // Add hover effect
        ScaleTransition grow = new ScaleTransition(Duration.millis(200), card);
        grow.setToX(1.05);
        grow.setToY(1.05);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), card);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        card.setOnMouseEntered(e -> grow.playFromStart());
        card.setOnMouseExited(e -> shrink.playFromStart());

        return card;
    }

    /**
     * Handles clearing the workout history.
     */
    private void handleClearWorkoutHistory() {
        AppContext context = AppContext.getInstance();
        Stage stage = getStage();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to clear workout history.", stage);
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", "clear_workout_history");
            request.put("memberId", context.getMemberId());
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                showAlert("Workout history cleared successfully!", stage);
                initializeWorkoutHistoryTable();
            } else {
                showAlert("Failed to clear workout history: " + responseJson.optString("message", "Unknown error"),
                        stage);
            }
        } catch (Exception e) {
            showAlert("Failed to clear workout history: " + e.getMessage(), stage);
        }
    }

    /**
     * Initializes subscription details panel.
     */
    private void initializeSubscriptionDetails() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn() && context.getSubscribedPlanName() != null) {
            subscriptionPlanLabel.setText("Plan: " + context.getSubscribedPlanName());
            subscriptionDurationLabel.setText("Duration: " + context.getSubscribedDuration().replace("_", " "));
            String startDate = context.getSubscriptionStartDate() != null ? context.getSubscriptionStartDate() : "N/A";
            String expiryDate = context.getSubscriptionExpiryDate() != null ? context.getSubscriptionExpiryDate()
                    : "N/A";
            subscriptionDatesLabel.setText("Valid: " + startDate + " to " + expiryDate);
            subscriptionDetailsPanel.setVisible(true);
        } else {
            subscriptionPlanLabel.setText("Plan: None");
            subscriptionDurationLabel.setText("Duration: N/A");
            subscriptionDatesLabel.setText("Valid: N/A");
            subscriptionDetailsPanel.setVisible(false);
        }
    }

    /**
     * Handles canceling a subscription.
     */
    @FXML
    private void handleCancelSubscription() {
        AppContext context = AppContext.getInstance();
        Stage stage = getStage();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to cancel a subscription.", stage);
            return;
        }

        // Create confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Subscription");
        confirmDialog.setHeaderText("Are you sure you want to cancel your subscription?");
        TextFlow textFlow = new TextFlow();
        Text warningText = new Text("This action cannot be undone and you will lose access to gym facilities immediately.");
        warningText.getStyleClass().add("dialog-text");
        textFlow.getChildren().addAll(warningText);

        configureConfirmationDialog(confirmDialog, textFlow, stage);

        // Show dialog and wait for response
        confirmDialog.showAndWait().ifPresent(buttonResponse -> {
            if (buttonResponse == ButtonType.OK) {
                try {
                    JSONObject request = new JSONObject();
                    request.put("action", "cancel_membership");
                    request.put("data", new JSONObject().put("memberId", context.getMemberId()));
                    String serverResponse = sendRequestToServer(request.toString());
                    JSONObject responseJson = new JSONObject(serverResponse);
                    if (responseJson.getString("status").equals("success")) {
                        context.clearSubscription();
                        initializeSubscriptionDetails();
                        initializeWorkoutHistoryTable();
                        clearHistoryButton.setVisible(false);
                    } else {
                        showAlert("Failed to cancel membership: " + responseJson.optString("message", "Unknown error"),
                                stage);
                    }
                } catch (Exception e) {
                    showAlert("Failed to cancel membership: " + e.getMessage(), stage);
                }
            }
        });
    }

    /**
     * Handles renewing a subscription (not implemented).
     */
    @FXML
    private void handleRenewSubscription() {
        AppContext context = AppContext.getInstance();
        Stage stage = getStage();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to renew a subscription.", stage);
            return;
        }

        // Create confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Renew Subscription");
        confirmDialog.setHeaderText("Would you like to renew your subscription?");
        TextFlow textFlow = new TextFlow();
        Text warningText = new Text("Your current plan will be extended with the same settings.");
        warningText.getStyleClass().add("dialog-text");
        textFlow.getChildren().addAll(warningText);

        configureConfirmationDialog(confirmDialog, textFlow, stage);

        // Show dialog and wait for response
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    JSONObject request = new JSONObject();
                    request.put("action", "renew_subscription");
                    JSONObject data = new JSONObject();
                    data.put("memberId", context.getMemberId());
                    data.put("planName", context.getSubscribedPlanName());
                    data.put("duration", context.getSubscribedDuration());
                    request.put("data", data);

                    String serverResponse = sendRequestToServer(request.toString());
                    JSONObject responseJson = new JSONObject(serverResponse);

                    if (responseJson.getString("status").equals("success")) {
                        showAlert("Subscription renewed successfully!", stage);
                        // Refresh subscription details
                        context.loadActiveSubscriptions(true);
                        initializeSubscriptionDetails();
                    } else {
                        showAlert("Failed to renew subscription: " +
                                responseJson.optString("message", "Unknown error"), stage);
                    }
                } catch (Exception e) {
                    showAlert("Failed to renew subscription: " + e.getMessage(), stage);
                }
            }
        });
    }

    /**
     * Toggles visibility of subscription details panel.
     */
    @FXML
    private void handleViewSubscriptionDetails() {
        subscriptionDetailsPanel.setVisible(!subscriptionDetailsPanel.isVisible());
    }

    // ----------------------- Server Communication Methods -----------------------

    /**
     * Sends a request to the server and returns the response.
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
     * Refreshes the classes displayed in the UI.
     */
    private void refreshClasses() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_classes");
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            System.out.println("get_classes response: " + responseJson.toString());
            if (responseJson.getString("status").equals("success")) {
                JSONArray classesArray = responseJson.getJSONArray("data");
                System.out.println("Number of classes received: " + classesArray.length());
                displayGymClasses(classesArray);
            } else {
                showAlert("Failed to refresh classes: " + responseJson.optString("message", "Unknown error"),
                        getStage());
            }
        } catch (Exception e) {
            showAlert("Failed to refresh classes: " + e.getMessage(), getStage());
            loadGymClassesFromFile();
        }
    }

    // ----------------------- Utility Methods -----------------------

    /**
     * Shows a styled alert dialog with the specified message.
     */
    private void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.NONE, "", ButtonType.OK);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // Content pane
        VBox contentPane = new VBox(15);
        contentPane.getStyleClass().add("custom-alert");
        contentPane.setPadding(new Insets(20));
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPrefSize(300, 200);

        // Message label
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(260);
        messageLabel.setAlignment(Pos.CENTER);

        // OK button
        Button okButton = new Button("OK");
        okButton.getStyleClass().add("alert-button");
        okButton.setPrefWidth(120);
        okButton.setPrefHeight(36);
        okButton.setOnAction(e -> alert.close());

        contentPane.getChildren().addAll(messageLabel, okButton);

        // Set content to DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(contentPane);
        dialogPane.getStylesheets().add(getClass().getResource("home.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: transparent;");
        dialogPane.setMinSize(300, 200);
        dialogPane.setMaxSize(300, 200);

        // Style the default OK button (hidden but ensures native closing)
        dialogPane.lookupButton(ButtonType.OK).setVisible(false);

        // Transparent stage
        Stage alertStage = (Stage) dialogPane.getScene().getWindow();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.getScene().setFill(null);

        // Allow closing with Escape key
        dialogPane.getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                alert.close();
            }
        });

        // Fade-in animation
        contentPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        alert.setOnShown(e -> fadeIn.play());

        alert.showAndWait();
    }

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

    /**
     * Gets the Stage from the classesContainer.
     */
    private Stage getStage() {
        return (Stage) tagline.getScene().getWindow();
    }

    // ----------------------- Navigation Methods -----------------------

    /**
     * Navigates to the home page.
     */
    @FXML
    private void goToHome() {
        navigateToPage("home.fxml", -1);
    }

    /**
     * Navigates to the booking page.
     */
    @FXML
    private void goToBooking() {
        navigateToPage("BookingPage.fxml", -1);
    }

    /**
     * Navigates to the subscription page.
     */
    @FXML
    private void goToSubscription() {
        navigateToPage("subscription.fxml", -1);
    }

    /**
     * Handles login/logout button action.
     */
    @FXML
    private void handleLoginButton() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            context.clear();
            loginButton.setText("Login");
            navigateToPage("home.fxml", -1);
        } else {
            fadeToPage("LoginPage.fxml");
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
            Scene currentScene = bookingButton.getScene();
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

    /**
     * Navigates to a new page with a slide transition.
     */
    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();
            Stage stage = getStage();
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
            showAlert("Unable to load page: " + e.getMessage(), getStage());
        }
    }
}