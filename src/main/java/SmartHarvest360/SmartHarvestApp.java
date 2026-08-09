package SmartHarvest360;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Main JavaFX application for SmartHarvest 360. */
public class SmartHarvestApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/FarmSetupScreen.fxml"));
        stage.setTitle("SmartHarvest 360");
        stage.setScene(new Scene(root, 960, 680));
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
