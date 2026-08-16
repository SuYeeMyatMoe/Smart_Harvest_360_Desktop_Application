package SmartHarvest360;

import SmartHarvest360.session.AppSession;
import SmartHarvest360.ui.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Temporary dark-mode audit (deleted after use): renders every screen in the
 * dark theme, verifies key regions use the dark palette, and flags any large
 * near-white surfaces left behind by unstyled default controls.
 */
public final class DarkAuditHarness {
    private DarkAuditHarness() {
    }

    private static final int TOLERANCE = 28;

    private record Check(Node node, boolean edgeSample, int[]... acceptable) {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of("screenshots", "dark-audit");
        Files.createDirectories(outDir);
        List<String> results = new ArrayList<>();

        AtomicReference<AppSession> sessionRef = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
        });

        runOnFx(() -> {
            try {
                AppSession session = AppSession.getInstance();
                sessionRef.set(session);
                Resource resource = new Resource(200.0, 20.0, 10_000.0, 5.0);
                Farm farm = new Farm(resource);
                farm.addCrop(new VegetableCrop("Tomato", 90, 50, 2, 30, 2.0, 5.5));
                farm.addCrop(new VegetableCrop("Corn", 100, 80, 4, 50, 1.8, 4.5));
                session.prepareFarm("Dark Audit", farm);
                session.startSimulation(farm, farm.getCrops().get(0));
                for (int i = 0; i < 400 && !session.isCropReady(); i++) {
                    session.getSeason().advanceDay();
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                ready.countDown();
            }
        });
        ready.await();
        if (failure.get() != null) {
            throw new AssertionError("setup failed", failure.get());
        }

        audit(outDir, results, sessionRef, "/fxml/WelcomeScreen.fxml", "welcome",
                List.of(new Check(null, false, null)), false);
        audit(outDir, results, sessionRef, "/fxml/FarmSetupScreen.fxml", "farm-setup",
                List.of(new Check(null, false, null)), false);
        audit(outDir, results, sessionRef, "/fxml/CropSelectionScreen.fxml", "crop-selection",
                List.of(new Check(null, false, null)), false);
        audit(outDir, results, sessionRef, "/fxml/FieldsOverviewScreen.fxml", "fields-overview",
                List.of(new Check(null, false, null)), false);
        audit(outDir, results, sessionRef, "/fxml/SimulationScreen.fxml", "simulation",
                List.of(new Check(null, false, null)), false);
        audit(outDir, results, sessionRef, "/fxml/HarvestMarketScreen.fxml", "harvest-market",
                List.of(new Check(null, false, null)), false);

        // Sell both crops in memory so the report has real data (no CSV writes).
        runOnFx(() -> {
            AppSession session = sessionRef.get();
            session.getSeason().getUnsoldReadyCrops().forEach(engine -> {
                session.addSale(new model.SaleRecord(
                        session.getCurrentDay(), engine.getCropName(), 30.0,
                        "Local Market", 5.0, 150.0, 60.0));
                session.markSold(engine.getCropName());
            });
        });
        audit(outDir, results, sessionRef, "/fxml/SeasonReportScreen.fxml", "season-report",
                List.of(new Check(null, false, null)), false);

        results.forEach(System.out::println);
        System.out.println("AUDIT SCREENSHOTS IN " + outDir.toAbsolutePath());
        System.exit(0);
    }

    private static void audit(Path outDir, List<String> results, AtomicReference<AppSession> sessionRef,
                              String fxml, String name, List<Check> checks, boolean dark) throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        runOnFx(() -> {
            try {
                Parent root = FXMLLoader.load(DarkAuditHarness.class.getResource(fxml));
                Stage stage = new Stage();
                stage.setOpacity(0.0);
                stage.setScene(new Scene(root, 960, 680));
                ThemeManager.apply(stage.getScene());
                stage.show();
                stageRef.set(stage);
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                loaded.countDown();
            }
        });
        loaded.await();
        if (failure.get() != null) {
            throw new AssertionError("load failed for " + fxml, failure.get());
        }
        if (!ThemeManager.isDark()) {
            runOnFx(() -> ThemeManager.toggle(lookup(stageRef.get(), "#themeButton")));
        }
        Thread.sleep(400); // let the dark stylesheet apply

        StringBuilder line = new StringBuilder(name + ":");
        AtomicReference<String> auditLine = new AtomicReference<>();
        runOnFx(() -> {
            WritableImage image = stageRef.get().getScene().snapshot(null);
            Path file = outDir.resolve(name + "-dark.png");
            writePng(image, file);
            PixelReader reader = image.getPixelReader();
            double scaleX = image.getWidth() / 960.0;
            double scaleY = image.getHeight() / 680.0;
            StringBuilder audit = new StringBuilder();

            // Key region checks.
            audit.append(checkRegion(reader, stageRef.get(), "#cityCombo", scaleX, scaleY,
                    rgb(0x10, 0x1A, 0x15)));
            audit.append(checkRegion(reader, stageRef.get(), "#budgetField", scaleX, scaleY,
                    rgb(0x10, 0x1A, 0x15)));
            audit.append(checkRegion(reader, stageRef.get(), ".crop-icon", scaleX, scaleY,
                    rgb(0x1E, 0x3A, 0x2C), rgb(0x63, 0xC7, 0x8F)));
            audit.append(checkRegion(reader, stageRef.get(), "#cropList", scaleX, scaleY,
                    rgb(0x18, 0x23, 0x1E), rgb(0x1C, 0x2A, 0x23)));
            audit.append(checkRegion(reader, stageRef.get(), "#simulationLog", scaleX, scaleY,
                    rgb(0x18, 0x23, 0x1E)));
            audit.append(checkRegion(reader, stageRef.get(), "#growthProgress", scaleX, scaleY,
                    rgb(0x2A, 0x3A, 0x32), rgb(0x43, 0xA4, 0x7D), rgb(0x1F, 0x7D, 0x59)));
            audit.append(checkRegion(reader, stageRef.get(), "#priceChart", scaleX, scaleY,
                    rgb(0x12, 0x1B, 0x16)));
            audit.append(checkRegion(reader, stageRef.get(), "#cropCombo", scaleX, scaleY,
                    rgb(0x10, 0x1A, 0x15)));
            audit.append(checkRegion(reader, stageRef.get(), "#marketList", scaleX, scaleY,
                    rgb(0x18, 0x23, 0x1E), rgb(0x1C, 0x2A, 0x23)));
            audit.append(checkRegion(reader, stageRef.get(), "#revenueChart", scaleX, scaleY,
                    rgb(0x18, 0x23, 0x1E)));
            audit.append(checkRegion(reader, stageRef.get(), ".field-card", scaleX, scaleY,
                    rgb(0x1A, 0x25, 0x1F), rgb(0x12, 0x1B, 0x16)));
            audit.append(checkRegion(reader, stageRef.get(), ".task-card", scaleX, scaleY,
                    rgb(0x1A, 0x25, 0x1F)));
            audit.append(checkRegion(reader, stageRef.get(), ".calendar-day", scaleX, scaleY,
                    rgb(0x1E, 0x2C, 0x24)));
            audit.append(checkRegion(reader, stageRef.get(), ".footer-bar", scaleX, scaleY,
                    rgb(0x16, 0x20, 0x1C)));

            // Near-white surface scan (unstyled default controls shine through).
            int bright = countBright(reader, image.getWidth(), image.getHeight());
            audit.append(bright > 4000 ? String.format(" [FAIL] %d bright px", bright)
                    : String.format(" [PASS] %d bright px", bright));
            auditLine.set(audit.toString());
        });
        line.append(auditLine.get());
        results.add(line.toString());
    }

    private static int[] rgb(int r, int g, int b) {
        return new int[]{r, g, b};
    }

    private static String checkRegion(PixelReader reader, Stage stage, String selector,
                                      double scaleX, double scaleY, int[]... acceptable) {
        Node node = lookup(stage, selector);
        if (node == null) {
            return String.format(" [WARN] %s not found", selector);
        }
        Bounds scene = node.localToScene(node.getBoundsInLocal());
        int x = (int) Math.round((scene.getMinX() + 6) * scaleX);
        int y = (int) Math.round((scene.getMinY() + scene.getHeight() / 2) * scaleY);
        int argb = reader.getArgb(x, y);
        for (int[] color : acceptable) {
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            if (Math.abs(r - color[0]) <= TOLERANCE && Math.abs(g - color[1]) <= TOLERANCE
                    && Math.abs(b - color[2]) <= TOLERANCE) {
                return String.format(" [PASS] %s #%06X", selector, argb & 0xFFFFFF);
            }
        }
        return String.format(" [FAIL] %s #%06X", selector, argb & 0xFFFFFF);
    }

    private static Node lookup(Stage stage, String selector) {
        return stage.getScene().getRoot().lookup(selector);
    }

    private static int countBright(PixelReader reader, int w, int h) {
        int count = 0;
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int argb = reader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                if (r > 238 && g > 238 && b > 238) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void writePng(WritableImage image, Path file) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(buffered, "png", file.toFile());
        } catch (Exception exception) {
            throw new RuntimeException("Could not write " + file, exception);
        }
    }

    private static void runOnFx(Runnable runnable) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            runnable.run();
            return null;
        });
        Platform.runLater(task);
        task.get();
    }
}
