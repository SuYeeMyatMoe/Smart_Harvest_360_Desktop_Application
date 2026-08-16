package SmartHarvest360.controllers;

import SmartHarvest360.Resource;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.Weather;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Controls the day-by-day simulation screen for the whole planted season. */
public class SimulationController {

    @FXML private Label cropLabel;
    @FXML private Label dayLabel;
    @FXML private Label weatherLabel;
    @FXML private Label waterLabel;
    @FXML private Label fertilizerLabel;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar growthProgress;
    @FXML private ListView<String> simulationLog;
    @FXML private Button nextDayButton;
    @FXML private Button harvestButton;
    @FXML private Button autoRunButton;
    @FXML private Button themeButton;
    @FXML private Button fieldsButton;

    private AppSession session;
    private boolean autoRunning;
    private Weather lastWeather;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
        session.ensureDemoData();

        List<String> names = session.getFarm().getCrops().stream()
                .map(crop -> crop.getName())
                .collect(Collectors.toList());
        cropLabel.setText(names.isEmpty() ? "No crops" : String.join(", ", names));

        simulationLog.getItems().setAll(session.getSimulationLog());
        simulationLog.setPlaceholder(new Label("Press \"Advance to Next Day\" to start the season."));

        if (autoRunButton != null) {
            autoRunButton.setText("\u25B6 Auto-run");
        }
        updateScreen(null, "Season not started");
    }

    @FXML
    private void handleToggleTheme() {
        SmartHarvest360.ui.ThemeManager.toggle(themeButton);
    }

    @FXML
    private void handleOpenFields() {
        SceneNavigator.switchTo(fieldsButton, "/fxml/FieldsOverviewScreen.fxml");
    }

    @FXML
    private void handleNextDay() {
        if (session.getSeason() == null || session.isCropReady()) {
            return;
        }
        SeasonSimulator.SeasonDayResult result = session.getSeason().advanceDay();
        lastWeather = result.weather();
        simulationLog.getItems().setAll(session.getSimulationLog());
        if (!simulationLog.getItems().isEmpty()) {
            simulationLog.scrollTo(simulationLog.getItems().size() - 1);
        }
        updateScreen(lastWeather, session.isCropReady() ? "READY" : "Growing");
        if (session.isCropReady() && autoRunning) {
            stopAutoRun();
        }
    }

    private void updateScreen(Weather weather, String status) {
        if (session.getSeason() == null) {
            return;
        }
        Resource resource = session.getFarm().getResource();
        dayLabel.setText("Day: " + session.getCurrentDay());
        if (weather == null) {
            weatherLabel.setText("No weather yet");
            weatherLabel.setGraphic(null);
        } else {
            weatherLabel.setText(weather.getLabel());
            weatherLabel.setGraphic(SmartHarvest360.ui.IconFactory.weatherIcon(weather));
        }
        waterLabel.setText(String.format(Locale.US, "Water: %.2f L", resource.getWater()));
        fertilizerLabel.setText(String.format(Locale.US, "Fertilizer: %.2f kg", resource.getFertilizer()));
        progressLabel.setText(String.format(Locale.US, "Growth: %d%%", session.getProgressPercent()));
        growthProgress.setProgress(session.getProgressFraction());
        boolean live = session.getSeason() != null && session.getSeason().usesRealWeather();
        statusLabel.setText(status + (live ? " · LIVE NASA weather" : ""));

        boolean ready = session.isCropReady();
        nextDayButton.setDisable(ready);
        harvestButton.setDisable(false);
        harvestButton.setVisible(ready);
        harvestButton.setManaged(ready);
    }

    @FXML
    private void handleAutoRun() {
        if (autoRunning) {
            stopAutoRun();
            return;
        }
        autoRunning = true;
        if (autoRunButton != null) {
            autoRunButton.setText("\u23F8 Stop");
        }
        scheduleAutoStep();
    }

    private void scheduleAutoStep() {
        if (!autoRunning) {
            return;
        }
        PauseTransition pause = new PauseTransition(Duration.millis(350));
        pause.setOnFinished(event -> {
            if (autoRunning) {
                handleNextDay();
                if (autoRunButton != null) {
                    autoRunButton.setDisable(false);
                }
                if (autoRunning) {
                    scheduleAutoStep();
                }
            }
        });
        pause.play();
    }

    private void stopAutoRun() {
        autoRunning = false;
        if (autoRunButton != null) {
            autoRunButton.setText("\u25B6 Auto-run");
        }
    }

    @FXML
    private void handleGoToHarvest() {
        SceneNavigator.switchTo(harvestButton, "/fxml/HarvestMarketScreen.fxml");
    }
}
