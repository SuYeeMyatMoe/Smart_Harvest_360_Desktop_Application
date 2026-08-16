package SmartHarvest360.controllers;

import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** First screen of the app: captures the farmer's name before farm setup. */
public class WelcomeController {

    @FXML private TextField farmerNameField;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Button themeButton;

    @FXML
    public void initialize() {
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
        String saved = AppSession.getInstance().getFarmerName();
        if (saved != null && !saved.isBlank()) {
            farmerNameField.setText(saved);
        }
    }

    @FXML
    private void handleStart() {
        String name = farmerNameField.getText() == null ? "" : farmerNameField.getText().trim();
        AppSession session = AppSession.getInstance();
        session.setFarmerName(name.isBlank() ? "Farmer" : name);
        statusLabel.setText("Welcome, " + session.getFarmerName() + "! Setting up your farm...");

        // Move focus off the name field before swapping scenes to avoid a Windows
        // UIA query racing against the disposed TextField, then switch on the
        // next pulse via Platform.runLater.
        startButton.requestFocus();
        Platform.runLater(() -> SceneNavigator.switchTo(startButton, "/fxml/FarmSetupScreen.fxml"));
    }

    @FXML
    private void handleToggleTheme() {
        SmartHarvest360.ui.ThemeManager.toggle(themeButton);
    }
}
