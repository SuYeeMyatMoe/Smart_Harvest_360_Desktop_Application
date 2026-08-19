package SmartHarvest360.navigation;

import SmartHarvest360.SmartHarvestApp;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

/** Centralizes JavaFX scene changes for the application. */
public final class SceneNavigator {
    private SceneNavigator() {
    }

    public static void switchTo(Node source, String fxmlResource) {
        try {
            URL resource = SceneNavigator.class.getResource(fxmlResource);
            if (resource == null) {
                resource = SmartHarvestApp.class.getResource(fxmlResource);
            }
            if (resource == null) {
                throw new IOException("Missing screen resource: " + fxmlResource);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();

            // Replace root instead of creating a new Scene. On Windows, setScene(new Scene(..., 960, 680))
            // plus centerOnScreen() resets maximized windows and can make the app look minimized.
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setFill(Color.web("#eef5f0"));
                Parent currentRoot = scene.getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(160), currentRoot);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> {
                    currentRoot.setOpacity(1.0);
                    StackPane animatedRoot = createAnimatedRoot(root, scene);
                    animatedRoot.setOpacity(0.0);
                    animatedRoot.setTranslateY(12.0);
                    animatedRoot.setScaleX(0.992);
                    animatedRoot.setScaleY(0.992);
                    scene.setRoot(animatedRoot);
                    resetPagePosition(root);
                    Platform.runLater(() -> resetPagePosition(root));
                    PauseTransition layoutReset = new PauseTransition(Duration.millis(140));
                    layoutReset.setOnFinished(reset -> resetPagePosition(root));
                    layoutReset.play();

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(420), animatedRoot);
                    fadeIn.setToValue(1.0);
                    TranslateTransition riseIn = new TranslateTransition(Duration.millis(420), animatedRoot);
                    riseIn.setToY(0.0);
                    ScaleTransition settle = new ScaleTransition(Duration.millis(420), animatedRoot);
                    settle.setToX(1.0);
                    settle.setToY(1.0);
                    new ParallelTransition(fadeIn, riseIn, settle).play();
                    playLeafSweep(animatedRoot, scene);
                });
                fadeOut.play();
            }

            if (stage.isIconified()) {
                stage.setIconified(false);
            }
            stage.toFront();
            stage.requestFocus();
        } catch (IOException | RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to open the next screen");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private static StackPane createAnimatedRoot(Parent page, Scene scene) {
        Pane leafLayer = new Pane();
        leafLayer.setMouseTransparent(true);
        leafLayer.setPickOnBounds(false);
        leafLayer.setMinSize(0, 0);
        StackPane wrapper = new StackPane(page, leafLayer);
        wrapper.setStyle("-fx-background-color: #eef5f0;");
        wrapper.setPickOnBounds(false);
        leafLayer.prefWidthProperty().bind(wrapper.widthProperty());
        leafLayer.prefHeightProperty().bind(wrapper.heightProperty());
        return wrapper;
    }

    private static void resetPagePosition(Parent page) {
        page.setFocusTraversable(true);
        page.requestFocus();
        if (page instanceof ScrollPane scrollPane) {
            scrollPane.setVvalue(0.0);
            scrollPane.setHvalue(0.0);
        }
        for (Node node : page.lookupAll(".app-scroll")) {
            if (node instanceof ScrollPane scrollPane) {
                scrollPane.setVvalue(0.0);
                scrollPane.setHvalue(0.0);
            }
        }
    }

    /** A short organic leaf sweep makes navigation feel alive without delaying it. */
    private static void playLeafSweep(StackPane wrapper, Scene scene) {
        Pane layer = (Pane) wrapper.getChildren().get(1);
        double width = Math.max(900, scene.getWidth());
        double height = Math.max(600, scene.getHeight());
        Color[] colors = {Color.web("#4fa878"), Color.web("#79b98b"), Color.web("#d7a84c")};
        for (int i = 0; i < 7; i++) {
            Ellipse leaf = new Ellipse(5 + i % 3, 12 + i % 2);
            leaf.setFill(colors[i % colors.length]);
            leaf.setOpacity(0.0);
            leaf.setLayoutX(width + 30 + i * 22);
            leaf.setLayoutY(45 + (i * 67) % Math.max(180, height - 180));
            leaf.setRotate(28 + i * 17);
            layer.getChildren().add(leaf);

            FadeTransition appear = new FadeTransition(Duration.millis(180), leaf);
            appear.setFromValue(0.0);
            appear.setToValue(0.72);
            appear.setAutoReverse(true);
            appear.setCycleCount(2);
            TranslateTransition drift = new TranslateTransition(Duration.millis(720 + i * 35), leaf);
            drift.setByX(-170 - i * 18);
            drift.setByY(70 + i * 9);
            RotateTransition spin = new RotateTransition(Duration.millis(720 + i * 35), leaf);
            spin.setByAngle(150 + i * 24);
            ParallelTransition motion = new ParallelTransition(appear, drift, spin);
            motion.setDelay(Duration.millis(i * 45));
            motion.setOnFinished(done -> layer.getChildren().remove(leaf));
            motion.play();
        }
    }
}
