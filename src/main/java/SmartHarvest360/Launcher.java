package SmartHarvest360;

/**
 * Application entry point. Keeping this class separate from JavaFX Application
 * avoids the JDK's module-path-only JavaFX launcher check.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        System.setProperty("prism.vsync", "true");
        SmartHarvestApp.main(args);
    }
}
