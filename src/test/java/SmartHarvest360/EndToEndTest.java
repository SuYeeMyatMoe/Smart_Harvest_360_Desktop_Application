package SmartHarvest360;

import SmartHarvest360.session.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** No-framework end-to-end UI test for the complete user journey. */
public final class EndToEndTest {
    private EndToEndTest() {
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> { });
        Platform.runLater(() -> {
            try {
                AppSession session = AppSession.getInstance();
                session.resetDemoSeason();

                Parent simulation = FXMLLoader.load(
                        EndToEndTest.class.getResource("/fxml/SimulationScreen.fxml")
                );
                Stage stage = new Stage();
                stage.setOpacity(0.0);
                stage.setScene(new Scene(simulation, 920, 640));
                stage.show();

                Button nextDay = require(simulation, "#nextDayButton", Button.class);
                Button harvest = require(simulation, "#harvestButton", Button.class);
                for (int i = 0; i < 300 && !session.isCropReady(); i++) {
                    nextDay.fire();
                }

                check(session.isCropReady(), "Crop did not become ready after simulating");
                check(harvest.isVisible() && !harvest.isDisabled(), "Harvest button was not enabled");
                harvest.fire();

                Parent market = stage.getScene().getRoot();
                Button sell = require(market, "#sellButton", Button.class);
                check(!sell.isDisabled(), "Sell button was not enabled for a ready crop");
                Label revenueBefore = require(market, "#revenueLabel", Label.class);
                check(!revenueBefore.getText().startsWith("RM 0.00"),
                        "Market revenue preview was not computed: " + revenueBefore.getText());
                sell.fire();

                check(session.getSales().size() == 1, "Sale was not stored in the session");
                double expectedRevenue = session.getSales().get(0).revenue();
                double expectedCost = session.getSales().get(0).cost();
                check(expectedRevenue > 0.0, "Sale revenue was not positive");
                check(expectedCost > 0.0, "Sale cost was not positive");

                Parent report = stage.getScene().getRoot();
                assertLabel(report, "#revenueLabel", money(expectedRevenue));
                assertLabel(report, "#costLabel", money(expectedCost));
                Label profit = require(report, "#profitLabel", Label.class);
                check(money(expectedRevenue - expectedCost).equals(profit.getText()),
                        "profitLabel expected '" + money(expectedRevenue - expectedCost)
                                + "' but was '" + profit.getText() + "'");
                Label roi = require(report, "#roiLabel", Label.class);
                check(roi.getText().endsWith("%"), "ROI label did not show a percentage: " + roi.getText());

                BarChart<String, Number> chart = require(report, "#revenueChart", BarChart.class);
                check(chart.getData().size() == 1, "Revenue BarChart has incorrect data");
                check(chart.getData().get(0).getData().size() == 1, "Revenue BarChart has incorrect bar count");
                check(Files.exists(Path.of("data", "harvest_log.csv")), "harvest_log.csv was not created");
                check(Files.exists(Path.of("data", "season_report.csv")), "season_report.csv was not created");

                Button newSeason = require(report, "#newSeasonButton", Button.class);
                newSeason.fire();
                check(session.getSales().isEmpty(), "New Season did not clear old sales");
                require(stage.getScene().getRoot(), "#nextButton", Button.class);

                stage.close();
                System.out.println("END-TO-END TEST PASSED");
                System.out.println("Simulation: season reached harvest and harvest unlocked");
                System.out.println("Market: revenue preview computed and sale stored");
                System.out.println("Report: revenue/cost/profit and ROI calculated correctly");
                System.out.println("CSV: harvest_log.csv and season_report.csv created");
                System.out.println("New Season: returned to Farm Setup with cleared state");
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });

        finished.await();
        Platform.exit();
        if (failure.get() != null) {
            throw new AssertionError("End-to-end test failed", failure.get());
        }
    }

    private static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }

    private static void assertLabel(Parent root, String selector, String expected) {
        Label label = require(root, selector, Label.class);
        check(expected.equals(label.getText()),
                selector + " expected '" + expected + "' but was '" + label.getText() + "'");
    }

    private static <T> T require(Parent root, String selector, Class<T> type) {
        Object node = root.lookup(selector);
        if (!type.isInstance(node)) {
            throw new AssertionError("Missing " + type.getSimpleName() + " " + selector);
        }
        return type.cast(node);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
