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

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

    private JSONObject editingItem = null;
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/yoga.jpg";

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

        // Set initial directory to project's resources/images folder
        String resourcePath = "padelfrontend/src/main/resources/images";
        File initialDirectory = new File(resourcePath);
        if (!initialDirectory.exists()) {
            initialDirectory.mkdirs();
        }
        fileChooser.setInitialDirectory(initialDirectory);

        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try {
                // Convert to relative path
                String projectDir = System.getProperty("user.dir");
                String relativePath = file.getPath().replace(projectDir + File.separator, "");

                // Store the relative path
                selectedImageFile = file;
                imageField.setText(relativePath);

                // Copy file to resources if it's not already there
                if (!file.getPath().startsWith(projectDir + File.separator + resourcePath)) {
                    File destFile = new File(resourcePath + File.separator + file.getName());
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    selectedImageFile = destFile;
                    imageField.setText("images/" + destFile.getName());
                }
            } catch (IOException e) {
                System.err.println("Error copying image file: " + e.getMessage());
                showMessage("Failed to process image file", false);
            }
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
                try {
                    JSONArray timesArray = itemData.getJSONArray("availableTimes");
                    times = String.join(", ", timesArray.toList().stream().map(Object::toString).toList());
                } catch (JSONException e) {
                    System.err.println("Failed to parse availableTimes for item: " + itemData.optString("name", "Unknown") + ", error: " + e.getMessage());
                }
            } else {
                // Fallback to times field for older data
                times = itemData.optString("times", "");
            }
            timesField.setText(times);
            locationField.setText(itemData.optString("location", ""));
        } else {
            formTitle.setText("Edit Gym Class");
            saveButton.setText("Update Class");
            instructorField.setText(itemData.optString("instructor", ""));
            timeField.setText(itemData.optString("time", ""));
            capacityField.setText(String.valueOf(itemData.optInt("capacity", 0)));
        }
        String imagePath = itemData.optString("image", "");
        if (!imagePath.isEmpty()) {
            if (imagePath.startsWith("file:")) {
                // Handle legacy file-based image paths
                String filePath = imagePath.replace("file:", "");
                imageField.setText(filePath);
                selectedImageFile = new File(filePath);
            } else {
                // Handle classpath resource paths
                imageField.setText(imagePath);
                selectedImageFile = null; // No file selected for resource paths
            }
        } else {
            imageField.setText("");
            selectedImageFile = null;
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

        // Handle image path
        String imagePath;
        if (selectedImageFile != null) {
            // Convert to relative path format
            String fileName = selectedImageFile.getName();
            imagePath = "images/" + fileName;

            // Ensure image is in resources directory
            try {
                File resourceDir = new File("padelfrontend/src/main/resources/images");
                if (!resourceDir.exists()) {
                    resourceDir.mkdirs();
                }

                File destFile = new File(resourceDir, fileName);
                if (!selectedImageFile.equals(destFile)) {
                    Files.copy(selectedImageFile.toPath(), destFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("Failed to copy image to resources: " + e.getMessage());
                showMessage("Failed to process image file", false);
                return;
            }
        } else if (editingItem != null) {
            imagePath = editingItem.optString("image", "");
        } else {
            imagePath = "";
        }
        itemData.put("image", imagePath);

        // Rest of the method remains the same
        if (isPadelMode) {
            itemData.put("times", timesField.getText());
            itemData.put("location", locationField.getText());
        } else {
            itemData.put("instructor", instructorField.getText());
            itemData.put("time", timeField.getText());
            try {
                itemData.put("capacity", Integer.parseInt(capacityField.getText().trim()));
            } catch (NumberFormatException e) {
                showMessage("Invalid capacity: Please enter a number.", false);
                return;
            }
            if (editingItem != null) {
                itemData.put("participants", editingItem.optJSONArray("participants"));
                itemData.put("waitlist", editingItem.optJSONArray("waitlist"));
                itemData.put("currentParticipants", editingItem.optInt("currentParticipants"));
                itemData.put("waitlistSize", editingItem.optInt("waitlistSize"));
            }
        }

        JSONObject request = new JSONObject();
        if (editingItem != null) {
            request.put("action", isPadelMode ? "update_padel_center" : "update_gym_class");
            request.put("oldData", editingItem);
            request.put("newData", itemData);
        } else {
            request.put("action", isPadelMode ? "save_padel_center" : "save_gym_class");
            request.put("data", itemData);
        }

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

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
                showMessage("No " + (isPadelMode ? "padel centers" : "gym classes") + " available.", false);
                socket.close();
                return;
            }

            JSONObject responseJson;
            try {
                responseJson = new JSONObject(response);
            } catch (JSONException e) {
                System.err.println("Failed to parse server response: " + e.getMessage());
                showMessage("Failed to load items: Invalid server response.", false);
                socket.close();
                return;
            }

            String status = responseJson.optString("status", "error");
            if (!status.equals("success")) {
                String message = responseJson.optString("message", "Unknown error");
                showMessage("Failed to load items: " + message, false);
                socket.close();
                return;
            }

            JSONArray items = responseJson.optJSONArray("data");
            if (items == null || items.length() == 0) {
                showMessage("No " + (isPadelMode ? "padel centers" : "gym classes") + " found.", false);
                socket.close();
                return;
            }

            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    VBox itemCard = createItemCard(item);
                    itemsContainer.getChildren().add(itemCard);
                } catch (JSONException e) {
                    System.err.println("Failed to process item at index " + i + ": " + e.getMessage());
                }
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
            String imagePath = itemData.optString("image", itemData.optString("imagePath", ""));
            // Remove any file: prefix and normalize path
            imagePath = imagePath.replace("file:", "").replace("\\", "/");

            Image image;
            if (imagePath.isEmpty()) {
                System.out.println("No image path provided for item: " + itemData.optString("name", "Unknown"));
                image = loadResourceImage(PLACEHOLDER_IMAGE_PATH);
            } else {
                image = loadResourceImage(imagePath);
            }
            imageView.setImage(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Failed to load image for item: " + itemData.optString("name", "Unknown") + ", error: " + e.getMessage());
            imageView.setImage(loadResourceImage(PLACEHOLDER_IMAGE_PATH));
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

    private Image loadResourceImage(String resourcePath) {
        try {
            // Remove any 'file:' prefix if present
            resourcePath = resourcePath.replace("file:", "");

            // If the path starts with a slash, remove it
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }

            // First try loading as a classpath resource
            InputStream stream = getClass().getResourceAsStream("/" + resourcePath);

            // If not found in classpath, try loading from resources directory
            if (stream == null) {
                File resourceFile = new File("padelfrontend/src/main/resources/" + resourcePath);
                if (resourceFile.exists()) {
                    stream = new FileInputStream(resourceFile);
                } else {
                    // If still not found, try loading the placeholder
                    stream = getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH);
                    if (stream == null) {
                        throw new IOException("Neither image nor placeholder could be found");
                    }
                }
            }

            Image image = new Image(stream);
            stream.close();

            if (image.isError()) {
                throw new IOException("Failed to load image");
            }
            return image;
        } catch (Exception e) {
            System.err.println("Failed to load image: " + resourcePath + ", error: " + e.getMessage());
            // Return a transparent 1x1 image as fallback
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        }
    }

    // --- Navigation ---

    /**
     * Navigates to the Home page with a fade transition effect.
     */
    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));
            Parent homePage = loader.load();
            AppContext context = AppContext.getInstance();
            context.clear();

            // Get HomeController to force update login button
            HomeController homeController = loader.getController();
            if (homeController != null) {
                homeController.updateLoginButton();
                homeController.updateJoinButton();
            }
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
                javafx.application.Platform.runLater(() -> {
                    homePage.requestLayout();
                    if (homeController != null) {
                        homeController.updateLoginButton();

                    }
                });
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