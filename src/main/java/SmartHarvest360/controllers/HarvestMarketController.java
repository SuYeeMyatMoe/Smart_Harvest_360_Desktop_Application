package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.Market;
import SmartHarvest360.MarketComparison;
import SmartHarvest360.MarketOption;
import SmartHarvest360.SalesReport;
import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.db.SaleRepository;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Harvest &amp; Market UI wired to Market, MarketOption, MarketComparison, and SalesReport.
 */
public class HarvestMarketController {
    @FXML private Label cropLabel;
    @FXML private Label availableYieldLabel;
    @FXML private Label bestMarketLabel;
    @FXML private Label bestPriceLabel;
    @FXML private Label recommendationLabel;
    @FXML private Label selectedBuyerLabel;
    @FXML private Label selectedDemandLabel;
    @FXML private Label quantityHintLabel;
    @FXML private Label unitBreakdownLabel;
    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label marginLabel;
    @FXML private Label reportQtyLabel;
    @FXML private Label reportRevenueLabel;
    @FXML private Label reportProfitLabel;
    @FXML private Label reportBestMarketLabel;
    @FXML private Label reportNoteLabel;
    @FXML private Label reportPreviewBadge;
    @FXML private Label statusLabel;
    @FXML private TextField quantityField;
    @FXML private TableView<MarketOption> marketTable;
    @FXML private TableColumn<MarketOption, String> marketNameCol;
    @FXML private TableColumn<MarketOption, Number> priceCol;
    @FXML private TableColumn<MarketOption, String> demandCol;
    @FXML private TableColumn<MarketOption, Number> scoreCol;
    @FXML private Button sellButton;

    private final Market marketService = new Market();
    private final MarketComparison marketComparison = new MarketComparison();
    private final SalesReport salesReport = new SalesReport();

    private AppSession session;
    private Crop crop;
    private MarketOption selectedOption;
    private MarketOption recommendedOption;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        session.ensureDemoData();
        crop = session.getActiveCrop();

        cropLabel.setText(crop.getName());
        availableYieldLabel.setText(String.format(Locale.US, "%.2f kg", crop.getYieldAmount()));
        quantityField.setText(String.format(Locale.US, "%.2f", crop.getYieldAmount()));
        quantityHintLabel.setText(String.format(Locale.US,
                "Max available harvest: %.2f kg", crop.getYieldAmount()));

        List<MarketOption> options = marketService.getMarketOptions(crop);
        recommendedOption = marketComparison.findBestMarket(options);
        selectedOption = recommendedOption;

        configureTable();
        marketTable.setItems(FXCollections.observableArrayList(options));
        if (recommendedOption != null) {
            marketTable.getSelectionModel().select(recommendedOption);
        }

        applySelectedOption(selectedOption);
        quantityField.textProperty().addListener((ignored, oldValue, newValue) -> {
            updateTotals();
            refreshSalesSnapshot();
        });
        updateTotals();
        refreshSalesSnapshot();
    }

    private void configureTable() {
        marketNameCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMarketName()));
        priceCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getPricePerKg()));
        demandCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDemand()));
        scoreCol.setCellValueFactory(cell ->
                new SimpleDoubleProperty(round2(marketComparison.calculateScore(cell.getValue()))));

        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        scoreCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        marketTable.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> {
                    if (selected != null) {
                        applySelectedOption(selected);
                        updateTotals();
                        refreshSalesSnapshot();
                    }
                }
        );
    }

    private void applySelectedOption(MarketOption option) {
        selectedOption = option;
        if (option == null) {
            bestMarketLabel.setText("-");
            selectedBuyerLabel.setText("-");
            bestPriceLabel.setText("RM 0.00 / kg");
            selectedDemandLabel.setText("Demand: -");
            recommendationLabel.setText("No market available.");
            return;
        }

        bestMarketLabel.setText(recommendedOption == null ? "-" : recommendedOption.getMarketName());
        selectedBuyerLabel.setText(option.getMarketName());
        bestPriceLabel.setText(String.format(Locale.US, "RM %.2f / kg", option.getPricePerKg()));
        selectedDemandLabel.setText("Demand: " + option.getDemand()
                + "  |  Score " + String.format(Locale.US, "%.2f",
                marketComparison.calculateScore(option)));
        recommendationLabel.setText(marketComparison.getRecommendation(recommendedOption));
        statusLabel.setText(option == recommendedOption
                ? "Using the recommended buyer."
                : "Selling to " + option.getMarketName() + ".");
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
        if (selectedOption == null) {
            showValidation("Select a market from the comparison table.");
            return;
        }

        SaleRecord sale = marketService.sell(
                session.getCurrentDay(),
                crop,
                quantity,
                selectedOption.getMarketName(),
                selectedOption.getPricePerKg()
        );

        try {
            CsvDataStore.appendHarvest(sale);
            session.addSale(sale);
            SaleRepository.insert(session.getFarmId(), sale);

            refreshSalesSnapshot();
            reportNoteLabel.setText("Sale recorded. Opening season report...");
            sellButton.setDisable(true);
            statusLabel.setText("Sale saved. Opening season report...");
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
        if (quantity == null || selectedOption == null) {
            revenueLabel.setText("RM 0.00");
            costLabel.setText("RM 0.00");
            profitLabel.setText("RM 0.00");
            marginLabel.setText("Margin -");
            unitBreakdownLabel.setText("");
            return;
        }

        double unitPrice = selectedOption.getPricePerKg();
        double costPerKg = crop.getCostPerKg();
        double revenue = quantity * unitPrice;
        double cost = quantity * costPerKg;
        double profit = revenue - cost;
        revenueLabel.setText(money(revenue));
        costLabel.setText(money(cost));
        profitLabel.setText(money(profit));
        unitBreakdownLabel.setText(String.format(Locale.US,
                "%.2f kg x RM %.2f/kg  |  cost RM %.2f/kg",
                quantity, unitPrice, costPerKg));
        if (revenue > 0) {
            marginLabel.setText(String.format(Locale.US, "Margin %.1f%%", profit / revenue * 100.0));
        } else {
            marginLabel.setText("Margin -");
        }
    }

    private void refreshSalesSnapshot() {
        Double quantity = readQuantity();
        List<SaleRecord> draft = new ArrayList<>(session.getSales());
        boolean preview = false;
        if (quantity != null && selectedOption != null) {
            draft.add(marketService.sell(
                    session.getCurrentDay(),
                    crop,
                    quantity,
                    selectedOption.getMarketName(),
                    selectedOption.getPricePerKg()
            ));
            preview = true;
        }

        if (reportPreviewBadge != null) {
            reportPreviewBadge.setText(preview ? "PREVIEW" : "RECORDED");
            reportPreviewBadge.setVisible(!draft.isEmpty());
        }

        if (draft.isEmpty()) {
            reportQtyLabel.setText("0 kg");
            reportRevenueLabel.setText("RM 0.00");
            reportProfitLabel.setText("RM 0.00");
            reportBestMarketLabel.setText("-");
            reportNoteLabel.setText("No sales yet. Confirm to add the first sale.");
            return;
        }

        reportQtyLabel.setText(String.format(Locale.US, "%.2f kg", salesReport.getTotalQuantity(draft)));
        reportRevenueLabel.setText(money(salesReport.getTotalRevenue(draft)));
        reportProfitLabel.setText(money(salesReport.getTotalProfit(draft)));
        reportBestMarketLabel.setText(salesReport.getBestSellingMarket(draft));
        reportNoteLabel.setText(preview
                ? "Includes this sale as a preview until you confirm."
                : "Based on recorded sales only.");
    }

    private Double readQuantity() {
        try {
            double quantity = Double.parseDouble(quantityField.getText().trim());
            return quantity > 0.0 ? quantity : null;
        } catch (NumberFormatException | NullPointerException exception) {
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

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
