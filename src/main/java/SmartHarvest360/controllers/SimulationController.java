package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.Resource;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.util.Locale;
import java.util.Random;

/** Controls the day-by-day simulation screen. */
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

    private final Random random = new Random();
    private AppSession session;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        session.ensureDemoData();

        Crop crop = session.getActiveCrop();
        cropLabel.setText(crop.getName() + " (" + crop.getType() + ")");

        if (session.getSimulationLog().isEmpty()) {
            session.getSimulationLog().add(
                    "Day 1 | Sunny | Water Used: 0.00L | Crop Status: Growing"
            );
        }
        simulationLog.getItems().setAll(session.getSimulationLog());
        updateScreen("Sunny", "Growing");
    }

    @FXML
    private void handleNextDay() {
        if (session.isCropReady()) {
            return;
        }

        Crop crop = session.getActiveCrop();
        Resource resource = session.getFarm().getResource();
        String weather = getRandomWeather();

        double normalDailyWater = crop.getWaterNeed() / Math.max(1, crop.getGrowthDays());
        double waterMultiplier = switch (weather) {
            case "Rain" -> 0.40;
            case "Cloudy" -> 0.75;
            default -> 1.00;
        };
        double waterUsed = normalDailyWater * waterMultiplier;
        int nextDay = session.getCurrentDay() + 1;
        double fertilizerUsed = nextDay % 10 == 0
                ? crop.getFertilizerNeed() / Math.max(1.0, crop.getGrowthDays() / 10.0)
                : 0.0;

        session.advanceDay();

        boolean hasWater = resource.isAvailable("water", waterUsed);
        boolean hasFertilizer = resource.isAvailable("fertilizer", fertilizerUsed);
        String cropStatus;

        if (!hasWater || !hasFertilizer) {
            cropStatus = !hasWater
                    ? "Paused - not enough water"
                    : "Paused - not enough fertilizer";
        } else {
            resource.consume("water", waterUsed);
            resource.consume("fertilizer", fertilizerUsed);
            session.addGrowthDay();
            cropStatus = session.isCropReady()
                    ? crop.getName() + " is ready to harvest!"
                    : "Growing";
        }

        String logLine = String.format(
                Locale.US,
                "Day %d | %s | Water Used: %.2fL | Crop Status: %s",
                session.getCurrentDay(), weather, hasWater ? waterUsed : 0.0, cropStatus
        );
        session.getSimulationLog().add(logLine);
        simulationLog.getItems().add(logLine);
        simulationLog.scrollTo(simulationLog.getItems().size() - 1);
        updateScreen(weather, cropStatus);
    }

    @FXML
    private void handleGoToHarvest() {
        if (session.isCropReady()) {
            SceneNavigator.switchTo(harvestButton, "/fxml/HarvestMarketScreen.fxml");
        }
    }

    private void updateScreen(String weather, String status) {
        Crop crop = session.getActiveCrop();
        Resource resource = session.getFarm().getResource();
        double progress = Math.min(1.0,
                session.getCompletedGrowthDays() / (double) crop.getGrowthDays());

        dayLabel.setText("Day: " + session.getCurrentDay());
        weatherLabel.setText("Weather: " + weather);
        waterLabel.setText(String.format(Locale.US, "Water: %.2f L", resource.getWater()));
        fertilizerLabel.setText(String.format(Locale.US, "Fertilizer: %.2f kg", resource.getFertilizer()));
        progressLabel.setText(String.format(Locale.US, "Growth: %.0f%%", progress * 100));
        growthProgress.setProgress(progress);
        statusLabel.setText(status);

        boolean ready = session.isCropReady();
        harvestButton.setVisible(ready);
        harvestButton.setManaged(ready);
        nextDayButton.setDisable(ready);
    }

    private String getRandomWeather() {
        String[] options = {"Sunny", "Rain", "Cloudy"};
        return options[random.nextInt(options.length)];
    }
}
