package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Controller for the Manager Page, handling management of Padel Centers and Gym Classes.
 * Provides functionality to add, edit, delete, and display items, with navigation to other pages.
 */
public class ManagerPageController {

    // Form fields for adding/editing Padel Centers or Gym Classes
    @FXML private TextField nameField;
    @FXML private TextField timesField;
    @FXML private TextField locationField;
    @FXML private TextField instructorField;
    @FXML private TextField timeField;
    @FXML private TextField capacityField;
    @FXML private TextField imageField;

    // UI Containers
    @FXML private VBox formContainer;
    @FXML private VBox itemsContainer;

    // Buttons for actions and navigation
    @FXML private Button saveButton;
    @FXML private Button homeButton;
    @FXML private Button bookingButton;
    @FXML private Button logoutButton;
    @FXML private Button showPadelButton;
    @FXML private Button showGymButton;

    // Labels for dynamic UI elements
    @FXML private Label formTitle;
    @FXML private Label nameLabel;
    @FXML private Label timesLabel;
    @FXML private Label locationLabel;
    @FXML private Label instructorLabel;
    @FXML private Label timeLabel;
    @FXML private Label capacityLabel;
    @FXML private Label itemsTitle;
    @FXML private Label messageLabel;

    // State variables
    private File selectedImageFile;
    private boolean isPadelMode = true; // Default to Padel Centers mode
    private final int loggedInMemberId = 1; // Simulated member ID for testing
    private JSONObject editingItem = null; // Tracks the item being edited

    /**
     * Initializes the Manager Page UI, setting the default mode to Padel Centers
     * and loading existing items.
     */
    @FXML
    private void initialize() {
        updateUIForMode();
        loadItems();
    }

    // --- Mode Switching ---

    /**
     * Switches the UI to Padel Centers mode and refreshes the displayed items.
     */
    @FXML
    private void showPadelCenters() {
        isPadelMode = true;
        updateUIForMode();
        loadItems();
    }

    /**
     * Switches the UI to Gym Classes mode and refreshes the displayed items.
     */
    @FXML
    private void showGymClasses() {
        isPadelMode = false;
        updateUIForMode();
        loadItems();
    }

    /**
     * Updates the UI based on the current mode (Padel Centers or Gym Classes).
     * Adjusts form fields visibility, labels, and button styles.
     */
    private void updateUIForMode() {
        if (isPadelMode) {
            formTitle.setText("Add a New Padel Center");
            nameLabel.setText("Center Name:");
            timesLabel.setVisible(true);
            timesLabel.setManaged(true);
            timesField.setVisible(true);
            timesField.setManaged(true);
            locationLabel.setVisible(true);
            locationLabel.setManaged(true);
            locationField.setVisible(true);
            locationField.setManaged(true);
            instructorLabel.setVisible(false);
            instructorLabel.setManaged(false);
            instructorField.setVisible(false);
            instructorField.setManaged(false);
            timeLabel.setVisible(false);
            timeLabel.setManaged(false);
            timeField.setVisible(false);
            timeField.setManaged(false);
            capacityLabel.setVisible(false);
            capacityLabel.setManaged(false);
            capacityField.setVisible(false);
            capacityField.setManaged(false);
            saveButton.setText("Save Center");
            itemsTitle.setText("Existing Padel Centers");
            showPadelButton.setStyle("-fx-background-color: #3a82f7;");
            showGymButton.setStyle("-fx-background-color: #0a1a36;");
        } else {
            formTitle.setText("Add a New Gym Class");
            nameLabel.setText("Class Name:");
            timesLabel.setVisible(false);
            timesLabel.setManaged(false);
            timesField.setVisible(false);
            timesField.setManaged(false);
            locationLabel.setVisible(false);
            locationLabel.setManaged(false);
            locationField.setVisible(false);
            locationField.setManaged(false);
            instructorLabel.setVisible(true);
            instructorLabel.setManaged(true);
            instructorField.setVisible(true);
            instructorField.setManaged(true);
            timeLabel.setVisible(true);
            timeLabel.setManaged(true);
            timeField.setVisible(true);
            timeField.setManaged(true);
            capacityLabel.setVisible(true);
            capacityLabel.setManaged(true);
            capacityField.setVisible(true);
            capacityField.setManaged(true);
            saveButton.setText("Save Class");
            itemsTitle.setText("Existing Gym Classes");
            showPadelButton.setStyle("-fx-background-color: #0a1a36;");
            showGymButton.setStyle("-fx-background-color: #3a82f7;");
        }
        clearForm();
        editingItem = null; // Reset editing state when switching modes
        hideMessage();
    }

    // --- Form Handling ---

    /**
     * Opens a file chooser dialog to select an image for the Padel Center or Gym Class.
     */
    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image for " + (isPadelMode ? "Padel Center" : "Gym Class"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imageField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Clears all form fields, resets the selected image, and clears the editing state.
     */
    private void clearForm() {
        nameField.clear();
        timesField.clear();
        locationField.clear();
        instructorField.clear();
        timeField.clear();
        capacityField.clear();
        imageField.clear();
        selectedImageFile = null;
        editingItem = null;
        // Reset form title and save button text
        if (isPadelMode) {
            formTitle.setText("Add a New Padel Center");
            saveButton.setText("Save Center");
        } else {
            formTitle.setText("Add a New Gym Class");
            saveButton.setText("Save Class");
        }
    }

    /**
     * Populates the form fields with the data of the item being edited.
     *
     * @param itemData The JSON data of the item to edit.
     */
    private void editItem(JSONObject itemData) {
        editingItem = itemData;
        nameField.setText(itemData.optString("name", ""));
        if (isPadelMode) {
            formTitle.setText("Edit Padel Center");
            saveButton.setText("Update Center");
            // Convert availableTimes array to comma-separated string
            String times = "";
            if (itemData.has("availableTimes")) {
                JSONArray timesArray = itemData.getJSONArray("availableTimes");
                times = String.join(", ", timesArray.toList().stream().map(Object::toString).toList());
            } else {
                // Fallback to times field if availableTimes isn't present (for older data)
                times = itemData.optString("times", "");
            }
            timesField.setText(times);
            locationField.setText(itemData.optString("location", ""));
        } else {
            formTitle.setText("Edit Gym Class");
            saveButton.setText("Update Class");
            instructorField.setText(itemData.optString("instructor", ""));
            timeField.setText(itemData.optString("time", "")); // Ensure time is populated
            capacityField.setText(String.valueOf(itemData.optInt("capacity", 0)));
        }
        String imagePath = itemData.optString("image", "");
        if (!imagePath.isEmpty()) {
            imageField.setText(imagePath.replace("file:", ""));
            selectedImageFile = new File(imagePath.replace("file:", ""));
        }
    }

    // --- Message Display ---

    /**
     * Displays a message above the Save button with a success or error style.
     * The message fades out after 3 seconds.
     *
     * @param message   The message to display.
     * @param isSuccess True for success (green), false for error (red).
     */
    private void showMessage(String message, boolean isSuccess) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().clear();
        messageLabel.getStyleClass().add("message-label");
        messageLabel.getStyleClass().add(isSuccess ? "success" : "error");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(this::hideMessage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Hides the message label and clears its text.
     */
    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    // --- Server Communication ---

    /**
     * Saves a new Padel Center or Gym Class, or updates an existing one, by sending the form data to the server.
     */
    @FXML
    private void saveItem() {
        JSONObject itemData = new JSONObject();
        itemData.put("name", nameField.getText());
        String imagePath = selectedImageFile != null ? "file:" + selectedImageFile.getAbsolutePath() : "";
        itemData.put("image", imagePath);

        if (isPadelMode) {
            itemData.put("times", timesField.getText());
            itemData.put("location", locationField.getText());
        } else {
            itemData.put("instructor", instructorField.getText());
            itemData.put("time", timeField.getText());
            itemData.put("capacity", Integer.parseInt(capacityField.getText().trim()));
            // Include existing participants and waitlist data if editing
            if (editingItem != null) {
                itemData.put("participants", editingItem.optJSONArray("participants"));
                itemData.put("waitlist", editingItem.optJSONArray("waitlist"));
                itemData.put("currentParticipants", editingItem.optInt("currentParticipants"));
                itemData.put("waitlistSize", editingItem.optInt("waitlistSize"));
            }
        }


        JSONObject request = new JSONObject();
        if (editingItem != null) {
            // Update request
            request.put("action", isPadelMode ? "update_padel_center" : "update_gym_class");
            request.put("oldData", editingItem);
            request.put("newData", itemData);
        } else {
            // Save new request
            request.put("action", isPadelMode ? "save_padel_center" : "save_gym_class");
            request.put("data", itemData);
        }

        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(request.toString());
            String response = in.readLine();
            System.out.println("Server response: " + response);

            JSONObject responseJson = new JSONObject(response);
            String status = responseJson.optString("status", "error");
            String message = responseJson.optString("message", "Unknown error");

            if (status.equals("success")) {
                showMessage(isPadelMode ?
                        (editingItem != null ? "Padel center updated successfully!" : "Padel center added successfully!") :
                        (editingItem != null ? "Gym class updated successfully!" : "Gym class added successfully!"), true);
                clearForm();
                loadItems();
            } else {
                showMessage(message, false);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to " + (editingItem != null ? "update" : "save") + " " +
                    (isPadelMode ? "center" : "class") + ": " + e.getMessage(), false);
        }
    }

    /**
     * Loads existing Padel Centers or Gym Classes from the server and displays them.
     */
    private void loadItems() {
        itemsContainer.getChildren().clear();

        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            JSONObject request = new JSONObject();
            request.put("action", isPadelMode ? "get_padel_centers" : "get_classes");
            out.println(request.toString());

            String response = in.readLine();
            System.out.println("Raw server response: " + response);

            if (response == null || response.trim().isEmpty()) {
                System.out.println("No items available.");
                socket.close();
                return;
            }

            JSONArray items;
            try {
                if (response.trim().startsWith("[")) {
                    items = new JSONArray(response);
                } else {
                    JSONObject singleObject = new JSONObject(response);
                    items = new JSONArray();
                    items.put(singleObject);
                }
            } catch (JSONException e) {
                System.out.println("Failed to parse server response: " + e.getMessage());
                showMessage("Failed to load items: Invalid server response.", false);
                socket.close();
                return;
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                VBox itemCard = createItemCard(item);
                itemsContainer.getChildren().add(itemCard);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to load items: " + e.getMessage(), false);
        }
    }

    /**
     * Deletes a Padel Center or Gym Class by sending a delete request to the server.
     *
     * @param itemData The JSON data of the item to delete.
     */
    private void deleteItem(JSONObject itemData) {
        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            JSONObject request = new JSONObject();
            request.put("action", isPadelMode ? "delete_padel_center" : "delete_gym_class");
            request.put("data", itemData);

            out.println(request.toString());
            String response = in.readLine();
            System.out.println("Server response: " + response);

            JSONObject responseJson = new JSONObject(response);
            String status = responseJson.optString("status", "error");
            String message = responseJson.optString("message", "Unknown error");

            if (status.equals("success")) {
                showMessage(isPadelMode ? "Padel center deleted successfully!" : "Gym class deleted successfully!", true);
                clearForm(); // Reset form in case we were editing this item
                loadItems();
            } else {
                showMessage(message, false);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to delete " + (isPadelMode ? "center" : "class") + ": " + e.getMessage(), false);
        }
    }

    // --- Item Card Creation ---

    /**
     * Creates a UI card for a Padel Center or Gym Class, including details, edit, and delete buttons.
     *
     * @param itemData The JSON data of the item to display.
     * @return A VBox containing the item's UI elements.
     */
    private VBox createItemCard(JSONObject itemData) {
        VBox itemCard = new VBox(5);
        itemCard.getStyleClass().add("center-card");

        ImageView imageView = new ImageView();
        try {
            String imageUrl = itemData.optString("image", itemData.optString("imagePath" , ""));
            Image image = new Image(imageUrl);
            imageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Failed to load image: " + itemData.optString("image", "N/A"));
        }
        imageView.getStyleClass().add("center-image");

        String name = itemData.optString("name", "Unknown Item");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("center-name");

        if (isPadelMode) {
            String location = itemData.optString("location", "Unknown Location");
            Label locationLabel = new Label("Location: " + location);
            locationLabel.getStyleClass().add("center-detail");

            String times = "No times available";
            if (itemData.has("availableTimes")) {
                JSONArray timesArray = itemData.getJSONArray("availableTimes");
                times = String.join(", ", timesArray.toList().stream().map(Object::toString).toList());
            }
            Label timesLabel = new Label("Available Times: " + times);
            timesLabel.getStyleClass().add("center-detail");

            itemCard.getChildren().addAll(imageView, nameLabel, locationLabel, timesLabel);
        } else {
            String instructor = itemData.optString("instructor", "Unknown Instructor");
            Label instructorLabel = new Label("Instructor: " + instructor);
            instructorLabel.getStyleClass().add("center-detail");

            String time = itemData.optString("time", "Unknown Time");
            Label timeLabel = new Label("Time: " + time);
            timeLabel.getStyleClass().add("center-detail");

            int capacity = itemData.optInt("capacity", 0);
            Label capacityLabel = new Label("Capacity: " + capacity);
            capacityLabel.getStyleClass().add("center-detail");

            int currentParticipants = itemData.optInt("currentParticipants", 0);
            Label participantsLabel = new Label("Current Participants: " + currentParticipants);
            participantsLabel.getStyleClass().add("center-detail");

            int waitlistSize = itemData.optInt("waitlistSize", 0);
            Label waitlistLabel = new Label("Waitlist Size: " + waitlistSize);
            waitlistLabel.getStyleClass().add("center-detail");

            itemCard.getChildren().addAll(imageView, nameLabel, instructorLabel, timeLabel, capacityLabel, participantsLabel, waitlistLabel);
        }

        // Edit and Delete buttons
        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("edit-button");
        editButton.setOnAction(event -> editItem(itemData));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(event -> deleteItem(itemData));

        HBox buttonBox = new HBox(10, editButton, deleteButton);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        itemCard.getChildren().add(buttonBox);
        return itemCard;
    }

    // --- Navigation ---

    /**
     * Navigates to the Home page with a fade transition effect.
     */
    @FXML
    private void goToHome() {
        try {
            Parent homePage = FXMLLoader.load(getClass().getResource("home.fxml"));
            Stage stage = (Stage) saveButton.getScene().getWindow();
            Scene currentScene = stage.getScene();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                currentScene.setRoot(homePage);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), homePage);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to navigate to Home: " + e.getMessage(), false);
        }
    }



    /**
     * Logs out the user and navigates to the Login page.
     */
    @FXML
    private void logout() {
        try {
            Parent loginPage = FXMLLoader.load(getClass().getResource("LoginPage.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(loginPage));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to logout: " + e.getMessage(), false);
        }
    }
}