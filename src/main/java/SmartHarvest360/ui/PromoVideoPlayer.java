package SmartHarvest360.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        MediaPlayer player;
        try {
            player = new MediaPlayer(media);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create the promo video player.", ex);
        }

        player.setAutoPlay(false);
        player.setCycleCount(1);
        player.setRate(1.0);
        openPlayer = player;

        MediaView view = new MediaView(player);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(false);
        view.setMouseTransparent(true);
        view.setManaged(false);

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

        Pane viewport = new Pane(view);
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
        stage.setMinWidth(760);
        stage.setMinHeight(500);

        AtomicBoolean started = new AtomicBoolean(false);
        PauseTransition resizeSettle = new PauseTransition(Duration.millis(160));
        resizeSettle.setOnFinished(event -> layoutVideo(player, view, viewport));

        ChangeListener<Number> resizeListener = (obs, oldValue, newValue) -> {
            if (oldValue != null && Math.abs(oldValue.doubleValue() - newValue.doubleValue()) < 1.0) {
                return;
            }
            resizeSettle.stop();
            resizeSettle.playFromStart();
        };
        viewport.widthProperty().addListener(resizeListener);
        viewport.heightProperty().addListener(resizeListener);

        Runnable startOnce = () -> {
            if (!stage.isShowing() || !usable(player)) {
                return;
            }
            layoutVideo(player, view, viewport);
            if (started.compareAndSet(false, true)) {
                player.play();
                playPause.setText("Pause");
            }
        };

        playPause.setOnAction(event -> {
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

        replay.setOnAction(event -> restart(player, playPause, started));
        close.setOnAction(event -> stage.close());

        player.setOnReady(() -> {
            System.out.println("SmartHarvest 360 promo video ready.");
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                System.out.println("Promo duration: " + total.toSeconds() + " seconds");
            }
            layoutVideo(player, view, viewport);
            startOnce.run();
        });

        player.setOnError(() -> {
            System.err.println("SmartHarvest 360 promo video error:");
            if (player.getError() != null) {
                player.getError().printStackTrace();
            }
            playPause.setText("Play");
            started.set(false);
        });

        player.setOnStalled(() -> recoverFromStall(player, playPause, started));

        player.setOnEndOfMedia(() -> {
            try {
                player.pause();
            } catch (Exception ignored) {
                // End-of-stream pause can fail on a halted decoder.
            }
            playPause.setText("Replay");
            started.set(false);
        });

        stage.setOnShown(event -> Platform.runLater(startOnce));

        stage.setOnHidden(event -> {
            viewport.widthProperty().removeListener(resizeListener);
            viewport.heightProperty().removeListener(resizeListener);
            view.setMediaPlayer(null);
            if (openStage == stage) {
                openStage = null;
            }
            disposePlayer();
            if (onClosed != null) {
                Platform.runLater(onClosed);
            }
        });

        stage.show();
    }

    /**
     * Size the video once to the pane. Live fitWidth bindings rescale every
     * pulse and make 1080p playback stutter on Windows GStreamer.
     */
    private static void layoutVideo(MediaPlayer player, MediaView view, Pane viewport) {
        double availW = viewport.getWidth();
        double availH = viewport.getHeight();
        if (availW <= 1 || availH <= 1) {
            return;
        }
        double videoW = 1920;
        double videoH = 1080;
        if (player != null && player.getMedia() != null) {
            double mediaW = player.getMedia().getWidth();
            double mediaH = player.getMedia().getHeight();
            if (mediaW > 0 && mediaH > 0) {
                videoW = mediaW;
                videoH = mediaH;
            }
        }
        double scale = Math.min(availW / videoW, availH / videoH);
        double drawW = Math.floor(videoW * scale);
        double drawH = Math.floor(videoH * scale);
        view.setPreserveRatio(true);
        view.setFitWidth(drawW);
        view.setFitHeight(0);
        view.setLayoutX(Math.floor((availW - drawW) / 2.0));
        view.setLayoutY(Math.floor((availH - drawH) / 2.0));
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
        return target.toUri().toString();
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

    private static void restart(MediaPlayer player, Button playPause, AtomicBoolean started) {
        if (!usable(player)) {
            return;
        }
        started.set(true);
        try {
            player.pause();
        } catch (Exception ignored) {
            // Pause before seek avoids a GStreamer race on Replay.
        }
        PauseTransition wait = new PauseTransition(Duration.millis(80));
        wait.setOnFinished(event -> {
            if (!usable(player)) {
                return;
            }
            player.seek(Duration.ZERO);
            PauseTransition playWait = new PauseTransition(Duration.millis(60));
            playWait.setOnFinished(playEvent -> {
                if (!usable(player)) {
                    return;
                }
                player.play();
                playPause.setText("Pause");
            });
            playWait.play();
        });
        wait.play();
    }

    private static void recoverFromStall(MediaPlayer player, Button playPause, AtomicBoolean started) {
        if (!usable(player)) {
            return;
        }
        Duration frozen = player.getCurrentTime();
        try {
            player.pause();
        } catch (Exception ignored) {
            return;
        }
        PauseTransition wait = new PauseTransition(Duration.millis(140));
        wait.setOnFinished(event -> {
            if (!usable(player)) {
                return;
            }
            if (frozen != null && !frozen.isUnknown()) {
                player.seek(frozen);
            }
            player.play();
            playPause.setText("Pause");
            started.set(true);
        });
        wait.play();
    }

    private static void disposePlayer() {
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
            player.pause();
            player.stop();
            player.dispose();
        } catch (Exception ignored) {
            // Previous decoder must be released before the next Watch Film click.
        }
    }
}
