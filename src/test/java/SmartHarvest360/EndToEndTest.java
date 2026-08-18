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
                AppSession.getInstance().resetDemoSeason();

                Parent simulation = FXMLLoader.load(
                        EndToEndTest.class.getResource("/fxml/SimulationScreen.fxml")
                );
                Stage stage = new Stage();
                stage.setOpacity(0.0);
                stage.setScene(new Scene(simulation, 920, 640));
                stage.show();

                Button nextDay = require(simulation, "#nextDayButton", Button.class);
                Button harvest = require(simulation, "#harvestButton", Button.class);
                for (int i = 0; i < 89; i++) {
                    nextDay.fire();
                }

                check(AppSession.getInstance().getCurrentDay() == 90, "Simulation did not reach day 90");
                check(AppSession.getInstance().isCropReady(), "Crop is not ready after 90 days");
                check(harvest.isVisible() && !harvest.isDisabled(), "Harvest button was not enabled");
                harvest.fire();

                Parent market = stage.getScene().getRoot();
                Button sell = require(market, "#sellButton", Button.class);
                assertLabel(market, "#revenueLabel", "RM 2,750.00");
                assertLabel(market, "#costLabel", "RM 1,100.00");
                assertLabel(market, "#profitLabel", "RM 1,650.00");
                sell.fire();

                check(AppSession.getInstance().getSales().size() == 1, "Sale was not stored in the session");
                Parent report = stage.getScene().getRoot();
                assertLabel(report, "#revenueLabel", "RM 2,750.00");
                assertLabel(report, "#costLabel", "RM 1,100.00");
                assertLabel(report, "#profitLabel", "RM 1,650.00");
                assertLabel(report, "#roiLabel", "150.00%");

                BarChart<?, ?> chart = require(report, "#revenueByCropChart", BarChart.class);
                check(!chart.getData().isEmpty(), "Revenue by crop chart has no data");
                check(Files.exists(Path.of("data", "harvest_log.csv")), "harvest_log.csv was not created");
                check(Files.exists(Path.of("data", "season_report.csv")), "season_report.csv was not created");

                Button newSeason = require(report, "#newSeasonButton", Button.class);
                newSeason.fire();
                check(AppSession.getInstance().getSales().isEmpty(), "New Season did not clear old sales");
                require(stage.getScene().getRoot(), "#nextButton", Button.class);
                check(AppSession.getInstance().getFarm() == null, "New Season did not clear farm state");

                stage.close();
                System.out.println("END-TO-END TEST PASSED");
                System.out.println("Simulation: day 90 and harvest unlocked");
                System.out.println("Market: revenue/cost/profit calculations correct");
                System.out.println("Report: ROI and charts correct");
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
