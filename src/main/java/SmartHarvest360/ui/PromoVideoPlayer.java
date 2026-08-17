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

import java.net.URL;

/** Opens the official 30-second promo inside a styled JavaFX player. */
public final class PromoVideoPlayer {
    private PromoVideoPlayer() { }

    public static void show(Window owner) {
        URL resource = PromoVideoPlayer.class.getResource("/video/SmartHarvest360_Official_Promo_30s.mp4");
        if (resource == null) {
            throw new IllegalStateException("Official promo video is missing.");
        }
        MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
        MediaView view = new MediaView(player);
        view.setPreserveRatio(true);
        view.setFitWidth(1120);
        view.setFitHeight(630);
        view.setSmooth(true);

        Label title = new Label("SmartHarvest 360 — Official Film");
        title.getStyleClass().add("video-title");
        Button playPause = new Button("Pause");
        playPause.getStyleClass().add("video-control");
        Button replay = new Button("Replay");
        replay.getStyleClass().add("video-control");
        Button close = new Button("Close");
        close.getStyleClass().add("video-close");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox controls = new HBox(10, title, spacer, replay, playPause, close);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(13, 18, 13, 18));
        controls.getStyleClass().add("video-controls");

        BorderPane root = new BorderPane(view, null, null, controls, null);
        root.getStyleClass().add("video-player-root");
        Scene scene = new Scene(root, 1120, 700);
        scene.getStylesheets().add(PromoVideoPlayer.class.getResource("/style.css").toExternalForm());
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("SmartHarvest 360 — Official Film");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(500);

        view.fitWidthProperty().bind(scene.widthProperty());
        view.fitHeightProperty().bind(scene.heightProperty().subtract(70));
        playPause.setOnAction(event -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                playPause.setText("Play");
            } else {
                player.play();
                playPause.setText("Pause");
            }
        });
        replay.setOnAction(event -> { player.seek(javafx.util.Duration.ZERO); player.play(); playPause.setText("Pause"); });
        close.setOnAction(event -> stage.close());
        player.setOnEndOfMedia(() -> { player.seek(javafx.util.Duration.ZERO); player.pause(); playPause.setText("Replay"); });
        stage.setOnHidden(event -> player.dispose());
        stage.show();
        player.play();
    }
}
