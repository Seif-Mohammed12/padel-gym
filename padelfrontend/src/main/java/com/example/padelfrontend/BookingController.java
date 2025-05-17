package com.example.padelfrontend;

import javafx.animation.*;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Controller for the Booking page, handling navigation, date picker, combo box interactions,
 * and dynamic loading of padel places.
 */
public class BookingController {

    @FXML private VBox placesContainer;
    @FXML private Button bookingButton;
    @FXML private Button subscriptionButton;
    @FXML private Button gymButton;
    @FXML private Button homeButton, loginButton, exploreButton;
    @FXML private HBox navbar;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<PadelCenter> centerSearchField;
    @FXML private ImageView dropdownicon;
    @FXML private ImageView calendaricon;
    private VBox currentOpenPanel = null; // New class field to track the open panel
    private boolean isComboBoxOpen = false;
    private boolean isDatePickerOpen = false;
    private static final Duration TRANSITION_DURATION = Duration.millis(400);
    private boolean isUpdating = false;
    private boolean justSelected = false;
    private ObservableList<PadelCenter> allCenters;
    private JSONArray allPadelCenters;
    private long lastUpdateTime = 0;
    private static final long DEBOUNCE_DELAY = 100;
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder.png";

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
        bookingButton.getStyleClass().add("active");
        datePicker.setValue(LocalDate.now()); // Set DatePicker to today's date
        configureDatePicker(datePicker, LocalDate.now());
        setupIcons();
        setupNavbar();
        setupCenterSearchField();
        applyDropdownStyles();
        fetchPadelPlaces();
        updateLoginButton();
        addButtonHoverEffects(exploreButton); // Add hover effects to Explore button
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
     * Sets up the centerSearchField ComboBox with auto-filtering and dropdown behavior.
     */
    private void setupCenterSearchField() {
        allCenters = FXCollections.observableArrayList();
        centerSearchField.setItems(allCenters);

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

        centerSearchField.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdating) return;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < DEBOUNCE_DELAY) return;
            lastUpdateTime = currentTime;

            isUpdating = true;
            try {
                if (newValue != null && !newValue.equals(oldValue)) {
                    updateFilteredItems(newValue);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Exception in textProperty listener: " + e.getMessage());
                Platform.runLater(() -> centerSearchField.getEditor().setText(oldValue != null ? oldValue : ""));
            } finally {
                isUpdating = false;
            }
        });

        centerSearchField.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            isComboBoxOpen = isShowing;
        });

        centerSearchField.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            justSelected = newValue != null;
        });

        centerSearchField.getEditor().setOnKeyTyped(event -> {
            try {
                TextField editor = centerSearchField.getEditor();
                if (justSelected) {
                    centerSearchField.getSelectionModel().clearSelection();
                    editor.setText("");
                    justSelected = false;
                }

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
                Platform.runLater(() -> centerSearchField.getEditor().setText(""));
            }
        });

        dropdownicon.setOnMouseClicked(event -> {
            if (centerSearchField.isShowing()) {
                Platform.runLater(() -> {
                    centerSearchField.hide();
                    isComboBoxOpen = false;
                    Platform.runLater(() -> {
                        try {
                            centerSearchField.getEditor().setText("");
                            if (centerSearchField.getScene() != null) {
                                centerSearchField.getScene().getRoot().requestFocus();
                            }
                        } catch (IllegalArgumentException e) {
                            System.err.println("Exception in dropdown icon click handler: " + e.getMessage());
                        }
                    });
                });
            } else {
                centerSearchField.show();
                isComboBoxOpen = true;
            }
            event.consume();
        });

        centerSearchField.setOnMouseClicked(event -> {
            if (event.getTarget() == dropdownicon) {
                event.consume();
            }
        });
    }

    /**
     * Applies the style class to the ComboBox for styling.
     */
    private void applyDropdownStyles() {
        centerSearchField.getStyleClass().add("custom-combo-box");
    }

    /**
     * Updates the filtered items in the ComboBox based on the input text.
     */
    private void updateFilteredItems(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            centerSearchField.setItems(allCenters);
            return;
        }

        String filter = filterText.toLowerCase();
        ObservableList<PadelCenter> filteredItems = allCenters.stream()
                .filter(center -> center.getName().toLowerCase().contains(filter))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        centerSearchField.setItems(filteredItems);
    }

    /**
     * Fetches padel places from the server and populates the UI.
     */
    private void fetchPadelPlaces() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject request = new JSONObject();
                request.put("action", "get_padel_centers");
                out.print(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                if ("success".equals(status)) {
                    allPadelCenters = jsonResponse.getJSONArray("data"); // Store raw data
                    Platform.runLater(() -> {
                        populatePadelCenters(allPadelCenters);
                        loadPadelPlaces(allPadelCenters); // Load all places initially
                    });
                } else {
                    throw new Exception(jsonResponse.optString("message", "Failed to fetch padel centers"));
                }

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Failed to load padel places: " + ex.getMessage(), getStage()));
            }
        }).start();
    }

    /**
     * Populates the centerSearchField ComboBox with padel centers.
     */
    private void populatePadelCenters(JSONArray centers) {
        allCenters.clear();
        for (int i = 0; i < centers.length(); i++) {
            JSONObject center = centers.getJSONObject(i);
            String name = center.optString("name", "Unknown Center");
            allCenters.add(new PadelCenter(name));
        }
        centerSearchField.setItems(allCenters);
    }

    /**
     * Displays padel places in the UI by creating cards for each place.
     */
    private void loadPadelPlaces(JSONArray placesArray) {
        placesContainer.getChildren().clear();
        placesContainer.setAlignment(Pos.CENTER);

        for (int i = 0; i < placesArray.length(); i++) {
            JSONObject place = placesArray.getJSONObject(i);
            String name = place.optString("name", "Unknown Center");
            String imagePath = place.optString("image", "");
            String location = place.optString("location", "Unknown Location");
            JSONArray times = place.optJSONArray("availableTimes", new JSONArray());

            HBox card = createPadelPlaceCard(name, imagePath, location, times);
            HBox centeringWrapper = new HBox(card);
            centeringWrapper.setAlignment(Pos.CENTER);
            placesContainer.getChildren().add(centeringWrapper);
        }
    }

    /**
     * Creates a UI card for a padel place with image, details, and available times.
     */
    private HBox createPadelPlaceCard(String name, String imagePath, String location, JSONArray times) {
        HBox card = new HBox(20);
        card.getStyleClass().add("padel-place-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(1100);
        card.setMaxWidth(1100);

        // Left section with image and details
        HBox leftSection = new HBox(10);
        leftSection.setAlignment(Pos.CENTER_LEFT);

        ImageView imageView = createPlaceImage(imagePath);
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        Rectangle clip = new Rectangle(120, 120);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        VBox details = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("padel-place-name");
        nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label locationLabel = new Label(location);
        locationLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #b0b0b0;");

        String dateText = datePicker.getValue() != null ?
                datePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                "No date selected";
        Label dateLabel = new Label("Date: " + dateText);
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b0b0b0;");

        details.getChildren().addAll(nameLabel, locationLabel, dateLabel);
        leftSection.getChildren().addAll(imageView, details);

        // Right section container
        VBox rightSection = new VBox();
        rightSection.setAlignment(Pos.TOP_RIGHT);
        rightSection.setMaxWidth(Double.MAX_VALUE);

        // Time slots grid
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(10);
        timeGrid.setVgap(10);
        timeGrid.setAlignment(Pos.TOP_RIGHT);

        // Booked times grid (below available times)
        GridPane bookedGrid = new GridPane();
        bookedGrid.setHgap(10);
        bookedGrid.setVgap(10);
        bookedGrid.setAlignment(Pos.TOP_RIGHT);

        // Booking panel (hidden by default)
        StackPane bookingPanelContainer = new StackPane();
        bookingPanelContainer.setAlignment(Pos.TOP_RIGHT);
        bookingPanelContainer.setVisible(false);
        bookingPanelContainer.setManaged(false);
        bookingPanelContainer.setOpacity(0);

        // Backdrop for depth
        Rectangle backdrop = new Rectangle(280, 280);
        backdrop.setFill(Color.color(0, 0, 0, 0.3));
        backdrop.setArcWidth(30);
        backdrop.setArcHeight(30);

        // Inner VBox for booking panel content
        VBox bookingPanel = new VBox(15);
        bookingPanel.getStyleClass().add("booking-panel");
        bookingPanel.setPadding(new Insets(20));
        bookingPanel.setAlignment(Pos.TOP_CENTER);
        bookingPanel.setPrefWidth(280);
        bookingPanel.setMinWidth(280);
        bookingPanel.setMaxWidth(280);
        bookingPanel.setPrefHeight(280);
        bookingPanel.setMinHeight(280);
        bookingPanel.setMaxHeight(320);

        // Booking panel content
        Label bookingTitle = new Label("Book Your Slot");
        bookingTitle.getStyleClass().add("booking-title");

        HBox courtInfo = new HBox(8);
        Label courtIcon = new Label("🏟️");
        courtIcon.getStyleClass().add("booking-icon");
        Label courtName = new Label(name);
        courtName.getStyleClass().add("booking-text");
        courtName.setWrapText(true);
        courtName.setMaxWidth(210);
        courtInfo.getChildren().addAll(courtIcon, courtName);
        courtInfo.setAlignment(Pos.CENTER);

        HBox timeInfo = new HBox(8);
        Label timeIcon = new Label("🕒");
        timeIcon.getStyleClass().add("booking-icon");
        Label timeLabel = new Label();
        timeLabel.getStyleClass().add("booking-text");
        timeInfo.getChildren().addAll(timeIcon, timeLabel);
        timeInfo.setAlignment(Pos.CENTER);

        HBox dateInfo = new HBox(8);
        Label dateIcon = new Label("📅");
        dateIcon.getStyleClass().add("booking-icon");
        Label dateInfoLabel = new Label(dateText);
        dateInfoLabel.getStyleClass().add("booking-text");
        dateInfo.getChildren().addAll(dateIcon, dateInfoLabel);
        dateInfo.setAlignment(Pos.CENTER);

        HBox buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER);

        Button actionButton = new Button("Confirm");
        actionButton.getStyleClass().add("book-now-button");
        actionButton.setPrefWidth(120);
        actionButton.setPrefHeight(36);

        buttonContainer.getChildren().add(actionButton);

        bookingPanel.getChildren().addAll(
                bookingTitle,
                new Separator(),
                courtInfo,
                timeInfo,
                dateInfo,
                new Region(),
                buttonContainer
        );
        VBox.setVgrow(new Region(), Priority.ALWAYS);

        bookingPanelContainer.getChildren().addAll(backdrop, bookingPanel);

        // Track selected time button
        final Button[] selectedTimeButton = {null};
        AppContext context = AppContext.getInstance();
        String memberId = context.isLoggedIn() ? context.getMemberId() : null;

        // Check for booked times
        JSONObject place = findPlaceByName(name);
        JSONArray bookedTimes = place != null && place.has("bookedTimes") ? place.getJSONObject("bookedTimes").names() : new JSONArray();
        int bookedIndex = 0;
        int bookedColumns = 5;

        if (bookedTimes != null && memberId != null) {
            for (int j = 0; j < bookedTimes.length(); j++) {
                String time = bookedTimes.getString(j);
                String bookedMemberId = place.getJSONObject("bookedTimes").getString(time);
                if (bookedMemberId.equals(memberId)) {
                    Button bookedBtn = new Button(time + " (Booked)");
                    bookedBtn.getStyleClass().add("booked-button");
                    bookedBtn.setOnAction(e -> {
                        timeLabel.setText(time);
                        actionButton.setText("Cancel");
                        actionButton.getStyleClass().remove("book-now-button");
                        actionButton.getStyleClass().add("cancel-button");

                        if (selectedTimeButton[0] != null) {
                            selectedTimeButton[0].getStyleClass().remove("time-button-selected");
                        }
                        bookedBtn.getStyleClass().add("time-button-selected");
                        selectedTimeButton[0] = bookedBtn;

                        if (!bookingPanelContainer.isVisible()) {
                            bookingPanelContainer.setVisible(true);
                            bookingPanelContainer.setManaged(true);
                            bookingPanel.getStyleClass().add("visible-panel");
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bookingPanelContainer);
                            fadeIn.setFromValue(0);
                            fadeIn.setToValue(1);
                            fadeIn.play();
                        }
                    });

                    int row = bookedIndex / bookedColumns;
                    int col = bookedIndex % bookedColumns;
                    bookedGrid.add(bookedBtn, col, row);
                    bookedIndex++;
                }
            }
        }

        // Available time slots
        int columns = 5;
        for (int j = 0; j < times.length(); j++) {
            String time = times.getString(j);
            Button timeBtn = new Button(time);
            timeBtn.getStyleClass().add("time-button");

            timeBtn.setOnAction(e -> {
                timeLabel.setText(time);
                actionButton.setText("Confirm");
                actionButton.getStyleClass().remove("cancel-button");
                actionButton.getStyleClass().add("book-now-button");

                if (selectedTimeButton[0] == timeBtn) {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), bookingPanelContainer);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(event -> {
                        bookingPanelContainer.setVisible(false);
                        bookingPanelContainer.setManaged(false);
                        bookingPanel.getStyleClass().remove("visible-panel");
                        timeBtn.getStyleClass().remove("time-button-selected");
                    });
                    fadeOut.play();
                    selectedTimeButton[0] = null;
                } else {
                    if (selectedTimeButton[0] != null) {
                        selectedTimeButton[0].getStyleClass().remove("time-button-selected");
                    }
                    timeBtn.getStyleClass().add("time-button-selected");
                    selectedTimeButton[0] = timeBtn;

                    if (!bookingPanelContainer.isVisible()) {
                        bookingPanelContainer.setVisible(true);
                        bookingPanelContainer.setManaged(true);
                        bookingPanel.getStyleClass().add("visible-panel");
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bookingPanelContainer);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();
                    }
                }
            });

            actionButton.setOnAction(e -> {
                if (actionButton.getText().equals("Confirm")) {
                    bookPadelCourt(name, timeLabel.getText(), datePicker.getValue());
                } else {
                    cancelPadelCourt(name, timeLabel.getText(), datePicker.getValue());
                }
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), bookingPanelContainer);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> {
                    bookingPanelContainer.setVisible(false);
                    bookingPanelContainer.setManaged(false);
                    bookingPanel.getStyleClass().remove("visible-panel");
                    if (selectedTimeButton[0] != null) {
                        selectedTimeButton[0].getStyleClass().remove("time-button-selected");
                        selectedTimeButton[0] = null;
                    }
                });
                fadeOut.play();
            });

            int row = j / columns;
            int col = j % columns;
            timeGrid.add(timeBtn, col, row);
        }

        // Add grids to right section
        rightSection.getChildren().addAll(timeGrid, bookedGrid, bookingPanelContainer);

        // Main content container
        VBox cardContent = new VBox(15);
        cardContent.setAlignment(Pos.TOP_CENTER);

        // Horizontal content with left section and right section
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.CENTER_LEFT);

        // Left section
        leftSection.setPrefWidth(500);
        HBox.setHgrow(leftSection, Priority.NEVER);

        // Right section
        rightSection.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        rightSection.setMaxWidth(Double.MAX_VALUE);

        // Spacer to push rightSection to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        mainContent.getChildren().addAll(leftSection, spacer, rightSection);

        cardContent.getChildren().add(mainContent);
        card.getChildren().add(cardContent);

        return card;
    }

    private JSONObject findPlaceByName(String name) {
        for (int i = 0; i < allPadelCenters.length(); i++) {
            JSONObject place = allPadelCenters.getJSONObject(i);
            if (place.optString("name").equals(name)) {
                return place;
            }
        }
        return null;
    }

    private void cancelPadelCourt(String courtName, String time, LocalDate date) {
        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            showAlert( "You must be logged in to cancel a booking.", getStage());
            return;
        }
        if (date == null) {
            showAlert("Please select a date.", getStage());
            return;
        }

        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject request = new JSONObject();
                request.put("action", "cancel_padel_booking");
                JSONObject data = new JSONObject();
                data.put("courtName", courtName);
                data.put("time", time);
                data.put("memberId", context.getMemberId());
                data.put("date", date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                request.put("data", data);
                out.print(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                Platform.runLater(() -> {
                    if ("success".equals(status)) {
                        showAlert( "Booking for " + courtName + " at " + time + " on " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " cancelled!", getStage());
                        fetchPadelPlaces(); // Refresh UI
                    } else {
                        showAlert( jsonResponse.optString("message", "Failed to cancel booking"), getStage());
                    }
                });

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Failed to cancel booking: " + ex.getMessage(), getStage()));
            }
        }).start();
    }

    @FXML
    private void onExploreClicked() {
        PadelCenter selectedCenter = centerSearchField.getSelectionModel().getSelectedItem();
        if (selectedCenter == null) {
            loadPadelPlaces(allPadelCenters); // Show all centers if no filter
        } else {
            JSONArray filteredCenters = new JSONArray();
            for (int i = 0; i < allPadelCenters.length(); i++) {
                JSONObject center = allPadelCenters.getJSONObject(i);
                if (center.getString("name").equals(selectedCenter.getName())) {
                    filteredCenters.put(center);
                }
            }
            loadPadelPlaces(filteredCenters); // Show only the selected center
        }
    }

    private void bookPadelCourt(String courtName, String time, LocalDate date) {
        AppContext context = AppContext.getInstance();
        if (!context.isLoggedIn()) {
            showAlert("You must be logged in to book a court.", getStage());
            return;
        }
        if (date == null) {
            showAlert("Please select a date.", getStage());
            return;
        }

        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject request = new JSONObject();
                request.put("action", "book_padel_court");
                JSONObject data = new JSONObject();
                data.put("courtName", courtName);
                data.put("time", time);
                data.put("memberId", context.getMemberId());
                data.put("memberName", context.getFirstName());
                data.put("date", date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                request.put("data", data);
                out.print(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new Exception("No response from server");
                }

                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                Platform.runLater(() -> {
                    if ("success".equals(status)) {
                        showAlert("Court " + courtName + " booked for " + time + " on " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "!", getStage());
                        fetchPadelPlaces(); // Refresh UI, closing all panels
                    } else {
                        showAlert( jsonResponse.optString("message", "Failed to book court"), getStage());
                    }
                });

                socket.close();
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Failed to book court: " + ex.getMessage(), getStage()));
            }
        }).start();
    }

    /**
     * Creates an image view for a padel place card using a classpath resource.
     *
     * @param imagePath The path to the image resource (e.g., /images/padel_center.jpg).
     * @return An ImageView with the loaded image or a fallback.
     */
    private ImageView createPlaceImage(String imagePath) {
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
            System.err.println("Failed to load image: '" + imagePath + "', using placeholder. Error: " + e.getMessage());
            image = loadResourceImage(PLACEHOLDER_IMAGE_PATH);
        }

        return new ImageView(image);
    }

    /**
     * Loads an image from a classpath resource, falling back to a transparent image if loading fails.
     *
     * @param resourcePath The classpath resource path (e.g., /images/padel_center.jpg).
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
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        }
    }

    /**
     * Sets up the date picker with a custom format and restricts past dates.
     */
    private void configureDatePicker(DatePicker datePicker, LocalDate today) {
        datePicker.setValue(today);
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isBefore(today)) {
                datePicker.setValue(today);
            }
            Platform.runLater(() -> {
                if (centerSearchField.getSelectionModel().getSelectedItem() != null) {
                    onExploreClicked();
                } else {
                    loadPadelPlaces(allPadelCenters);
                }
                // Update open booking panel's date label
                if (currentOpenPanel != null) {
                    for (Node node : currentOpenPanel.getChildren()) {
                        if (node instanceof Label && ((Label) node).getText().startsWith("Date: ")) {
                            String dateText = newValue != null ? newValue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "No date selected";
                            ((Label) node).setText("Date: " + dateText);
                        }
                    }
                }
            });
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
            if (node instanceof Button button) {
                addButtonHoverEffects(button);

                var existingHandler = button.getOnAction();
                button.setOnAction(event -> {
                    setInactiveButtons();
                    button.getStyleClass().add("active");

                    if (existingHandler == null) {
                        if (button == homeButton) {
                            goToHome();
                        } else if (button == bookingButton) {
                            // nothing
                        } else if (button == subscriptionButton) {
                            goToSubscription();
                        } else if (button == gymButton) {
                            goToGym();
                        }
                    }
                    if (existingHandler != null) {
                        existingHandler.handle(event);
                    }
                });
            }
        }
    }

    /**
     * Adds hover effects to a button using scale transitions.
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
    private void setInactiveButtons() {
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
     * Navigates to the Home page with a slide transition.
     */
    @FXML
    private void goToHome() {
        navigateToPage("home.fxml", -1);
    }

    @FXML
    private void goToLogin() {
        AppContext context = AppContext.getInstance();
        if (context.isLoggedIn()) {
            context.clear();
            loginButton.setText("Login");
            loginButton.setOnMouseEntered(null);
            loginButton.setOnMouseExited(null);
            homeButton.getStyleClass().add("active");
            loginButton.setMinWidth(Region.USE_COMPUTED_SIZE);
            navigateToPage("home.fxml", -1);
        } else {
            fadeToPage("LoginPage.fxml");
        }
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
     * Navigates to a new page with a sequenced fade transition.
     * @param fxmlFile The FXML file of the target page.
     */
    private void fadeToPage(String fxmlFile) {
        try {
            // Load the new page
            Parent newPage = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene currentScene = datePicker.getScene();
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
     * Navigates to the specified page with a slide transition.
     */
    private void navigateToPage(String fxmlFile, int direction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newPage = loader.load();

            Scene currentScene = placesContainer.getScene();
            Parent currentPage = currentScene.getRoot();

            StackPane transitionPane = new StackPane(currentPage, newPage);
            newPage.translateXProperty().set(direction * currentScene.getWidth());
            currentScene.setRoot(transitionPane);

            TranslateTransition slideOut = new TranslateTransition(TRANSITION_DURATION, currentPage);
            slideOut.setToX(-direction * currentScene.getWidth());
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideIn = new TranslateTransition(TRANSITION_DURATION, newPage);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            slideIn.setOnFinished(e -> {
                transitionPane.getChildren().clear();
                currentScene.setRoot(newPage);
            });

            slideOut.play();
            slideIn.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to navigate to page: " + e.getMessage(), getStage());
        }
    }

    /**
     * Shows an alert dialog with the specified title and message.
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

    private Stage getStage() {
        return (Stage) datePicker.getScene().getWindow();
    }
}