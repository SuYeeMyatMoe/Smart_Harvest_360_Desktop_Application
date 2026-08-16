package SmartHarvest360;

import SmartHarvest360.ui.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Main JavaFX application for SmartHarvest 360. */
public class SmartHarvestApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/WelcomeScreen.fxml"));
        SmartHarvest360.navigation.SceneNavigator.registerRoot("/fxml/WelcomeScreen.fxml");
        stage.setTitle("SmartHarvest 360");
        Scene scene = new Scene(root, 1180, 700);
        ThemeManager.apply(scene);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/smartharvest-logo.png")));
        stage.setMinWidth(800);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
