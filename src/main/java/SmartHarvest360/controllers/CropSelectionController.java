package SmartHarvest360.controllers;

import SmartHarvest360.CSVFileHandler;
import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.db.CropRepository;
import SmartHarvest360.db.Database;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lets the farmer browse crops.csv and plant selections on the farm. */
public class CropSelectionController {

    @FXML private ListView<Crop> cropList;
    @FXML private ListView<String> plantedList;
    @FXML private Label farmSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label selectedNameLabel;
    @FXML private Label selectedTypeLabel;
    @FXML private Label growthBonusLabel;
    @FXML private Label marketPriceLabel;
    @FXML private Label expectedProfitLabel;
    @FXML private Label growthDaysLabel;
    @FXML private Label plantedCountLabel;
    @FXML private Button addButton;
    @FXML private Button startButton;

    private final CSVFileHandler csvFileHandler = new CSVFileHandler();
    private AppSession session;
    private Farm farm;
    private final List<Crop> catalog = new ArrayList<>();

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        ensureFarmReady();
        farm = session.getFarm();

        String farmName = session.getFarmName() == null ? "Your Farm" : session.getFarmName();
        Resource resource = farm.getResource();
        farmSummaryLabel.setText(String.format(
                Locale.US,
                "%s · Budget RM %,.2f · Water %.0f L · Fertilizer %.0f kg",
                farmName, resource.getBudget(), resource.getWater(), resource.getFertilizer()
        ));

        cropList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Crop crop, boolean empty) {
                super.updateItem(crop, empty);
                if (empty || crop == null) {
                    setText(null);
                    return;
                }
                setText(String.format(
                        Locale.US,
                        "%-10s | %-9s | RM%.2f/kg",
                        crop.getName(), crop.getType(), crop.getMarketPrice()
                ));
            }
        });

        cropList.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> showCropDetails(selected)
        );

        loadCatalog();
        refreshPlantedList();
    }

    @FXML
    private void handleAddToFarm() {
        Crop selected = cropList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a crop from the list first.");
            return;
        }

        boolean alreadyPlanted = farm.getCrops().stream()
                .anyMatch(crop -> crop.getName().equalsIgnoreCase(selected.getName()));
        if (alreadyPlanted) {
            statusLabel.setText(selected.getName() + " is already on the farm.");
            return;
        }

        farm.addCrop(selected);
        refreshPlantedList();
        statusLabel.setText("Crop Added Successfully! " + selected.getName() + " has been added to the farm.");
    }

    @FXML
    private void handleStartSimulation() {
        if (farm.getCrops().isEmpty()) {
            statusLabel.setText("Add at least one crop before starting the simulation.");
            return;
        }

        Crop selected = cropList.getSelectionModel().getSelectedItem();
        Crop active;
        if (selected != null && farm.getCrops().stream()
                .anyMatch(crop -> crop.getName().equalsIgnoreCase(selected.getName()))) {
            active = selected;
        } else {
            active = farm.getCrops().get(0);
        }

        session.startSimulation(farm, active);
        SceneNavigator.switchTo(startButton, "/fxml/SimulationScreen.fxml");
    }

    private void loadCatalog() {
        try {
            List<Crop> loaded = csvFileHandler.loadCrops(Path.of("data", "crops.csv").toString());
            catalog.clear();
            catalog.addAll(loaded);
            cropList.getItems().setAll(catalog);
            CropRepository.upsertAll(catalog);

            if (!catalog.isEmpty()) {
                cropList.getSelectionModel().selectFirst();
            }
            statusLabel.setText("Loaded " + catalog.size() + " crops from CSV · " + Database.statusLabel());
        } catch (IOException exception) {
            statusLabel.setText("Could not load crops.csv: " + exception.getMessage());
            addButton.setDisable(true);
            startButton.setDisable(true);
        }
    }

    private void showCropDetails(Crop crop) {
        if (crop == null) {
            selectedNameLabel.setText("—");
            selectedTypeLabel.setText("Type: —");
            growthBonusLabel.setText("—");
            marketPriceLabel.setText("—");
            expectedProfitLabel.setText("RM 0.00");
            growthDaysLabel.setText("Growth days: —");
            return;
        }

        double bonus = crop.calculateGrowthBonus();
        double revenue = crop.getYieldAmount() * crop.getMarketPrice() * bonus;
        double cost = crop.getYieldAmount() * crop.getCostPerKg();
        double profit = revenue - cost;

        selectedNameLabel.setText(crop.getName());
        selectedTypeLabel.setText("[" + crop.getType() + "]");
        growthBonusLabel.setText(String.format(Locale.US, "%.2fx", bonus));
        marketPriceLabel.setText(String.format(Locale.US, "RM%.2f/kg", crop.getMarketPrice()));
        expectedProfitLabel.setText(String.format(Locale.US, "RM%,.2f", profit));
        growthDaysLabel.setText("Growth days: " + crop.getGrowthDays()
                + " · Yield: " + String.format(Locale.US, "%.0f kg", crop.getYieldAmount()));
    }

    private void refreshPlantedList() {
        plantedList.getItems().setAll(
                farm.getCrops().stream()
                        .map(crop -> crop.getName() + " (" + crop.getType() + ")")
                        .toList()
        );
        int count = farm.getCrops().size();
        plantedCountLabel.setText(count + (count == 1 ? " crop planted" : " crops planted"));
        startButton.setDisable(count == 0);
    }

    private void ensureFarmReady() {
        if (session.getFarm() != null) {
            return;
        }
        Resource resource = new Resource(200.0, 20.0, 10_000.0, 5.0);
        session.prepareFarm("Unnamed Farm", new Farm(resource));
    }
}
