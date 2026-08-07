package SmartHarvest360;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Verifies that all screens remain accessible in a 640x480 window. */
public final class ResponsiveLayoutTest {
    private ResponsiveLayoutTest() {
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> { });
        Platform.runLater(() -> {
            try {
                verify("SimulationScreen.fxml", "#nextDayButton");
                verify("HarvestMarketScreen.fxml", "#sellButton");
                verify("SeasonReportScreen.fxml", "#newSeasonButton");
                System.out.println("RESPONSIVE LAYOUT TEST PASSED (640x480)");
                System.out.println("Logo loaded on all screens");
                System.out.println("Horizontal and vertical scrolling available");
                System.out.println("Primary action buttons remain reachable");
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });

        finished.await();
        Platform.exit();
        if (failure.get() != null) {
            throw new AssertionError("Responsive layout test failed", failure.get());
        }
    }

    private static void verify(String fxml, String buttonSelector) throws Exception {
        Parent root = FXMLLoader.load(ResponsiveLayoutTest.class.getResource("/fxml/" + fxml));
        check(root instanceof ScrollPane, fxml + " root is not scrollable");
        ScrollPane scrollPane = (ScrollPane) root;

        Stage stage = new Stage();
        stage.setOpacity(0.0);
        stage.setScene(new Scene(root, 640, 480));
        stage.show();
        root.applyCss();
        root.layout();

        ImageView logo = require(root, "#appLogo", ImageView.class);
        check(logo.getImage() != null && !logo.getImage().isError(), fxml + " logo did not load");
        check(hasVisibleBar(root, Orientation.HORIZONTAL), fxml + " has no horizontal scrollbar");
        check(hasVisibleBar(root, Orientation.VERTICAL), fxml + " has no vertical scrollbar");

        Button action = require(root, buttonSelector, Button.class);
        scrollPane.setHvalue(scrollPane.getHmax());
        scrollPane.setVvalue(scrollPane.getVmax());
        root.layout();
        Bounds actionBounds = action.localToScene(action.getBoundsInLocal());
        check(actionBounds.getMinY() < 480 && actionBounds.getMaxY() > 0,
                fxml + " primary action is not reachable after scrolling");
        stage.close();
    }

    private static boolean hasVisibleBar(Parent root, Orientation orientation) {
        for (Node node : root.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == orientation && bar.isVisible()) {
                return true;
            }
        }
        return false;
    }

    private static <T> T require(Parent root, String selector, Class<T> type) {
        Node node = root.lookup(selector);
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
