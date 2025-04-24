package com.example.padelfrontend;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BookingController {

    @FXML
    private VBox placesContainer;
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
    private DatePicker datePicker;
    @FXML
    private ComboBox centerSearchField;
    @FXML
    private ImageView dropdownicon, calendaricon;

    private boolean isComboBoxOpen = false;
    private boolean isDatePickerOpen = false;

    @FXML
    private void openCalendar() {

        if (isDatePickerOpen) {
            datePicker.hide();
        } else {
            datePicker.show();
        }
        isDatePickerOpen = !isDatePickerOpen;
    }

    @FXML
    private void showCombobox() {
        if (isComboBoxOpen) {
            centerSearchField.hide();
        } else {
            centerSearchField.show();
        }
        isComboBoxOpen = !isComboBoxOpen;
    }

    @FXML
    public void initialize() {
        bookingButton.getStyleClass().add("active");
        configureDatePicker(datePicker, LocalDate.now());
        dropdownicon.setCursor(Cursor.HAND);
        calendaricon.setCursor(Cursor.HAND);
        try {
            // Path to your JSON file
            String jsonFilePath = "padel-places.json";

            // Read file content
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

            // Parse JSON content into a JSONArray
            JSONArray placesArray = new JSONArray(content);

            // Pass the entire array to the loadPadelPlaces method
            loadPadelPlaces(placesArray); // This should now work
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    @FXML
    public void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

    private void configureDatePicker(DatePicker datePicker, LocalDate today) {
        if (datePicker.getValue() != null && datePicker.getValue().isBefore(today)) {
            datePicker.setValue(today);
        }

        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isBefore(today)) {
                datePicker.setValue(today);
            }
        });
        datePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return (date == null) ? "" : date.format(formatter);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(string, formatter);
            }
        });
    }


    // Method to add a padel place to the VBox dynamically
    public void loadPadelPlaces(JSONArray placesArray) {
        placesContainer.getChildren().clear(); // Clear previous results
        placesContainer.setAlignment(Pos.CENTER); // Ensure children are centered, not stretched

        for (int i = 0; i < placesArray.length(); i++) {
            JSONObject place = placesArray.getJSONObject(i);
            String name = place.getString("name");
            JSONArray times = place.getJSONArray("availableTimes");
            String imagePath = place.getString("image"); // Retrieve the image path from JSON
            String location = place.getString("location"); // Retrieve the location from JSON

            // Create the main card container using HBox
            HBox card = new HBox(20);
            card.getStyleClass().add("padel-place-card");
            card.setPadding(new Insets(20));
            card.setPrefWidth(1100); // Reduced width from previous change
            card.setMaxWidth(1100); // Enforce the maximum width to prevent stretching

            // Left side: image, name, and location
            HBox leftSection = new HBox(10);
            leftSection.setAlignment(Pos.CENTER_LEFT);

            // Load the image from the JSON path
            ImageView imageView = new ImageView(new Image(imagePath));
            imageView.setFitWidth(120); // From previous change
            imageView.setFitHeight(120); // From previous change

            // Apply a rectangular clip with rounded corners for curved edges
            Rectangle clip = new Rectangle(120, 120); // Match the image dimensions
            clip.setArcWidth(20); // Controls the horizontal curvature of the corners
            clip.setArcHeight(20); // Controls the vertical curvature of the corners
            imageView.setClip(clip);

            // Name and location in a VBox
            VBox details = new VBox(5);
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("padel-place-name");
            nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 15 0 0 0");

            Label locationLabel = new Label(location);
            locationLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #b0b0b0; -fx-padding: 0 0 0 0"); // Light gray for location

            details.getChildren().addAll(nameLabel, locationLabel);
            leftSection.getChildren().addAll(imageView, details);

            // Right side: available times in a grid
            GridPane timeGrid = new GridPane();
            timeGrid.setHgap(10);
            timeGrid.setVgap(10);
            timeGrid.setAlignment(Pos.CENTER_RIGHT);

            // Add time buttons to the grid (5 columns max per row as in the image)
            int columns = 5;
            for (int j = 0; j < times.length(); j++) {
                String time = times.getString(j);
                Button timeBtn = new Button(time);
                timeBtn.getStyleClass().add("time-button");

                int row = j / columns;
                int col = j % columns;
                timeGrid.add(timeBtn, col, row);
            }

            // Add a spacer to push the grid to the right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add the left section, spacer, and time grid to the card
            card.getChildren().addAll(leftSection, spacer, timeGrid);

            // Wrap the card in a centering HBox
            HBox centeringWrapper = new HBox(card);
            centeringWrapper.setAlignment(Pos.CENTER); // Center the card horizontally

            // Add the wrapped card to the main container
            placesContainer.getChildren().add(centeringWrapper);
        }
    }

    @FXML
    private void goToHome() {
        try {
            // Load the Home page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));
            Parent homePage = loader.load();

            // Get the current Stage and Scene
            Stage stage = (Stage) placesContainer.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            // Create a temporary StackPane to hold both scenes during the transition
            StackPane transitionPane = new StackPane(currentPage, homePage);
            homePage.translateXProperty().set(-currentScene.getWidth()); // Start off-screen to the left
            currentScene.setRoot(transitionPane); // Set temp container first

            // Slide out the current scene to the right
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), currentPage);
            slideOut.setToX(currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH); // Smooth ease-in/ease-out effect

            // Slide in the new scene from the left
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), homePage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH); // Smooth ease-in/ease-out effect

            // Clean up the StackPane and set the new root after the transition
            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear(); // Remove both nodes from StackPane
                currentScene.setRoot(homePage); // Set the new root
            });

            // Play both transitions
            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
