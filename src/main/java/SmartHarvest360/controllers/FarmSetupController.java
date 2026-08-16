package SmartHarvest360.controllers;

import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.SeasonGoal;
import SmartHarvest360.db.Database;
import SmartHarvest360.db.FarmRepository;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import SmartHarvest360.weather.NasaPowerClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.util.Locale;

/** Collects starting farm resources and creates the Farm session. */
public class FarmSetupController {

    @FXML private ComboBox<String> cityCombo;
    @FXML private TextField farmNameField;
    @FXML private TextField budgetField;
    @FXML private TextField waterField;
    @FXML private TextField fertilizerField;
    @FXML private TextField landField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label farmNameError;
    @FXML private Label budgetError;
    @FXML private Label waterError;
    @FXML private Label fertilizerError;
    @FXML private Label landError;
    @FXML private Label latitudeError;
    @FXML private Label longitudeError;
    @FXML private Label locationError;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Button nextButton;
    @FXML private Button themeButton;
    @FXML private Button fieldsButton;
    @FXML private RadioButton roiGoalRadio;
    @FXML private RadioButton yieldGoalRadio;
    @FXML private RadioButton conserveGoalRadio;

    private static final String CUSTOM_OPTION = "Custom (enter coordinates below)";

    @FXML
    public void initialize() {
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
        connectionStatusLabel.setText("CSV ready · " + Database.statusLabel());
        if (AppSession.getInstance().getFarmName() != null) {
            farmNameField.setText(AppSession.getInstance().getFarmName());
        }

        ToggleGroup goalGroup = new ToggleGroup();
        roiGoalRadio.setToggleGroup(goalGroup);
        yieldGoalRadio.setToggleGroup(goalGroup);
        conserveGoalRadio.setToggleGroup(goalGroup);
        SeasonGoal savedGoal = AppSession.getInstance().getSeasonGoal();
        (savedGoal == SeasonGoal.MAXIMIZE_YIELD ? yieldGoalRadio
                : savedGoal == SeasonGoal.CONSERVE_RESOURCES ? conserveGoalRadio
                : roiGoalRadio).setSelected(true);

        cityCombo.getItems().add(CUSTOM_OPTION);
        for (NasaPowerClient.CityPreset city : NasaPowerClient.CITIES) {
            cityCombo.getItems().add(city.name());
        }
        cityCombo.valueProperty().addListener((ignored, oldValue, selected) -> applyCityPreset(selected));
        cityCombo.setValue(CUSTOM_OPTION);
        applyCityPreset(CUSTOM_OPTION);
    }

    private void applyCityPreset(String selected) {
        if (selected == null || selected.equals(CUSTOM_OPTION)) {
            return;
        }
        for (NasaPowerClient.CityPreset city : NasaPowerClient.CITIES) {
            if (city.name().equals(selected)) {
                latitudeField.setText(String.format(Locale.US, "%.4f", city.latitude()));
                longitudeField.setText(String.format(Locale.US, "%.4f", city.longitude()));
                locationError.setText("");
                return;
            }
        }
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
    private void handleNext() {
        clearErrors();

        String farmName = farmNameField.getText() == null ? "" : farmNameField.getText().trim();
        Double budget = parsePositive(budgetField, budgetError, "Enter a budget greater than zero.");
        Double water = parsePositive(waterField, waterError, "Enter water greater than zero.");
        Double fertilizer = parsePositive(fertilizerField, fertilizerError, "Enter fertilizer greater than zero.");
        Double land = parsePositive(landField, landError, "Enter land greater than zero.");

        boolean valid = true;
        if (farmName.isEmpty()) {
            farmNameError.setText("Farm name is required.");
            valid = false;
        }
        if (budget == null || water == null || fertilizer == null || land == null) {
            valid = false;
        }

        NasaPowerClient.Location location = parseLocation();
        if (location == null && (!latitudeField.getText().isBlank()
                || !longitudeField.getText().isBlank())) {
            valid = false;
        }
        if (!valid) {
            statusLabel.setText("Fix the highlighted fields to continue.");
            return;
        }

        Resource resource = new Resource(water, fertilizer, budget, land);
        Farm farm = new Farm(resource);
        AppSession session = AppSession.getInstance();
        session.prepareFarm(farmName, farm);
        session.setLocation(location);
        session.setSeasonGoal(yieldGoalRadio.isSelected() ? SeasonGoal.MAXIMIZE_YIELD
                : conserveGoalRadio.isSelected() ? SeasonGoal.CONSERVE_RESOURCES
                : SeasonGoal.MAXIMIZE_ROI);

        FarmRepository.insert(farmName, farm).ifPresent(session::setFarmId);

        if (location != null) {
            statusLabel.setText("Farm ready. Real NASA POWER weather enabled. Opening crop selection...");
        } else {
            statusLabel.setText("Farm ready. Opening crop selection...");
        }
        connectionStatusLabel.setText("CSV ready · " + Database.statusLabel());

        nextButton.requestFocus();
        Platform.runLater(() -> SceneNavigator.switchTo(nextButton, "/fxml/CropSelectionScreen.fxml"));
    }

    private NasaPowerClient.Location parseLocation() {
        String latText = latitudeField.getText() == null ? "" : latitudeField.getText().trim();
        String lonText = longitudeField.getText() == null ? "" : longitudeField.getText().trim();
        latitudeError.setText("");
        longitudeError.setText("");
        locationError.setText("");

        if (latText.isEmpty() && lonText.isEmpty()) {
            return null;
        }
        if (latText.isEmpty() || lonText.isEmpty()) {
            locationError.setText("Provide both latitude and longitude to enable live weather.");
            return null;
        }
        try {
            double lat = Double.parseDouble(latText);
            double lon = Double.parseDouble(lonText);
            if (lat < -90.0 || lat > 90.0) {
                latitudeError.setText("Latitude must be between -90 and 90.");
                return null;
            }
            if (lon < -180.0 || lon > 180.0) {
                longitudeError.setText("Longitude must be between -180 and 180.");
                return null;
            }
            return new NasaPowerClient.Location(lat, lon);
        } catch (NumberFormatException exception) {
            latitudeError.setText("Enter a valid number.");
            longitudeError.setText("Enter a valid number.");
            return null;
        }
    }

    private Double parsePositive(TextField field, Label errorLabel, String message) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (value <= 0.0) {
                errorLabel.setText(message);
                return null;
            }
            return value;
        } catch (Exception exception) {
            errorLabel.setText(message);
            return null;
        }
    }

    private void clearErrors() {
        farmNameError.setText("");
        budgetError.setText("");
        waterError.setText("");
        fertilizerError.setText("");
        landError.setText("");
        latitudeError.setText("");
        longitudeError.setText("");
        locationError.setText("");
    }
}
