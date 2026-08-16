package SmartHarvest360.controllers;

import SmartHarvest360.Crop;
import SmartHarvest360.RandomEvent;
import SmartHarvest360.Resource;
import SmartHarvest360.SeasonGoal;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.SimulationEngine;
import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SeasonHistory;
import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.session.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Builds the final season totals, ROI, chart, and CSV report. */
public class SeasonReportController {
    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label roiLabel;
    @FXML private Label reportStatusLabel;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private Button newSeasonButton;
    @FXML private Button themeButton;
    @FXML private Button fieldsButton;
    @FXML private Label goalBadge;
    @FXML private Label gradeLabel;
    @FXML private Label comparisonLabel;
    @FXML private Label narrativeLabel;
    @FXML private Label suggestionLabel;
    @FXML private Label efficiencyLabel;

    private AppSession session;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        SmartHarvest360.ui.ThemeManager.syncButton(themeButton);
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

        List<SeasonHistory> pastSeasons = loadPastSeasons();

        try {
            CsvDataStore.saveSeasonReport(sales);
            if (!sales.isEmpty()) {
                CsvDataStore.appendSeasonHistory(buildSeasonHistory(sales, revenue, cost, profit, roi));
                reportStatusLabel.setText("Season report saved to data/season_report.csv"
                        + " | history appended to data/season_history.csv");
            } else {
                reportStatusLabel.setText("Season report saved to data/season_report.csv");
            }
        } catch (IOException exception) {
            reportStatusLabel.setText("Report displayed, but CSV saving failed: " + exception.getMessage());
        }

        buildHowYouDid(pastSeasons, sales, revenue, cost, profit, roi);
    }


    /** Past seasons from the append-only history file, captured before this season is appended. */
    private static List<SeasonHistory> loadPastSeasons() {
        try {
            return CsvDataStore.loadSeasonHistory();
        } catch (IOException exception) {
            return new ArrayList<>();
        }
    }

    private SeasonHistory buildSeasonHistory(List<SaleRecord> sales,
                                             double revenue, double cost,
                                             double profit, double roi) {
        SeasonSimulator season = session.getSeason();
        Map<RandomEvent, Long> eventCounts = season == null
                ? Map.of()
                : season.getEvents().stream()
                        .collect(Collectors.groupingBy(SeasonSimulator.SeasonEvent::event,
                                Collectors.counting()));

        double endingWater = 0.0;
        double endingFertilizer = 0.0;
        double endingBudget = 0.0;
        String plantedCrops = "";
        if (session.getFarm() != null) {
            Resource resource = session.getFarm().getResource();
            endingWater = resource.getWater();
            endingFertilizer = resource.getFertilizer();
            endingBudget = resource.getBudget();
            plantedCrops = session.getFarm().getCrops().stream()
                    .map(Crop::getName)
                    .collect(Collectors.joining(", "));
        }

        return new SeasonHistory(
                session.getSeasonGoal().name(),
                revenue, cost, profit, roi,
                sales.stream().mapToDouble(SaleRecord::quantity).sum(),
                season == null ? 0 : season.getWaterShortageDays(),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.PEST, 0L)),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.DROUGHT, 0L)),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.FROST, 0L)),
                endingWater, endingFertilizer, endingBudget,
                plantedCrops
        );
    }

    /** Grades this season against the chosen goal, compares it to past seasons, and narrates the results. */
    private void buildHowYouDid(List<SeasonHistory> pastSeasons, List<SaleRecord> sales,
                                double revenue, double cost, double profit, double roi) {
        SeasonGoal goal = session.getSeasonGoal();
        goalBadge.setText(goal.getLabel().toUpperCase(Locale.US));

        if (sales.isEmpty()) {
            gradeLabel.setText("No sales were recorded this season, so there is no result to grade yet.");
            comparisonLabel.setText("First season — no comparison yet.");
            narrativeLabel.setText("No season data available.");
            suggestionLabel.setText("Plant and sell at least one crop to unlock your season report.");
            efficiencyLabel.setText("");
            return;
        }

        double yieldKg = sales.stream().mapToDouble(SaleRecord::quantity).sum();
        SeasonSimulator season = session.getSeason();
        int waterShortageDays = season == null ? 0 : season.getWaterShortageDays();
        long pestDays = countEvent(season, RandomEvent.PEST);
        long droughtDays = countEvent(season, RandomEvent.DROUGHT);
        long frostDays = countEvent(season, RandomEvent.FROST);

        double endingWater = 0.0;
        double endingFertilizer = 0.0;
        double endingBudget = 0.0;
        double landUnits = 0.0;
        if (session.getFarm() != null) {
            Resource resource = session.getFarm().getResource();
            endingWater = resource.getWater();
            endingFertilizer = resource.getFertilizer();
            endingBudget = resource.getBudget();
            landUnits = session.getFarm().getCrops().size();
        }

        double waterUsed = 0.0;
        if (season != null) {
            waterUsed = season.getEngines().stream()
                    .mapToDouble(SimulationEngine::getTotalWaterUsed)
                    .sum();
        }

        gradeLabel.setText(gradeText(goal, roi, yieldKg,
                endingWater, endingFertilizer, endingBudget, waterShortageDays, pastSeasons));
        comparisonLabel.setText(comparisonText(roi, pastSeasons));
        narrativeLabel.setText(narrativeText(season, waterShortageDays, pestDays, droughtDays, frostDays));
        suggestionLabel.setText(suggestionText(goal, sales,
                waterShortageDays, pestDays, droughtDays, frostDays, endingBudget));
        efficiencyLabel.setText(efficiencyText(profit, landUnits, waterUsed));
    }

    private static long countEvent(SeasonSimulator season, RandomEvent event) {
        if (season == null) {
            return 0L;
        }
        return season.getEvents().stream()
                .filter(entry -> entry.event() == event)
                .count();
    }

    private static String gradeText(SeasonGoal goal, double roi, double yieldKg,
                                    double endingWater, double endingFertilizer, double endingBudget,
                                    int waterShortageDays, List<SeasonHistory> pastSeasons) {
        if (goal == SeasonGoal.MAXIMIZE_YIELD) {
            if (pastSeasons.isEmpty()) {
                return String.format(Locale.US,
                        "Your yield of %.2f kg sets the baseline for future seasons. "
                                + "Try higher-yield crops to grow it.",
                        yieldKg);
            }
            double average = average(pastSeasons, SeasonHistory::yieldKg);
            if (yieldKg > average + 0.05) {
                return String.format(Locale.US,
                        "Your %.2f kg yield beat the %.2f kg average of your %d previous season(s).",
                        yieldKg, average, pastSeasons.size());
            }
            if (yieldKg < average - 0.05) {
                return String.format(Locale.US,
                        "Your %.2f kg yield fell short of the %.2f kg average of your %d previous season(s).",
                        yieldKg, average, pastSeasons.size());
            }
            return String.format(Locale.US,
                    "Your %.2f kg yield matched the %.2f kg average of your %d previous season(s).",
                    yieldKg, average, pastSeasons.size());
        }

        if (goal == SeasonGoal.CONSERVE_RESOURCES) {
            String intact = String.format(Locale.US,
                    "You finished with %.2f L water, %.2f kg fertilizer, and RM %.2f budget intact.",
                    endingWater, endingFertilizer, endingBudget);
            if (waterShortageDays > 0) {
                return "Water ran dry on " + waterShortageDays
                        + " day(s), so conservation wasn't fully met. " + intact;
            }
            if (pastSeasons.isEmpty()) {
                return "You never ran out of water. " + intact + " This sets the conservation baseline.";
            }
            double averageBudget = average(pastSeasons, SeasonHistory::endingBudget);
            if (endingBudget > averageBudget + 1.0) {
                return "You conserved more budget than the average of your " + pastSeasons.size()
                        + " previous season(s). " + intact;
            }
            return "You finished without water shortages. " + intact
                    + " Budget left was similar to past seasons.";
        }

        if (pastSeasons.isEmpty()) {
            return String.format(Locale.US,
                    "Your ROI of %.2f%% sets the baseline for future seasons. "
                            + "Invest to grow your return next time.",
                    roi);
        }
        double average = average(pastSeasons, SeasonHistory::roi);
        if (roi > average + 0.05) {
            return String.format(Locale.US,
                    "Your ROI of %.2f%% beat the %.2f%% average of your %d previous season(s).",
                    roi, average, pastSeasons.size());
        }
        if (roi < average - 0.05) {
            return String.format(Locale.US,
                    "Your ROI of %.2f%% fell short of the %.2f%% average of your %d previous season(s).",
                    roi, average, pastSeasons.size());
        }
        return String.format(Locale.US,
                "Your ROI of %.2f%% matched the %.2f%% average of your %d previous season(s).",
                roi, average, pastSeasons.size());
    }

    private static String comparisonText(double roi, List<SeasonHistory> pastSeasons) {
        if (pastSeasons.isEmpty()) {
            return "First season — no comparison yet.";
        }
        SeasonHistory last = pastSeasons.get(pastSeasons.size() - 1);
        String direction = roi >= last.roi() ? "up from" : "down from";
        return String.format(Locale.US,
                "ROI this season: %.2f%% (%s %.2f%% last season).",
                roi, direction, last.roi());
    }

    private static String narrativeText(SeasonSimulator season, int waterShortageDays,
                                        long pestDays, long droughtDays, long frostDays) {
        if (season == null) {
            return "No simulation data was available for this season.";
        }
        List<String> sentences = new ArrayList<>();
        if (waterShortageDays > 0) {
            sentences.add("Water ran short on " + waterShortageDays + " day(s), stressing the crops.");
        } else {
            sentences.add("Water supply held up for the whole season.");
        }
        if (pestDays > 0) {
            sentences.add("Pests were active on " + pestDays + " day(s).");
        }
        if (droughtDays > 0) {
            sentences.add("Dry weather set in on " + droughtDays + " day(s).");
        }
        if (frostDays > 0) {
            sentences.add("Frost hit the field on " + frostDays + " day(s).");
        }
        if (pestDays == 0 && droughtDays == 0 && frostDays == 0) {
            sentences.add("No notable weather or pest events disturbed the season.");
        }
        String mostAffected = mostAffectedCrop(season);
        if (mostAffected != null) {
            sentences.add(mostAffected + " took the brunt of the season's events.");
        }
        return String.join(" ", sentences);
    }

    private static String mostAffectedCrop(SeasonSimulator season) {
        return season.getEvents().stream()
                .collect(Collectors.groupingBy(SeasonSimulator.SeasonEvent::cropName, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static String suggestionText(SeasonGoal goal, List<SaleRecord> sales,
                                         int waterShortageDays, long pestDays,
                                         long droughtDays, long frostDays, double endingBudget) {
        List<String> tips = new ArrayList<>();
        if (waterShortageDays > 0) {
            tips.add("reserve more water next season after " + waterShortageDays + " shortage day(s)");
        } else if (pestDays >= 5) {
            tips.add("budget for pest control after " + pestDays + " pest day(s)");
        } else if (droughtDays >= 5) {
            tips.add("plant drought-hardy crops after " + droughtDays + " dry day(s)");
        } else if (frostDays >= 5) {
            tips.add("shift planting or pick frost-hardy crops after " + frostDays + " frost day(s)");
        }
        if (endingBudget <= 0.0) {
            tips.add("start next season with a larger budget (it was fully spent)");
        }
        String weakestCropTip = weakestCropTip(sales);
        if (weakestCropTip != null) {
            tips.add(weakestCropTip);
        }
        if (tips.isEmpty()) {
            if (goal == SeasonGoal.CONSERVE_RESOURCES) {
                tips.add("keep your conservation strategy and consider expanding land to grow more");
            } else {
                tips.add("reinvest profit into more land or higher-value crops");
            }
        }
        return "For next season: " + String.join("; and ", tips) + ".";
    }

    /** Per-crop ROI tip, only when more than one crop was sold and one clearly underperformed. */
    private static String weakestCropTip(List<SaleRecord> sales) {
        Map<String, double[]> perCrop = new LinkedHashMap<>();
        for (SaleRecord sale : sales) {
            double[] totals = perCrop.computeIfAbsent(sale.cropName(), key -> new double[2]);
            totals[0] += sale.revenue();
            totals[1] += sale.cost();
        }
        if (perCrop.size() < 2) {
            return null;
        }
        String weakest = null;
        double weakestRoi = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> entry : perCrop.entrySet()) {
            double cost = entry.getValue()[1];
            double roi = cost == 0.0 ? 0.0 : (entry.getValue()[0] - cost) / cost * 100.0;
            if (roi < weakestRoi) {
                weakestRoi = roi;
                weakest = entry.getKey();
            }
        }
        if (weakestRoi >= 10.0) {
            return null;
        }
        return "reconsider " + weakest + ", which returned only "
                + String.format(Locale.US, "%.1f", weakestRoi) + "% this season";
    }

    private static String efficiencyText(double profit, double landUnits, double waterUsed) {
        List<String> parts = new ArrayList<>();
        if (landUnits > 0.0) {
            parts.add("RM " + String.format(Locale.US, "%.2f", profit / landUnits) + " per land plot");
        }
        if (waterUsed > 0.0) {
            parts.add("RM " + String.format(Locale.US, "%.2f", profit / waterUsed) + " per litre of water");
        }
        return parts.isEmpty() ? "" : "Efficiency: " + String.join(" | ", parts) + ".";
    }

    private static double average(List<SeasonHistory> seasons, ToDoubleFunction<SeasonHistory> extractor) {
        if (seasons.isEmpty()) {
            return 0.0;
        }
        return seasons.stream().mapToDouble(extractor).average().orElse(0.0);
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
private void handleNewSeason() {
        session.resetDemoSeason();
        SceneNavigator.resetTo(newSeasonButton, "/fxml/FarmSetupScreen.fxml");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void populateChart(List<SaleRecord> sales) {
        Map<String, Double> revenueByCrop = new LinkedHashMap<>();
        for (SaleRecord sale : sales) {
            if (sale.revenue() <= 0.0) {
                continue;
            }
            revenueByCrop.merge(sale.cropName(), sale.revenue(), Double::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        revenueByCrop.forEach((crop, revenue) -> series.getData().add(new XYChart.Data<>(crop, revenue)));
        revenueChart.getData().setAll(series);
        revenueChart.setLegendVisible(false);
        attachValueLabels(series);
    }

    private void attachValueLabels(XYChart.Series<String, Number> series) {
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node bar = data.getNode();
                if (bar == null) {
                    continue;
                }
                double value = data.getYValue().doubleValue();

                Tooltip tip = new Tooltip(money(value));
                tip.getStyleClass().add("chart-value-tooltip");
                Tooltip.install(bar, tip);

                if (bar instanceof StackPane pane) {
                    double barHeight = pane.getBoundsInParent().getHeight();
                    if (barHeight <= 0.0) {
                        continue;
                    }
                    Label valueLabel = new Label(money(value));
                    valueLabel.getStyleClass().add("chart-bar-label");
                    StackPane.setAlignment(valueLabel, Pos.TOP_CENTER);
                    pane.getChildren().add(valueLabel);
                    valueLabel.setTranslateY(-(barHeight / 2.0 + 6.0));
                }
            }
        });
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }
}
