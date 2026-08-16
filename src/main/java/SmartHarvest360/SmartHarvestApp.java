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
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/IntroScreen.fxml"));
        stage.setTitle("SmartHarvest 360 — AI Farm Intelligence");
        stage.setScene(new Scene(root, 1180, 760));
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
