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
 * Controller for the Gym page, responsible for rendering UI and communicating with the server.
 */
public class GymController {

    @FXML private FlowPane classesContainer;
    @FXML private Button bookingButton;
    @FXML private Button subscriptionButton;
    @FXML private Button gymButton;
    @FXML private Button homeButton;
    @FXML private HBox navbar;
    @FXML private Button loginButton;
    @FXML private TabPane tabPane;
    @FXML private TableView<JSONObject> workoutHistoryTable;
    @FXML private VBox subscriptionDetailsPanel;
    @FXML private Label subscriptionPlanLabel;
    @FXML private Label subscriptionDurationLabel;
    @FXML private Label subscriptionDatesLabel;
    @FXML private Button clearHistoryButton; // New button for clearing workout history

    private static final String GYM_CLASSES_FILE = "padelbackend/files/gym-classes.json";
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/yoga.jpg";
    private static final String CLASS_ICON_PATH = "/icons/dumbbell.png";
    private static final String BOOKING_ICON_PATH = "/icons/booking.png";
    private static final String WAITLIST_ICON_PATH = "/icons/hourglass.png";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";

    @FXML
    public void initialize() {
        setActiveButton(gymButton);
        AppContext context = AppContext.getInstance();
        loginButton.setText(context.isLoggedIn() ? "Welcome, " + context.getUsername() : "Login");
        loadGymClassesFromFile();
        initializeWorkoutHistoryTable();
        initializeSubscriptionDetails();
        addNavButtonHoverEffects();
        initializeClearHistoryButton();
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

    private void loadGymClassesFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(GYM_CLASSES_FILE)));
            JSONArray classesArray = new JSONArray(content);
            displayGymClasses(classesArray);
        } catch (IOException e) {
            showAlert("Error", "Unable to load gym classes: " + e.getMessage());
        }
    }

    private void displayGymClasses(JSONArray classesArray) {
        classesContainer.getChildren().clear();
        classesContainer.setHgap(20);
        classesContainer.setVgap(20);
        classesContainer.setPadding(new Insets(20));
        for (int i = 0; i < classesArray.length(); i++) {
            classesContainer.getChildren().add(createGymClassCard(classesArray.getJSONObject(i)));
        }
    }

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

    private ImageView createClassImage(String imagePath) {
        ImageView imageView = new ImageView();
        try {
            Image image = new Image(imagePath);
            if (image.isError()) throw new IllegalArgumentException("Image failed to load");
            imageView.setImage(image);
        } catch (Exception e) {
            imageView.setImage(new Image(getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH)));
        }
        imageView.setFitWidth(260);
        imageView.setFitHeight(150);
        Rectangle clip = new Rectangle(260, 150);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);
        return imageView;
    }

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

    private HBox createCapacityIndicator(int capacity, int currentParticipants) {
        HBox capacityBox = new HBox(15);
        capacityBox.setAlignment(Pos.CENTER_LEFT);

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

    private Button createActionButton(boolean isFull, String className) {
        Button actionButton = new Button(isFull ? "Join Waitlist" : "Book");
        actionButton.getStyleClass().add("gym-action-button");
        ImageView arrowIcon = new ImageView(new Image(getClass().getResourceAsStream(isFull ? WAITLIST_ICON_PATH : BOOKING_ICON_PATH)));
        arrowIcon.setFitWidth(16);
        arrowIcon.setFitHeight(16);
        actionButton.setGraphic(arrowIcon);
        actionButton.setOnAction(e -> handleBookingAction(isFull, className));
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

    private void handleBookingAction(boolean isFull, String className) {
        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            showAlert("Error", "You must be logged in to book a class.");
            navigateToPage("login.fxml", -1);
            return;
        }
        if (!context.isActive()) {
            showAlert("Error", "Your membership is inactive. Please renew or contact support.");
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", isFull ? "join_waitlist" : "book_gym_class");
            request.put("data", new JSONObject().put("className", className).put("memberId", context.getMemberId()));
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                String message = responseJson.getBoolean("waitlisted")
                        ? "You have been added to the waitlist!"
                        : "Class booked successfully!";
                showAlert("Success", message);
                refreshClasses();
                if (!isFull) {
                    initializeWorkoutHistoryTable();
                }
            } else {
                showAlert("Error", "Failed to process booking: " + responseJson.optString("message", "Unknown error"));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to process booking: " + e.getMessage());
        }
    }

    private void initializeWorkoutHistoryTable() {
        workoutHistoryTable.getColumns().clear();
        TableColumn<JSONObject, String> classNameColumn = new TableColumn<>("Class Name");
        classNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getString("className")));
        TableColumn<JSONObject, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getString("date")));
        TableColumn<JSONObject, String> instructorColumn = new TableColumn<>("Instructor");
        instructorColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getString("instructor")));
        workoutHistoryTable.getColumns().addAll(classNameColumn, dateColumn, instructorColumn);

        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            try {
                JSONObject request = new JSONObject();
                request.put("action", "get_workout_history");
                request.put("memberId", context.getMemberId());
                String response = sendRequestToServer(request.toString());
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.getString("status").equals("success")) {
                    JSONArray workoutsArray = responseJson.getJSONArray("data");
                    workoutHistoryTable.getItems().clear();
                    for (int i = 0; i < workoutsArray.length(); i++) {
                        workoutHistoryTable.getItems().add(workoutsArray.getJSONObject(i));
                    }
                } else {
                    showAlert("Error", "Failed to load workout history: " + responseJson.optString("message", "Unknown error"));
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to load workout history: " + e.getMessage());
            }
        } else {
            workoutHistoryTable.getItems().clear();
        }
        workoutHistoryTable.setPlaceholder(new Label("No workout history available."));
        workoutHistoryTable.getStyleClass().add("workout-history-table");
    }

    private void initializeClearHistoryButton() {
        if (clearHistoryButton != null) {
            clearHistoryButton.setOnAction(e -> handleClearWorkoutHistory());
            clearHistoryButton.setVisible(AppContext.getInstance().isLoggedIn());
        }
    }

    private void handleClearWorkoutHistory() {
        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            showAlert("Error", "You must be logged in to clear workout history.");
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", "clear_workout_history");
            request.put("memberId", context.getMemberId());
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                showAlert("Success", "Workout history cleared successfully!");
                initializeWorkoutHistoryTable(); // Refresh table
            } else {
                showAlert("Error", "Failed to clear workout history: " + responseJson.optString("message", "Unknown error"));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to clear workout history: " + e.getMessage());
        }
    }

    private void initializeSubscriptionDetails() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn() && context.getSubscribedPlanName() != null) {
            subscriptionPlanLabel.setText("Plan: " + context.getSubscribedPlanName());
            subscriptionDurationLabel.setText("Duration: " + context.getSubscribedDuration().replace("_", " "));
            subscriptionDatesLabel.setText("Valid: 2025-05-01 to 2025-06-01");
        } else {
            subscriptionPlanLabel.setText("Plan: None");
            subscriptionDurationLabel.setText("Duration: N/A");
            subscriptionDatesLabel.setText("Valid: N/A");
            subscriptionDetailsPanel.setVisible(false);
        }
    }

    @FXML
    private void handleCancelSubscription() {
        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            showAlert("Error", "You must be logged in to cancel a subscription.");
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("action", "cancel_membership");
            request.put("data", new JSONObject().put("memberId", context.getMemberId()));
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                showAlert("Success", "Membership canceled successfully!");
                //context.clearSubscription(); // Clear subscription data
                initializeSubscriptionDetails(); // Refresh UI
                initializeWorkoutHistoryTable(); // Refresh table (history cleared by server)
                clearHistoryButton.setVisible(false); // Hide button
            } else {
                showAlert("Error", "Failed to cancel membership: " + responseJson.optString("message", "Unknown error"));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to cancel membership: " + e.getMessage());
        }
    }

    @FXML
    private void handleRenewSubscription() {
        showAlert("Info", "Renew/Early Renew Subscription clicked (logic not implemented).");
    }

    @FXML
    private void handleViewSubscriptionDetails() {
        subscriptionDetailsPanel.setVisible(!subscriptionDetailsPanel.isVisible());
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

    private void refreshClasses() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "get_classes");
            String response = sendRequestToServer(request.toString());
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getString("status").equals("success")) {
                JSONArray classesArray = responseJson.getJSONArray("data");
                displayGymClasses(classesArray);
            } else {
                showAlert("Error", "Failed to refresh classes: " + responseJson.optString("message", "Unknown error"));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh classes: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
    private void goToSubscription() {
        navigateToPage("subscription.fxml", -1);
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
            showAlert("Error", "Unable to load page: " + e.getMessage());
        }
    }
}