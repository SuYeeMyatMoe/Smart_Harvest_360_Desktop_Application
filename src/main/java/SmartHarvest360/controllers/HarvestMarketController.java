package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.Market;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.db.SaleRepository;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Controls market comparison and completes the sale for every ready crop. */
public class HarvestMarketController {
    @FXML private Label cropLabel;
    @FXML private Label bestMarketLabel;
    @FXML private Label bestPriceLabel;
    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label statusLabel;
    @FXML private TextField quantityField;
    @FXML private ListView<String> marketList;
    @FXML private ComboBox<Crop> cropCombo;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private Button sellButton;
    @FXML private Button themeButton;
    @FXML private Button fieldsButton;

    private AppSession session;
    private Crop crop;
    private String bestMarket;
    private double bestPrice;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
        session.ensureDemoData();

        List<Crop> ready = new ArrayList<>();
        for (SmartHarvest360.SimulationEngine engine : session.getUnsoldReadyCrops()) {
            ready.add(engine.getCrop());
        }

        cropCombo.setItems(FXCollections.observableArrayList(ready));
        cropCombo.setButtonCell(cropCell());
        cropCombo.setCellFactory(list -> cropCell());
        cropCombo.valueProperty().addListener((obs, oldValue, selected) -> showCrop(selected));

        if (ready.isEmpty()) {
            statusLabel.setText("No ready crops to sell yet. Run the season first.");
            sellButton.setDisable(true);
            return;
        }
        quantityField.textProperty().addListener((ignored, oldValue, newValue) -> updateTotals());
        cropCombo.getSelectionModel().selectFirst();
    }

    private ListCell<Crop> cropCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Crop item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getType() + ")");
                }
            }
        };
    }

    private void showCrop(Crop selected) {
        if (selected == null) {
            return;
        }
        crop = selected;
        cropLabel.setText(selected.getName());
        quantityField.setText(String.format(Locale.US, "%.2f", selected.getYieldAmount()));

        Map<String, Double> prices = currentPricesFor(selected);
        bestMarket = prices.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Local Market");
        bestPrice = prices.get(bestMarket);

        marketList.getItems().clear();
        prices.forEach((market, price) -> marketList.getItems().add(
                String.format(Locale.US, "%s  -  RM %.2f/kg%s",
                        market, price, market.equals(bestMarket) ? "  (Best)" : "")
        ));

        bestMarketLabel.setText(bestMarket);
        bestPriceLabel.setText(String.format(Locale.US, "RM %.2f / kg", bestPrice));
        updateTotals();
        buildChart(selected);
    }

    private Map<String, Double> currentPricesFor(Crop selected) {
        Map<String, Double> prices = session.getSeason().getCurrentPrices(selected.getName());
        return prices != null ? prices : new Market().getMarketPrices(selected);
    }

    private void buildChart(Crop selected) {
        if (priceChart == null) {
            return;
        }
        priceChart.getData().clear();
        Map<String, XYChart.Series<Number, Number>> seriesMap = new LinkedHashMap<>();
        for (SeasonSimulator.DaySnapshot snapshot : session.getSeason().getPriceHistory()) {
            Map<String, Double> cropPrices = snapshot.pricesByCrop().get(selected.getName());
            if (cropPrices == null) {
                continue;
            }
            for (Map.Entry<String, Double> entry : cropPrices.entrySet()) {
                XYChart.Series<Number, Number> series = seriesMap.computeIfAbsent(
                        entry.getKey(),
                        name -> {
                            XYChart.Series<Number, Number> created = new XYChart.Series<>();
                            created.setName(name);
                            return created;
                        });
                series.getData().add(new XYChart.Data<>(snapshot.day(), entry.getValue()));
            }
        }
        priceChart.getData().addAll(seriesMap.values());
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
    private void handleSell() {
        if (crop == null) {
            return;
        }
        Double quantity = readQuantity();
        if (quantity == null) {
            showValidation("Enter a valid quantity greater than zero.");
            return;
        }
        if (quantity > crop.getYieldAmount()) {
            showValidation(String.format(Locale.US,
                    "Available harvest is only %.2f kg.", crop.getYieldAmount()));
            return;
        }

        double revenue = quantity * bestPrice;
        double cost = quantity * crop.getCostPerKg();
        SaleRecord sale = new SaleRecord(
                session.getCurrentDay(), crop.getName(), quantity, bestMarket,
                bestPrice, revenue, cost
        );

        try {
            CsvDataStore.appendHarvest(sale);
            session.addSale(sale);
            SaleRepository.insert(session.getFarmId(), sale);
            session.markSold(crop.getName());

            if (session.getUnsoldReadyCrops().isEmpty()) {
                statusLabel.setText("All crops sold. Opening the season report...");
                SceneNavigator.switchTo(sellButton, "/fxml/SeasonReportScreen.fxml");
                return;
            }
            cropCombo.getItems().remove(crop);
            if (!cropCombo.getItems().isEmpty()) {
                cropCombo.getSelectionModel().selectFirst();
            }
            statusLabel.setText("Sale saved. Select the next crop to sell.");
        } catch (IOException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("The sale could not be saved");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private void updateTotals() {
        Double quantity = readQuantity();
        if (quantity == null || crop == null) {
            revenueLabel.setText("RM 0.00");
            costLabel.setText("RM 0.00");
            profitLabel.setText("RM 0.00");
            return;
        }

        double revenue = quantity * bestPrice;
        double cost = quantity * crop.getCostPerKg();
        revenueLabel.setText(money(revenue));
        costLabel.setText(money(cost));
        profitLabel.setText(money(revenue - cost));
    }

    private Double readQuantity() {
        try {
            double quantity = Double.parseDouble(quantityField.getText().trim());
            return quantity > 0.0 ? quantity : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void showValidation(String message) {
        statusLabel.setText(message);
        quantityField.requestFocus();
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }
}
