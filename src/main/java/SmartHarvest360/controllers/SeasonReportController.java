package SmartHarvest360.controllers;

import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.GradePredictor;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SimDayLog;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the final season totals, ROI, chart, and downloadable CSV report. */
public class SeasonReportController {
    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label roiLabel;
    @FXML private Label reportStatusLabel;
    @FXML private Label predictedGradeReportLabel;
    @FXML private Label actualGradeReportLabel;
    @FXML private Label gradeCoachLabel;
    @FXML private Label filePathLabel;
    @FXML private BarChart<String, Number> revenueByCropChart;
    @FXML private BarChart<String, Number> financeChart;
    @FXML private LineChart<Number, Number> growthChart;
    @FXML private AreaChart<Number, Number> inputChart;
    @FXML private Button newSeasonButton;
    @FXML private Button downloadReportButton;
    @FXML private Button openFolderButton;
    @FXML private ScrollPane reportScroll;

    private AppSession session;
    private Path lastSavedPath;
    private String predictedGrade = "—";
    private String actualGrade = "—";

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        List<SaleRecord> sales = session.getSales();

        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;

        animateMoney(revenueLabel, revenue);
        animateMoney(costLabel, cost);
        animateMoney(profitLabel, profit);
        animatePercent(roiLabel, roi);
        populateCharts(sales, revenue, cost, profit);
        populateGradeCompare(roi, profit);
        Platform.runLater(() -> {
            reportScroll.setVvalue(0.0);
            reportScroll.setHvalue(0.0);
            animateCharts();
        });

        try {
            CsvDataStore.saveSeasonReport(sales);
            lastSavedPath = CsvDataStore.seasonReportPath();
            reportStatusLabel.setText("Auto-saved season report + activity log under data/");
            filePathLabel.setText(lastSavedPath.toString());
        } catch (IOException exception) {
            reportStatusLabel.setText("Report shown, but auto-save failed: " + exception.getMessage());
            filePathLabel.setText(CsvDataStore.dataDirectory().toString());
        }
    }

    @FXML
    private void handleDownloadReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Download Season Report");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV report", "*.csv")
        );
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String farm = session.getFarmName() == null ? "farm" : session.getFarmName().replaceAll("[^a-zA-Z0-9_-]+", "_");
        chooser.setInitialFileName("SmartHarvest_SeasonReport_" + farm + "_" + stamp + ".csv");
        try {
            chooser.setInitialDirectory(CsvDataStore.downloadsDirectory().toFile());
        } catch (IOException ignored) {
            // Default location is fine.
        }

        Window window = downloadReportButton.getScene() == null
                ? null : downloadReportButton.getScene().getWindow();
        File chosen = chooser.showSaveDialog(window);
        if (chosen == null) {
            reportStatusLabel.setText("Download cancelled.");
            return;
        }

        try {
            Path saved = CsvDataStore.exportSeasonReport(chosen.toPath(), session);
            lastSavedPath = saved;
            reportStatusLabel.setText("Downloaded full report (summary + sales + activity).");
            filePathLabel.setText(saved.toString());
        } catch (IOException exception) {
            reportStatusLabel.setText("Download failed: " + exception.getMessage());
        }
    }

    @FXML
    private void handleOpenDataFolder() {
        Path folder = CsvDataStore.dataDirectory();
        boolean opened = CsvDataStore.openInFileManager(folder);
        if (opened) {
            reportStatusLabel.setText("Opened data folder.");
        } else {
            reportStatusLabel.setText("Could not open folder - copy the path below.");
        }
        filePathLabel.setText(folder.toString());
    }

    @FXML
    private void handleNewSeason() {
        session.beginNewSeason();
        SceneNavigator.switchTo(newSeasonButton, "/fxml/FarmSetupScreen.fxml");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void populateGradeCompare(double roi, double profit) {
        AdvisorResult advice = session.getAdvisorResult();
        predictedGrade = advice == null ? "—" : advice.getPredictedGrade();
        double adjustedRoi = GradePredictor.careAdjustedRoi(roi, session.getCareScore());
        actualGrade = GradePredictor.gradeFromRoi(adjustedRoi, profit);
        if (profit >= 0 && session.getCareScore() >= 80) {
            actualGrade = GradePredictor.nudgeGrade(actualGrade, Math.min(100, session.getCareScore()));
        }
        predictedGradeReportLabel.setText("Predicted: " + predictedGrade
                + "  ·  Care: " + session.getCareScore());
        actualGradeReportLabel.setText("Actual: " + actualGrade);

        if (advice == null || "—".equals(predictedGrade)) {
            gradeCoachLabel.setText("No plant-time prediction was stored for this season.");
            return;
        }
        if (predictedGrade.equalsIgnoreCase(actualGrade)) {
            gradeCoachLabel.setText("Match — field care kept the season on the predicted grade path.");
        } else if (GradePredictor.indexOfGrade(actualGrade) > GradePredictor.indexOfGrade(predictedGrade)) {
            gradeCoachLabel.setText(String.format(Locale.US,
                    "Improved — predicted %s, finished %s thanks to care score %d.",
                    predictedGrade, actualGrade, session.getCareScore()));
        } else {
            gradeCoachLabel.setText(String.format(Locale.US,
                    "Mismatch — model expected %s but season graded %s. Follow Protect on storms and the fertilizer plan to lift care.",
                    predictedGrade, actualGrade));
        }
    }

    private void populateCharts(List<SaleRecord> sales, double revenue, double cost, double profit) {
        Map<String, Double> revenueByCrop = new LinkedHashMap<>();
        for (SaleRecord sale : sales) {
            revenueByCrop.merge(sale.cropName(), sale.revenue(), Double::sum);
        }
        if (revenueByCrop.isEmpty()) {
            revenueByCrop.put("No sales", 0.0);
        }

        XYChart.Series<String, Number> cropSeries = new XYChart.Series<>();
        cropSeries.setName("Revenue");
        for (Map.Entry<String, Double> entry : revenueByCrop.entrySet()) {
            cropSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        revenueByCropChart.getData().setAll(cropSeries);

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");
        revenueSeries.getData().add(new XYChart.Data<>("Season", revenue));
        XYChart.Series<String, Number> costSeries = new XYChart.Series<>();
        costSeries.setName("Cost");
        costSeries.getData().add(new XYChart.Data<>("Season", cost));
        XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName("Profit");
        profitSeries.getData().add(new XYChart.Data<>("Season", profit));
        financeChart.getData().setAll(revenueSeries, costSeries, profitSeries);
        financeChart.setLegendVisible(true);

        XYChart.Series<Number, Number> growthSeries = new XYChart.Series<>();
        growthSeries.setName("Growth %");
        XYChart.Series<Number, Number> waterSeries = new XYChart.Series<>();
        waterSeries.setName("Water (L)");
        XYChart.Series<Number, Number> fertSeries = new XYChart.Series<>();
        fertSeries.setName("Fertilizer (kg)");

        List<SimDayLog> logs = session.getDayLogs();
        if (logs == null || logs.isEmpty()) {
            growthSeries.getData().add(new XYChart.Data<>(0, 0));
            waterSeries.getData().add(new XYChart.Data<>(0, 0));
            fertSeries.getData().add(new XYChart.Data<>(0, 0));
        } else {
            for (SimDayLog log : logs) {
                if (log == null || "Setup".equalsIgnoreCase(log.getAction())) {
                    continue;
                }
                int day = log.getDay();
                growthSeries.getData().add(new XYChart.Data<>(day, log.getGrowthPercent()));
                waterSeries.getData().add(new XYChart.Data<>(day, log.getWaterUsed()));
                fertSeries.getData().add(new XYChart.Data<>(day, log.getFertilizerUsed()));
            }
            if (growthSeries.getData().isEmpty()) {
                growthSeries.getData().add(new XYChart.Data<>(0, 0));
                waterSeries.getData().add(new XYChart.Data<>(0, 0));
                fertSeries.getData().add(new XYChart.Data<>(0, 0));
            }
        }

        growthChart.getData().setAll(growthSeries);
        inputChart.getData().setAll(waterSeries, fertSeries);
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }

    private void animateMoney(Label label, double target) {
        animateNumber(label, target, value -> money(value));
    }

    private void animatePercent(Label label, double target) {
        animateNumber(label, target, value -> String.format(Locale.US, "%.2f%%", value));
    }

    private void animateNumber(Label label, double target, java.util.function.DoubleFunction<String> formatter) {
        final int frames = 36;
        Timeline timeline = new Timeline();
        for (int frame = 0; frame <= frames; frame++) {
            double progress = frame / (double) frames;
            double eased = 1.0 - Math.pow(1.0 - progress, 3);
            double value = target * eased;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(frame * 24.0),
                    event -> label.setText(formatter.apply(value))));
        }
        timeline.play();
    }

    private void animateCharts() {
        for (javafx.scene.Node chart : List.of(revenueByCropChart, financeChart, growthChart, inputChart)) {
            chart.setScaleX(0.96);
            chart.setScaleY(0.96);
            chart.setOpacity(0.0);
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(620), chart);
            fade.setToValue(1.0);
            ScaleTransition scale = new ScaleTransition(Duration.millis(620), chart);
            scale.setToX(1.0);
            scale.setToY(1.0);
            new javafx.animation.ParallelTransition(fade, scale).play();
        }
    }
}
