package SmartHarvest360.ui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;

/**
 * Toggles a dark theme across the whole application.
 * The active theme is remembered while screens are switched.
 */
public final class ThemeManager {
    private static final String LIGHT = ThemeManager.class.getResource("/style.css").toExternalForm();
    private static final String DARK = ThemeManager.class.getResource("/style-dark.css").toExternalForm();
    private static boolean dark;

    private ThemeManager() {
    }

    public static boolean isDark() {
        return dark;
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().remove(LIGHT);
        scene.getStylesheets().remove(DARK);
        scene.getStylesheets().add(dark ? DARK : LIGHT);
    }

    public static void toggle(Node node) {
        dark = !dark;
        if (node != null) {
            apply(node.getScene());
            updateButton(node);
        }
    }

    public static void syncButton(Button button) {
        if (button != null) {
            button.setText(dark ? "Light Mode" : "Dark Mode");
            button.setGraphic(dark ? IconFactory.sunIcon() : IconFactory.moonIcon());
        }
    }

    private static void updateButton(Node node) {
        if (node instanceof Button button) {
            syncButton(button);
        }
    }
}
