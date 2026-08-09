package SmartHarvest360.controllers;

import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.db.Database;
import SmartHarvest360.db.FarmRepository;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Collects starting farm resources and creates the Farm session. */
public class FarmSetupController {

    @FXML private TextField farmNameField;
    @FXML private TextField budgetField;
    @FXML private TextField waterField;
    @FXML private TextField fertilizerField;
    @FXML private TextField landField;
    @FXML private Label farmNameError;
    @FXML private Label budgetError;
    @FXML private Label waterError;
    @FXML private Label fertilizerError;
    @FXML private Label landError;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Button nextButton;

    @FXML
    public void initialize() {
        connectionStatusLabel.setText("CSV ready · " + Database.statusLabel());
        if (AppSession.getInstance().getFarmName() != null) {
            farmNameField.setText(AppSession.getInstance().getFarmName());
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

        boolean valid = true;
        if (farmName.isEmpty()) {
            farmNameError.setText("Farm name is required.");
            valid = false;
        }
        if (budget == null || water == null || fertilizer == null || land == null) {
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
    }
}
