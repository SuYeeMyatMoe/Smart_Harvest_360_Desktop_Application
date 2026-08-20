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
import SmartHarvest360.ui.Crop3DView;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;

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

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;


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
 * The speed slider controls simulation day frequency.
 * The growth clip plays forward once to the last frame,
 * then holds. It is never restarted from the beginning.
 */
public class SimulationController {

    private static final double FIELD_W = 266;
    private static final double FIELD_H = 230;
    private static final double SOIL_H = 42;

    /**
     * Use the full clip. A small tail is kept so the last
     * visible frame is shown instead of a black end-of-stream.
     */
    private static final double VIDEO_END_FRACTION = 1.0;
    private static final double VIDEO_LAST_FRAME_MS = 80.0;

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
    private Crop3DView crop3dView;

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
     * Invalidates stale MediaPlayer callbacks after dispose/reload.
     */
    private int videoGeneration;

    private boolean videoReady;

    /**
     * True after the clip has reached its last visible frame.
     * Prevents seeking back to the start (the old loop).
     */
    private boolean videoFinished;

    private boolean videoLifecycleHooked;

    private int videoLoadAttempts;

    /**
     * Transition used to stop video at exact simulation position.
     */
    private PauseTransition videoStopTransition;

    /**
     * Keeps a stalled GStreamer pipeline moving without seeking to 0.
     */
    private Timeline videoWatchdog;


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
        stopPlantVideo();
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
        skyRect.setVisible(false);
        skyRect.setManaged(false);
        soilRect.setVisible(false);
        soilRect.setManaged(false);

        fieldClip = new Rectangle(FIELD_W, FIELD_H);
        fieldClip.setArcWidth(18);
        fieldClip.setArcHeight(18);
        fieldPane.setClip(fieldClip);

        if (plantLayer != null) {
            plantLayer.setVisible(false);
            plantLayer.setManaged(false);
        }
    }


    // =========================================================
    // VIDEO
    // =========================================================

    private void configureVideo() {
        plantVideoView.getStyleClass().add("media-view");
        plantVideoView.setFitWidth(FIELD_W);
        plantVideoView.setFitHeight(FIELD_H);
        plantVideoView.setPreserveRatio(false);
        plantVideoView.setSmooth(true);
        plantVideoView.setCache(false);
        plantVideoView.setManaged(true);
        plantVideoView.setVisible(false);
        StackPane.setAlignment(plantVideoView, Pos.CENTER);

        if (crop3dView != null) {
            crop3dView.setVisible(true);
            Crop crop = session.getActiveCrop();
            if (crop != null) {
                crop3dView.setCrop(crop.getName());
                crop3dView.setGrowth(Math.max(0.08, growthPctFraction()), false);
            }
        }

        hookVideoLifecycle();
    }

    /**
     * Fills the rounded crop-field frame with no letterbox bars.
     */
    private void sizeVideoToFillFrame(MediaPlayer player) {
        if (plantVideoView == null) {
            return;
        }
        plantVideoView.setPreserveRatio(false);
        plantVideoView.setFitWidth(FIELD_W);
        plantVideoView.setFitHeight(FIELD_H);
    }

    private void hookVideoLifecycle() {
        if (videoLifecycleHooked || plantVideoView == null) {
            return;
        }
        videoLifecycleHooked = true;
        plantVideoView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                Platform.runLater(() -> {
                    if (plantVideoView.getScene() == null) {
                        stopPlantVideo();
                    }
                });
                return;
            }
            Platform.runLater(() -> {
                PauseTransition wait = new PauseTransition(Duration.millis(120));
                wait.setOnFinished(event -> loadCropVideo(session.getActiveCrop()));
                wait.play();
            });
        });
        if (plantVideoView.getScene() != null) {
            loadCropVideo(session.getActiveCrop());
        }
    }

    private void refreshCropVisual(Crop crop, double progress) {
        if (crop3dView != null && crop != null) {
            crop3dView.setCrop(crop.getName());
            crop3dView.setGrowth(Math.max(0.08, progress), playing);
            crop3dView.setVisible(!videoReady);
        }
        if (crop != null
                && loadedVideoCrop != null
                && !crop.getName().trim().toLowerCase(Locale.ROOT).equals(loadedVideoCrop)) {
            loadCropVideo(crop);
        }
    }

    /**
     * Loads the growth video for the active crop once.
     * Recreating MediaPlayer on the same file is what freezes Windows GStreamer
     * (ERROR_MEDIA_INVALID) and leaves a stuck last frame.
     */
    private void loadCropVideo(Crop crop) {
        if (crop == null || plantVideoView == null) {
            return;
        }
        if (plantVideoView.getScene() == null) {
            return;
        }

        String cropName = crop.getName().trim().toLowerCase(Locale.ROOT);
        if (cropName.equals(loadedVideoCrop) && isPlayerUsable()) {
            muteVideo();
            videoReady = true;
            plantVideoView.setVisible(true);
            if (crop3dView != null) {
                crop3dView.setVisible(false);
            }
            return;
        }

        String videoFile = getVideoFileName(cropName);
        if (videoFile == null) {
            showVideoFallback();
            statusLabel.setText("No growth video for " + crop.getName());
            return;
        }

        URL videoUrl = getClass().getResource("/videos/" + videoFile);
        if (videoUrl == null) {
            showVideoFallback();
            statusLabel.setText("Video not found: " + videoFile);
            return;
        }

        stopPlantVideo();

        try {
            String playableUri = playableVideoUri(videoFile, videoUrl);
            System.out.println("Loading crop video: " + cropName + " -> " + playableUri);

            Media media = new Media(playableUri);
            MediaPlayer player = new MediaPlayer(media);
            plantMediaPlayer = player;
            loadedVideoCrop = cropName;
            videoReady = false;
            int generation = videoGeneration;

            player.setCycleCount(1);
            player.setAutoPlay(false);
            videoFinished = false;
            plantVideoView.setMediaPlayer(player);
            plantVideoView.setVisible(true);
            sizeVideoToFillFrame(player);

            player.setOnReady(() -> {
                if (generation != videoGeneration || plantMediaPlayer != player) {
                    return;
                }
                muteVideo();
                videoReady = true;
                videoFinished = false;
                videoLoadAttempts = 0;
                if (crop3dView != null) {
                    crop3dView.setVisible(false);
                }
                sizeVideoToFillFrame(player);
                plantVideoView.setVisible(true);
                player.setRate(1.0);
                System.out.println("Video READY: " + cropName
                        + " (" + player.getTotalDuration().toMillis() + " ms)");
                ensureGrowthVideoPlaying();
            });

            player.setOnEndOfMedia(() -> {
                if (generation != videoGeneration || plantMediaPlayer != player) {
                    return;
                }
                if (resumeIfNotReallyEnded(player)) {
                    return;
                }
                videoFinished = true;
                stopVideoWatchdog();
                player.pause();
                muteVideo();
            });

            player.setOnError(() -> {
                if (generation != videoGeneration) {
                    return;
                }
                System.err.println("SmartHarvest 360 video error: " + player.getError());
                showVideoFallback();
                if (videoLoadAttempts < 2 && plantVideoView.getScene() != null) {
                    videoLoadAttempts++;
                    loadedVideoCrop = null;
                    PauseTransition retry = new PauseTransition(Duration.millis(400));
                    retry.setOnFinished(event -> loadCropVideo(crop));
                    retry.play();
                }
            });

            player.setOnStalled(() -> {
                if (generation != videoGeneration || plantMediaPlayer != player) {
                    return;
                }
                if (videoFinished || !playing) {
                    return;
                }
                nudgePlayback(player);
            });
        } catch (Exception ex) {
            System.err.println("Could not load crop video: " + ex.getMessage());
            showVideoFallback();
            statusLabel.setText("Unable to load crop video");
        }
    }

    private String playableVideoUri(String videoFile, URL resource) throws Exception {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "smartharvest-videos");
        Files.createDirectories(directory);
        Path target = directory.resolve(videoFile);
        long sourceSize = resource.openConnection().getContentLengthLong();
        if (Files.notExists(target) || Files.size(target) == 0
                || (sourceSize > 0 && Files.size(target) != sourceSize)) {
            try (InputStream input = resource.openStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target.toAbsolutePath().toUri().toString();
    }

    private void showVideoFallback() {
        videoReady = false;
        if (plantVideoView != null) {
            plantVideoView.setVisible(false);
        }
        if (crop3dView != null) {
            crop3dView.setVisible(true);
        }
    }

    private boolean isPlayerUsable() {
        if (plantMediaPlayer == null) {
            return false;
        }
        MediaPlayer.Status status = plantMediaPlayer.getStatus();
        return status == MediaPlayer.Status.READY
                || status == MediaPlayer.Status.PAUSED
                || status == MediaPlayer.Status.PLAYING
                || status == MediaPlayer.Status.STOPPED
                || status == MediaPlayer.Status.STALLED;
    }

    private void muteVideo() {
        if (plantMediaPlayer == null) {
            return;
        }
        try {
            plantMediaPlayer.setMute(true);
            plantMediaPlayer.setVolume(0.0);
        } catch (Exception ignored) {
            // Audio is not required.
        }
    }

    private String getVideoFileName(String cropName) {
        String name = cropName == null ? "" : cropName.trim().toLowerCase(Locale.ROOT);
        if (name.contains("chilli") || name.contains("chili")) {
            return "chilli.mp4";
        }
        if (name.contains("paddy") || name.contains("rice")) {
            return "paddy.mp4";
        }
        if (name.contains("corn") || name.contains("maize")) {
            return "corn.mp4";
        }
        if (name.contains("durian")) {
            return "durian.mp4";
        }
        if (name.contains("tomato")) {
            return "tomato.mp4";
        }
        if (name.contains("papaya")) {
            return "papaya.mp4";
        }
        return null;
    }

    /**
     * Play the growth clip once through. Seeking or pausing to match
     * each simulation day is what froze it on a single frame.
     */
    private void synchronizeVideoWithSimulation() {
        ensureGrowthVideoPlaying();
    }

    private void playOnceTowardEnd() {
        ensureGrowthVideoPlaying();
    }

    private void positionVideoAtGrowth(double progress) {
        ensureGrowthVideoPlaying();
    }

    private void ensureGrowthVideoPlaying() {
        if (plantMediaPlayer == null || videoFinished) {
            return;
        }
        if (!playing || session.isCropReady()) {
            pauseGrowthVideo();
            return;
        }
        if (plantMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            startVideoWatchdog(plantMediaPlayer, videoGeneration);
            return;
        }
        stopVideoTransitionOnly();
        muteVideo();
        plantMediaPlayer.setRate(1.0);
        if (plantMediaPlayer.getStatus() == MediaPlayer.Status.READY
                || plantMediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
            plantMediaPlayer.seek(Duration.ZERO);
        }
        plantMediaPlayer.play();
        startVideoWatchdog(plantMediaPlayer, videoGeneration);
    }

    private void pauseGrowthVideo() {
        stopVideoWatchdog();
        stopVideoTransitionOnly();
        if (plantMediaPlayer == null) {
            return;
        }
        MediaPlayer.Status status = plantMediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING
                || status == MediaPlayer.Status.STALLED) {
            plantMediaPlayer.pause();
        }
        muteVideo();
    }

    /**
     * Windows often reports END_OF_MEDIA or STALLED in the middle of a
     * time-lapse. Resume a little ahead of the stuck frame — never from 0.
     */
    private boolean resumeIfNotReallyEnded(MediaPlayer player) {
        if (player == null || videoFinished || !playing) {
            return false;
        }
        Duration now = player.getCurrentTime();
        Duration total = player.getTotalDuration();
        if (total == null || total.isUnknown() || total.toMillis() <= 0) {
            nudgePlayback(player);
            return true;
        }
        if (now == null || now.isUnknown() || now.toMillis() < total.toMillis() * 0.90) {
            nudgePlayback(player);
            return true;
        }
        return false;
    }

    private void nudgePlayback(MediaPlayer player) {
        if (player == null || videoFinished || plantMediaPlayer != player || !playing) {
            return;
        }
        Duration now = player.getCurrentTime();
        Duration total = player.getTotalDuration();
        if (now != null && !now.isUnknown()
                && total != null && !total.isUnknown()
                && total.subtract(now).lessThanOrEqualTo(Duration.millis(180))) {
            videoFinished = true;
            stopVideoWatchdog();
            player.pause();
            return;
        }
        Duration resume = (now == null || now.isUnknown())
                ? Duration.ZERO
                : now.add(Duration.millis(50));
        try {
            player.seek(resume);
        } catch (Exception ignored) {
            // Decoder may reject seek while stalled; play() still unsticks it.
        }
        player.play();
    }

    private void startVideoWatchdog(MediaPlayer player, int generation) {
        if (videoWatchdog != null || player == null) {
            return;
        }
        AtomicReference<Duration> lastTime = new AtomicReference<>(Duration.UNKNOWN);
        Timeline watch = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            if (generation != videoGeneration || plantMediaPlayer != player
                    || videoFinished || !playing) {
                return;
            }
            Duration now = player.getCurrentTime();
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown() && now != null && !now.isUnknown()
                    && total.subtract(now).lessThanOrEqualTo(Duration.millis(200))) {
                return;
            }
            MediaPlayer.Status status = player.getStatus();
            boolean stuckClock = now != null && !now.isUnknown()
                    && lastTime.get() != null && !lastTime.get().isUnknown()
                    && !now.greaterThan(lastTime.get());
            if (now != null && !now.isUnknown()) {
                lastTime.set(now);
            }
            if (status == MediaPlayer.Status.STALLED
                    || status == MediaPlayer.Status.HALTED
                    || stuckClock) {
                nudgePlayback(player);
            }
        }));
        watch.setCycleCount(Timeline.INDEFINITE);
        videoWatchdog = watch;
        watch.play();
    }

    private void stopVideoWatchdog() {
        Timeline watch = videoWatchdog;
        videoWatchdog = null;
        if (watch != null) {
            watch.stop();
        }
    }

    private void stopVideoTransitionOnly() {
        if (videoStopTransition != null) {
            videoStopTransition.stop();
            videoStopTransition = null;
        }
    }

    private void stopPlantVideo() {
        stopVideoTransitionOnly();
        stopVideoWatchdog();
        videoGeneration++;
        videoReady = false;
        videoFinished = false;
        MediaPlayer player = plantMediaPlayer;
        plantMediaPlayer = null;
        loadedVideoCrop = null;
        if (plantVideoView != null) {
            plantVideoView.setMediaPlayer(null);
            plantVideoView.setVisible(false);
        }
        if (crop3dView != null) {
            crop3dView.setVisible(true);
        }
        if (player == null) {
            return;
        }
        try {
            player.setOnReady(null);
            player.setOnError(null);
            player.setOnEndOfMedia(null);
            player.setOnStalled(null);
            player.pause();
            player.stop();
            player.dispose();
        } catch (Exception ignored) {
            // Previous decoder must still be released before a new one opens.
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

        if (isPlayerUsable()) {
            playOnceTowardEnd();
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
        ensureGrowthVideoPlaying();
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
            );
        }


        if (
                session.isCropReady()
        ) {

            stopAutoPlay();

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

        refreshCropVisual(crop, progress);

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