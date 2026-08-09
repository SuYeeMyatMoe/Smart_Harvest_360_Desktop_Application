package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Controls market comparison and completes the crop sale. */
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
    @FXML private Button sellButton;

    private AppSession session;
    private Crop crop;
    private String bestMarket;
    private double bestPrice;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        session.ensureDemoData();
        crop = session.getActiveCrop();

        cropLabel.setText(crop.getName());
        quantityField.setText(String.format(Locale.US, "%.2f", crop.getYieldAmount()));

        Map<String, Double> prices = buildSampleMarketPrices(crop.getMarketPrice());
        bestMarket = prices.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
        bestPrice = prices.get(bestMarket);

        prices.forEach((market, price) -> marketList.getItems().add(
                String.format(Locale.US, "%s  -  RM %.2f/kg%s",
                        market, price, market.equals(bestMarket) ? "  (Best)" : "")
        ));

        bestMarketLabel.setText(bestMarket);
        bestPriceLabel.setText(String.format(Locale.US, "RM %.2f / kg", bestPrice));
        quantityField.textProperty().addListener((ignored, oldValue, newValue) -> updateTotals());
        updateTotals();
    }

    @FXML
    private void handleSell() {
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
            sellButton.setDisable(true);
            statusLabel.setText("Sale saved successfully.");
            SceneNavigator.switchTo(sellButton, "/fxml/SeasonReportScreen.fxml");
        } catch (IOException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("The sale could not be saved");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private void updateTotals() {
        Double quantity = readQuantity();
        if (quantity == null) {
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
        statusLabel.setText("Ready to complete the sale.");
    }

    private Double readQuantity() {
        try {
            double quantity = Double.parseDouble(quantityField.getText().trim());
            return quantity > 0.0 ? quantity : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, Double> buildSampleMarketPrices(double referencePrice) {
        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Local Market", referencePrice);
        prices.put("Farm Cooperative", round(referencePrice * 0.97));
        prices.put("Wholesale Buyer", round(referencePrice * 0.94));
        return prices;
    }

    private void showValidation(String message) {
        statusLabel.setText(message);
        quantityField.requestFocus();
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
