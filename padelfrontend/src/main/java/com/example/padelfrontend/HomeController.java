package com.example.padelfrontend;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the Home page, handling navigation, date picker, and combo box interactions.
 */
public class HomeController {

    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<PadelCenter> centerSearchField;
    @FXML
    private ImageView dropdownicon;
    @FXML
    private ImageView calendaricon;
    @FXML
    private HBox navbar;
    @FXML
    private Button bookingButton;
    @FXML
    private Button subscriptionButton;
    @FXML
    private Button gymButton;
    @FXML
    private Button homeButton;

    private boolean isComboBoxOpen = false;
    private boolean isDatePickerOpen = false;
    private boolean isUpdating = false;
    private boolean justSelected = false; // Flag to track if an item was just selected
    private static final Duration TRANSITION_DURATION = Duration.millis(400);
    private static final String PADEL_CENTERS_FILE = "padelbackend/padel-classes.json";
    private ObservableList<PadelCenter> allCenters;
    private long lastUpdateTime = 0;
    private static final long DEBOUNCE_DELAY = 100;

    /**
     * A simple class to represent a padel center.
     */
    static class PadelCenter {
        private final String name;

        public PadelCenter(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @FXML
    public void initialize() {
        configureDatePicker(datePicker, LocalDate.now());
        setupIcons();
        setupNavbar();
        homeButton.getStyleClass().add("active");
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
            public String toString(PadelCenter center) {
                return center != null ? center.getName() : "";
            }

            @Override
            public PadelCenter fromString(String string) {
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
        ObservableList<PadelCenter> filteredItems = allCenters.stream()
                .filter(center -> center.getName().toLowerCase().contains(filter))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        centerSearchField.setItems(filteredItems);
    }

    private ObservableList<PadelCenter> loadPadelCentersFromFile() {
        ObservableList<PadelCenter> centers = FXCollections.observableArrayList();
        try {
            String content = new String(Files.readAllBytes(Paths.get(PADEL_CENTERS_FILE)));
            JSONArray centersArray = new JSONArray(content);
            for (int i = 0; i < centersArray.length(); i++) {
                JSONObject centerJson = centersArray.getJSONObject(i);
                String name = centerJson.getString("name");
                centers.add(new PadelCenter(name));
            }
        } catch (IOException e) {
            System.err.println("Failed to load padel centers from file: " + e.getMessage());
        }
        return centers;
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
                return date == null ? "" : date.format(formatter);
            }

            @Override
            public LocalDate fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalDate.parse(string, formatter);
            }
        });
    }

    private void setupIcons() {
        dropdownicon.setCursor(Cursor.HAND);
        calendaricon.setCursor(Cursor.HAND);
    }

    private void setupNavbar() {
        for (Node node : navbar.getChildren()) {
            if (node instanceof Button button && button.getStyleClass().contains("nav-button")) {
                addButtonHoverEffects(button);
            }
        }
    }

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

    @FXML
    public void setInactiveButtons() {
        homeButton.getStyleClass().remove("active");
        bookingButton.getStyleClass().remove("active");
        subscriptionButton.getStyleClass().remove("active");
        gymButton.getStyleClass().remove("active");
    }

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
    private void gotoLogin() {
        try {
            Parent loginPage = FXMLLoader.load(getClass().getResource("LoginPage.fxml"));
            Scene currentScene = datePicker.getScene();
            Parent currentPage = currentScene.getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentPage);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                currentScene.setRoot(loginPage);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginPage);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();

            Stage stage = (Stage) datePicker.getScene().getWindow();
            Scene currentScene = stage.getScene();
            Parent currentPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(TRANSITION_DURATION, currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());

            TranslateTransition slideIn = new TranslateTransition(TRANSITION_DURATION, newPage);
            slideIn.setToX(0);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}