package SmartHarvest360.controllers;

import SmartHarvest360.CSVFileHandler;
import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.data.DataPaths;
import SmartHarvest360.db.CropRepository;
import SmartHarvest360.db.Database;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
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
    @FXML private Label recommendedBadge;
    @FXML private Label growthBonusLabel;
    @FXML private Label marketPriceLabel;
    @FXML private Label expectedProfitLabel;
    @FXML private Label yieldLabel;
    @FXML private Label charTypeLabel;
    @FXML private Label growthDaysLabel;
    @FXML private Label charWaterLabel;
    @FXML private Label charFertilizerLabel;
    @FXML private Label plantedCountLabel;
    @FXML private Label plantedBudgetLabel;
    @FXML private Label plantedLandLabel;
    @FXML private Button addButton;
    @FXML private Button startButton;
    @FXML private Button themeButton;
    @FXML private Button fieldsButton;

    private final CSVFileHandler csvFileHandler = new CSVFileHandler();
    private AppSession session;
    private Farm farm;
    private final List<Crop> catalog = new ArrayList<>();
    private String bestCropName;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
        ensureFarmReady();
        farm = session.getFarm();

        setupCropList();

        String farmerName = session.getFarmerName();
        String greeting = (farmerName == null || farmerName.isBlank()) ? "" : "Good day, " + farmerName + " · ";
        String farmName = session.getFarmName() == null ? "Your Farm" : session.getFarmName();
        Resource resource = farm.getResource();
        farmSummaryLabel.setText(greeting + String.format(
                Locale.US,
                "%s · Budget RM %,.2f · Water %.0f L · Fertilizer %.0f kg · Land %.0f acres",
                farmName, resource.getBudget(), resource.getWater(),
                resource.getFertilizer(), resource.getLand()
        ));

        Tooltip.install(addButton, new Tooltip("Plant the selected crop. Each crop costs its planting cost and uses 1 acre of land."));
        Tooltip.install(startButton, new Tooltip("Start the season with every crop planted on the farm."));

        cropList.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> showCropDetails(selected)
        );

        loadCatalog();
        refreshPlantedList();
    }

    private void setupCropList() {
        cropList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Crop crop, boolean empty) {
                super.updateItem(crop, empty);
                if (empty || crop == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(buildCropRow(crop));
            }
        });
    }

    private Node buildCropRow(Crop crop) {
        Label icon = new Label();
        icon.getStyleClass().addAll("crop-icon", styleClassFor(crop.getType()));
        icon.setGraphic(SmartHarvest360.ui.IconFactory.cropIcon(crop.getType()));

        Label name = new Label(crop.getName());
        name.getStyleClass().add("crop-name");

        HBox nameLine = new HBox(6, name);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        if (crop.getName().equals(bestCropName)) {
            Label badge = new Label("BEST ROI");
            badge.getStyleClass().add("recommendation-badge");
            nameLine.getChildren().add(badge);
        }

        Label type = new Label(crop.getType());
        type.getStyleClass().add("crop-category");

        VBox info = new VBox(3, nameLine, type);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label price = new Label(String.format(Locale.US, "RM%.2f", crop.getMarketPrice()));
        price.getStyleClass().add("crop-price");
        Label unit = new Label("per kg");
        unit.getStyleClass().add("crop-price-sub");

        VBox priceBox = new VBox(1, price, unit);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(10, icon, info, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("crop-row");
        return row;
    }

    private String styleClassFor(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (t.contains("vegetable")) {
            return "crop-icon-vegetable";
        }
        if (t.contains("fruit")) {
            return "crop-icon-fruit";
        }
        if (t.contains("grain")) {
            return "crop-icon-grain";
        }
        return "crop-icon-default";
    }

    private void computeBestCrop() {
        bestCropName = null;
        double budget = farm.getResource().getBudget();
        double bestRatio = Double.NEGATIVE_INFINITY;
        for (Crop crop : catalog) {
            double cost = crop.getPlantingCost();
            if (cost <= 0.0 || cost > budget) {
                continue;
            }
            double ratio = expectedProfit(crop) / cost;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestCropName = crop.getName();
            }
        }
        if (bestRatio <= 0.0) {
            bestCropName = null;
        }
    }

    private double expectedProfit(Crop crop) {
        double bonus = crop.calculateGrowthBonus();
        return crop.getYieldAmount() * crop.getMarketPrice() * bonus
                - crop.getYieldAmount() * crop.getCostPerKg();
    }

    private void showCropDetails(Crop crop) {
        if (crop == null) {
            selectedNameLabel.setText("No crop selected");
            selectedTypeLabel.setText("");
            growthBonusLabel.setText("—");
            marketPriceLabel.setText("—");
            expectedProfitLabel.setText("—");
            yieldLabel.setText("—");
            charTypeLabel.setText("—");
            growthDaysLabel.setText("—");
            charWaterLabel.setText("—");
            charFertilizerLabel.setText("—");
            recommendedBadge.setVisible(false);
            recommendedBadge.setManaged(false);
            return;
        }

        double bonus = crop.calculateGrowthBonus();
        double profit = expectedProfit(crop);

        selectedNameLabel.setText(crop.getName());
        selectedTypeLabel.setText("[" + crop.getType() + "]");
        growthBonusLabel.setText(String.format(Locale.US, "%.2fx", bonus));
        marketPriceLabel.setText(String.format(Locale.US, "RM%.2f/kg", crop.getMarketPrice()));
        expectedProfitLabel.setText(String.format(Locale.US, "RM%,.2f", profit));
        yieldLabel.setText(String.format(Locale.US, "%.0f kg", crop.getYieldAmount()));
        charTypeLabel.setText(crop.getType());
        growthDaysLabel.setText(crop.getGrowthDays() + " days");
        charWaterLabel.setText(String.format(Locale.US, "%.0f L", crop.getWaterNeed()));
        charFertilizerLabel.setText(String.format(Locale.US, "%.0f kg", crop.getFertilizerNeed()));

        boolean isBest = crop.getName().equals(bestCropName);
        recommendedBadge.setVisible(isBest);
        recommendedBadge.setManaged(isBest);
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

        boolean planted = farm.plantCrop(selected);
        refreshPlantedList();
        if (planted) {
            statusLabel.setText("Crop Added Successfully! " + selected.getName() + " has been added to the farm.");
        } else {
            statusLabel.setText("Not enough budget or land to plant " + selected.getName() + ".");
        }
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
            List<Crop> loaded = csvFileHandler.loadCrops(DataPaths.cropsFile().toString());
            catalog.clear();
            catalog.addAll(loaded);
            computeBestCrop();
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

    private void refreshPlantedList() {
        plantedList.setPlaceholder(new Label("No crops planted yet."));
        plantedList.getItems().setAll(
                farm.getCrops().stream()
                        .map(crop -> crop.getName() + " (" + crop.getType() + ")")
                        .toList()
        );
        int count = farm.getCrops().size();
        Resource resource = farm.getResource();
        plantedCountLabel.setText(String.valueOf(count));
        plantedBudgetLabel.setText(String.format(Locale.US, "RM%,.2f", resource.getBudget()));
        plantedLandLabel.setText(String.format(Locale.US, "%.1f acres", resource.getLand()));
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
