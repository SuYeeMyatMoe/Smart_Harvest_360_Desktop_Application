package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Market;
import SmartHarvest360.Resource;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Premium dashboard for planted fields, tasks, and season milestones. */
public final class FieldsOverviewController {
    @FXML private Label dayValueLabel;
    @FXML private Label careValueLabel;
    @FXML private Label waterValueLabel;
    @FXML private Label budgetValueLabel;
    @FXML private Label fieldCountBadge;
    @FXML private Label seasonTitleLabel;
    @FXML private Label seasonCaptionLabel;
    @FXML private VBox taskList;
    @FXML private GridPane seasonGrid;
    @FXML private FlowPane fieldContainer;
    @FXML private Button backButton;

    private AppSession session;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        session.ensureDemoData();
        populateSummary();
        populateTasks();
        populateCalendar();
        populateFields();
    }

    private void populateSummary() {
        Resource resource = session.getFarm().getResource();
        dayValueLabel.setText("DAY " + session.getCurrentDay());
        careValueLabel.setText(session.getCareScore() + "%");
        waterValueLabel.setText(String.format(Locale.US, "%,.1f L", resource.getWater()));
        budgetValueLabel.setText(String.format(Locale.US, "RM %,.0f", resource.getBudget()));
    }

    private void populateTasks() {
        taskList.getChildren().clear();
        List<Crop> crops = session.getFarm().getCrops();
        Resource resource = session.getFarm().getResource();
        if (crops.isEmpty()) {
            taskList.getChildren().add(taskCard("🌱", "Plant your first crop",
                    "Choose a crop from the catalog to begin a season.", "NEXT"));
            return;
        }
        int shortageDays = (int) session.getDayLogs().stream()
                .filter(log -> log.getWaterUsed() == 0.0).count();
        for (Crop crop : crops) {
            double progress = progressFor(crop);
            boolean sold = session.getSales().stream()
                    .anyMatch(sale -> sale.cropName().equalsIgnoreCase(crop.getName()));
            int remaining = Math.max(0, crop.getGrowthDays() - session.getCompletedGrowthDays());
            if (sold) {
                taskList.getChildren().add(taskCard(iconFor(crop), "Sold · " + crop.getName(),
                        "Harvest completed and recorded in the market report.", "SOLD"));
            } else if (progress >= 1.0) {
                taskList.getChildren().add(taskCard(iconFor(crop), "Harvest · " + crop.getName(),
                        "This field reached full maturity and is ready to sell.", "READY"));
            } else if (progress >= 0.85) {
                taskList.getChildren().add(taskCard(iconFor(crop), "Prepare harvest · " + crop.getName(),
                        remaining + " simulated day(s) remain until maturity.", "READY SOON"));
            } else {
                taskList.getChildren().add(taskCard(iconFor(crop), "Growing · " + crop.getName(),
                        String.format(Locale.US, "Day %d of %d · %d%% grown",
                                session.getCurrentDay(), crop.getGrowthDays(), Math.round(progress * 100)),
                        "ON TRACK"));
            }
        }
        double totalNeed = crops.stream().mapToDouble(Crop::getWaterNeed).sum();
        if (shortageDays > 0 || resource.getWater() < Math.max(20.0, totalNeed * 0.20)) {
            taskList.getChildren().add(taskCard("💧", "Water reserve is low",
                    shortageDays + " shortage day(s) recorded · use Conserve and monitor rainfall.", "ATTENTION"));
        }
        String careText = session.getCareScore() >= 80
                ? "Excellent care supports a stronger final grade."
                : "Follow the grade coach to raise the care score.";
        taskList.getChildren().add(taskCard("✦", "Quality target", careText,
                session.getCareScore() >= 80 ? "EXCELLENT" : "IMPROVE"));
    }

    private Node taskCard(String icon, String title, String subtitle, String status) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("fields-task-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("fields-task-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("fields-task-subtitle");
        VBox copy = new VBox(3, titleLabel, subtitleLabel);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label pill = new Label(status);
        pill.getStyleClass().addAll("fields-pill", "fields-pill-"
                + status.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", "-"));
        HBox card = new HBox(12, iconLabel, copy, pill);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("fields-task-card");
        return card;
    }

    private void populateCalendar() {
        List<Crop> crops = session.getFarm().getCrops();
        int totalDays = crops.stream().mapToInt(Crop::getGrowthDays).max().orElse(90);
        int day = Math.max(1, session.getCurrentDay());
        seasonTitleLabel.setText("Season timeline · " + totalDays + " days");
        seasonCaptionLabel.setText("Day " + day + " highlighted · completed days glow green");
        seasonGrid.getChildren().clear();
        int columns = 10;
        for (int value = 1; value <= totalDays; value++) {
            Label cell = new Label(String.valueOf(value));
            cell.getStyleClass().add("fields-calendar-day");
            if (value < day) cell.getStyleClass().add("fields-calendar-done");
            else if (value == day) cell.getStyleClass().add("fields-calendar-today");
            else cell.getStyleClass().add("fields-calendar-future");
            seasonGrid.add(cell, (value - 1) % columns, (value - 1) / columns);
        }
    }

    private void populateFields() {
        fieldContainer.getChildren().clear();
        Farm farm = session.getFarm();
        List<Crop> crops = farm == null ? List.of() : farm.getCrops();
        fieldCountBadge.setText(crops.size() + (crops.size() == 1 ? " ACTIVE FIELD" : " ACTIVE FIELDS"));
        if (crops.isEmpty()) {
            Label empty = new Label("No fields planted yet. Return to Crop Selection to begin.");
            empty.getStyleClass().add("fields-empty-state");
            fieldContainer.getChildren().add(empty);
            return;
        }
        int index = 0;
        for (Crop crop : crops) {
            Node card = fieldCard(crop);
            fieldContainer.getChildren().add(card);
            animateCard(card, index++);
        }
    }

    private Node fieldCard(Crop crop) {
        double progress = progressFor(crop);
        Map<String, Double> prices = new Market().getMarketPrices(crop, session.getFarmLocation());
        Map.Entry<String, Double> bestMarket = prices.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        String marketName = bestMarket == null ? "Reference market" : bestMarket.getKey();
        double bestPrice = bestMarket == null ? crop.getMarketPrice() : bestMarket.getValue();
        double revenue = crop.getYieldAmount() * bestPrice * crop.calculateGrowthBonus();
        double cost = crop.getYieldAmount() * crop.getCostPerKg();
        double profit = revenue - cost;
        double margin = revenue <= 0.0 ? 0.0 : profit / revenue * 100.0;

        StackPane map = fieldMap(crop, progress, margin);

        Label name = new Label(crop.getName() + " Field");
        name.getStyleClass().add("fields-card-title");
        String state = session.getFarmLocation() == null ? "Malaysia" : session.getFarmLocation();
        Label type = new Label(crop.getType().toUpperCase(Locale.ROOT) + " · " + state.toUpperCase(Locale.ROOT));
        type.getStyleClass().add("fields-card-type");
        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("fields-progress");
        Label progressLabel = new Label(String.format(Locale.US,
                "%d%% grown · %d day(s) to harvest", Math.round(progress * 100),
                Math.max(0, crop.getGrowthDays() - session.getCompletedGrowthDays())));
        progressLabel.getStyleClass().add("fields-progress-copy");
        HBox firstStats = new HBox(8,
                metric("HARVEST DAY", String.valueOf(crop.getGrowthDays())),
                metric("LAND", "1.0 acre"),
                metric("EXPECTED YIELD", String.format(Locale.US, "%.0f kg", crop.getYieldAmount())));
        HBox secondStats = new HBox(8,
                metric("WATER NEED", String.format(Locale.US, "%.0f L", crop.getWaterNeed())),
                metric("FERTILIZER", String.format(Locale.US, "%.1f kg", crop.getFertilizerNeed())),
                metric("EXPECTED PROFIT", String.format(Locale.US, "RM %,.0f", profit)));
        Label market = new Label(String.format(Locale.US, "BEST BUYER  ·  %s  ·  RM %.2f/kg",
                marketName, bestPrice));
        market.setWrapText(true);
        market.getStyleClass().add("fields-market-strip");
        VBox card = new VBox(10, map, name, type, bar, progressLabel, firstStats, secondStats, market);
        card.getStyleClass().add("fields-card");
        card.setPrefWidth(505);
        return card;
    }

    private double progressFor(Crop crop) {
        return Math.min(1.0,
                session.getCompletedGrowthDays() / (double) Math.max(1, crop.getGrowthDays()));
    }

    private StackPane fieldMap(Crop crop, double progress, double margin) {
        StackPane map = new StackPane();
        map.getStyleClass().add("fields-map");
        Polygon northWest = polygon("fields-plot", 0, 0, 135, 0, 92, 50, 0, 64);
        Polygon northEast = polygon("fields-plot", 145, 0, 320, 0, 320, 75, 210, 48);
        Polygon southWest = polygon("fields-plot-accent", 0, 72, 62, 102, 135, 140, 0, 140);
        Polygon southEast = polygon("fields-plot", 212, 125, 260, 82, 320, 82, 320, 140, 140, 140);
        Polygon center = polygon("fields-center-plot", 96, 48, 205, 50, 257, 80,
                208, 126, 135, 134, 55, 91);
        for (Polygon plot : List.of(northWest, northEast, southWest, southEast)) {
            Tooltip.install(plot, new Tooltip("Adjacent field plot\nHover to inspect the farm map"));
            plot.setCursor(Cursor.HAND);
            plot.setOnMouseEntered(event -> plot.setOpacity(0.72));
            plot.setOnMouseExited(event -> plot.setOpacity(1.0));
        }
        Tooltip.install(center, new Tooltip(crop.getName() + " target field\n"
                + Math.round(progress * 100) + "% growth · " + String.format(Locale.US, "%.1f%% margin", margin)));
        center.setCursor(Cursor.HAND);
        center.setOnMouseEntered(event -> {
            center.setScaleX(1.035);
            center.setScaleY(1.035);
            center.setEffect(new DropShadow(12, Color.web("#187557")));
        });
        center.setOnMouseExited(event -> {
            center.setScaleX(1.0);
            center.setScaleY(1.0);
            center.setEffect(null);
        });
        javafx.scene.layout.Pane plots = new javafx.scene.layout.Pane(
                northWest, northEast, southWest, southEast, center);
        plots.setPrefSize(320, 140);

        Label cropIcon = new Label(iconFor(crop));
        cropIcon.getStyleClass().add("fields-crop-icon");
        Label progressBadge = new Label(Math.round(progress * 100) + "% GROWN");
        progressBadge.getStyleClass().add("fields-map-badge");
        StackPane.setAlignment(progressBadge, Pos.TOP_RIGHT);
        Label marginBadge = new Label(String.format(Locale.US, "↗ %.1f%% MARGIN", margin));
        marginBadge.getStyleClass().addAll("fields-map-badge", "fields-margin-badge");
        StackPane.setAlignment(marginBadge, Pos.TOP_LEFT);
        map.getChildren().addAll(plots, cropIcon, progressBadge, marginBadge);
        return map;
    }

    private Polygon polygon(String styleClass, double... points) {
        Polygon polygon = new Polygon(points);
        polygon.getStyleClass().add(styleClass);
        return polygon;
    }

    private Node metric(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("fields-mini-key");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("fields-mini-value");
        VBox box = new VBox(2, keyLabel, valueLabel);
        box.getStyleClass().add("fields-mini-metric");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String iconFor(Crop crop) {
        String name = crop.getName().toLowerCase(Locale.ROOT);
        if (name.contains("paddy") || name.contains("rice")) return "🌾";
        if (name.contains("durian")) return "🌳";
        if (name.contains("papaya")) return "🌴";
        if (name.contains("corn")) return "🌽";
        if (name.contains("chili")) return "🌶";
        if (name.contains("tomato")) return "🍅";
        return crop.getType().equalsIgnoreCase("Fruit") ? "🍃" : "🌱";
    }

    private void animateCard(Node card, int index) {
        card.setOpacity(0.0);
        card.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(360), card);
        fade.setDelay(Duration.millis(index * 80L));
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(360), card);
        rise.setDelay(Duration.millis(index * 80L));
        rise.setToY(0.0);
        fade.play();
        rise.play();
    }

    @FXML
    private void handleBack() {
        SceneNavigator.switchTo(backButton, "/fxml/SimulationScreen.fxml");
    }
}
