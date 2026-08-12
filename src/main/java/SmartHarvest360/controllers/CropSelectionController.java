package SmartHarvest360.controllers;

import SmartHarvest360.CSVFileHandler;
import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.db.CropRepository;
import SmartHarvest360.db.Database;
import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.ml.WekaAdvisorService;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Browse/edit crops.csv, plant multiple crops, and apply Weka advice. */
public class CropSelectionController {

    private static final Path CROPS_CSV = Path.of("data", "crops.csv");

    @FXML private TableView<Crop> cropTable;
    @FXML private TableColumn<Crop, String> adviceCol;
    @FXML private TableColumn<Crop, String> nameCol;
    @FXML private TableColumn<Crop, String> typeCol;
    @FXML private TableColumn<Crop, Integer> growthCol;
    @FXML private TableColumn<Crop, Double> waterCol;
    @FXML private TableColumn<Crop, Double> fertCol;
    @FXML private TableColumn<Crop, Double> yieldCol;
    @FXML private TableColumn<Crop, Double> costCol;
    @FXML private TableColumn<Crop, Double> priceCol;
    @FXML private TableColumn<Crop, Double> profitCol;
    @FXML private ListView<String> plantedList;
    @FXML private Label farmSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label plantedCountLabel;
    @FXML private Label advisorStatusLabel;
    @FXML private Label recommendedCropLabel;
    @FXML private Label fertilizerPlanLabel;
    @FXML private Label predictedGradeLabel;
    @FXML private Label advisorRationaleLabel;
    @FXML private TextField editNameField;
    @FXML private ComboBox<String> editTypeCombo;
    @FXML private TextField editGrowthField;
    @FXML private TextField editWaterField;
    @FXML private TextField editFertField;
    @FXML private TextField editYieldField;
    @FXML private TextField editCostField;
    @FXML private TextField editPriceField;
    @FXML private Button refreshAdviceButton;
    @FXML private Button applyAdviceButton;
    @FXML private Button updateRowButton;
    @FXML private Button saveCatalogButton;
    @FXML private Button removePlantedButton;
    @FXML private Button addButton;
    @FXML private Button backButton;
    @FXML private Button startButton;

    private final CSVFileHandler csvFileHandler = new CSVFileHandler();
    private final ObservableList<Crop> catalog = FXCollections.observableArrayList();
    private AppSession session;
    private Farm farm;
    private String recommendedCropName = "";

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        ensureFarmReady();
        farm = session.getFarm();

        String farmName = session.getFarmName() == null ? "Your Farm" : session.getFarmName();
        Resource resource = farm.getResource();
        FarmProfile profile = session.getFarmProfile();
        String place = profile == null
                ? "Malaysia"
                : profile.getLocation() + " · " + profile.getSoilType();
        farmSummaryLabel.setText(String.format(
                Locale.US,
                "%s · %s · Budget RM %,.2f · Water %.0f L · Fertilizer %.0f kg",
                farmName, place, resource.getBudget(), resource.getWater(), resource.getFertilizer()
        ));
        dbStatusLabel.setText("CSV ready · " + Database.statusLabel());

        editTypeCombo.setItems(FXCollections.observableArrayList("Vegetable", "Fruit"));
        configureTable();
        plantedList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        loadCatalog();
        refreshPlantedList();
        refreshAdvice(true);
    }

    @FXML
    private void handleBackToSetup() {
        SceneNavigator.switchTo(backButton, "/fxml/FarmSetupScreen.fxml");
    }

    @FXML
    private void handleRefreshAdvice() {
        refreshAdvice(true);
        statusLabel.setText("Advice refreshed for your location, soil, and resources.");
    }

    @FXML
    private void handleApplyAdvice() {
        refreshAdvice(true);
        Crop recommended = findByName(recommendedCropName);
        if (recommended == null) {
            statusLabel.setText("No matching catalog crop for recommendation: " + recommendedCropName);
            return;
        }
        selectCropInTable(recommended);
        boolean planted = plantCrop(recommended);
        if (planted) {
            statusLabel.setText("Applied advice: selected and planted " + recommended.getName() + ".");
        } else {
            statusLabel.setText("Applied advice: " + recommended.getName()
                    + " selected (already on the farm).");
        }
        refreshAdvice(false);
    }

    @FXML
    private void handleAddToFarm() {
        List<Crop> selected = new ArrayList<>(cropTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            statusLabel.setText("Select one or more crops in the table first (Ctrl/Cmd-click).");
            return;
        }

        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (Crop crop : selected) {
            if (plantCrop(crop)) {
                added++;
            } else {
                skipped.add(crop.getName());
            }
        }

        if (added == 0) {
            statusLabel.setText("Already planted: " + String.join(", ", skipped));
            return;
        }
        String msg = "Added " + added + " crop" + (added == 1 ? "" : "s") + " to the farm.";
        if (!skipped.isEmpty()) {
            msg += " Skipped (already planted): " + String.join(", ", skipped) + ".";
        }
        statusLabel.setText(msg);
    }

    @FXML
    private void handleRemovePlanted() {
        int index = plantedList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= farm.getCrops().size()) {
            statusLabel.setText("Select a planted crop to remove.");
            return;
        }
        Crop removed = farm.getCrops().remove(index);
        refreshPlantedList();
        statusLabel.setText("Removed " + removed.getName() + " from the farm.");
    }

    @FXML
    private void handleUpdateRow() {
        Crop selected = cropTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a catalog row to update.");
            return;
        }
        try {
            String name = editNameField.getText().trim();
            String type = editTypeCombo.getSelectionModel().getSelectedItem();
            int growth = Integer.parseInt(editGrowthField.getText().trim());
            double water = Double.parseDouble(editWaterField.getText().trim());
            double fert = Double.parseDouble(editFertField.getText().trim());
            double yield = Double.parseDouble(editYieldField.getText().trim());
            double cost = Double.parseDouble(editCostField.getText().trim());
            double price = Double.parseDouble(editPriceField.getText().trim());

            if (name.isEmpty() || type == null || type.isBlank()) {
                statusLabel.setText("Name and type are required.");
                return;
            }
            if (growth <= 0 || water <= 0 || fert < 0 || yield <= 0 || cost < 0 || price <= 0) {
                statusLabel.setText("Use positive values for growth, water, yield, and price.");
                return;
            }

            boolean nameTaken = catalog.stream()
                    .anyMatch(crop -> crop != selected && crop.getName().equalsIgnoreCase(name));
            if (nameTaken) {
                statusLabel.setText("Another crop already uses the name " + name + ".");
                return;
            }

            int index = catalog.indexOf(selected);
            Crop updated = CSVFileHandler.createCrop(
                    name, type, growth, water, fert, yield, cost, price);
            catalog.set(index, updated);
            cropTable.getSelectionModel().clearSelection();
            cropTable.getSelectionModel().select(updated);
            cropTable.refresh();
            fillEditor(updated);
            statusLabel.setText("Row updated in memory. Click Save catalog to write CSV + MySQL.");
        } catch (NumberFormatException exception) {
            statusLabel.setText("Enter valid numbers in the edit fields.");
        }
    }

    @FXML
    private void handleSaveCatalog() {
        try {
            commitTableEdits();
            List<Crop> rebuilt = new ArrayList<>();
            for (Crop crop : catalog) {
                rebuilt.add(CSVFileHandler.createCrop(
                        crop.getName(),
                        crop.getType(),
                        crop.getGrowthDays(),
                        crop.getWaterNeed(),
                        crop.getFertilizerNeed(),
                        crop.getYieldAmount(),
                        crop.getCostPerKg(),
                        crop.getMarketPrice()
                ));
            }
            catalog.setAll(rebuilt);
            csvFileHandler.saveCrops(CROPS_CSV.toString(), rebuilt);
            CropRepository.upsertAll(rebuilt);
            dbStatusLabel.setText("CSV saved · " + Database.statusLabel());
            statusLabel.setText("Catalog saved to data/crops.csv"
                    + (Database.isAvailable() ? " and upserted to MySQL crops table." : " (MySQL offline)."));
            cropTable.refresh();
            refreshAdvice(false);
        } catch (IOException exception) {
            statusLabel.setText("Could not save crops.csv: " + exception.getMessage());
        }
    }

    @FXML
    private void handleStartSimulation() {
        if (farm.getCrops().isEmpty()) {
            statusLabel.setText("Add at least one crop before starting the simulation.");
            return;
        }

        Crop active = resolveActiveCrop();
        AdvisorResult advice = WekaAdvisorService.getInstance().advise(
                session.getFarmProfile(), farm.getResource(), active);
        session.setAdvisorResult(advice);
        session.startSimulation(farm, active);
        SceneNavigator.switchTo(startButton, "/fxml/SimulationScreen.fxml");
    }

    private void configureTable() {
        cropTable.setItems(catalog);
        cropTable.setEditable(true);
        cropTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cropTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        adviceCol.setCellValueFactory(cell -> new SimpleStringProperty(
                isRecommended(cell.getValue()) ? "★" : ""
        ));
        adviceCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: 800;");
        adviceCol.setEditable(false);

        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue().trim());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        typeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        typeCol.setOnEditCommit(event -> {
            String type = event.getNewValue() == null ? "" : event.getNewValue().trim();
            if (!type.equalsIgnoreCase("Vegetable") && !type.equalsIgnoreCase("Fruit")) {
                statusLabel.setText("Type must be Vegetable or Fruit.");
                cropTable.refresh();
                return;
            }
            event.getRowValue().setType(type.equalsIgnoreCase("Fruit") ? "Fruit" : "Vegetable");
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        growthCol.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getGrowthDays()).asObject());
        growthCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        growthCol.setOnEditCommit(event -> {
            event.getRowValue().setGrowthDays(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        waterCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getWaterNeed()).asObject());
        waterCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        waterCol.setOnEditCommit(event -> {
            event.getRowValue().setWaterNeed(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        fertCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getFertilizerNeed()).asObject());
        fertCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        fertCol.setOnEditCommit(event -> {
            event.getRowValue().setFertilizerNeed(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        yieldCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getYieldAmount()).asObject());
        yieldCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        yieldCol.setOnEditCommit(event -> {
            event.getRowValue().setYieldAmount(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        costCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getCostPerKg()).asObject());
        costCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        costCol.setOnEditCommit(event -> {
            event.getRowValue().setCostPerKg(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        priceCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getMarketPrice()).asObject());
        priceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        priceCol.setOnEditCommit(event -> {
            event.getRowValue().setMarketPrice(event.getNewValue());
            cropTable.refresh();
            fillEditor(event.getRowValue());
        });

        profitCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(expectedProfit(cell.getValue())).asObject());
        profitCol.setEditable(false);
        profitCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(String.format(Locale.US, "RM %,.2f", value));
            }
        });

        cropTable.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> fillEditor(selected)
        );
        cropTable.setRowFactory(table -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(Crop crop, boolean empty) {
                super.updateItem(crop, empty);
                if (empty || crop == null) {
                    getStyleClass().remove("recommended-row");
                    return;
                }
                if (isRecommended(crop)) {
                    if (!getStyleClass().contains("recommended-row")) {
                        getStyleClass().add("recommended-row");
                    }
                } else {
                    getStyleClass().remove("recommended-row");
                }
            }
        });
    }

    private void refreshAdvice(boolean selectRecommended) {
        WekaAdvisorService advisor = WekaAdvisorService.getInstance();
        // Always score from farm profile/resources so refresh is not stuck on the current row.
        AdvisorResult advice = advisor.advise(session.getFarmProfile(), farm.getResource(), null);
        session.setAdvisorResult(advice);
        recommendedCropName = advice.getRecommendedCrop() == null ? "" : advice.getRecommendedCrop();
        Crop catalogMatch = findByName(recommendedCropName);
        String displayCrop = catalogMatch != null ? catalogMatch.getName() : recommendedCropName;

        advisorStatusLabel.setText(advisor.getStatusMessage());
        recommendedCropLabel.setText(displayCrop.isBlank() ? "-" : displayCrop);
        fertilizerPlanLabel.setText(advice.getFertilizerPlan()
                + " - " + advice.getFertilizerKgTip());
        predictedGradeLabel.setText(advice.getPredictedGrade() == null ? "-" : advice.getPredictedGrade());
        advisorRationaleLabel.setText(advice.getRationale());

        if (selectRecommended) {
            if (catalogMatch != null) {
                selectCropInTable(catalogMatch);
            }
        }
        cropTable.refresh();
    }

    private void selectCropInTable(Crop crop) {
        cropTable.getSelectionModel().clearSelection();
        int index = catalog.indexOf(crop);
        if (index < 0) {
            for (int i = 0; i < catalog.size(); i++) {
                if (catalog.get(i).getName().equalsIgnoreCase(crop.getName())) {
                    index = i;
                    crop = catalog.get(i);
                    break;
                }
            }
        }
        if (index >= 0) {
            cropTable.getSelectionModel().select(index);
            cropTable.scrollTo(index);
            fillEditor(crop);
        }
    }

    private boolean plantCrop(Crop crop) {
        boolean alreadyPlanted = farm.getCrops().stream()
                .anyMatch(existing -> existing.getName().equalsIgnoreCase(crop.getName()));
        if (alreadyPlanted) {
            return false;
        }
        farm.addCrop(CSVFileHandler.copyOf(crop));
        refreshPlantedList();
        return true;
    }

    private Crop resolveActiveCrop() {
        Crop recommended = farm.getCrops().stream()
                .filter(crop -> WekaAdvisorService.namesMatch(crop.getName(), recommendedCropName))
                .findFirst()
                .orElse(null);
        if (recommended != null) {
            return recommended;
        }
        Crop selected = cropTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return farm.getCrops().stream()
                    .filter(crop -> crop.getName().equalsIgnoreCase(selected.getName()))
                    .findFirst()
                    .orElse(farm.getCrops().get(0));
        }
        return farm.getCrops().get(0);
    }

    private void loadCatalog() {
        try {
            List<Crop> loaded = csvFileHandler.loadCrops(CROPS_CSV.toString());
            catalog.setAll(loaded);
            CropRepository.upsertAll(loaded);
            dbStatusLabel.setText("CSV ready · " + Database.statusLabel());
            if (!catalog.isEmpty()) {
                cropTable.getSelectionModel().selectFirst();
                fillEditor(catalog.get(0));
            }
            statusLabel.setText("Loaded " + catalog.size() + " crops from CSV · " + Database.statusLabel());
        } catch (IOException exception) {
            statusLabel.setText("Could not load crops.csv: " + exception.getMessage());
            addButton.setDisable(true);
            startButton.setDisable(true);
            applyAdviceButton.setDisable(true);
        }
    }

    private void fillEditor(Crop crop) {
        if (crop == null) {
            editNameField.clear();
            editTypeCombo.getSelectionModel().clearSelection();
            editGrowthField.clear();
            editWaterField.clear();
            editFertField.clear();
            editYieldField.clear();
            editCostField.clear();
            editPriceField.clear();
            return;
        }
        editNameField.setText(crop.getName());
        editTypeCombo.getSelectionModel().select(crop.getType());
        editGrowthField.setText(String.valueOf(crop.getGrowthDays()));
        editWaterField.setText(formatNumber(crop.getWaterNeed()));
        editFertField.setText(formatNumber(crop.getFertilizerNeed()));
        editYieldField.setText(formatNumber(crop.getYieldAmount()));
        editCostField.setText(formatNumber(crop.getCostPerKg()));
        editPriceField.setText(formatNumber(crop.getMarketPrice()));
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
        if (removePlantedButton != null) {
            removePlantedButton.setDisable(count == 0);
        }
    }

    private void commitTableEdits() {
        if (cropTable.getEditingCell() != null) {
            cropTable.edit(-1, null);
        }
    }

    private Crop findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        return catalog.stream()
                .filter(crop -> WekaAdvisorService.namesMatch(crop.getName(), trimmed))
                .findFirst()
                .orElse(null);
    }

    private boolean isRecommended(Crop crop) {
        return crop != null
                && recommendedCropName != null
                && !recommendedCropName.isBlank()
                && WekaAdvisorService.namesMatch(crop.getName(), recommendedCropName);
    }

    private static double expectedProfit(Crop crop) {
        double bonus = crop.calculateGrowthBonus();
        double revenue = crop.getYieldAmount() * crop.getMarketPrice() * bonus;
        double cost = crop.getYieldAmount() * crop.getCostPerKg();
        return revenue - cost;
    }

    private static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void ensureFarmReady() {
        if (session.getFarm() != null) {
            if (session.getFarmProfile() == null) {
                session.setFarmProfile(new FarmProfile("Selangor", "Loam"));
            }
            return;
        }
        Resource resource = new Resource(200.0, 20.0, 10_000.0, 5.0);
        session.prepareFarm("Unnamed Farm", new Farm(resource));
        session.setFarmProfile(new FarmProfile("Selangor", "Loam"));
    }
}
