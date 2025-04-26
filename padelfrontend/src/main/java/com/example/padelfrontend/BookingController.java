package com.example.padelfrontend;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.stream.Collectors;

/**
 * Controller for the Booking page, handling navigation, date picker, combo box interactions,
 * and dynamic loading of padel places.
 */
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
    private ComboBox<HomeController.PadelCenter> centerSearchField;
    @FXML
    private ImageView dropdownicon;
    @FXML
    private ImageView calendaricon;

    private boolean isComboBoxOpen = false;
    private boolean isDatePickerOpen = false;
    private static final String JSON_FILE_PATH = "padelbackend/padel-classes.json";
    private static final Duration TRANSITION_DURATION = Duration.millis(400);
    private boolean isUpdating = false;
    private boolean justSelected = false; // Flag to track if an item was just selected

    private ObservableList<HomeController.PadelCenter> allCenters;
    private long lastUpdateTime = 0;
    private static final long DEBOUNCE_DELAY = 100;

    /**
     * Initializes the controller after FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        bookingButton.getStyleClass().add("active");
        configureDatePicker(datePicker, LocalDate.now());
        setupIcons();
        setupNavbar();
        loadPadelPlacesFromFile();
        setupCenterSearchField();
        applyDropdownStyles();
    }

    /**
     * Sets up the centerSearchField ComboBox with auto-filtering and dropdown behavior.
     */
    private void setupCenterSearchField() {
        // Load padel centers from JSON
        allCenters = loadPadelCentersFromFile();
        centerSearchField.setItems(allCenters);

        // Set up the StringConverter to display only the center name using lambda
        centerSearchField.setConverter(new StringConverter<>() {
            @Override
            public String toString(HomeController.PadelCenter center) {
                return center != null ? center.getName() : "";
            }

            @Override
            public HomeController.PadelCenter fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                return allCenters.stream()
                        .filter(center -> center.getName().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Add listener to filter items based on editor's text input with debouncing
        centerSearchField.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdating) return; // Prevent recursive updates
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < DEBOUNCE_DELAY) return; // Debounce rapid updates
            lastUpdateTime = currentTime;

            isUpdating = true;
            try {
                // Only update filtered items
                if (newValue != null && !newValue.equals(oldValue)) {
                    updateFilteredItems(newValue);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Exception in textProperty listener: " + e.getMessage());
                e.printStackTrace();
                // Reset the editor to a safe state
                Platform.runLater(() -> centerSearchField.getEditor().setText(oldValue != null ? oldValue : ""));
            } finally {
                isUpdating = false;
            }
        });

        // Track dropdown state and ensure sync
        centerSearchField.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            isComboBoxOpen = isShowing;
        });

        // Track when an item is selected
        centerSearchField.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            justSelected = newValue != null; // Set flag when an item is selected
        });

        // Add key event handler to handle typing after selection
        centerSearchField.getEditor().setOnKeyTyped(event -> {
            try {
                TextField editor = centerSearchField.getEditor();

                // Clear the current selection if this is the first key after selecting an item
                if (justSelected) {
                    centerSearchField.getSelectionModel().clearSelection();
                    editor.setText(""); // Clear the editor to start fresh
                    justSelected = false; // Reset the flag
                }

                // Update filtered items and dropdown state after the key type
                Platform.runLater(() -> {
                    String currentText = editor.getText();
                    if (currentText != null) {
                        updateFilteredItems(currentText);
                        if (!isComboBoxOpen && !currentText.isEmpty()) {
                            centerSearchField.show();
                            isComboBoxOpen = true;
                        } else if (currentText.isEmpty() && isComboBoxOpen) {
                            centerSearchField.hide();
                            isComboBoxOpen = false;
                        }
                    }
                });
            } catch (IllegalArgumentException e) {
                System.err.println("Exception in key typed handler: " + e.getMessage());
                e.printStackTrace();
                // Reset the editor to a safe state
                Platform.runLater(() -> centerSearchField.getEditor().setText(""));
            }
        });

        // Handle dropdown icon click to toggle the dropdown
        dropdownicon.setOnMouseClicked(event -> {
            if (centerSearchField.isShowing()) {
                // Use Platform.runLater to ensure the hide action is processed after other events
                Platform.runLater(() -> {
                    centerSearchField.hide();
                    isComboBoxOpen = false;
                    // Safely clear the editor without causing selection issues
                    Platform.runLater(() -> {
                        try {
                            centerSearchField.getEditor().setText("");
                            if (centerSearchField.getScene() != null) {
                                centerSearchField.getScene().getRoot().requestFocus();
                            }
                        } catch (IllegalArgumentException e) {
                            System.err.println("Exception in dropdown icon click handler: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                });
            } else {
                centerSearchField.show();
                isComboBoxOpen = true;
            }
            event.consume(); // Prevent event from propagating to the ComboBox
        });

        // Prevent ComboBox from handling the click event on the icon
        centerSearchField.setOnMouseClicked(event -> {
            if (event.getTarget() == dropdownicon) {
                event.consume(); // Ensure the ComboBox doesn't process this click
            }
        });
    }

    /**
     * Applies the style class to the ComboBox for styling in home.css.
     */
    private void applyDropdownStyles() {
        // Add a unique style class to the ComboBox for styling
        centerSearchField.getStyleClass().add("custom-combo-box");
    }

    private void updateFilteredItems(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            centerSearchField.setItems(allCenters); // Reset to all items
            return;
        }

        // Filter items based on input (case-insensitive)
        String filter = filterText.toLowerCase();
        ObservableList<HomeController.PadelCenter> filteredItems = allCenters.stream()
                .filter(center -> center.getName().toLowerCase().contains(filter))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        centerSearchField.setItems(filteredItems);
    }

    private ObservableList<HomeController.PadelCenter> loadPadelCentersFromFile() {
        ObservableList<HomeController.PadelCenter> centers = FXCollections.observableArrayList();
        try {
            String content = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
            JSONArray centersArray = new JSONArray(content);
            for (int i = 0; i < centersArray.length(); i++) {
                JSONObject centerJson = centersArray.getJSONObject(i);
                String name = centerJson.getString("name");
                centers.add(new HomeController.PadelCenter(name));
            }
        } catch (IOException e) {
            System.err.println("Failed to load padel centers from file: " + e.getMessage());
        }
        return centers;
    }

    /**
     * Sets up the date picker with a custom format and restricts past dates.
     *
     * @param datePicker The DatePicker to configure.
     * @param today      The current date to set as the minimum.
     */
    private void configureDatePicker(DatePicker datePicker, LocalDate today) {
        // Restrict past dates
        if (datePicker.getValue() != null && datePicker.getValue().isBefore(today)) {
            datePicker.setValue(today);
        }
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isBefore(today)) {
                datePicker.setValue(today);
            }
        });

        // Custom date format (dd/MM/yyyy)
        datePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(formatter);
            }

            @Override
            public LocalDate fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalDate.parse(string, formatter);
            }
        });
    }

    /**
     * Sets up the cursor for interactive icons.
     */
    private void setupIcons() {
        dropdownicon.setCursor(Cursor.HAND);
        calendaricon.setCursor(Cursor.HAND);
    }

    /**
     * Configures the navbar buttons with hover effects.
     */
    private void setupNavbar() {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button && button.getStyleClass().contains("nav-button")) {
                addButtonHoverEffects(button);
            }
        }
    }

    /**
     * Adds hover effects to a button using scale transitions.
     *
     * @param button The button to apply hover effects to.
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
     * Sets all navbar buttons to inactive by removing the "active" style.
     */
    @FXML
    public void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

    /**
     * Toggles the visibility of the date picker.
     */
    @FXML
    private void openCalendar() {
        if (isDatePickerOpen) {
            datePicker.hide();
        } else {
            datePicker.show();
        }
        isDatePickerOpen = !isDatePickerOpen;
    }

    /**
     * Toggles the visibility of the combo box.
     */
    @FXML
    private void showCombobox() {
        if (isComboBoxOpen) {
            centerSearchField.hide();
        } else {
            centerSearchField.show();
        }
        isComboBoxOpen = !isComboBoxOpen;
    }

    /**
     * Loads padel places from the JSON file and displays them.
     */
    private void loadPadelPlacesFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
            JSONArray placesArray = new JSONArray(content);
            loadPadelPlaces(placesArray);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load padel places: " + e.getMessage());
        }
    }

    /**
     * Displays padel places in the UI by creating cards for each place.
     *
     * @param placesArray The JSON array of padel places.
     */
    private void loadPadelPlaces(JSONArray placesArray) {
        placesContainer.getChildren().clear();
        placesContainer.setAlignment(Pos.CENTER);

        for (int i = 0; i < placesArray.length(); i++) {
            JSONObject place = placesArray.getJSONObject(i);
            HBox card = createPadelPlaceCard(
                    place.getString("name"),
                    place.getString("image"),
                    place.getString("location"),
                    place.getJSONArray("availableTimes")
            );
            HBox centeringWrapper = new HBox(card);
            centeringWrapper.setAlignment(Pos.CENTER);
            placesContainer.getChildren().add(centeringWrapper);
        }
    }

    /**
     * Creates a UI card for a padel place with image, details, and available times.
     *
     * @param name         The name of the padel place.
     * @param imagePath    The path to the place's image.
     * @param location     The location of the place.
     * @param times        The array of available times.
     * @return An HBox containing the padel place card.
     */
    private HBox createPadelPlaceCard(String name, String imagePath, String location, JSONArray times) {
        // Create the main card container
        HBox card = new HBox(20);
        card.getStyleClass().add("padel-place-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(1100);
        card.setMaxWidth(1100);

        // Left section: image, name, and location
        HBox leftSection = new HBox(10);
        leftSection.setAlignment(Pos.CENTER_LEFT);

        // Load and style the image
        ImageView imageView = new ImageView(new Image(imagePath));
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        Rectangle clip = new Rectangle(120, 120);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        // Name and location details
        VBox details = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("padel-place-name");
        nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 15 0 0 0");

        Label locationLabel = new Label(location);
        locationLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #b0b0b0; -fx-padding: 0 0 0 0");

        details.getChildren().addAll(nameLabel, locationLabel);
        leftSection.getChildren().addAll(imageView, details);

        // Right section: available times in a grid
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(10);
        timeGrid.setVgap(10);
        timeGrid.setAlignment(Pos.CENTER_RIGHT);

        // Add time buttons to the grid (5 columns max per row)
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

        // Assemble the card
        card.getChildren().addAll(leftSection, spacer, timeGrid);
        return card;
    }

    /**
     * Navigates to the Home page with a slide transition.
     */
    @FXML
    private void goToHome() {
        navigateToPage("home.fxml", -1);
    }

    /**
     * Navigates to the Gym page with a slide transition.
     */
    @FXML
    private void goToGym() {
        navigateToPage("gym.fxml", 1);
    }

    /**
     * Navigates to the Subscription page with a slide transition.
     */
    @FXML
    private void goToSubscription() {
        navigateToPage("subscription.fxml", 1);
    }

    /**
     * Navigates to the specified page with a slide transition.
     *
     * @param fxmlFile  The FXML file of the target page.
     * @param direction The direction of the slide (1 for right-to-left, -1 for left-to-right).
     */
    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();

            Stage stage = (Stage) placesContainer.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            // Create a temporary container for the transition
            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            // Slide out the current page
            TranslateTransition slideOut = new TranslateTransition(TRANSITION_DURATION, currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            // Slide in the new page
            TranslateTransition slideIn = new TranslateTransition(TRANSITION_DURATION, newPage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            // Clean up after the transition
            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate to page: " + e.getMessage());
        }
    }

    /**
     * Shows an alert dialog with the specified title and message.
     *
     * @param title   The title of the alert.
     * @param message The message to display.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}