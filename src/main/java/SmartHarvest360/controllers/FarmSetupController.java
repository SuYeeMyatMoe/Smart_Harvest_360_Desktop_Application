package SmartHarvest360.controllers;

import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.db.Database;
import SmartHarvest360.db.FarmRepository;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Collects starting farm resources and creates the Farm session. */
public class FarmSetupController {

    @FXML private TextField farmNameField;
    @FXML private TextField budgetField;
    @FXML private TextField waterField;
    @FXML private TextField fertilizerField;
    @FXML private TextField landField;
    @FXML private ComboBox<String> locationCombo;
    @FXML private ComboBox<String> soilCombo;
    @FXML private Label farmNameError;
    @FXML private Label budgetError;
    @FXML private Label waterError;
    @FXML private Label fertilizerError;
    @FXML private Label landError;
    @FXML private Label locationError;
    @FXML private Label soilError;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Button nextButton;

    @FXML
    public void initialize() {
        connectionStatusLabel.setText("CSV ready · " + Database.statusLabel());
        locationCombo.setItems(FXCollections.observableArrayList(FarmProfile.MALAYSIA_STATES));
        soilCombo.setItems(FXCollections.observableArrayList(FarmProfile.SOIL_TYPES));
        locationCombo.getSelectionModel().select("Selangor");
        soilCombo.getSelectionModel().select("Loam");

        AppSession session = AppSession.getInstance();
        if (session.getFarmName() != null) {
            farmNameField.setText(session.getFarmName());
        }
        if (session.getFarmProfile() != null) {
            locationCombo.getSelectionModel().select(session.getFarmProfile().getLocation());
            soilCombo.getSelectionModel().select(session.getFarmProfile().getSoilType());
        }
    }

    @FXML
    private void handleNext() {
        clearErrors();

        String farmName = farmNameField.getText() == null ? "" : farmNameField.getText().trim();
        Double budget = parsePositive(budgetField, budgetError, "Enter a budget greater than zero.");
        Double water = parsePositive(waterField, waterError, "Enter water greater than zero.");
        Double fertilizer = parsePositive(fertilizerField, fertilizerError, "Enter fertilizer greater than zero.");
        Double land = parsePositive(landField, landError, "Enter land greater than zero.");
        String location = locationCombo.getSelectionModel().getSelectedItem();
        String soil = soilCombo.getSelectionModel().getSelectedItem();

        boolean valid = true;
        if (farmName.isEmpty()) {
            farmNameError.setText("Farm name is required.");
            valid = false;
        }
        if (budget == null || water == null || fertilizer == null || land == null) {
            valid = false;
        }
        if (location == null || location.isBlank()) {
            locationError.setText("Select a Malaysia state.");
            valid = false;
        }
        if (soil == null || soil.isBlank()) {
            soilError.setText("Select a soil type.");
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
        session.setFarmProfile(new FarmProfile(location, soil));

        FarmRepository.insert(farmName, farm).ifPresent(session::setFarmId);

        statusLabel.setText("Farm ready. Opening crop selection...");
        connectionStatusLabel.setText("CSV ready · " + Database.statusLabel());
        SceneNavigator.switchTo(nextButton, "/fxml/CropSelectionScreen.fxml");
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
        locationError.setText("");
        soilError.setText("");
    }
}
