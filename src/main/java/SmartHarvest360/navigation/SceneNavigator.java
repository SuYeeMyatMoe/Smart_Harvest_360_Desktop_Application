package SmartHarvest360.navigation;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes JavaFX scene changes and keeps a view-history stack so every
 * sub-page can offer a prominent Back button and a clickable breadcrumb trail.
 * The root dashboard (Welcome) has no nav bar: Back stays hidden there.
 */
public final class SceneNavigator {
    private static final List<String> historyStack = new ArrayList<>();
    private static String currentScreenPath;

    private SceneNavigator() {
    }

    /** Registers the initial root dashboard so the Back button stays hidden there. */
    public static void registerRoot(String fxmlResource) {
        historyStack.clear();
        historyStack.add(fxmlResource);
        currentScreenPath = fxmlResource;
    }

    /** Pushes a screen onto the view stack and renders it. Same-screen navigation only refreshes. */
    public static void switchTo(Node source, String fxmlResource) {
        clearFocusFromTextInput(source);
        if (currentScreenPath == null || !currentScreenPath.equals(fxmlResource)) {
            historyStack.add(fxmlResource);
        }
        loadScreen(source, fxmlResource);
    }

    /** Pops the top of the view stack and returns to the previous page (no-op at the root). */
    public static void goBack(Node source) {
        if (historyStack.size() <= 1) {
            return;
        }
        clearFocusFromTextInput(source);
        historyStack.remove(historyStack.size() - 1);
        loadScreen(source, historyStack.get(historyStack.size() - 1));
    }

    /** Jumps to a breadcrumb segment, truncating the stack so later screens are dropped. */
    public static void jumpTo(Node source, int index) {
        if (index < 0 || index >= historyStack.size()) {
            return;
        }
        clearFocusFromTextInput(source);
        while (historyStack.size() - 1 > index) {
            historyStack.remove(historyStack.size() - 1);
        }
        loadScreen(source, historyStack.get(historyStack.size() - 1));
    }

    /** Clears the view history and starts a fresh flow at the given screen (e.g. New Season). */
    public static void resetTo(Node source, String fxmlResource) {
        clearFocusFromTextInput(source);
        historyStack.clear();
        historyStack.add(fxmlResource);
        loadScreen(source, fxmlResource);
    }

    /** The screen the user was on before the current one, or null at the start. */
    public static String getPreviousScreenPath() {
        return historyStack.size() >= 2 ? historyStack.get(historyStack.size() - 2) : null;
    }

    public static String getCurrentScreenPath() {
        return currentScreenPath;
    }

    private static void loadScreen(Node source, String fxmlResource) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlResource));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();

            // Preserve the window's current size and maximized state. The inner scene
            // size is used (not the decorated window height) so the window never
            // grows a title bar's worth of pixels on every navigation.
            Scene current = stage.getScene();
            double width = Math.max(current != null ? current.getWidth() : stage.getWidth(), 720.0);
            double height = Math.max(current != null ? current.getHeight() : stage.getHeight(), 640.0);
            boolean maximized = stage.isMaximized();

            Scene scene = new Scene(root, width, height);
            SmartHarvest360.ui.ThemeManager.apply(scene);
            stage.setScene(scene);
            if (maximized) {
                stage.setMaximized(true);
            } else {
                stage.centerOnScreen();
            }
            currentScreenPath = fxmlResource;
            installNavBar(root, fxmlResource);
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to open the next screen");
            alert.setContentText(exception.getMessage());
            alert.initOwner((Stage) source.getScene().getWindow());
            alert.show();
        }
    }

    /**
     * Fills the {@code #navBarSlot} placeholder in each sub-page FXML with a
     * prominent {@code <\u2190 Back} button and the dynamic breadcrumb trail.
     * The root dashboard has no slot, so nothing is shown there.
     */
    private static void installNavBar(Parent root, String currentPath) {
        Node slotNode = root.lookup("#navBarSlot");
        if (!(slotNode instanceof HBox slot)) {
            return; // root dashboard (Welcome) carries no nav bar
        }
        slot.getChildren().clear();
        boolean canGoBack = historyStack.size() > 1;

        Button back = new Button("\u2190 Back");
        back.getStyleClass().add("back-button");
        back.setVisible(canGoBack);
        back.setManaged(canGoBack);
        back.setOnAction(event -> goBack(back));
        slot.getChildren().add(back);

        HBox crumbBar = new HBox(6);
        crumbBar.setAlignment(Pos.CENTER_LEFT);
        crumbBar.getStyleClass().add("breadcrumb-bar");
        for (int i = 0; i < historyStack.size(); i++) {
            String path = historyStack.get(i);
            boolean last = i == historyStack.size() - 1;
            if (i > 0) {
                Label separator = new Label("\u203A");
                separator.getStyleClass().add("crumb-sep");
                crumbBar.getChildren().add(separator);
            }
            Label crumb = new Label(labelFor(path));
            if (last) {
                crumb.getStyleClass().add("crumb-current");
            } else {
                crumb.getStyleClass().add("crumb-link");
                int index = i;
                crumb.setOnMouseClicked(event -> jumpTo(back, index));
                crumb.setCursor(Cursor.HAND);
            }
            crumbBar.getChildren().add(crumb);
        }
        slot.getChildren().add(crumbBar);
    }

    private static String labelFor(String fxmlResource) {
        switch (fxmlResource) {
            case "/fxml/WelcomeScreen.fxml": return "Welcome";
            case "/fxml/FarmSetupScreen.fxml": return "Farm Setup";
            case "/fxml/CropSelectionScreen.fxml": return "Crops";
            case "/fxml/SimulationScreen.fxml": return "Simulation";
            case "/fxml/HarvestMarketScreen.fxml": return "Market";
            case "/fxml/SeasonReportScreen.fxml": return "Report";
            case "/fxml/FieldsOverviewScreen.fxml": return "Fields";
            default: return fxmlResource;
        }
    }

    private static void clearFocusFromTextInput(Node source) {
        if (source == null || source.getScene() == null) {
            return;
        }
        if (source.getScene().getFocusOwner() instanceof TextInputControl) {
            source.requestFocus();
        }
    }
}