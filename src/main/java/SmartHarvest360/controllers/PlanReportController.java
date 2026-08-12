package SmartHarvest360.controllers;

import SmartHarvest360.navigation.SceneNavigator;
import SmartHarvest360.plan.DetailedPlanReport;
import SmartHarvest360.plan.DetailedPlanReportBuilder;
import SmartHarvest360.plan.PlanRecommendation;
import SmartHarvest360.plan.PlanReportFileHandler;
import SmartHarvest360.plan.PlanStep;
import SmartHarvest360.session.AppSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Shows the detailed plan (steps + recommendations) after simulation.
 */
public class PlanReportController {

    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label cropLabel;
    @FXML private Label locationLabel;
    @FXML private Label plantGradeLabel;
    @FXML private Label liveGradeLabel;
    @FXML private Label careLabel;
    @FXML private Label fertilizerLabel;
    @FXML private Label filePathLabel;
    @FXML private TableView<PlanStep> stepsTable;
    @FXML private TableColumn<PlanStep, Number> stepNoCol;
    @FXML private TableColumn<PlanStep, String> stepActionCol;
    @FXML private TableColumn<PlanStep, String> stepWeatherCol;
    @FXML private TableColumn<PlanStep, String> stepOutcomeCol;
    @FXML private TableColumn<PlanStep, String> stepNoteCol;
    @FXML private TableView<PlanRecommendation> recommendTable;
    @FXML private TableColumn<PlanRecommendation, String> recPriorityCol;
    @FXML private TableColumn<PlanRecommendation, String> recTopicCol;
    @FXML private TableColumn<PlanRecommendation, String> recAdviceCol;
    @FXML private Button downloadPlanButton;
    @FXML private Button continueButton;

    private AppSession session;
    private DetailedPlanReport report;

    @FXML
    public void initialize() {
        session = AppSession.getInstance();
        report = session.getDetailedPlanReport();
        if (report == null) {
            report = DetailedPlanReportBuilder.fromSession(session);
            session.setDetailedPlanReport(report);
        }

        configureTables();
        bindReport(report);

        try {
            Path saved = PlanReportFileHandler.saveToDataFolder(report);
            statusLabel.setText("Plan auto-saved");
            filePathLabel.setText(saved.toString());
        } catch (IOException exception) {
            statusLabel.setText("Plan ready (auto-save failed)");
            filePathLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleDownloadPlan() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Download Detailed Plan Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setInitialFileName("SmartHarvest_DetailedPlan_" + stamp + ".csv");

        Window window = downloadPlanButton.getScene() == null
                ? null : downloadPlanButton.getScene().getWindow();
        File chosen = chooser.showSaveDialog(window);
        if (chosen == null) {
            statusLabel.setText("Download cancelled");
            return;
        }

        try {
            Path saved = PlanReportFileHandler.exportTo(chosen.toPath(), report);
            statusLabel.setText("Plan downloaded");
            filePathLabel.setText(saved.toString());
        } catch (IOException exception) {
            statusLabel.setText("Download failed: " + exception.getMessage());
        }
    }

    @FXML
    private void handleContinueToMarket() {
        SceneNavigator.switchTo(continueButton, "/fxml/HarvestMarketScreen.fxml");
    }

    private void configureTables() {
        stepNoCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getStepNumber()));
        stepActionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        stepWeatherCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWeather()));
        stepOutcomeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOutcome()));
        stepNoteCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCoachingNote()));
        stepsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        recPriorityCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority()));
        recTopicCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTopic()));
        recAdviceCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAdvice()));
        recommendTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void bindReport(DetailedPlanReport plan) {
        summaryLabel.setText(plan.getSummary());
        cropLabel.setText(plan.getCropName());
        locationLabel.setText(plan.getLocation() + " / " + plan.getSoil());
        plantGradeLabel.setText(plan.getPlantGrade());
        liveGradeLabel.setText(plan.getLiveGrade());
        careLabel.setText(String.valueOf(plan.getCareScore()));
        fertilizerLabel.setText("Fertilizer plan: " + plan.getFertilizerPlan());
        stepsTable.setItems(FXCollections.observableArrayList(plan.getSteps()));
        recommendTable.setItems(FXCollections.observableArrayList(plan.getRecommendations()));
    }
}
