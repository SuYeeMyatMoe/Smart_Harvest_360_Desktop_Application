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
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javafx.util.Duration;

import java.net.URL;
import java.util.Locale;
import java.util.Random;


/**
 * SmartHarvest 360 Crop Simulation Controller.
 *
 * SIMULATION RULE:
 *
 * 1 simulation day = 1 growth day.
 *
 * Therefore:
 *
 * Corn 90 days -> 90 simulation days
 * Paddy X days -> X simulation days
 * Tomato X days -> X simulation days
 * Durian X days -> X simulation days
 *
 * IMPORTANT:
 *
 * The crop video is ONLY a visual representation.
 * The video NEVER determines crop readiness.
 *
 * Video synchronization:
 *
 * 0% simulation -> 0% video
 * 50% simulation -> 47.5% video
 * 100% simulation -> 95% video
 *
 * The final 5% of the video is intentionally unused.
 *
 * AUDIO:
 *
 * Crop videos are completely muted.
 *
 * SPEED:
 *
 * The speed slider controls BOTH:
 *
 * 1. Simulation day frequency
 * 2. Video playback rate
 *
 * Therefore:
 *
 * 1x -> normal
 * 2x -> twice as fast
 * 5x -> five times as fast
 * 10x -> ten times as fast
 */
public class SimulationController {

    private static final double FIELD_W = 266;
    private static final double FIELD_H = 230;
    private static final double SOIL_H = 42;

    /**
     * Crop video intentionally stops at 95%.
     */
    private static final double VIDEO_END_FRACTION = 0.95;

    /**
     * Small amount of extra time used to make the final
     * video position settle cleanly.
     */
    private static final double VIDEO_EXTRA_DELAY_MS = 30.0;

    /**
     * Minimum autoplay interval.
     *
     * IMPORTANT:
     *
     * This used to be 700 ms, which effectively made
     * high-speed simulation still feel slow.
     *
     * A much smaller value allows 5x / 10x simulation
     * to actually behave like 5x / 10x.
     */
    private static final double MIN_AUTOPLAY_MS = 80.0;

    /**
     * Maximum practical speed available to the simulation.
     */
    private static final double MAX_SIMULATION_SPEED = 10.0;


    // =========================================================
    // FXML
    // =========================================================

    @FXML
    private Label cropLabel;

    @FXML
    private Label dayLabel;

    @FXML
    private Label weatherLabel;

    @FXML
    private Label waterLabel;

    @FXML
    private Label fertilizerLabel;

    @FXML
    private Label progressLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label liveGradeLabel;

    @FXML
    private Label careScoreLabel;

    @FXML
    private Label predictedGradeSimLabel;

    @FXML
    private Label fertilizerPlanSimLabel;

    @FXML
    private Label recommendedActionLabel;

    @FXML
    private Label dailyTipLabel;

    @FXML
    private Label speedLabel;

    @FXML
    private Label fieldWeatherBadge;

    @FXML
    private Label lastActionLabel;

    @FXML
    private ProgressBar growthProgress;

    @FXML
    private Slider speedSlider;

    @FXML
    private TableView<SimDayLog> activityTable;

    @FXML
    private TableColumn<SimDayLog, Number> dayCol;

    @FXML
    private TableColumn<SimDayLog, String> weatherCol;

    @FXML
    private TableColumn<SimDayLog, String> actionCol;

    @FXML
    private TableColumn<SimDayLog, Number> waterCol;

    @FXML
    private TableColumn<SimDayLog, Number> fertCol;

    @FXML
    private TableColumn<SimDayLog, Number> growthCol;

    @FXML
    private TableColumn<SimDayLog, String> statusCol;

    @FXML
    private StackPane fieldPane;

    @FXML
    private Pane plantLayer;

    @FXML
    private Rectangle skyRect;

    @FXML
    private Rectangle soilRect;

    @FXML
    private MediaView plantVideoView;

    @FXML
    private Button irrigateButton;

    @FXML
    private Button conserveButton;

    @FXML
    private Button fertilizeButton;

    @FXML
    private Button protectButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextDayButton;

    @FXML
    private Button applyTipButton;

    @FXML
    private Button harvestButton;

    @FXML
    private Button fieldsOverviewButton;


    // =========================================================
    // STATE
    // =========================================================

    private final Random random = new Random();

    private final ObservableList<SimDayLog> tableRows =
            FXCollections.observableArrayList();

    private AppSession session;

    private Timeline autoPlay;

    private boolean playing = false;

    private String lastWeather = "Sunny";

    private String recommendedAction = "Irrigate";

    private String selectedAction = "Irrigate";

    private Rectangle fieldClip;

    /**
     * Crop growth video player.
     */
    private MediaPlayer plantMediaPlayer;

    /**
     * Name of currently loaded crop video.
     */
    private String loadedVideoCrop;

    /**
     * Transition used to stop video at exact simulation position.
     */
    private PauseTransition videoStopTransition;


    // =========================================================
    // INITIALIZATION
    // =========================================================

    @FXML
    public void initialize() {

        session = AppSession.getInstance();

        session.ensureDemoData();

        if (session.getFarmProfile() == null) {

            session.setFarmProfile(
                    new FarmProfile(
                            "Selangor",
                            "Loam"
                    )
            );
        }

        if (session.getAdvisorResult() == null) {

            session.setAdvisorResult(
                    WekaAdvisorService.getInstance().advise(
                            session.getFarmProfile(),
                            session.getFarm().getResource(),
                            session.getActiveCrop()
                    )
            );
        }

        Crop crop = session.getActiveCrop();

        if (crop == null) {

            statusLabel.setText(
                    "No crop selected."
            );

            return;
        }

        cropLabel.setText(
                crop.getName()
        );

        configureField();

        configureVideo();

        configureTable();

        configureSpeed();

        tableRows.setAll(
                session.getDayLogs()
        );

        if (tableRows.isEmpty()) {

            int growthPct = growthPercent();

            SimDayLog start =
                    new SimDayLog(
                            session.getCurrentDay(),
                            "Sunny",
                            "Setup",
                            0,
                            0,
                            "Season started",
                            growthPct
                    );

            session.addDayLog(start);

            tableRows.add(start);
        }

        updateScreen(
                lastWeather,
                "Growing"
        );

        synchronizeVideoWithSimulation();

        highlightActionCards(
                recommendedAction
        );
    }


    // =========================================================
    // ACTION BUTTONS
    // =========================================================

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

        runActionNow(
                recommendedAction
        );
    }


    @FXML
    private void handleNextDay() {

        runActionNow(
                recommendedAction
        );
    }


    @FXML
    private void handlePlayPause() {

        if (session.isCropReady()) {

            stopAutoPlay();

            updateReadyState();

            return;
        }

        if (playing) {

            stopAutoPlay();

        } else {

            startAutoPlay();
        }
    }


    @FXML
    private void handleFieldsOverview(ActionEvent event) {
        stopAutoPlay();
        Node source = event.getSource() instanceof Node node ? node : fieldsOverviewButton;
        SceneNavigator.switchTo(source, "/fxml/FieldsOverviewScreen.fxml");
    }

    @FXML
    private void handleGoToHarvest() {

        stopAutoPlay();

        stopPlantVideo();

        if (session.isCropReady()) {

            session.setDetailedPlanReport(
                    DetailedPlanReportBuilder.fromSession(
                            session
                    )
            );

            SceneNavigator.switchTo(
                    harvestButton,
                    "/fxml/PlanReportScreen.fxml"
            );
        }
    }


    /**
     * Manual action / Coach Step.
     *
     * Every call advances EXACTLY ONE simulation day.
     *
     * The speed slider does NOT change this rule.
     */
    private void runActionNow(
            String action
    ) {

        if (session.isCropReady()) {

            updateReadyState();

            return;
        }

        selectedAction =
                action;

        highlightActionCards(
                action
        );

        lastActionLabel.setText(
                "Last action: " + action
        );

        stopVideoTransitionOnly();

        advanceOneDay(true);
    }


    // =========================================================
    // FIELD
    // =========================================================

    private void configureField() {

        skyRect.setManaged(false);

        soilRect.setManaged(false);

        skyRect.setWidth(
                FIELD_W
        );

        skyRect.setHeight(
                FIELD_H
        );

        skyRect.setArcWidth(18);

        skyRect.setArcHeight(18);

        skyRect.setLayoutX(0);

        skyRect.setLayoutY(0);

        soilRect.setWidth(
                FIELD_W
        );

        soilRect.setHeight(
                SOIL_H
        );

        soilRect.setLayoutX(0);

        soilRect.setLayoutY(
                FIELD_H - SOIL_H
        );

        fieldClip =
                new Rectangle(
                        FIELD_W,
                        FIELD_H
                );

        fieldClip.setArcWidth(18);

        fieldClip.setArcHeight(18);

        fieldPane.setClip(
                fieldClip
        );

        if (plantLayer != null) {

            plantLayer.setVisible(false);

            plantLayer.setManaged(false);
        }
    }


    // =========================================================
    // VIDEO
    // =========================================================

    private void configureVideo() {

        plantVideoView.setFitWidth(
                FIELD_W
        );

        plantVideoView.setFitHeight(
                FIELD_H - SOIL_H
        );

        /*
         * Keep the existing field dimensions.
         *
         * The video fills the crop area.
         */
        plantVideoView.setPreserveRatio(
                false
        );

        plantVideoView.setSmooth(
                true
        );

        plantVideoView.setManaged(
                false
        );

        plantVideoView.setLayoutX(0);

        plantVideoView.setLayoutY(0);

        plantVideoView.setVisible(
                false
        );

        loadCropVideo(
                session.getActiveCrop()
        );
    }


    /**
     * Loads the correct video for the active crop.
     *
     * Supported crops:
     *
     * tomato
     * paddy / rice
     * corn / maize
     * durian
     * chilli / chili
     * papaya
     */
    private void loadCropVideo(
            Crop crop
    ) {

        if (crop == null) {

            return;
        }

        String cropName =
                crop.getName()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * Do not recreate the media player when
         * the same crop is already loaded.
         */
        if (
                cropName.equals(
                        loadedVideoCrop
                )
                        &&
                plantMediaPlayer != null
        ) {

            /*
             * Make absolutely sure the existing player
             * remains muted.
             */
            muteVideo();

            return;
        }

        stopPlantVideo();

        String videoFile =
                getVideoFileName(
                        cropName
                );

        if (videoFile == null) {

            statusLabel.setText(
                    "No growth video for "
                            + crop.getName()
            );

            return;
        }

        URL videoUrl =
                getClass().getResource(
                        "/videos/" + videoFile
                );

        if (videoUrl == null) {

            statusLabel.setText(
                    "Video not found: "
                            + videoFile
            );

            System.err.println(
                    "SmartHarvest 360: Could not find video: "
                            + "/videos/"
                            + videoFile
            );

            return;
        }

        try {

            System.out.println(
                    "Loading crop video: "
                            + cropName
                            + " -> "
                            + videoFile
            );

            Media media =
                    new Media(
                            videoUrl.toExternalForm()
                    );

            plantMediaPlayer =
                    new MediaPlayer(
                            media
                    );

            /*
             * Video plays only once.
             */
            plantMediaPlayer.setCycleCount(1);

            /*
             * The simulation controls when the video moves.
             */
            plantMediaPlayer.setAutoPlay(false);

            /*
             * =================================================
             * AUDIO OFF
             * =================================================
             *
             * Completely mute crop videos.
             */
            muteVideo();

            plantVideoView.setMediaPlayer(
                    plantMediaPlayer
            );

            plantVideoView.setVisible(
                    true
            );

            loadedVideoCrop =
                    cropName;

            plantMediaPlayer.setOnReady(
                    () -> {

                        System.out.println(
                                "Video READY: "
                                        + cropName
                        );

                        /*
                         * Keep audio permanently disabled.
                         */
                        muteVideo();

                        /*
                         * Make the video speed match the
                         * currently selected simulation speed.
                         *
                         * When not autoplaying, we still keep
                         * the normal rate.
                         */
                        if (playing) {

                            plantMediaPlayer.setRate(
                                    getSimulationSpeed()
                            );

                        } else {

                            plantMediaPlayer.setRate(
                                    1.0
                            );
                        }

                        Duration duration =
                                plantMediaPlayer
                                        .getTotalDuration();

                        if (duration != null) {

                            System.out.println(
                                    "Video duration: "
                                            + duration.toMillis()
                                            + " ms"
                            );
                        }

                        /*
                         * Position immediately at the current
                         * simulation growth.
                         */
                        positionVideoAtGrowth(
                                growthPctFraction()
                                        * VIDEO_END_FRACTION
                        );
                    }
            );

            plantMediaPlayer.setOnEndOfMedia(
                    () -> {

                        /*
                         * Video reaching its end is NOT the
                         * same thing as crop readiness.
                         */
                        if (plantMediaPlayer != null) {

                            plantMediaPlayer.pause();

                            muteVideo();

                            Duration total =
                                    plantMediaPlayer
                                            .getTotalDuration();

                            if (
                                    total != null
                            ) {

                                plantMediaPlayer.seek(
                                        total.multiply(
                                                VIDEO_END_FRACTION
                                        )
                                );
                            }
                        }
                    }
            );

            plantMediaPlayer.setOnError(
                    () -> {

                        if (
                                plantMediaPlayer != null
                        ) {

                            System.err.println(
                                    "SmartHarvest 360 video error: "
                                            + plantMediaPlayer.getError()
                            );
                        }

                        System.err.println(
                                "Crop video could not be played."
                        );
                    }
            );

        } catch (Exception ex) {

            System.err.println(
                    "Could not load crop video: "
                            + ex.getMessage()
            );

            ex.printStackTrace();

            statusLabel.setText(
                    "Unable to load crop video"
            );
        }
    }


    /**
     * Completely disables video audio.
     */
    private void muteVideo() {

        if (plantMediaPlayer == null) {

            return;
        }

        try {

            plantMediaPlayer.setMute(true);

            plantMediaPlayer.setVolume(0.0);

        } catch (Exception ignored) {

            // Video audio is not required by the simulation.
        }
    }


    /**
     * Maps crop names to files inside:
     *
     * src/main/resources/videos/
     */
    private String getVideoFileName(
            String cropName
    ) {

        return switch (cropName) {

            case "chilli",
                 "chili" ->
                    "chilli.mp4";

            case "paddy",
                 "rice" ->
                    "paddy.mp4";

            case "corn",
                 "maize" ->
                    "corn.mp4";

            case "durian" ->
                    "durian.mp4";

            case "tomato" ->
                    "tomato.mp4";

            case "papaya" ->
                    "papaya.mp4";

            default ->
                    null;
        };
    }


    /**
     * Synchronizes video with simulation growth.
     *
     * Example for 90-day corn:
     *
     * Day 0  -> 0%
     * Day 1  -> 1.11%
     * Day 45 -> 50%
     * Day 90 -> 100%
     *
     * Video uses only 95% of its duration.
     */
    private void synchronizeVideoWithSimulation() {

        if (plantMediaPlayer == null) {

            return;
        }

        MediaPlayer.Status status =
                plantMediaPlayer.getStatus();

        if (
                status == MediaPlayer.Status.UNKNOWN
                        ||
                status == MediaPlayer.Status.DISPOSED
                        ||
                status == MediaPlayer.Status.HALTED
        ) {

            return;
        }

        Duration totalDuration =
                plantMediaPlayer.getTotalDuration();

        if (
                totalDuration == null
                        ||
                totalDuration.isUnknown()
                        ||
                totalDuration.isIndefinite()
                        ||
                totalDuration.lessThanOrEqualTo(
                                Duration.ZERO
                        )
        ) {

            return;
        }

        double simulationProgress =
                growthPctFraction();

        double videoProgress =
                Math.min(
                        VIDEO_END_FRACTION,
                        simulationProgress
                                * VIDEO_END_FRACTION
                );

        /*
         * When autoplay is active, video rate must match
         * simulation rate.
         */
        if (playing) {

            plantMediaPlayer.setRate(
                    getSimulationSpeed()
            );

            muteVideo();

        } else {

            plantMediaPlayer.setRate(
                    1.0
            );

            muteVideo();
        }

        animateVideoToGrowth(
                videoProgress
        );
    }


    /**
     * Moves video from its current position to the
     * current simulation position.
     *
     * IMPORTANT:
     *
     * Video playback rate follows the simulation speed.
     *
     * At:
     *
     * 1x -> normal
     * 2x -> 2x video
     * 5x -> 5x video
     * 10x -> 10x video
     */
    private void animateVideoToGrowth(
            double targetProgress
    ) {

        if (plantMediaPlayer == null) {

            return;
        }

        Duration totalDuration =
                plantMediaPlayer.getTotalDuration();

        if (
                totalDuration == null
                        ||
                totalDuration.isUnknown()
                        ||
                totalDuration.isIndefinite()
                        ||
                totalDuration.lessThanOrEqualTo(
                                Duration.ZERO
                        )
        ) {

            return;
        }

        double clampedTarget =
                Math.max(
                        0.0,
                        Math.min(
                                VIDEO_END_FRACTION,
                                targetProgress
                        )
                );

        Duration targetTime =
                totalDuration.multiply(
                        clampedTarget
                );

        Duration currentTime =
                plantMediaPlayer.getCurrentTime();

        if (
                currentTime == null
                        ||
                        currentTime.isUnknown()
                        ||
                        currentTime.isIndefinite()
        ) {

            currentTime =
                    Duration.ZERO;
        }

        stopVideoTransitionOnly();

        double currentMillis =
                currentTime.toMillis();

        double targetMillis =
                targetTime.toMillis();

        /*
         * If video has somehow moved ahead, immediately
         * correct it.
         */
        if (
                currentMillis
                        >
                        targetMillis + 25
        ) {

            plantMediaPlayer.pause();

            plantMediaPlayer.seek(
                    targetTime
            );

            muteVideo();

            return;
        }

        double distanceMillis =
                targetMillis
                        -
                        currentMillis;

        /*
         * Nothing meaningful to animate.
         */
        if (distanceMillis <= 15) {

            plantMediaPlayer.pause();

            plantMediaPlayer.seek(
                    targetTime
            );

            muteVideo();

            return;
        }

        /*
         * =====================================================
         * VIDEO SPEED
         * =====================================================
         *
         * Use exactly the same speed as the simulation.
         */
        double speed =
                playing
                        ? getSimulationSpeed()
                        : 1.0;

        plantMediaPlayer.setRate(
                speed
        );

        muteVideo();

        /*
         * Start from the player's current position.
         *
         * IMPORTANT:
         *
         * Do NOT seek to currentTime immediately before play().
         * MediaPlayer.seek() is asynchronous, so doing:
         *
         *     seek(currentTime);
         *     play();
         *
         * can race with the seek operation and prevent the
         * animation from starting reliably (especially when
         * advancing the simulation one day at a time).
         *
         * The player is already at currentTime, so simply play
         * from there.
         */
        plantMediaPlayer.play();

        /*
         * Because MediaPlayer is running at the selected
         * playback rate, the real-world time needed to cover
         * this portion of the video is:
         *
         * distance / playbackRate
         *
         * This is what makes 2x actually twice as fast.
         */
        double stopAfterMillis =
                Math.max(
                        25.0,
                        (
                                distanceMillis
                                        /
                                        speed
                        )
                                + VIDEO_EXTRA_DELAY_MS
                );

        videoStopTransition =
                new PauseTransition(
                        Duration.millis(
                                stopAfterMillis
                        )
                );

        videoStopTransition.setOnFinished(
                event -> {

                    if (plantMediaPlayer == null) {

                        return;
                    }

                    plantMediaPlayer.pause();

                    /*
                     * Always return to the exact simulation
                     * position after the visual movement.
                     */
                    plantMediaPlayer.seek(
                            targetTime
                    );

                    muteVideo();

                    videoStopTransition = null;
                }
        );

        videoStopTransition.play();
    }


    /**
     * Positions video without animation.
     */
    private void positionVideoAtGrowth(
            double progress
    ) {

        if (plantMediaPlayer == null) {

            return;
        }

        Duration totalDuration =
                plantMediaPlayer.getTotalDuration();

        if (
                totalDuration == null
                        ||
                        totalDuration.isUnknown()
                        ||
                        totalDuration.isIndefinite()
                        ||
                        totalDuration.lessThanOrEqualTo(
                                Duration.ZERO
                        )
        ) {

            return;
        }

        double clamped =
                Math.max(
                        0.0,
                        Math.min(
                                VIDEO_END_FRACTION,
                                progress
                        )
                );

        Duration target =
                totalDuration.multiply(
                        clamped
                );

        stopVideoTransitionOnly();

        plantMediaPlayer.pause();

        plantMediaPlayer.setRate(
                1.0
        );

        muteVideo();

        plantMediaPlayer.seek(
                target
        );
    }


    /**
     * Stops only the video transition.
     */
    private void stopVideoTransitionOnly() {

        if (
                videoStopTransition != null
        ) {

            videoStopTransition.stop();

            videoStopTransition = null;
        }
    }


    /**
     * Fully disposes crop video.
     */
    private void stopPlantVideo() {

        stopVideoTransitionOnly();

        if (
                plantMediaPlayer != null
        ) {

            try {

                plantMediaPlayer.pause();

                plantMediaPlayer.stop();

                plantMediaPlayer.dispose();

            } catch (Exception ignored) {

                // Nothing else required.
            }

            plantMediaPlayer = null;
        }

        loadedVideoCrop = null;

        if (
                plantVideoView != null
        ) {

            plantVideoView.setMediaPlayer(
                    null
            );

            plantVideoView.setVisible(
                    false
            );
        }
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        activityTable.setItems(
                tableRows
        );

        activityTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        dayCol.setCellValueFactory(
                c ->
                        new SimpleIntegerProperty(
                                c.getValue().getDay()
                        )
        );

        weatherCol.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getWeather()
                        )
        );

        actionCol.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getAction()
                        )
        );

        waterCol.setCellValueFactory(
                c ->
                        new SimpleDoubleProperty(
                                c.getValue().getWaterUsed()
                        )
        );

        fertCol.setCellValueFactory(
                c ->
                        new SimpleDoubleProperty(
                                c.getValue().getFertilizerUsed()
                        )
        );

        growthCol.setCellValueFactory(
                c ->
                        new SimpleIntegerProperty(
                                c.getValue().getGrowthPercent()
                        )
        );

        statusCol.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getStatus()
                        )
        );

        waterCol.setCellFactory(
                col ->
                        numericCell("%.1f")
        );

        fertCol.setCellFactory(
                col ->
                        numericCell("%.2f")
        );

        growthCol.setCellFactory(
                col ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Number value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );

                                setText(
                                        empty || value == null
                                                ? null
                                                : value.intValue()
                                                + "%"
                                );
                            }
                        }
        );
    }


    private static TableCell<SimDayLog, Number> numericCell(
            String pattern
    ) {

        return new TableCell<>() {

            @Override
            protected void updateItem(
                    Number value,
                    boolean empty
            ) {

                super.updateItem(
                        value,
                        empty
                );

                if (
                        empty
                                || value == null
                ) {

                    setText(null);

                    return;
                }

                setText(
                        String.format(
                                Locale.US,
                                pattern,
                                value.doubleValue()
                        )
                );
            }
        };
    }


    // =========================================================
    // SPEED
    // =========================================================

    private void configureSpeed() {

        /*
         * =====================================================
         * IMPORTANT SPEED FIX
         * =====================================================
         *
         * Allow up to 10x even if the original FXML slider
         * had a smaller maximum.
         */
        if (
                speedSlider.getMax()
                        <
                        MAX_SIMULATION_SPEED
        ) {

            speedSlider.setMax(
                    MAX_SIMULATION_SPEED
            );
        }

        /*
         * Prevent an invalid zero / negative speed.
         */
        if (
                speedSlider.getValue()
                        <
                        0.5
        ) {

            speedSlider.setValue(
                    1.0
            );
        }

        speedSlider.valueProperty()
                .addListener(
                        (obs, oldV, newV) -> {

                            double speed =
                                    Math.max(
                                            0.5,
                                            Math.min(
                                                    MAX_SIMULATION_SPEED,
                                                    newV.doubleValue()
                                            )
                                    );

                            speedLabel.setText(
                                    String.format(
                                            Locale.US,
                                            "%.1f×",
                                            speed
                                    )
                            );

                            /*
                             * Immediately change video playback
                             * rate when the slider moves.
                             */
                            if (
                                    plantMediaPlayer != null
                            ) {

                                if (playing) {

                                    plantMediaPlayer.setRate(
                                            speed
                                    );

                                    muteVideo();
                                }
                            }

                            /*
                             * If autoplay is already running,
                             * restart timer using the new speed.
                             */
                            if (playing) {

                                restartAutoPlay();
                            }
                        }
                );

        speedLabel.setText(
                String.format(
                        Locale.US,
                        "%.1f×",
                        getSimulationSpeed()
                )
        );
    }


    /**
     * Returns current simulation speed.
     */
    private double getSimulationSpeed() {

        if (speedSlider == null) {

            return 1.0;
        }

        return Math.max(
                0.5,
                Math.min(
                        MAX_SIMULATION_SPEED,
                        speedSlider.getValue()
                )
        );
    }


    // =========================================================
    // AUTOPLAY
    // =========================================================

    private void startAutoPlay() {

        stopAutoPlayTimerOnly();

        if (session.isCropReady()) {

            updateReadyState();

            return;
        }

        playing = true;

        playPauseButton.setText(
                "Pause"
        );

        double speed =
                getSimulationSpeed();

        /*
         * =====================================================
         * REAL SPEED CALCULATION
         * =====================================================
         *
         * Base = 1500 ms per simulation day.
         *
         * 1x  -> 1500 ms
         * 2x  -> 750 ms
         * 5x  -> 300 ms
         * 10x -> 150 ms
         *
         * There is no old 700 ms bottleneck anymore.
         */
        double millis =
                Math.max(
                        MIN_AUTOPLAY_MS,
                        1500.0 / speed
                );

        /*
         * Make video use the same speed.
         */
        if (
                plantMediaPlayer != null
        ) {

            plantMediaPlayer.setRate(
                    speed
            );

            muteVideo();
        }

        autoPlay =
                new Timeline(
                        new KeyFrame(
                                Duration.millis(
                                        millis
                                ),
                                event -> {

                                    if (
                                            session.isCropReady()
                                    ) {

                                        stopAutoPlay();

                                        return;
                                    }

                                    selectedAction =
                                            recommendedAction;

                                    highlightActionCards(
                                            recommendedAction
                                    );

                                    advanceOneDay(
                                            true
                                    );
                                }
                        )
                );

        autoPlay.setCycleCount(
                Timeline.INDEFINITE
        );

        autoPlay.play();

        statusLabel.setText(
                "Simulation running @ "
                        +
                        String.format(
                                Locale.US,
                                "%.1f×",
                                speed
                        )
        );
    }


    /**
     * Restarts autoplay after speed changes.
     */
    private void restartAutoPlay() {

        if (!playing) {

            return;
        }

        startAutoPlay();
    }


    private void stopAutoPlayTimerOnly() {

        if (
                autoPlay != null
        ) {

            autoPlay.stop();

            autoPlay = null;
        }
    }


    private void stopAutoPlay() {

        playing = false;

        playPauseButton.setText(
                "Play"
        );

        stopAutoPlayTimerOnly();

        /*
         * Return video to normal playback rate while paused.
         */
        if (
                plantMediaPlayer != null
        ) {

            plantMediaPlayer.pause();

            plantMediaPlayer.setRate(
                    1.0
            );

            muteVideo();
        }

        /*
         * Do not reset crop video.
         *
         * Keep it exactly where simulation is.
         */
        synchronizeVideoWithSimulation();
    }


    // =========================================================
    // ONE SIMULATION DAY
    // =========================================================

    private void advanceOneDay(
            boolean animate
    ) {

        if (
                session.isCropReady()
        ) {

            stopAutoPlay();

            updateReadyState();

            return;
        }

        Crop crop =
                session.getActiveCrop();

        if (crop == null) {

            statusLabel.setText(
                    "No crop selected."
            );

            return;
        }

        Resource resource =
                session.getFarm()
                        .getResource();

        String weather =
                getRandomWeather();

        String action =
                selectedAction == null
                        ? "Irrigate"
                        : selectedAction;

        double baseWater =
                crop.getWaterNeed()
                        /
                        Math.max(
                                1,
                                crop.getGrowthDays()
                        );

        double weatherFactor =
                switch (weather) {

                    case "Rain" ->
                            0.45;

                    case "Cloudy" ->
                            0.75;

                    case "Storm" ->
                            0.55;

                    case "Heat" ->
                            1.35;

                    default ->
                            1.05;
                };

        double waterUsed =
                baseWater * weatherFactor;

        double fertUsed = 0.0;

        String status;

        switch (action) {

            case "Conserve" -> {

                waterUsed *= 0.55;
            }

            case "Fertilize" -> {

                fertUsed =
                        Math.max(
                                0.4,
                                crop.getFertilizerNeed()
                                        /
                                        Math.max(
                                                8.0,
                                                crop.getGrowthDays()
                                                        / 5.0
                                        )
                        );
            }

            case "Protect" -> {

                waterUsed *= 0.7;

                fertUsed = 0.15;
            }

            default -> {

                waterUsed *= 1.15;
            }
        }


        /*
         * =====================================================
         * EXACTLY ONE CALENDAR DAY
         * =====================================================
         *
         * Every button press / Coach Step / autoplay tick
         * advances exactly ONE day.
         */
        session.advanceDay();


        boolean hasWater =
                resource.isAvailable(
                        "water",
                        waterUsed
                );

        boolean hasFert =
                fertUsed <= 0
                        ||
                resource.isAvailable(
                        "fertilizer",
                        fertUsed
                );


        boolean grew = false;


        if (
                !hasWater
                        ||
                !hasFert
        ) {

            status =
                    !hasWater
                            ? "Low water"
                            : "Low fertilizer";

            waterUsed = 0;

            fertUsed = 0;

            /*
             * Simulation day still advances.
             *
             * No growth point is awarded when resources
             * are unavailable.
             */
            session.adjustCareScore(
                    -4
            );

        } else {

            resource.consume(
                    "water",
                    waterUsed
            );

            if (
                    fertUsed > 0
            ) {

                resource.consume(
                        "fertilizer",
                        fertUsed
                );
            }


            /*
             * =================================================
             * CRITICAL GROWTH RULE
             * =================================================
             *
             * ONE successful simulation day =
             * ONE growth day.
             *
             * Day 1  -> +1 growth
             * Day 2  -> +1 growth
             * ...
             * Day 90 -> +1 growth
             */
            session.addGrowthDay();

            grew = true;

            status =
                    session.isCropReady()
                            ? "Ready to harvest"
                            : "Growing";

            applyCareForAction(
                    action,
                    weather
            );
        }


        int growthPct =
                growthPercent();


        SimDayLog row =
                new SimDayLog(
                        session.getCurrentDay(),
                        weather,
                        action,
                        waterUsed,
                        fertUsed,
                        status,
                        growthPct
                );


        session.addDayLog(
                row
        );

        tableRows.add(
                row
        );


        activityTable.scrollTo(
                tableRows.size() - 1
        );


        lastActionLabel.setText(
                "Last action: "
                        +
                        action
                        +
                        " - "
                        +
                        weather
        );


        /*
         * Update UI before video synchronization.
         */
        updateScreen(
                weather,
                status
        );


        /*
         * =====================================================
         * VIDEO SYNCHRONIZATION
         * =====================================================
         *
         * Coach Step:
         *
         * click -> day +1 -> growth +1 -> video +1 day
         *
         * Autoplay:
         *
         * timer -> day +1 -> growth +1 -> video +1 day
         */
        if (animate) {

            synchronizeVideoWithSimulation();

        } else {

            positionVideoAtGrowth(
                    growthPctFraction()
                            * VIDEO_END_FRACTION
            );
        }


        if (
                session.isCropReady()
        ) {

            stopAutoPlay();

            positionVideoAtGrowth(
                    VIDEO_END_FRACTION
            );

            statusLabel.setText(
                    crop.getName()
                            +
                            " is ready - continue to market."
            );

            updateReadyState();

        } else if (
                !grew
        ) {

            /*
             * The simulation did not pause.
             *
             * The plant simply failed to grow that day
             * because resources were unavailable.
             */
            statusLabel.setText(
                    status
                            +
                            " - try Conserve or follow the coach."
            );

        } else if (
                !playing
        ) {

            statusLabel.setText(
                    action
                            +
                            " applied - care "
                            +
                            session.getCareScore()
            );
        }
    }


    // =========================================================
    // CARE SCORE
    // =========================================================

    private void applyCareForAction(
            String action,
            String weather
    ) {

        boolean match =
                action.equalsIgnoreCase(
                        recommendedAction
                );

        if (match) {

            session.adjustCareScore(
                    6
            );

        } else {

            session.adjustCareScore(
                    -1
            );
        }


        if (
                (
                        "Storm".equals(weather)
                                ||
                        "Heat".equals(weather)
                )
                        &&
                "Protect".equals(action)
        ) {

            session.adjustCareScore(
                    5
            );

        } else if (
                (
                        "Storm".equals(weather)
                                ||
                        "Heat".equals(weather)
                )
                        &&
                !"Protect".equals(action)
        ) {

            session.adjustCareScore(
                    -7
            );
        }


        if (
                "Fertilize".equals(action)
                        &&
                match
        ) {

            session.adjustCareScore(
                    2
            );
        }
    }


    // =========================================================
    // SCREEN
    // =========================================================

    private void updateScreen(
            String weather,
            String status
    ) {

        lastWeather =
                weather;

        Crop crop =
                session.getActiveCrop();

        Resource resource =
                session.getFarm()
                        .getResource();

        double progress =
                growthPctFraction();


        dayLabel.setText(
                String.valueOf(
                        session.getCurrentDay()
                )
        );

        weatherLabel.setText(
                weather
        );

        fieldWeatherBadge.setText(
                weather
        );


        waterLabel.setText(
                String.format(
                        Locale.US,
                        "%.0f L",
                        resource.getWater()
                )
        );


        fertilizerLabel.setText(
                String.format(
                        Locale.US,
                        "%.1f kg",
                        resource.getFertilizer()
                )
        );


        progressLabel.setText(
                String.format(
                        Locale.US,
                        "%.0f%%",
                        progress * 100
                )
        );


        growthProgress.setProgress(
                progress
        );


        /*
         * Only show simulation status here.
         *
         * Video being paused is independent of
         * simulation state.
         */
        if (!playing) {

            statusLabel.setText(
                    status
            );
        }


        careScoreLabel.setText(
                String.valueOf(
                        session.getCareScore()
                )
        );


        updateAdvisorPanel(
                weather,
                status
        );


        updateSky(
                weather
        );


        highlightActionCards(
                recommendedAction
        );


        loadCropVideo(
                crop
        );


        /*
         * Synchronize video after UI state has been updated.
         */
        synchronizeVideoWithSimulation();


        updateReadyState();
    }


    // =========================================================
    // ADVISOR
    // =========================================================

    private void updateAdvisorPanel(
            String weather,
            String status
    ) {

        AdvisorResult advice =
                session.getAdvisorResult();

        String plantGrade =
                advice == null
                        ? "C"
                        : advice.getPredictedGrade();


        String live =
                GradePredictor.nudgeGrade(
                        plantGrade,
                        session.getCareScore()
                );


        liveGradeLabel.setText(
                live
        );


        if (advice == null) {

            predictedGradeSimLabel.setText(
                    "Plant-time grade: -"
            );

            fertilizerPlanSimLabel.setText(
                    "Fertilizer plan: -"
            );

            recommendedActionLabel.setText(
                    "Irrigate"
            );

            dailyTipLabel.setText(
                    "No advisor result - irrigate carefully."
            );

            recommendedAction =
                    "Irrigate";

            return;
        }


        predictedGradeSimLabel.setText(
                "Plant-time grade: "
                        +
                        plantGrade
                        +
                        " -> live path "
                        +
                        live
        );


        fertilizerPlanSimLabel.setText(
                "Fertilizer: "
                        +
                        advice.getFertilizerPlan()
                        +
                        " - "
                        +
                        advice.getFertilizerKgTip()
        );


        String tipWeather =
                status != null
                        &&
                        status.toLowerCase(
                                Locale.ROOT
                        ).contains(
                                "low"
                        )
                        ? "Heat"
                        : weather;


        WekaAdvisorService.GradeTip tip =
                WekaAdvisorService
                        .getInstance()
                        .gradeImprovementTip(
                                advice,
                                session.getFarm()
                                        .getResource(),
                                tipWeather,
                                session.getCareScore()
                        );


        recommendedAction =
                tip.action();


        recommendedActionLabel.setText(
                tip.action()
        );


        dailyTipLabel.setText(
                tip.message()
        );
    }


    // =========================================================
    // WEATHER VISUAL
    // =========================================================

    private void updateSky(
            String weather
    ) {

        Color fill =
                switch (weather) {

                    case "Rain" ->
                            Color.web(
                                    "#b7c9d4"
                            );

                    case "Cloudy" ->
                            Color.web(
                                    "#c9d5cf"
                            );

                    case "Storm" ->
                            Color.web(
                                    "#8fa0ab"
                            );

                    case "Heat" ->
                            Color.web(
                                    "#f0d9a8"
                            );

                    default ->
                            Color.web(
                                    "#cfe6dc"
                            );
                };


        skyRect.setFill(
                fill
        );
    }


    // =========================================================
    // ACTION CARDS
    // =========================================================

    private void highlightActionCards(
            String action
    ) {

        setCardStyle(
                irrigateButton,
                "Irrigate".equalsIgnoreCase(
                        action
                )
        );


        setCardStyle(
                conserveButton,
                "Conserve".equalsIgnoreCase(
                        action
                )
        );


        setCardStyle(
                fertilizeButton,
                "Fertilize".equalsIgnoreCase(
                        action
                )
        );


        setCardStyle(
                protectButton,
                "Protect".equalsIgnoreCase(
                        action
                )
        );
    }


    private void setCardStyle(
            Button button,
            boolean recommended
    ) {

        button.getStyleClass().removeAll(
                "action-card",
                "action-card-recommended",
                "action-card-compact",
                "action-card-compact-recommended"
        );


        button.getStyleClass().add(
                recommended
                        ? "action-card-compact-recommended"
                        : "action-card-compact"
        );
    }


    // =========================================================
    // READY STATE
    // =========================================================

    private void updateReadyState() {

        boolean ready =
                session.isCropReady();


        harvestButton.setVisible(
                ready
        );

        harvestButton.setManaged(
                ready
        );


        nextDayButton.setDisable(
                ready
        );

        playPauseButton.setDisable(
                ready
        );

        irrigateButton.setDisable(
                ready
        );

        conserveButton.setDisable(
                ready
        );

        fertilizeButton.setDisable(
                ready
        );

        protectButton.setDisable(
                ready
        );

        applyTipButton.setDisable(
                ready
        );


        if (ready) {

            stopAutoPlay();

            /*
             * Make absolutely sure final video frame corresponds
             * to 100% crop growth.
             */
            positionVideoAtGrowth(
                    VIDEO_END_FRACTION
            );
        }
    }


    // =========================================================
    // GROWTH CALCULATION
    // =========================================================

    private int growthPercent() {

        return (int)
                Math.round(
                        growthPctFraction()
                                * 100
                );
    }


    /**
     * Calculates actual crop growth.
     *
     * This is the ONLY growth calculation.
     *
     * completedGrowthDays / crop.getGrowthDays()
     *
     * Therefore:
     *
     * 1 / 90 = 1.11%
     * 45 / 90 = 50%
     * 90 / 90 = 100%
     */
    private double growthPctFraction() {

        Crop crop =
                session.getActiveCrop();

        if (
                crop == null
                        ||
                crop.getGrowthDays() <= 0
        ) {

            return 0;
        }

        return Math.min(
                1.0,
                session.getCompletedGrowthDays()
                        /
                        (double)
                                crop.getGrowthDays()
        );
    }


    // =========================================================
    // WEATHER
    // =========================================================

    private String getRandomWeather() {

        SmartHarvest360.Weather liveWeather =
                session.pollLiveWeather();

        if (
                liveWeather != null
        ) {

            return liveWeather.getLabel();
        }


        String[] options = {
                "Sunny",
                "Rain",
                "Cloudy",
                "Storm",
                "Heat"
        };


        return options[
                random.nextInt(
                        options.length
                )
        ];
    }
}