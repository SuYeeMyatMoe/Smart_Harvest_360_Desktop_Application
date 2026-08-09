package SmartHarvest360.controllers;

import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the final season totals, ROI, chart, and CSV report. */
public class SeasonReportController {
    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label roiLabel;
    @FXML private Label reportStatusLabel;
    @FXML private PieChart revenueChart;
    @FXML private Button newSeasonButton;

    private AppSession session;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        List<SaleRecord> sales = session.getSales();

        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;

        revenueLabel.setText(money(revenue));
        costLabel.setText(money(cost));
        profitLabel.setText(money(profit));
        roiLabel.setText(String.format(Locale.US, "%.2f%%", roi));
        populateChart(sales);

        try {
            CsvDataStore.saveSeasonReport(sales);
            reportStatusLabel.setText("Season report saved to data/season_report.csv");
        } catch (IOException exception) {
            reportStatusLabel.setText("Report displayed, but CSV saving failed: " + exception.getMessage());
        }
    }

    @FXML
    private void handleNewSeason() {
        session.resetDemoSeason();
        SceneNavigator.switchTo(newSeasonButton, "/fxml/SimulationScreen.fxml");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void populateChart(List<SaleRecord> sales) {
        Map<String, Double> revenueByCrop = new LinkedHashMap<>();
        for (SaleRecord sale : sales) {
            revenueByCrop.merge(sale.cropName(), sale.revenue(), Double::sum);
        }

        var chartData = FXCollections.<PieChart.Data>observableArrayList();
        revenueByCrop.forEach((crop, revenue) -> chartData.add(new PieChart.Data(crop, revenue)));
        revenueChart.setData(chartData);
        revenueChart.setTitle("Revenue by Crop");
        revenueChart.setLegendVisible(true);
        revenueChart.setLabelsVisible(true);
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }
}
