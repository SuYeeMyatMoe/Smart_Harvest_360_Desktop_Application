package SmartHarvest360.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/** Centralizes JavaFX scene changes for the application. */
public final class SceneNavigator {
    private SceneNavigator() {
    }

    public static void switchTo(Node source, String fxmlResource) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlResource));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();

            // Replace root instead of creating a new Scene. On Windows, setScene(new Scene(..., 960, 680))
            // plus centerOnScreen() resets maximized windows and can make the app look minimized.
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
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
}
