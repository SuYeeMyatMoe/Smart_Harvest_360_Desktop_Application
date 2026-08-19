package SmartHarvest360.controllers;

import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.ui.PromoVideoPlayer;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Plays the offline cinematic introduction before Farm Setup. */
public final class IntroController {
    @FXML private StackPane introRoot;
    @FXML private ImageView heroImage;
    @FXML private Region glowOrb;
    @FXML private VBox introContent;
    @FXML private Button enterButton;

    private ScaleTransition cameraPush;
    private TranslateTransition lightDrift;

    @FXML
    public void initialize() {
        heroImage.fitWidthProperty().bind(introRoot.widthProperty());
        heroImage.fitHeightProperty().bind(introRoot.heightProperty());

        introContent.setOpacity(0.0);
        introContent.setTranslateY(24.0);
        enterButton.setOpacity(0.0);

        cameraPush = new ScaleTransition(Duration.seconds(7.5), heroImage);
        cameraPush.setFromX(1.02);
        cameraPush.setFromY(1.02);
        cameraPush.setToX(1.10);
        cameraPush.setToY(1.10);

        javafx.animation.FadeTransition reveal = new javafx.animation.FadeTransition(Duration.seconds(1.15), introContent);
        reveal.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.seconds(1.15), introContent);
        rise.setToY(0.0);

        javafx.animation.FadeTransition buttonReveal = new javafx.animation.FadeTransition(Duration.millis(650), enterButton);
        buttonReveal.setToValue(1.0);

        lightDrift = new TranslateTransition(Duration.seconds(5.0), glowOrb);
        lightDrift.setFromX(-70);
        lightDrift.setToX(70);
        lightDrift.setAutoReverse(true);
        lightDrift.setCycleCount(TranslateTransition.INDEFINITE);

        cameraPush.play();
        lightDrift.play();
        new javafx.animation.SequentialTransition(
                new ParallelTransition(reveal, rise),
                buttonReveal
        ).play();
    }

    @FXML
    private void handleEnter() {
        enterButton.setDisable(true);
        enterButton.setText("Loading...");
        // Keep the cinematic screen visible while Farm Setup is being created.
        // SceneNavigator performs the fade only after the destination is ready.
        javafx.animation.PauseTransition showLoading =
                new javafx.animation.PauseTransition(Duration.millis(80));
        showLoading.setOnFinished(event ->
                SceneNavigator.switchTo(enterButton, "/fxml/FarmSetupScreen.fxml"));
        showLoading.play();
    }

    @FXML
    private void handleWatchFilm() {
        if (cameraPush != null) {
            cameraPush.pause();
        }
        if (lightDrift != null) {
            lightDrift.pause();
        }
        PromoVideoPlayer.show(introRoot.getScene().getWindow(), () -> {
            if (lightDrift != null) {
                lightDrift.play();
            }
        });
    }
}
