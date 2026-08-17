package SmartHarvest360.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;

/**
 * Opens the official SmartHarvest 360 30-second promo
 * inside a styled JavaFX video player.
 *
 * This class is completely independent from the
 * crop-growth simulation video.
 */
public final class PromoVideoPlayer {

    private PromoVideoPlayer() {
        // Utility class - no instances.
    }

    /**
     * Opens the official 30-second promotional film.
     *
     * @param owner parent window, if available
     */
    public static void show(Window owner) {

        /*
         * IMPORTANT:
         *
         * The official promo is stored in:
         *
         * src/main/resources/video/
         *
         * NOT:
         *
         * src/main/resources/videos/
         */
        URL resource =
                PromoVideoPlayer.class.getResource(
                        "/video/SmartHarvest360_Official_Promo_30s.mp4"
                );

        if (resource == null) {

            throw new IllegalStateException(
                    "Official promo video is missing: "
                            + "/video/SmartHarvest360_Official_Promo_30s.mp4"
            );
        }

        Media media;

        try {

            media =
                    new Media(
                            resource.toExternalForm()
                    );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to load the official promo video.",
                    ex
            );
        }

        MediaPlayer player;

        try {

            player =
                    new MediaPlayer(media);

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to create the promo video player.",
                    ex
            );
        }

        MediaView view =
                new MediaView(player);

        view.setPreserveRatio(true);
        view.setSmooth(true);

        /*
         * Initial size.
         * These values are also adjusted dynamically
         * after the Scene is created.
         */
        view.setFitWidth(1120);
        view.setFitHeight(630);

        // =========================================================
        // TITLE
        // =========================================================

        Label title =
                new Label(
                        "SmartHarvest 360 — Official Film"
                );

        title.getStyleClass().add(
                "video-title"
        );

        // =========================================================
        // PLAY / PAUSE
        // =========================================================

        Button playPause =
                new Button("Play");

        playPause.getStyleClass().add(
                "video-control"
        );

        // =========================================================
        // REPLAY
        // =========================================================

        Button replay =
                new Button("Replay");

        replay.getStyleClass().add(
                "video-control"
        );

        // =========================================================
        // CLOSE
        // =========================================================

        Button close =
                new Button("Close");

        close.getStyleClass().add(
                "video-close"
        );

        // =========================================================
        // SPACER
        // =========================================================

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                javafx.scene.layout.Priority.ALWAYS
        );

        // =========================================================
        // CONTROLS
        // =========================================================

        HBox controls =
                new HBox(
                        10,
                        title,
                        spacer,
                        replay,
                        playPause,
                        close
                );

        controls.setAlignment(
                Pos.CENTER_LEFT
        );

        controls.setPadding(
                new Insets(
                        13,
                        18,
                        13,
                        18
                )
        );

        controls.getStyleClass().add(
                "video-controls"
        );

        // =========================================================
        // ROOT
        // =========================================================

        BorderPane root =
                new BorderPane(
                        view,
                        null,
                        null,
                        controls,
                        null
                );

        root.getStyleClass().add(
                "video-player-root"
        );

        // =========================================================
        // SCENE
        // =========================================================

        Scene scene =
                new Scene(
                        root,
                        1120,
                        700
                );

        URL stylesheet =
                PromoVideoPlayer.class.getResource(
                        "/style.css"
                );

        if (stylesheet != null) {

            scene.getStylesheets().add(
                    stylesheet.toExternalForm()
            );
        }

        // =========================================================
        // STAGE
        // =========================================================

        Stage stage =
                new Stage();

        stage.initModality(
                Modality.WINDOW_MODAL
        );

        if (owner != null) {

            stage.initOwner(owner);
        }

        stage.setTitle(
                "SmartHarvest 360 — Official Film"
        );

        stage.setScene(scene);

        stage.setMinWidth(760);
        stage.setMinHeight(500);

        /*
         * Keep video synchronized with the
         * available window size.
         */
        view.fitWidthProperty().bind(
                scene.widthProperty()
        );

        view.fitHeightProperty().bind(
                scene.heightProperty()
                        .subtract(70)
        );

        // =========================================================
        // PLAY / PAUSE BUTTON
        // =========================================================

        playPause.setOnAction(event -> {

            MediaPlayer.Status status =
                    player.getStatus();

            if (
                    status ==
                            MediaPlayer.Status.PLAYING
            ) {

                player.pause();

                playPause.setText(
                        "Play"
                );

            } else {

                player.play();

                playPause.setText(
                        "Pause"
                );
            }
        });

        // =========================================================
        // REPLAY BUTTON
        // =========================================================

        replay.setOnAction(event -> {

            /*
             * Always start from the beginning.
             */
            player.seek(
                    Duration.ZERO
            );

            player.play();

            playPause.setText(
                    "Pause"
            );
        });

        // =========================================================
        // CLOSE BUTTON
        // =========================================================

        close.setOnAction(event -> {

            player.stop();

            stage.close();
        });

        // =========================================================
        // MEDIA READY
        // =========================================================

        player.setOnReady(() -> {

            System.out.println(
                    "SmartHarvest 360 promo video ready."
            );

            System.out.println(
                    "Promo duration: "
                            + player
                            .getTotalDuration()
                            .toSeconds()
                            + " seconds"
            );

            /*
             * Start automatically only after
             * JavaFX confirms that the media
             * is ready.
             */
            player.play();

            playPause.setText(
                    "Pause"
            );
        });

        // =========================================================
        // MEDIA ERROR
        // =========================================================

        player.setOnError(() -> {

            System.err.println(
                    "SmartHarvest 360 promo video error:"
            );

            if (player.getError() != null) {

                player.getError().printStackTrace();
            }

            playPause.setText(
                    "Play"
            );
        });

        // =========================================================
        // END OF VIDEO
        // =========================================================

        player.setOnEndOfMedia(() -> {

            /*
             * Do NOT automatically loop.
             *
             * When the 30-second film finishes,
             * leave it at the end and show Replay.
             */
            playPause.setText(
                    "Replay"
            );
        });

        // =========================================================
        // STAGE CLOSED
        // =========================================================

        stage.setOnHidden(event -> {

            try {

                player.stop();

            } catch (Exception ignored) {
                // Player may already be stopped.
            }

            try {

                player.dispose();

            } catch (Exception ignored) {
                // Player may already be disposed.
            }
        });

        // =========================================================
        // SHOW WINDOW
        // =========================================================

        stage.show();

        /*
         * If JavaFX has already reached READY before
         * stage.show(), make sure the film starts.
         */
        if (
                player.getStatus()
                        == MediaPlayer.Status.READY
        ) {

            player.play();

            playPause.setText(
                    "Pause"
            );
        }
    }
}