package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.SimulationEngine;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard-style overview of the planted fields: each planted crop is shown as
 * its own field card, alongside real session milestones and a season-progress calendar.
 */
public class FieldsOverviewController {

    @FXML private Button themeButton;
    @FXML private Button backButton;
    @FXML private Label monthTitleLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox taskList;
    @FXML private FlowPane fieldContainer;
    @FXML private Label fieldCountBadge;
    @FXML private Label seasonCaption;

    private AppSession session;
    private String returnPath;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);

        String previous = SceneNavigator.getPreviousScreenPath();
        returnPath = (previous == null || previous.isBlank())
                ? "/fxml/WelcomeScreen.fxml" : previous;

        buildSeasonCalendar();
        buildTasks();
        buildFields();
    }

    /* ---- Upcoming tasks ---- */

    private void buildTasks() {
        taskList.getChildren().clear();
        SeasonSimulator season = session.getSeason();
        List<SimulationEngine> engines = season == null ? List.of() : season.getEngines();
        int shortageDays = season == null ? 0 : season.getWaterShortageDays();

        boolean any = false;
        for (SimulationEngine engine : engines) {
            Crop crop = engine.getCrop();
            boolean ready = engine.isReady();
            if (ready && !session.isSold(engine.getCropName())) {
                any = true;
                taskList.getChildren().add(taskCard(
                        SmartHarvest360.ui.IconFactory.cropIcon(crop.getType()),
                        "Harvesting - " + engine.getCropName() + " field",
                        "Growth is complete and ready to sell",
                        "READY NOW", "pill-ready"));
            } else if (ready) {
                any = true;
                taskList.getChildren().add(taskCard(
                        SmartHarvest360.ui.IconFactory.cropIcon(crop.getType()),
                        "Harvested - " + engine.getCropName() + " field",
                        "Growth complete and sold at market",
                        "SOLD", "pill"));
            } else if (engine.getGrowthProgress() >= 0.85) {
                any = true;
                taskList.getChildren().add(taskCard(
                        SmartHarvest360.ui.IconFactory.cropIcon(crop.getType()),
                        "Harvesting - " + engine.getCropName() + " field",
                        "Approaching full growth, sell soon",
                        "READY SOON", "pill-soon"));
            } else if (shortageDays > 0) {
                any = true;
                taskList.getChildren().add(taskCard(
                        SmartHarvest360.ui.IconFactory.warningIcon(),
                        "Attention - " + engine.getCropName() + " field",
                        "Water ran short on " + shortageDays + " day(s) this season",
                        "NEEDS ATTENTION", "pill-attention"));
            } else {
                any = true;
                taskList.getChildren().add(taskCard(
                        SmartHarvest360.ui.IconFactory.cropIcon(crop.getType()),
                        "Growing - " + engine.getCropName() + " field",
                        String.format(Locale.US, "Day %d of %d \u00b7 %d%% grown",
                                session.getCurrentDay(), crop.getGrowthDays(),
                                Math.round(engine.getGrowthProgress() * 100)),
                        "ON TRACK", "pill"));
            }
        }
        if (!any) {
            taskList.getChildren().add(taskCard(
                    SmartHarvest360.ui.IconFactory.sproutIcon(),
                    "No active tasks",
                    "Plant crops in Crop Selection to unlock field milestones.",
                    "", ""));
        }
    }

    private Node taskCard(Node icon, String title, String subtitle, String pill, String pillClass) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("task-card");

        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("task-icon");
        iconLabel.setGraphic(icon);

        VBox text = new VBox(2);
        text.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("task-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("task-subtitle");
        text.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label pillLabel = new Label(pill);
        pillLabel.getStyleClass().add("pill");
        if (!pillClass.isEmpty()) {
            pillLabel.getStyleClass().add(pillClass);
        }

        card.getChildren().addAll(iconLabel, text, pillLabel);
        return card;
    }

    /* ---- Season progress calendar ---- */

    private void buildSeasonCalendar() {
        SeasonSimulator season = session.getSeason();
        List<SimulationEngine> engines = season == null ? List.of() : season.getEngines();

        // Find the longest crop to determine total season days
        int maxDays = 120; // default
        for (SimulationEngine engine : engines) {
            int days = engine.getCrop().getGrowthDays();
            if (days > maxDays) {
                maxDays = days;
            }
        }

        int currentDay = season == null ? 0 : season.getCurrentDay();

        // Title: show season day range
        monthTitleLabel.setText("Season Progress \u2014 " + maxDays + " days");

        calendarGrid.getChildren().clear();

        // Compact grid: 10 columns for readability
        int cols = 10;
        int totalCells = maxDays;

        for (int i = 0; i < totalCells; i++) {
            int dayNum = i + 1;
            Label cell = new Label(String.valueOf(dayNum));
            cell.getStyleClass().add("calendar-day");
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMaxHeight(Double.MAX_VALUE);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);

            // Highlight current day
            if (dayNum == currentDay && currentDay > 0) {
                cell.getStyleClass().add("calendar-today");
            }
            // Mark future days as dimmer
            else if (dayNum > currentDay) {
                cell.getStyleClass().add("calendar-future");
            }
            // Mark completed days
            else if (dayNum < currentDay) {
                cell.getStyleClass().add("calendar-done");
            }

            int col = i % cols;
            int row = i / cols;
            calendarGrid.add(cell, col, row);
        }

        // Update the season caption
        if (seasonCaption != null) {
            if (currentDay > 0) {
                seasonCaption.setText(String.format(Locale.US,
                        "Day %d of %d shown for the current season.", currentDay, maxDays));
            } else {
                seasonCaption.setText("Start simulating to see season progress.");
            }
        }
    }

    /* ---- Field cards ---- */

    private void buildFields() {
        fieldContainer.getChildren().clear();
        List<Crop> crops = session.getFarm() == null ? List.of() : session.getFarm().getCrops();
        if (crops.isEmpty()) {
            fieldCountBadge.setVisible(false);
            fieldContainer.getChildren().add(emptyState());
            return;
        }
        fieldCountBadge.setText(crops.size() + (crops.size() == 1 ? " FIELD" : " FIELDS"));
        for (Crop crop : crops) {
            fieldContainer.getChildren().add(fieldCard(crop));
        }
    }

    private Node fieldCard(Crop crop) {
        SeasonSimulator season = session.getSeason();
        SimulationEngine engine = season == null ? null : season.findEngine(crop);
        double progress = engine == null ? 0.0 : engine.getGrowthProgress();

        StackPane plot = new StackPane();
        plot.setPrefWidth(280);
        plot.setPrefHeight(140);
        plot.setMaxWidth(Double.MAX_VALUE);
        plot.getStyleClass().add("field-plot");

        // Determine target color based on crop type
        Color targetColor = getTargetColorForCrop(crop.getType());
        Color mapBgColor = getMapBgColorForCrop(crop.getType());

        // Expected profit margin from the best live market price, falling back to the crop's reference price.
        double bestPrice = crop.getMarketPrice();
        Map<String, Double> livePrices = season == null ? null : season.getCurrentPrices(crop.getName());
        if (livePrices != null && !livePrices.isEmpty()) {
            for (double price : livePrices.values()) {
                if (price > bestPrice) {
                    bestPrice = price;
                }
            }
        }
        double margin = bestPrice <= 0.0 ? 0.0 : (bestPrice - crop.getCostPerKg()) / bestPrice;
        String badgeText = String.format(Locale.US, "\u2197 %.2f", margin);

        // Create the mosaic field map visualization with overlay badge
        Pane mosaicMap = drawMosaicFieldMap(targetColor, mapBgColor, badgeText);
        plot.getChildren().add(mosaicMap);

        Label name = new Label(crop.getName());
        name.getStyleClass().add("field-name");
        Label type = new Label(crop.getType() + " field");
        type.getStyleClass().add("field-type");

        String progressText = engine == null ? "\u2014" : Math.round(progress * 100) + "%";
        VBox stats = new VBox(3);
        stats.getChildren().addAll(
                statRow("Harvest day", String.valueOf(crop.getGrowthDays())),
                statRow("Land", "1.0 acre"),
                statRow("Progress", progressText)
        );

        VBox card = new VBox(6, plot, name, type, stats);
        card.getStyleClass().add("field-card");
        card.setPrefWidth(310);
        return card;
    }

    /**
     * Creates a mosaic-style field map pane with surrounding polygon plots,
     * a central highlighted field, and an overlay badge.
     *
     * @param targetColor The color for the central target field
     * @param mapBgColor  The background color for the map pane
     * @param badgeText   The text to display in the overlay badge (e.g., "↗ 0.17")
     * @return A Pane containing the mosaic field map visualization
     */
    private Pane drawMosaicFieldMap(Color targetColor, Color mapBgColor, String badgeText) {
        Pane mapPane = new Pane();
        mapPane.setPrefSize(280, 140);
        mapPane.setStyle("-fx-background-color: " + toHexString(mapBgColor) + ";");

        // Clip the map pane so surrounding polygons stay within rounded corners
        Rectangle clip = new Rectangle(280, 140);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        mapPane.setClip(clip);

        // --- 1. Surrounding Plot Network (Tiled Polygons) ---
        // Create a lighter version of the background color for surrounding plots
        Color surroundingColor = mapBgColor.deriveColor(0, 0.15, 1.0, 0.35);
        Color hoverColor = mapBgColor.deriveColor(0, 0.25, 1.0, 0.55); // Darker on hover

        // Top-Left Adjacent Field
        Polygon topLeftPlot = new Polygon(
            0.0, 0.0,
            120.0, 0.0,
            78.0, 42.0,
            0.0, 52.0
        );
        topLeftPlot.setFill(surroundingColor);
        setupPlotHover(topLeftPlot, surroundingColor, hoverColor, "Adjacent Field", "North Plot");

        // Top-Right Adjacent Field
        Polygon topRightPlot = new Polygon(126.0, 0.0, 280.0, 0.0, 280.0, 75.0, 185.0, 40.0);
        topRightPlot.setFill(surroundingColor);
        setupPlotHover(topRightPlot, surroundingColor, hoverColor, "Adjacent Field", "Northeast Plot");

        // Bottom-Left Adjacent Field
        Polygon bottomLeftPlot = new Polygon(0.0, 58.0, 40.0, 92.0, 114.0, 140.0, 0.0, 140.0);
        bottomLeftPlot.setFill(surroundingColor);
        setupPlotHover(bottomLeftPlot, surroundingColor, hoverColor, "Adjacent Field", "Southwest Plot");

        // Bottom-Right Adjacent Field
        Polygon bottomRightPlot = new Polygon(196.0, 125.0, 238.0, 80.0, 280.0, 81.0, 280.0, 140.0, 122.0, 140.0);
        bottomRightPlot.setFill(surroundingColor);
        setupPlotHover(bottomRightPlot, surroundingColor, hoverColor, "Adjacent Field", "Southeast Plot");

        // --- 2. Central Highlighted Field (Asymmetrical 6-Sided Polygon) ---
        Polygon centerField = new Polygon(
            82.0, 41.0,    // Top-Left corner (raised 5px)
            180.0, 44.0,   // Top-Right corner
            232.0, 76.0,   // Far-Right vertex
            190.0, 128.0,  // Bottom-Right corner (lowered 8px)
            118.0, 134.0,  // Bottom-Left corner
            44.0, 88.0     // Far-Left vertex
        );
        centerField.setFill(targetColor);
        setupCenterFieldHover(centerField, targetColor, "Target Crop", "Primary field");

        // --- 3. White Road Outlines ---
        // Adding uniform white strokes with rounded joins for clean channels
        Polygon[] allPlots = {topLeftPlot, topRightPlot, bottomLeftPlot, bottomRightPlot, centerField};
        for (Polygon plot : allPlots) {
            plot.setStroke(Color.WHITE);
            plot.setStrokeWidth(3.0);
            plot.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }

        mapPane.getChildren().addAll(topLeftPlot, topRightPlot, bottomLeftPlot, bottomRightPlot, centerField);

        // --- 4. Overlay Badge (Top-Left Corner) ---
        if (badgeText != null && !badgeText.isEmpty()) {
            // Badge container with white rounded background
            HBox badgeContainer = new HBox(4);
            badgeContainer.setAlignment(Pos.CENTER_LEFT);
            badgeContainer.setPadding(new Insets(4, 8, 4, 6));
            badgeContainer.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(12), Insets.EMPTY)
            ));
            badgeContainer.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 4, 0, 0, 1);");

            // Arrow icon (↗) in a small green circle
            Label arrowIcon = new Label("↗");
            arrowIcon.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 10px; -fx-font-weight: bold;");

            // Metric value text
            Label metricLabel = new Label(badgeText);
            metricLabel.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 11px; -fx-font-weight: 600;");

            badgeContainer.getChildren().addAll(arrowIcon, metricLabel);

            // Position badge at top-left with margin
            badgeContainer.setLayoutX(8);
            badgeContainer.setLayoutY(8);

            mapPane.getChildren().add(badgeContainer);
        }

        return mapPane;
    }

    /**
     * Converts a JavaFX Color to a hex string representation.
     *
     * @param color The color to convert
     * @return Hex string in format #RRGGBB
     */
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private Node statRow(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("field-stat");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("field-stat");
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(keyLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node emptyState() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(660);
        box.setPrefHeight(160);
        box.getStyleClass().add("card");

        Label icon = new Label();
        icon.getStyleClass().add("empty-state-icon");
        icon.setGraphic(SmartHarvest360.ui.IconFactory.sproutIcon());
        Label title = new Label("No fields planted yet");
        title.getStyleClass().add("empty-state-title");
        Label hint = new Label("Head to Crop Selection to get started.");
        hint.getStyleClass().add("section-caption");

        box.getChildren().addAll(icon, title, hint);
        return box;
    }

    /* ---- Plot interaction helpers ---- */

    /**
     * Sets up hover effects and tooltip for surrounding plot polygons.
     *
     * @param polygon     The polygon to make interactive
     * @param normalColor The fill color when not hovered
     * @param hoverColor  The fill color when hovered
     * @param title       Tooltip title text
     * @param subtitle    Tooltip subtitle text
     */
    private void setupPlotHover(Polygon polygon, Color normalColor, Color hoverColor,
                                String title, String subtitle) {
        // Create tooltip
        Tooltip tooltip = new Tooltip(title + "\n" + subtitle);
        tooltip.setStyle("-fx-font-size: 11px; -fx-font-weight: normal;");
        Tooltip.install(polygon, tooltip);

        // Hover effects
        polygon.setOnMouseEntered(e -> {
            polygon.setFill(hoverColor);
            polygon.setStrokeWidth(4.0);
            polygon.toFront();
        });

        polygon.setOnMouseExited(e -> {
            polygon.setFill(normalColor);
            polygon.setStrokeWidth(3.0);
        });

        // Set cursor once for hand pointer
        polygon.setCursor(javafx.scene.Cursor.HAND);
    }

    /**
     * Sets up enhanced hover effects and tooltip for the central target field.
     *
     * @param polygon     The central field polygon
     * @param normalColor The fill color when not hovered
     * @param title       Tooltip title text
     * @param subtitle    Tooltip subtitle text
     */
    private void setupCenterFieldHover(Polygon polygon, Color normalColor,
                                       String title, String subtitle) {
        // Create enhanced tooltip with more details
        Tooltip tooltip = new Tooltip(title + "\n" + subtitle + "\nClick for details");
        tooltip.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        Tooltip.install(polygon, tooltip);

        // Enhanced hover effects for center field
        polygon.setOnMouseEntered(e -> {
            // Brighten the color
            Color brightened = normalColor.deriveColor(0, 0, 1.15, 1.0);
            polygon.setFill(brightened);
            polygon.setStrokeWidth(5.0);
            polygon.toFront();

            // Add subtle glow effect
            DropShadow glow = new DropShadow();
            glow.setColor(normalColor.deriveColor(0, 0.8, 1.0, 0.6));
            glow.setRadius(8);
            polygon.setEffect(glow);
        });

        polygon.setOnMouseExited(e -> {
            polygon.setFill(normalColor);
            polygon.setStrokeWidth(3.0);
            polygon.setEffect(null);
        });

        // Set cursor once for hand pointer
        polygon.setCursor(javafx.scene.Cursor.HAND);
    }

    /* ---- Crop type helpers ---- */

    /**
     * Returns the target color for the central field based on crop type.
     * Green for wheat/grains, orange for corn, red for chili, etc.
     */
    private Color getTargetColorForCrop(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (t.contains("wheat") || t.contains("grain")) {
            return Color.web("#86EFAC"); // Green for wheat/grains
        }
        if (t.contains("corn")) {
            return Color.web("#FB923C"); // Orange for corn
        }
        if (t.contains("chili") || t.contains("pepper")) {
            return Color.web("#EF4444"); // Red for chili/pepper
        }
        if (t.contains("lettuce") || t.contains("vegetable")) {
            return Color.web("#86EFAC"); // Light green for lettuce/vegetables
        }
        if (t.contains("fruit")) {
            return Color.web("#F472B6"); // Pink for fruits
        }
        return Color.web("#FB923C"); // Default orange
    }

    /**
     * Returns the map background color based on crop type.
     * Dynamic pastel versions matching the crop's color palette.
     */
    private Color getMapBgColorForCrop(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (t.contains("wheat") || t.contains("grain")) {
            return Color.web("#F0FDF4"); // Light pastel green background
        }
        if (t.contains("corn")) {
            return Color.web("#FFF7ED"); // Light peach/orange background
        }
        if (t.contains("chili") || t.contains("pepper")) {
            return Color.web("#FFF1F2"); // Light pink/red background
        }
        if (t.contains("lettuce") || t.contains("vegetable")) {
            return Color.web("#F0FDF4"); // Light pastel green background
        }
        if (t.contains("fruit")) {
            return Color.web("#FDF2F8"); // Light pink background
        }
        return Color.web("#FFF7ED"); // Default light peach
    }

    /* ---- Navigation ---- */

    @FXML
    private void handleBack() {
        SceneNavigator.goBack(backButton);
    }

    @FXML
    private void handleToggleTheme() {
        SmartHarvest360.ui.ThemeManager.toggle(themeButton);
    }
}
