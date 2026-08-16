package SmartHarvest360;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Simple no-framework smoke test that loads all application FXML screens. */
public final class FxmlSmokeTest {
    private FxmlSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> { });
        Platform.runLater(() -> {
            try {
                for (String screen : List.of(
                        "/fxml/WelcomeScreen.fxml",
                        "/fxml/FarmSetupScreen.fxml",
                        "/fxml/CropSelectionScreen.fxml",
                        "/fxml/SimulationScreen.fxml",
                        "/fxml/HarvestMarketScreen.fxml",
                        "/fxml/SeasonReportScreen.fxml",
                        "/fxml/FieldsOverviewScreen.fxml"
                )) {
                    FXMLLoader.load(FxmlSmokeTest.class.getResource(screen));
                    System.out.println("Loaded " + screen);
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });

        finished.await();
        Platform.exit();
        if (failure.get() != null) {
            throw new AssertionError("FXML smoke test failed", failure.get());
        }
    }
}
