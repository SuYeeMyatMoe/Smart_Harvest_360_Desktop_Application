package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.Resource;
import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.ml.GradePredictor;
import SmartHarvest360.ml.WekaAdvisorService;
import SmartHarvest360.model.SimDayLog;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.plan.DetailedPlanReportBuilder;
import SmartHarvest360.session.AppSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Locale;
import java.util.Random;

/** AnyLogic-style field simulation with table log, speed control, and grade coach. */
public class SimulationController {

    private static final double FIELD_W = 266;
    private static final double FIELD_H = 230;
    private static final double SOIL_H = 42;
    private static final double PLANT_AREA_H = FIELD_H - SOIL_H;
    private static final double MAX_STEM = 120;
    private static final double MAX_CANOPY = 24;
    private static final double PLANT_BOX_W = 70;

    @FXML private Label cropLabel;
    @FXML private Label dayLabel;
    @FXML private Label weatherLabel;
    @FXML private Label waterLabel;
    @FXML private Label fertilizerLabel;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private Label liveGradeLabel;
    @FXML private Label careScoreLabel;
    @FXML private Label predictedGradeSimLabel;
    @FXML private Label fertilizerPlanSimLabel;
    @FXML private Label recommendedActionLabel;
    @FXML private Label dailyTipLabel;
    @FXML private Label speedLabel;
    @FXML private Label fieldWeatherBadge;
    @FXML private Label lastActionLabel;
    @FXML private ProgressBar growthProgress;
    @FXML private Slider speedSlider;
    @FXML private TableView<SimDayLog> activityTable;
    @FXML private TableColumn<SimDayLog, Number> dayCol;
    @FXML private TableColumn<SimDayLog, String> weatherCol;
    @FXML private TableColumn<SimDayLog, String> actionCol;
    @FXML private TableColumn<SimDayLog, Number> waterCol;
    @FXML private TableColumn<SimDayLog, Number> fertCol;
    @FXML private TableColumn<SimDayLog, Number> growthCol;
    @FXML private TableColumn<SimDayLog, String> statusCol;
    @FXML private StackPane fieldPane;
    @FXML private Pane plantLayer;
    @FXML private Rectangle skyRect;
    @FXML private Rectangle soilRect;
    @FXML private Pane plantBox;
    @FXML private Rectangle stemRect;
    @FXML private Circle canopyCircle;
    @FXML private Ellipse leafLeftLower;
    @FXML private Ellipse leafRightLower;
    @FXML private Ellipse leafLeftUpper;
    @FXML private Ellipse leafRightUpper;
    @FXML private Button irrigateButton;
    @FXML private Button conserveButton;
    @FXML private Button fertilizeButton;
    @FXML private Button protectButton;
    @FXML private Button playPauseButton;
    @FXML private Button nextDayButton;
    @FXML private Button applyTipButton;
    @FXML private Button harvestButton;

    private final Random random = new Random();
    private final ObservableList<SimDayLog> tableRows = FXCollections.observableArrayList();
    private AppSession session;
    private Timeline autoPlay;
    private boolean playing;
    private String lastWeather = "Sunny";
    private String recommendedAction = "Irrigate";
    private String selectedAction = "Irrigate";
    private Rectangle fieldClip;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        session.ensureDemoData();
        if (session.getFarmProfile() == null) {
            session.setFarmProfile(new FarmProfile("Selangor", "Loam"));
        }
        if (session.getAdvisorResult() == null) {
            session.setAdvisorResult(WekaAdvisorService.getInstance().advise(
                    session.getFarmProfile(),
                    session.getFarm().getResource(),
                    session.getActiveCrop()));
        }

        Crop crop = session.getActiveCrop();
        cropLabel.setText(crop.getName());
        configureField();
        configureTable();
        configureSpeed();

        tableRows.setAll(session.getDayLogs());
        if (tableRows.isEmpty()) {
            int growthPct = growthPercent();
            SimDayLog start = new SimDayLog(
                    session.getCurrentDay(), "Sunny", "Setup",
                    0, 0, "Season started", growthPct
            );
            session.addDayLog(start);
            tableRows.add(start);
        }

        updateScreen(lastWeather, "Growing");
        animatePlantTo(growthPctFraction());
        highlightActionCards(recommendedAction);
    }

    @FXML
    private void handleIrrigate() {
        runActionNow("Irrigate");
    }

    @FXML
    private void handleConserve() {
        runActionNow("Conserve");
    }

    @FXML
    private void handleFertilize() {
        runActionNow("Fertilize");
    }

    @FXML
    private void handleProtect() {
        runActionNow("Protect");
    }

    @FXML
    private void handleApplyTip() {
        runActionNow(recommendedAction);
    }

    @FXML
    private void handlePlayPause() {
        if (session.isCropReady()) {
            stopAutoPlay();
            return;
        }
        if (playing) {
            stopAutoPlay();
        } else {
            startAutoPlay();
        }
    }

    @FXML
    private void handleNextDay() {
        runActionNow(recommendedAction);
    }

    @FXML
    private void handleGoToHarvest() {
        stopAutoPlay();
        if (session.isCropReady()) {
            session.setDetailedPlanReport(DetailedPlanReportBuilder.fromSession(session));
            SceneNavigator.switchTo(harvestButton, "/fxml/PlanReportScreen.fxml");
        }
    }

    private void runActionNow(String action) {
        if (session.isCropReady()) {
            updateReadyState();
            return;
        }
        selectedAction = action;
        highlightActionCards(action);
        lastActionLabel.setText("Last action: " + action);
        advanceOneDay(true);
    }

    private void configureField() {
        skyRect.setManaged(false);
        soilRect.setManaged(false);
        skyRect.setWidth(FIELD_W);
        skyRect.setHeight(FIELD_H);
        skyRect.setArcWidth(18);
        skyRect.setArcHeight(18);
        skyRect.setLayoutX(0);
        skyRect.setLayoutY(0);

        soilRect.setWidth(FIELD_W);
        soilRect.setHeight(SOIL_H);
        // managed=false skips StackPane alignment — pin soil to the bottom explicitly.
        soilRect.setLayoutX(0);
        soilRect.setLayoutY(FIELD_H - SOIL_H);

        fieldClip = new Rectangle(FIELD_W, FIELD_H);
        fieldClip.setArcWidth(18);
        fieldClip.setArcHeight(18);
        fieldPane.setClip(fieldClip);

        Rectangle plantClip = new Rectangle(FIELD_W, PLANT_AREA_H);
        plantLayer.setClip(plantClip);
        plantLayer.setPrefSize(FIELD_W, PLANT_AREA_H);
        plantLayer.setMaxSize(FIELD_W, PLANT_AREA_H);
        StackPane.setAlignment(plantLayer, javafx.geometry.Pos.TOP_CENTER);
    }

    private void configureTable() {
        activityTable.setItems(tableRows);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        dayCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDay()));
        weatherCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWeather()));
        actionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        waterCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getWaterUsed()));
        fertCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getFertilizerUsed()));
        growthCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getGrowthPercent()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        waterCol.setCellFactory(col -> numericCell("%.1f"));
        fertCol.setCellFactory(col -> numericCell("%.2f"));
        growthCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.intValue() + "%");
            }
        });
    }

    private static TableCell<SimDayLog, Number> numericCell(String pattern) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(String.format(Locale.US, pattern, value.doubleValue()));
            }
        };
    }

    private void configureSpeed() {
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            speedLabel.setText(String.format(Locale.US, "%.1f×", newV.doubleValue()));
            if (playing) {
                startAutoPlay();
            }
        });
        speedLabel.setText(String.format(Locale.US, "%.1f×", speedSlider.getValue()));
    }

    private void startAutoPlay() {
        stopAutoPlay();
        playing = true;
        playPauseButton.setText("Pause");
        double speed = Math.max(0.5, speedSlider.getValue());
        double millis = Math.max(120, 900 / speed);
        autoPlay = new Timeline(new KeyFrame(Duration.millis(millis), e -> {
            // Auto-run follows the coach so Play is useful, not random toggles.
            selectedAction = recommendedAction;
            highlightActionCards(recommendedAction);
            advanceOneDay(true);
        }));
        autoPlay.setCycleCount(Timeline.INDEFINITE);
        autoPlay.play();
        statusLabel.setText("Auto-play following coach @ "
                + String.format(Locale.US, "%.1f×", speed));
    }

    private void stopAutoPlay() {
        playing = false;
        playPauseButton.setText("Play");
        if (autoPlay != null) {
            autoPlay.stop();
            autoPlay = null;
        }
    }

    private void advanceOneDay(boolean animate) {
        if (session.isCropReady()) {
            stopAutoPlay();
            updateReadyState();
            return;
        }

        Crop crop = session.getActiveCrop();
        Resource resource = session.getFarm().getResource();
        String weather = getRandomWeather();
        String action = selectedAction == null ? "Irrigate" : selectedAction;

        double baseWater = crop.getWaterNeed() / Math.max(1, crop.getGrowthDays());
        double weatherFactor = switch (weather) {
            case "Rain" -> 0.45;
            case "Cloudy" -> 0.75;
            case "Storm" -> 0.55;
            case "Heat" -> 1.35;
            default -> 1.05;
        };

        double waterUsed = baseWater * weatherFactor;
        double fertUsed = 0.0;
        double growthGain = 1.0;
        String status;

        switch (action) {
            case "Conserve" -> {
                waterUsed *= 0.55;
                growthGain = weather.equals("Rain") ? 1.0 : 0.75;
            }
            case "Fertilize" -> {
                fertUsed = Math.max(0.4, crop.getFertilizerNeed() / Math.max(8.0, crop.getGrowthDays() / 5.0));
                growthGain = 1.25;
            }
            case "Protect" -> {
                waterUsed *= 0.7;
                fertUsed = 0.15;
                growthGain = (weather.equals("Storm") || weather.equals("Heat")) ? 1.05 : 0.85;
            }
            default -> {
                waterUsed *= 1.15;
                growthGain = weather.equals("Sunny") || weather.equals("Heat") ? 1.2 : 1.0;
            }
        }

        session.advanceDay();

        boolean hasWater = resource.isAvailable("water", waterUsed);
        boolean hasFert = fertUsed <= 0 || resource.isAvailable("fertilizer", fertUsed);
        boolean grew = false;

        if (!hasWater || !hasFert) {
            status = !hasWater ? "Paused - low water" : "Paused - low fertilizer";
            waterUsed = 0;
            fertUsed = 0;
            session.adjustCareScore(-4);
        } else {
            resource.consume("water", waterUsed);
            if (fertUsed > 0) {
                resource.consume("fertilizer", fertUsed);
            }
            int baseStep = Math.max(1, (int) Math.ceil(crop.getGrowthDays() / 24.0));
            int ticks = Math.max(1, (int) Math.round(baseStep * growthGain));
            for (int i = 0; i < ticks && !session.isCropReady(); i++) {
                session.addGrowthDay();
            }
            grew = true;
            status = session.isCropReady() ? "Ready to harvest" : "Growing";
            applyCareForAction(action, weather);
        }

        int growthPct = growthPercent();
        SimDayLog row = new SimDayLog(
                session.getCurrentDay(), weather, action,
                waterUsed, fertUsed, status, growthPct
        );
        session.addDayLog(row);
        tableRows.add(row);
        activityTable.scrollTo(tableRows.size() - 1);
        lastActionLabel.setText("Last action: " + action + " - " + weather);

        updateScreen(weather, status);
        animatePlantTo(growthPct / 100.0);

        if (session.isCropReady()) {
            stopAutoPlay();
            statusLabel.setText(crop.getName() + " is ready - continue to market.");
        } else if (!grew) {
            statusLabel.setText(status + " - try Conserve or follow the coach.");
        } else if (!playing) {
            statusLabel.setText(action + " applied - care " + session.getCareScore());
        }
    }

    private void applyCareForAction(String action, String weather) {
        boolean match = action.equalsIgnoreCase(recommendedAction);
        if (match) {
            session.adjustCareScore(6);
        } else {
            session.adjustCareScore(-1);
        }
        if (("Storm".equals(weather) || "Heat".equals(weather)) && "Protect".equals(action)) {
            session.adjustCareScore(5);
        } else if (("Storm".equals(weather) || "Heat".equals(weather)) && !"Protect".equals(action)) {
            session.adjustCareScore(-7);
        }
        if ("Fertilize".equals(action) && match) {
            session.adjustCareScore(2);
        }
    }

    private void updateScreen(String weather, String status) {
        lastWeather = weather;
        Crop crop = session.getActiveCrop();
        Resource resource = session.getFarm().getResource();
        double progress = growthPctFraction();

        dayLabel.setText(String.valueOf(session.getCurrentDay()));
        weatherLabel.setText(weather);
        fieldWeatherBadge.setText(weather);
        waterLabel.setText(String.format(Locale.US, "%.0f L", resource.getWater()));
        fertilizerLabel.setText(String.format(Locale.US, "%.1f kg", resource.getFertilizer()));
        progressLabel.setText(String.format(Locale.US, "%.0f%%", progress * 100));
        growthProgress.setProgress(progress);
        if (!playing) {
            statusLabel.setText(status);
        }
        careScoreLabel.setText(String.valueOf(session.getCareScore()));
        updateAdvisorPanel(weather, status);
        updateSky(weather);
        highlightActionCards(recommendedAction);
        updateReadyState();
    }

    private void updateAdvisorPanel(String weather, String status) {
        AdvisorResult advice = session.getAdvisorResult();
        String plantGrade = advice == null ? "C" : advice.getPredictedGrade();
        String live = GradePredictor.nudgeGrade(plantGrade, session.getCareScore());
        liveGradeLabel.setText(live);

        if (advice == null) {
            predictedGradeSimLabel.setText("Plant-time grade: -");
            fertilizerPlanSimLabel.setText("Fertilizer plan: -");
            recommendedActionLabel.setText("Irrigate");
            dailyTipLabel.setText("No advisor result - irrigate carefully.");
            recommendedAction = "Irrigate";
            return;
        }

        predictedGradeSimLabel.setText("Plant-time grade: " + plantGrade
                + " -> live path " + live);
        fertilizerPlanSimLabel.setText("Fertilizer: " + advice.getFertilizerPlan()
                + " - " + advice.getFertilizerKgTip());

        String tipWeather = status != null && status.toLowerCase(Locale.ROOT).contains("paused")
                ? "Heat"
                : weather;
        WekaAdvisorService.GradeTip tip = WekaAdvisorService.getInstance()
                .gradeImprovementTip(advice, session.getFarm().getResource(), tipWeather, session.getCareScore());
        recommendedAction = tip.action();
        recommendedActionLabel.setText(tip.action());
        dailyTipLabel.setText(tip.message());
    }

    private void updateSky(String weather) {
        Color fill = switch (weather) {
            case "Rain" -> Color.web("#b7c9d4");
            case "Cloudy" -> Color.web("#c9d5cf");
            case "Storm" -> Color.web("#8fa0ab");
            case "Heat" -> Color.web("#f0d9a8");
            default -> Color.web("#cfe6dc");
        };
        skyRect.setFill(fill);
    }

    private void animatePlantTo(double progress) {
        double clamped = Math.max(0.05, Math.min(1.0, progress));
        double stemHeight = 18 + clamped * (MAX_STEM - 18);
        double canopyRadius = 8 + clamped * (MAX_CANOPY - 8);
        double groundY = PLANT_AREA_H - 4; // plant rooted just above soil line
        double stemTop = groundY - stemHeight;
        double centerX = PLANT_BOX_W / 2.0;

        // Absolute layout: stem grows upward from soil; canopy sits on top.
        plantBox.setPrefSize(PLANT_BOX_W, PLANT_AREA_H);
        plantBox.setLayoutX((FIELD_W - PLANT_BOX_W) / 2.0);
        plantBox.setLayoutY(0);

        stemRect.setX(centerX - 4);
        stemRect.setY(stemTop);
        stemRect.setWidth(8);
        stemRect.setHeight(stemHeight);

        canopyCircle.setCenterX(centerX);
        canopyCircle.setCenterY(stemTop - canopyRadius * 0.35);
        canopyCircle.setRadius(canopyRadius);
        canopyCircle.setFill(Color.web(clamped > 0.85 ? "#1f8f58" : "#2f9e68"));

        // Leaves appear as the plant grows; stay attached to the stem.
        double leafScale = Math.max(0.25, clamped);
        boolean showUpper = clamped >= 0.35;
        boolean showLower = clamped >= 0.15;

        positionLeaf(leafLeftLower, centerX - 18 * leafScale, stemTop + stemHeight * 0.62,
                11 * leafScale, 5.5 * leafScale, -32, showLower, "#45a866");
        positionLeaf(leafRightLower, centerX + 18 * leafScale, stemTop + stemHeight * 0.55,
                11 * leafScale, 5.5 * leafScale, 32, showLower, "#45a866");
        positionLeaf(leafLeftUpper, centerX - 16 * leafScale, stemTop + stemHeight * 0.32,
                10 * leafScale, 5 * leafScale, -38, showUpper, "#2f9e68");
        positionLeaf(leafRightUpper, centerX + 16 * leafScale, stemTop + stemHeight * 0.28,
                10 * leafScale, 5 * leafScale, 38, showUpper, "#2f9e68");
    }

    private void positionLeaf(
            Ellipse leaf,
            double cx,
            double cy,
            double rx,
            double ry,
            double rotate,
            boolean visible,
            String color
    ) {
        leaf.setCenterX(cx);
        leaf.setCenterY(cy);
        leaf.setRadiusX(Math.max(2, rx));
        leaf.setRadiusY(Math.max(1.5, ry));
        leaf.setRotate(rotate);
        leaf.setFill(Color.web(color));
        leaf.setVisible(visible);
        leaf.setManaged(false);
    }

    private void highlightActionCards(String action) {
        setCardStyle(irrigateButton, "Irrigate".equalsIgnoreCase(action));
        setCardStyle(conserveButton, "Conserve".equalsIgnoreCase(action));
        setCardStyle(fertilizeButton, "Fertilize".equalsIgnoreCase(action));
        setCardStyle(protectButton, "Protect".equalsIgnoreCase(action));
    }

    private void setCardStyle(Button button, boolean recommended) {
        button.getStyleClass().removeAll("action-card", "action-card-recommended",
                "action-card-compact", "action-card-compact-recommended");
        button.getStyleClass().add(recommended
                ? "action-card-compact-recommended"
                : "action-card-compact");
    }

    private void updateReadyState() {
        boolean ready = session.isCropReady();
        harvestButton.setVisible(ready);
        harvestButton.setManaged(ready);
        nextDayButton.setDisable(ready);
        playPauseButton.setDisable(ready);
        irrigateButton.setDisable(ready);
        conserveButton.setDisable(ready);
        fertilizeButton.setDisable(ready);
        protectButton.setDisable(ready);
        applyTipButton.setDisable(ready);
        if (ready) {
            stopAutoPlay();
        }
    }

    private int growthPercent() {
        return (int) Math.round(growthPctFraction() * 100);
    }

    private double growthPctFraction() {
        Crop crop = session.getActiveCrop();
        if (crop == null || crop.getGrowthDays() <= 0) {
            return 0;
        }
        return Math.min(1.0, session.getCompletedGrowthDays() / (double) crop.getGrowthDays());
    }

    private String getRandomWeather() {
        String[] options = {"Sunny", "Rain", "Cloudy", "Storm", "Heat"};
        return options[random.nextInt(options.length)];
    }
}
