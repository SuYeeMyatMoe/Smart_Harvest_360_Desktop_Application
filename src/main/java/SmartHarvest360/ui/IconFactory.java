package SmartHarvest360.ui;

import SmartHarvest360.Weather;
import javafx.scene.shape.SVGPath;

import java.util.Locale;

/**
 * Font-independent vector icons for crops, weather, and status messages.
 * <p>
 * Icons are rendered as filled SVG paths, so they never depend on a font that
 * carries emoji glyphs (raw emoji rendered as tofu boxes in the app's font).
 * Colours come from CSS classes so the light and dark themes can tint them.
 */
public final class IconFactory {

    /** Broccoli-style vegetable glyph, drawn on a 24x24 design grid. */
    private static final String VEGETABLE =
            "M11 21 L 11 14 L 13 14 L 13 21 Z"
            + " M8.5 14 C 6.1 14 4.2 12.1 4.2 9.8 C 4.2 7.5 6.1 5.6 8.5 5.6"
            + " C 10.9 5.6 12.8 7.5 12.8 9.8 C 12.8 12.1 10.9 14 8.5 14 Z"
            + " M12 11.2 C 9.6 11.2 7.7 9.3 7.7 7 C 7.7 4.7 9.6 2.8 12 2.8"
            + " C 14.4 2.8 16.3 4.7 16.3 7 C 16.3 9.3 14.4 11.2 12 11.2 Z"
            + " M15.5 14 C 13.1 14 11.2 12.1 11.2 9.8 C 11.2 7.5 13.1 5.6 15.5 5.6"
            + " C 17.9 5.6 19.8 7.5 19.8 9.8 C 19.8 12.1 17.9 14 15.5 14 Z";

    /** Tomato glyph. */
    private static final String FRUIT =
            "M12 20.5 C 16.7 20.5 20.5 16.7 20.5 12 C 20.5 7.3 16.7 3.5 12 3.5"
            + " C 7.3 3.5 3.5 7.3 3.5 12 C 3.5 16.7 7.3 20.5 12 20.5 Z"
            + " M12 3.5 C 10.4 1.8 7.6 2 6.4 3.5 C 8.2 5.4 10.6 5.6 12 3.5 Z";

    /** Wheat-ear glyph. */
    private static final String GRAIN =
            "M11 21 L 11 13.5 L 13 13.5 L 13 21 Z"
            + " M12 13.5 C 8.7 13.5 6 11.2 6 8.2 C 6 5.2 8.7 2.9 12 2.9"
            + " C 15.3 2.9 18 5.2 18 8.2 C 18 11.2 15.3 13.5 12 13.5 Z"
            + " M12 15 C 8.2 15 6 12.4 6 9.6 C 8.3 10.2 10.8 12.2 12 15 Z"
            + " M12 17.5 C 15.8 17.5 18 14.9 18 12.1 C 15.7 12.7 13.2 14.7 12 17.5 Z";

    /** Seedling glyph (default category / empty state). */
    private static final String SPROUT =
            "M11 20.5 L 11 10 C 11 6.8 8.8 4.6 6.2 4.4 C 6.9 6.9 8.9 8.8 11 9.3 Z"
            + " M13 20.5 L 13 10 C 13 6.8 15.2 4.6 17.8 4.4 C 17.1 6.9 15.1 8.8 13 9.3 Z";

    /** Warning triangle glyph. */
    private static final String WARNING =
            "M12 3.2 L 21.6 19.6 L 2.4 19.6 Z"
            + " M11 9.4 L 13 9.4 L 12.7 14.8 L 11.3 14.8 Z"
            + " M12 16.4 C 11.66 16.4 11.38 16.68 11.38 17.02 C 11.38 17.36"
            + " 11.66 17.64 12 17.64 C 12.34 17.64 12.62 17.36 12.62 17.02"
            + " C 12.62 16.68 12.34 16.4 12 16.4 Z";

    /** Sun glyph (weather + light-mode toggle). */
    private static final String SUN =
            "M11 0.9 L 13 0.9 L 13 3.4 L 11 3.4 Z"
            + " M11 20.6 L 13 20.6 L 13 23.1 L 11 23.1 Z"
            + " M0.9 11 L 3.4 11 L 3.4 13 L 0.9 13 Z"
            + " M20.6 11 L 23.1 11 L 23.1 13 L 20.6 13 Z"
            + " M12 18 C 15.31 18 18 15.31 18 12 C 18 8.69 15.31 6 12 6"
            + " C 8.69 6 6 8.69 6 12 C 6 15.31 8.69 18 12 18 Z";

    /** Crescent moon glyph (dark-mode toggle). */
    private static final String MOON =
            "M12.1 2 C 6.5 2 2 6.5 2 12.1 C 2 17.7 6.5 22.2 12.1 22.2"
            + " C 14.3 22.2 16.3 21.5 18 20.3 C 13.6 20 10.1 16.5 10.1 12.1"
            + " C 10.1 7.7 13.6 4.2 18 3.9 C 16.3 2.7 14.3 2 12.1 2 Z";

    /** Cloud glyph. */
    private static final String CLOUD =
            "M3.8 19.3 L 21.4 19.3"
            + " C 23.1 19.3 24.4 17.9 24.2 16.2"
            + " C 24.1 14.9 23.3 13.9 22.2 13.5"
            + " C 22.1 10.9 20.6 8.9 18.5 8.1"
            + " C 17.8 5.7 15.8 4 13.5 4"
            + " C 11.9 4 10.5 4.8 9.6 6.1"
            + " C 8.4 5.2 6.9 4.9 5.7 5.4"
            + " C 3.7 6.1 2.4 7.9 2.4 10"
            + " C 2.4 11 2.7 11.9 3.1 12.7"
            + " C 2.1 13.4 1.5 14.5 1.5 15.8"
            + " C 1.5 17.8 3.2 19.3 5.2 19.3 Z";

    /** Rain glyph: cloud with three falling drops. */
    private static final String RAIN = CLOUD
            + " M6.5 20.6 L 8 20.6 L 7.2 23.4 L 5.7 23.4 Z"
            + " M11 22 L 12.5 22 L 11.7 24.8 L 10.2 24.8 Z"
            + " M15.5 20.6 L 17 20.6 L 16.2 23.4 L 14.7 23.4 Z";

    private IconFactory() {
    }

    /** Crop-type icon (vegetable / fruit / grain, defaulting to a sprout). */
    public static SVGPath cropIcon(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (t.contains("vegetable")) {
            return icon(VEGETABLE, "icon-path-vegetable");
        }
        if (t.contains("fruit")) {
            return icon(FRUIT, "icon-path-fruit");
        }
        if (t.contains("grain")) {
            return icon(GRAIN, "icon-path-grain");
        }
        return icon(SPROUT, "icon-path-default");
    }

    /** Weather icon for the given condition, or null for "no weather yet". */
    public static SVGPath weatherIcon(Weather weather) {
        if (weather == null) {
            return null;
        }
        return switch (weather) {
            case SUNNY -> icon(SUN, "icon-path-sun");
            case RAIN -> icon(RAIN, "icon-path-rain");
            case CLOUDY -> icon(CLOUD, "icon-path-cloud");
        };
    }

    /** Sun glyph for the light-mode toggle button. */
    public static SVGPath sunIcon() {
        return icon(SUN, "icon-path-sun", 0.6);
    }

    /** Moon glyph for the dark-mode toggle button. */
    public static SVGPath moonIcon() {
        return icon(MOON, "icon-path-moon", 0.6);
    }

    /** Seedling glyph for the "no fields yet" empty state. */
    public static SVGPath sproutIcon() {
        return icon(SPROUT, "icon-path-default");
    }

    /** Warning triangle glyph for "needs attention" alerts. */
    public static SVGPath warningIcon() {
        return icon(WARNING, "icon-path-warning");
    }

    private static SVGPath icon(String pathData, String styleClass) {
        return icon(pathData, styleClass, 0.9);
    }

    private static SVGPath icon(String pathData, String styleClass, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setScaleX(scale);
        path.setScaleY(scale);
        path.getStyleClass().add("icon-path");
        path.getStyleClass().add(styleClass);
        return path;
    }
}
