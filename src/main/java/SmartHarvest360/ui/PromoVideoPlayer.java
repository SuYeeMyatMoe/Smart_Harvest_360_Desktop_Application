package SmartHarvest360.ui;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opens the official SmartHarvest 360 promo
 * inside a styled JavaFX video player.
 *
 * This class is completely independent from the
 * crop-growth simulation video.
 */
public final class PromoVideoPlayer {

    private static final String PROMO_RESOURCE =
            "/video/SmartHarvest360_Official_Promo.mp4";

    private static Stage openStage;
    private static MediaPlayer openPlayer;
    private static Timeline stallWatch;
    private static Parent cachedOwnerRoot;

    private PromoVideoPlayer() {
    }

    public static void show(Window owner) {
        show(owner, null);
    }

    /**
     * Opens the official promotional film.
     *
     * @param owner parent window, if available
     * @param onClosed runs after the player window closes
     */
    public static void show(Window owner, Runnable onClosed) {
        if (openStage != null && openStage.isShowing()) {
            openStage.toFront();
            openStage.requestFocus();
            return;
        }

        disposePlayer();
        cacheOwner(owner);

        MediaView view = new MediaView();
        view.setPreserveRatio(true);
        view.setSmooth(false);
        view.setCache(false);
        view.setMouseTransparent(true);

        Label title = new Label("SmartHarvest 360 — Official Film");
        title.getStyleClass().add("video-title");

        Button playPause = new Button("Play");
        playPause.getStyleClass().add("video-control");

        Button replay = new Button("Replay");
        replay.getStyleClass().add("video-control");

        Button close = new Button("Close");
        close.getStyleClass().add("video-close");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10, title, spacer, replay, playPause, close);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(13, 18, 13, 18));
        controls.getStyleClass().add("video-controls");

        StackPane viewport = new StackPane(view);
        viewport.setStyle("-fx-background-color: #031a15;");
        viewport.setMinSize(0, 0);

        BorderPane root = new BorderPane();
        root.setCenter(viewport);
        root.setBottom(controls);
        root.getStyleClass().add("video-player-root");

        Scene scene = new Scene(root, 1120, 700);
        URL stylesheet = PromoVideoPlayer.class.getResource("/style.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        Stage stage = new Stage();
        openStage = stage;
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("SmartHarvest 360 — Official Film");
        stage.setScene(scene);
        stage.setWidth(1120);
        stage.setHeight(700);
        stage.setMinWidth(1120);
        stage.setMinHeight(700);
        stage.setResizable(false);

        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean prepared = new AtomicBoolean(false);

        playPause.setOnAction(event -> {
            MediaPlayer player = openPlayer;
            if (!usable(player)) {
                return;
            }
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                playPause.setText("Play");
            } else {
                player.play();
                playPause.setText("Pause");
                started.set(true);
            }
        });

        replay.setOnAction(event -> restart(playPause, started));
        close.setOnAction(event -> stage.close());

        stage.setOnShown(event -> {
            PauseTransition settle = new PauseTransition(Duration.millis(220));
            settle.setOnFinished(ready -> whenViewportReady(viewport, () ->
                    prepareAndStart(view, viewport, playPause, started, prepared)));
            settle.play();
        });

        stage.setOnHidden(event -> {
            stopWatch();
            view.setMediaPlayer(null);
            if (openStage == stage) {
                openStage = null;
            }
            disposePlayer();
            restoreOwner();
            if (onClosed != null) {
                Platform.runLater(onClosed);
            }
        });

        stage.show();
    }

    /**
     * Wait until the window has a real size, then create the decoder.
     * Changing MediaView size after play starts is what freezes GStreamer
     * on Windows — even when the user never presses Replay.
     */
    private static void whenViewportReady(StackPane viewport, Runnable action) {
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable runOnce = () -> {
            if (!ran.compareAndSet(false, true)) {
                return;
            }
            action.run();
        };
        if (viewport.getWidth() > 80 && viewport.getHeight() > 80) {
            runOnce.run();
            return;
        }
        ChangeListener<Number> listener = new ChangeListener<>() {
            @Override
            public void changed(
                    javafx.beans.value.ObservableValue<? extends Number> observable,
                    Number oldValue,
                    Number newValue
            ) {
                if (viewport.getWidth() > 80 && viewport.getHeight() > 80) {
                    viewport.widthProperty().removeListener(this);
                    viewport.heightProperty().removeListener(this);
                    runOnce.run();
                }
            }
        };
        viewport.widthProperty().addListener(listener);
        viewport.heightProperty().addListener(listener);
        PauseTransition timeout = new PauseTransition(Duration.millis(500));
        timeout.setOnFinished(event -> {
            viewport.widthProperty().removeListener(listener);
            viewport.heightProperty().removeListener(listener);
            runOnce.run();
        });
        timeout.play();
    }

    private static void prepareAndStart(
            MediaView view,
            StackPane viewport,
            Button playPause,
            AtomicBoolean started,
            AtomicBoolean prepared
    ) {
        if (!prepared.compareAndSet(false, true)) {
            return;
        }
        if (openStage == null || !openStage.isShowing()) {
            prepared.set(false);
            return;
        }

        view.setFitWidth(Math.max(viewport.getWidth(), 1));
        view.setFitHeight(Math.max(viewport.getHeight(), 1));

        URL resource = PromoVideoPlayer.class.getResource(PROMO_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("Official promo video is missing: " + PROMO_RESOURCE);
        }

        Media media;
        try {
            media = new Media(playableUri(resource));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load the official promo video.", ex);
        }

        MediaPlayer player = new MediaPlayer(media);
        player.setAutoPlay(false);
        player.setCycleCount(1);
        player.setRate(1.0);
        openPlayer = player;

        player.setOnReady(() -> Platform.runLater(() -> {
            if (openPlayer != player) {
                return;
            }
            System.out.println("SmartHarvest 360 promo video ready.");
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                System.out.println("Promo duration: " + total.toSeconds() + " seconds");
            }
            if (view.getMediaPlayer() != player) {
                view.setMediaPlayer(player);
            }
            PauseTransition start = new PauseTransition(Duration.millis(120));
            start.setOnFinished(event -> {
                if (openPlayer != player) {
                    return;
                }
                if (started.compareAndSet(false, true)) {
                    player.play();
                    playPause.setText("Pause");
                    watchForStall(player);
                }
            });
            start.play();
        }));

        player.setOnError(() -> {
            System.err.println("SmartHarvest 360 promo video error:");
            if (player.getError() != null) {
                player.getError().printStackTrace();
            }
            playPause.setText("Play");
            started.set(false);
            stopWatch();
        });

        player.setOnEndOfMedia(() -> {
            playPause.setText("Replay");
            started.set(false);
            stopWatch();
        });

        player.setOnStalled(() -> Platform.runLater(() -> {
            if (openPlayer == player && started.get()) {
                player.play();
            }
        }));
    }

    /**
     * If the decoder reports PLAYING but time stops, nudge it forward a
     * single frame. Never seek back to the start unless Replay is clicked.
     */
    private static void watchForStall(MediaPlayer player) {
        stopWatch();
        AtomicReference<Duration> lastTime = new AtomicReference<>(Duration.UNKNOWN);
        AtomicBoolean grace = new AtomicBoolean(true);
        PauseTransition gracePeriod = new PauseTransition(Duration.seconds(1.2));
        gracePeriod.setOnFinished(event -> grace.set(false));
        gracePeriod.play();

        Timeline watch = new Timeline(new KeyFrame(Duration.millis(400), event -> {
            if (openPlayer != player || !usable(player) || grace.get()) {
                return;
            }
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.STALLED) {
                player.play();
                return;
            }
            if (status != MediaPlayer.Status.PLAYING) {
                return;
            }
            Duration now = player.getCurrentTime();
            Duration total = player.getTotalDuration();
            if (now == null || now.isUnknown()) {
                return;
            }
            if (total != null && !total.isUnknown()
                    && total.subtract(now).lessThanOrEqualTo(Duration.millis(350))) {
                return;
            }
            Duration previous = lastTime.getAndSet(now);
            if (previous == null || previous.isUnknown()) {
                return;
            }
            if (now.greaterThan(previous)) {
                return;
            }
            player.play();
        }));
        watch.setCycleCount(Timeline.INDEFINITE);
        stallWatch = watch;
        watch.play();
    }

    private static String playableUri(URL resource) throws Exception {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "smartharvest-videos");
        Files.createDirectories(directory);
        Path target = directory.resolve("SmartHarvest360_Official_Promo.mp4");
        long sourceSize = resource.openConnection().getContentLengthLong();
        if (Files.notExists(target) || Files.size(target) == 0
                || (sourceSize > 0 && Files.size(target) != sourceSize)) {
            try (InputStream input = resource.openStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target.toAbsolutePath().toUri().toString();
    }

    private static boolean usable(MediaPlayer player) {
        if (player == null) {
            return false;
        }
        MediaPlayer.Status status = player.getStatus();
        return status == MediaPlayer.Status.READY
                || status == MediaPlayer.Status.PAUSED
                || status == MediaPlayer.Status.PLAYING
                || status == MediaPlayer.Status.STOPPED
                || status == MediaPlayer.Status.STALLED;
    }

    private static void restart(Button playPause, AtomicBoolean started) {
        MediaPlayer player = openPlayer;
        if (!usable(player)) {
            return;
        }
        started.set(true);
        player.seek(Duration.ZERO);
        Platform.runLater(() -> {
            if (!usable(openPlayer)) {
                return;
            }
            openPlayer.play();
            playPause.setText("Pause");
            watchForStall(openPlayer);
        });
    }

    private static void cacheOwner(Window owner) {
        restoreOwner();
        if (owner == null || owner.getScene() == null || owner.getScene().getRoot() == null) {
            return;
        }
        cachedOwnerRoot = owner.getScene().getRoot();
        cachedOwnerRoot.setCache(true);
        cachedOwnerRoot.setCacheHint(CacheHint.SPEED);
    }

    private static void restoreOwner() {
        if (cachedOwnerRoot == null) {
            return;
        }
        cachedOwnerRoot.setCache(false);
        cachedOwnerRoot.setCacheHint(CacheHint.DEFAULT);
        cachedOwnerRoot = null;
    }

    private static void stopWatch() {
        Timeline watch = stallWatch;
        stallWatch = null;
        if (watch != null) {
            watch.stop();
        }
    }

    private static void disposePlayer() {
        stopWatch();
        MediaPlayer player = openPlayer;
        openPlayer = null;
        if (player == null) {
            return;
        }
        try {
            player.setOnReady(null);
            player.setOnError(null);
            player.setOnEndOfMedia(null);
            player.setOnStalled(null);
            if (player.getStatus() == MediaPlayer.Status.PLAYING
                    || player.getStatus() == MediaPlayer.Status.STALLED) {
                player.pause();
            }
            player.dispose();
        } catch (Exception ignored) {
            // Previous decoder must be released before the next Watch Film click.
        }
    }
}
