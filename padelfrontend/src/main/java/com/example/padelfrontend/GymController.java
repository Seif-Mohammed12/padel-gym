package com.example.padelfrontend;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class GymController {

    @FXML
    private VBox graphPlaceholder;



    public void initialize() {
        // Example data
        int cardio = 5;
        int strength = 3;
        int yoga = 2;
        int total = cardio + strength + yoga;

        // Create PieChart
        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(true);              // Show legend
        pieChart.setLabelsVisible(true);              // Show labels on slices
        pieChart.setLegendSide(Side.BOTTOM);           // Optional: position legend

        // Create data with percentages in the names
        PieChart.Data cardioData = new PieChart.Data("Cardio", cardio);
        PieChart.Data strengthData = new PieChart.Data("Strength", strength);
        PieChart.Data yogaData = new PieChart.Data("Yoga", yoga);

        pieChart.getData().addAll(cardioData, strengthData, yogaData);

        // Add percentage to display name and tooltip
        for (PieChart.Data data : pieChart.getData()) {
            double percent = (data.getPieValue() / total) * 100;
            String percentageLabel = String.format("%.1f%%", percent);

            // Append percentage to slice label
            data.setName(data.getName() + " " + percentageLabel);

            // Tooltip with details
            Tooltip tooltip = new Tooltip(data.getName());
            Tooltip.install(data.getNode(), tooltip);
        }

        // Clear and insert chart
        graphPlaceholder.getChildren().clear();
        graphPlaceholder.getChildren().add(pieChart);
    }

}
