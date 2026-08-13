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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Harvest & Market screen.
 *
 * Buyers are selected based on the farm location
 * stored in AppSession.
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
    @FXML private TableColumn<MarketOption, String> marketTypeCol;
    @FXML private TableColumn<MarketOption, Number> priceCol;
    @FXML private TableColumn<MarketOption, String> demandCol;
    @FXML private TableColumn<MarketOption, Number> scoreCol;

    @FXML private Button sellButton;

    private final Market marketService =
            new Market();

    private final MarketComparison marketComparison =
            new MarketComparison();

    private final SalesReport salesReport =
            new SalesReport();

    private AppSession session;

    private Crop crop;

    private MarketOption selectedOption;

    private MarketOption recommendedOption;

    @FXML
    public void initialize() {

        session =
                AppSession.getInstance();

        /*
         * Keep existing behaviour for screens that
         * open the market screen without a farm.
         */
        session.ensureDemoData();

        crop =
                session.getActiveCrop();

        cropLabel.setText(
                crop.getName()
        );

        availableYieldLabel.setText(
                String.format(
                        Locale.US,
                        "%.2f kg",
                        crop.getYieldAmount()
                )
        );

        quantityField.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        crop.getYieldAmount()
                )
        );

        quantityHintLabel.setText(
                String.format(
                        Locale.US,
                        "Max available harvest: %.2f kg",
                        crop.getYieldAmount()
                )
        );

        /*
         * Get the farm location.
         */
        String farmLocation =
                session.getFarmLocation();

        /*
         * Load buyers based on the farm location.
         */
        List<MarketOption> options =
                marketService.getMarketOptions(
                        crop,
                        farmLocation
                );

        recommendedOption =
                marketComparison.findBestMarket(
                        options
                );

        selectedOption =
                recommendedOption;

        configureTable();

        marketTable.setItems(
                FXCollections.observableArrayList(
                        options
                )
        );

        if (recommendedOption != null) {

            marketTable
                    .getSelectionModel()
                    .select(
                            recommendedOption
                    );
        }

        applySelectedOption(
                selectedOption
        );

        /*
         * Update totals whenever quantity changes.
         */
        quantityField.textProperty().addListener(
                (ignored, oldValue, newValue) -> {

                    updateTotals();

                    refreshSalesSnapshot();
                }
        );

        updateTotals();

        refreshSalesSnapshot();
    }

    /**
     * Configures the buyer comparison table.
     */
    private void configureTable() {

        /*
         * Buyer name + logo.
         */
        marketNameCol.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getMarketName()
                        )
        );

        /*
         * Display logo and name together.
         */
        marketNameCol.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final ImageView imageView =
                                    new ImageView();

                            private final Label label =
                                    new Label();

                            private final HBox box =
                                    new HBox(
                                            8,
                                            imageView,
                                            label
                                    );

                            {
                                imageView.setFitWidth(32);
                                imageView.setFitHeight(32);
                                imageView.setPreserveRatio(true);

                                box.setAlignment(
                                        javafx.geometry.Pos.CENTER_LEFT
                                );
                            }

                            @Override
                            protected void updateItem(
                                    String item,
                                    boolean empty) {

                                super.updateItem(
                                        item,
                                        empty
                                );

                                if (empty
                                        || item == null) {

                                    setGraphic(null);

                                    return;
                                }

                                label.setText(item);

                                MarketOption option =
                                        getTableView()
                                                .getItems()
                                                .get(
                                                        getIndex()
                                                );

                                loadLogo(
                                        option.getLogoPath()
                                );

                                setGraphic(box);
                            }

                            private void loadLogo(
                                    String logoPath) {

                                imageView.setImage(
                                        null
                                );

                                if (logoPath == null
                                        || logoPath.isBlank()) {

                                    return;
                                }

                                try {

                                    InputStream stream =
                                            getClass()
                                                    .getResourceAsStream(
                                                            logoPath
                                                    );

                                    if (stream != null) {

                                        imageView.setImage(
                                                new Image(
                                                        stream
                                                )
                                        );
                                    }

                                } catch (Exception ignored) {
                                    /*
                                     * If the logo is missing,
                                     * the buyer name still appears.
                                     */
                                }
                            }
                        }
        );

        /*
         * Buyer type.
         */
        marketTypeCol.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getBuyerType()
                        )
        );

        /*
         * Price.
         */
        priceCol.setCellValueFactory(
                cell ->
                        new SimpleDoubleProperty(
                                cell.getValue()
                                        .getPricePerKg()
                        )
        );

        /*
         * Demand.
         */
        demandCol.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                cell.getValue()
                                        .getDemand()
                        )
        );

        /*
         * Score.
         */
        scoreCol.setCellValueFactory(
                cell ->
                        new SimpleDoubleProperty(
                                round2(
                                        marketComparison
                                                .calculateScore(
                                                        cell.getValue()
                                                )
                        )
                )
        );

        priceCol.setStyle(
                "-fx-alignment: CENTER-RIGHT;"
        );

        scoreCol.setStyle(
                "-fx-alignment: CENTER-RIGHT;"
        );

        /*
         * Buyer selection.
         */
        marketTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (ignored, oldValue, selected) -> {

                            if (selected != null) {

                                applySelectedOption(
                                        selected
                                );

                                updateTotals();

                                refreshSalesSnapshot();
                            }
                        }
                );
    }

    /**
     * Applies the selected buyer to the sale overview.
     */
    private void applySelectedOption(
            MarketOption option) {

        selectedOption =
                option;

        if (option == null) {

            bestMarketLabel.setText(
                    "-"
            );

            selectedBuyerLabel.setText(
                    "-"
            );

            bestPriceLabel.setText(
                    "RM 0.00 / kg"
            );

            selectedDemandLabel.setText(
                    "Demand: -"
            );

            recommendationLabel.setText(
                    "No buyer available."
            );

            return;
        }

        bestMarketLabel.setText(
                recommendedOption == null
                        ? "-"
                        : recommendedOption
                                .getMarketName()
        );

        selectedBuyerLabel.setText(
                option.getMarketName()
        );

        bestPriceLabel.setText(
                String.format(
                        Locale.US,
                        "RM %.2f / kg",
                        option.getPricePerKg()
                )
        );

        selectedDemandLabel.setText(
                "Demand: "
                        + option.getDemand()
                        + "  |  "
                        + String.format(
                                Locale.US,
                                "%.1f km away",
                                option.getDistanceKm()
                        )
                        + "  |  Score "
                        + String.format(
                                Locale.US,
                                "%.2f",
                                marketComparison
                                        .calculateScore(
                                                option
                                        )
                        )
        );

        recommendationLabel.setText(
                marketComparison.getRecommendation(
                        recommendedOption
                )
        );

        if (option == recommendedOption) {

            statusLabel.setText(
                    "Using the recommended buyer."
            );

        } else {

            statusLabel.setText(
                    "Selling to "
                            + option.getMarketName()
                            + "."
            );
        }
    }

    @FXML
    private void handleSell() {

        Double quantity =
                readQuantity();

        if (quantity == null) {

            showValidation(
                    "Enter a valid quantity greater than zero."
            );

            return;
        }

        if (quantity
                > crop.getYieldAmount()) {

            showValidation(
                    String.format(
                            Locale.US,
                            "Available harvest is only %.2f kg.",
                            crop.getYieldAmount()
                    )
            );

            return;
        }

        if (selectedOption == null) {

            showValidation(
                    "Select a buyer from the comparison table."
            );

            return;
        }

        SaleRecord sale =
                marketService.sell(
                        session.getCurrentDay(),
                        crop,
                        quantity,
                        selectedOption.getMarketName(),
                        selectedOption.getPricePerKg()
                );

        try {

            CsvDataStore.appendHarvest(
                    sale
            );

            session.addSale(
                    sale
            );

            SaleRepository.insert(
                    session.getFarmId(),
                    sale
            );

            refreshSalesSnapshot();

            reportNoteLabel.setText(
                    "Sale recorded. Opening season report..."
            );

            sellButton.setDisable(
                    true
            );

            statusLabel.setText(
                    "Sale saved. Opening season report..."
            );

            SceneNavigator.switchTo(
                    sellButton,
                    "/fxml/SeasonReportScreen.fxml"
            );

        } catch (IOException exception) {

            Alert alert =
                    new Alert(
                            Alert.AlertType.ERROR
                    );

            alert.setHeaderText(
                    "The sale could not be saved"
            );

            alert.setContentText(
                    exception.getMessage()
            );

            alert.showAndWait();
        }
    }

    /**
     * Updates revenue, cost, profit and margin.
     */
    private void updateTotals() {

        Double quantity =
                readQuantity();

        if (quantity == null
                || selectedOption == null) {

            revenueLabel.setText(
                    "RM 0.00"
            );

            costLabel.setText(
                    "RM 0.00"
            );

            profitLabel.setText(
                    "RM 0.00"
            );

            marginLabel.setText(
                    "Margin -"
            );

            unitBreakdownLabel.setText(
                    ""
            );

            return;
        }

        double unitPrice =
                selectedOption
                        .getPricePerKg();

        double costPerKg =
                crop.getCostPerKg();

        double revenue =
                quantity * unitPrice;

        double cost =
                quantity * costPerKg;

        double profit =
                revenue - cost;

        revenueLabel.setText(
                money(revenue)
        );

        costLabel.setText(
                money(cost)
        );

        profitLabel.setText(
                money(profit)
        );

        unitBreakdownLabel.setText(
                String.format(
                        Locale.US,
                        "%.2f kg x RM %.2f/kg  |  cost RM %.2f/kg",
                        quantity,
                        unitPrice,
                        costPerKg
                )
        );

        if (revenue > 0) {

            marginLabel.setText(
                    String.format(
                            Locale.US,
                            "Margin %.1f%%",
                            profit
                                    / revenue
                                    * 100.0
                    )
            );

        } else {

            marginLabel.setText(
                    "Margin -"
            );
        }
    }

    /**
     * Updates the sales preview.
     */
    private void refreshSalesSnapshot() {

        Double quantity =
                readQuantity();

        List<SaleRecord> draft =
                new ArrayList<>(
                        session.getSales()
                );

        boolean preview =
                false;

        if (quantity != null
                && selectedOption != null) {

            draft.add(
                    marketService.sell(
                            session.getCurrentDay(),
                            crop,
                            quantity,
                            selectedOption
                                    .getMarketName(),
                            selectedOption
                                    .getPricePerKg()
                    )
            );

            preview =
                    true;
        }

        if (reportPreviewBadge != null) {

            reportPreviewBadge.setText(
                    preview
                            ? "PREVIEW"
                            : "RECORDED"
            );

            reportPreviewBadge.setVisible(
                    !draft.isEmpty()
            );
        }

        if (draft.isEmpty()) {

            reportQtyLabel.setText(
                    "0 kg"
            );

            reportRevenueLabel.setText(
                    "RM 0.00"
            );

            reportProfitLabel.setText(
                    "RM 0.00"
            );

            reportBestMarketLabel.setText(
                    "-"
            );

            reportNoteLabel.setText(
                    "No sales yet. Confirm to add the first sale."
            );

            return;
        }

        reportQtyLabel.setText(
                String.format(
                        Locale.US,
                        "%.2f kg",
                        salesReport
                                .getTotalQuantity(
                                        draft
                                )
                )
        );

        reportRevenueLabel.setText(
                money(
                        salesReport
                                .getTotalRevenue(
                                        draft
                                )
                )
        );

        reportProfitLabel.setText(
                money(
                        salesReport
                                .getTotalProfit(
                                        draft
                                )
                )
        );

        reportBestMarketLabel.setText(
                salesReport
                        .getBestSellingMarket(
                                draft
                        )
        );

        reportNoteLabel.setText(
                preview
                        ? "Includes this sale as a preview until you confirm."
                        : "Based on recorded sales only."
        );
    }

    private Double readQuantity() {

        try {

            double quantity =
                    Double.parseDouble(
                            quantityField
                                    .getText()
                                    .trim()
                    );

            return quantity > 0.0
                    ? quantity
                    : null;

        } catch (
                NumberFormatException |
                NullPointerException exception) {

            return null;
        }
    }

    private void showValidation(
            String message) {

        statusLabel.setText(
                message
        );

        quantityField.requestFocus();
    }

    private static String money(
            double value) {

        return String.format(
                Locale.US,
                "RM %,.2f",
                value
        );
    }

    private static double round2(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}