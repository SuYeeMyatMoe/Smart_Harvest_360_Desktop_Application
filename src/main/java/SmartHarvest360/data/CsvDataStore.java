package SmartHarvest360.data;

import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.ml.GradePredictor;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SimDayLog;
import SmartHarvest360.session.AppSession;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * CSV file handling for harvest logs, season reports, and user downloads.
 */
public final class CsvDataStore {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path HARVEST_LOG = DATA_DIRECTORY.resolve("harvest_log.csv");
    private static final Path SEASON_REPORT = DATA_DIRECTORY.resolve("season_report.csv");
    private static final Path ACTIVITY_LOG = DATA_DIRECTORY.resolve("activity_log.csv");
    private static final Path DOWNLOADS = DATA_DIRECTORY.resolve("downloads");

    private CsvDataStore() {
    }

    public static Path dataDirectory() {
        return DATA_DIRECTORY.toAbsolutePath().normalize();
    }

    public static Path seasonReportPath() {
        return SEASON_REPORT.toAbsolutePath().normalize();
    }

    public static Path harvestLogPath() {
        return HARVEST_LOG.toAbsolutePath().normalize();
    }

    public static Path downloadsDirectory() throws IOException {
        Files.createDirectories(DOWNLOADS);
        return DOWNLOADS.toAbsolutePath().normalize();
    }

    public static void appendHarvest(SaleRecord sale) throws IOException {
        Files.createDirectories(DATA_DIRECTORY);
        if (Files.notExists(HARVEST_LOG)) {
            Files.writeString(
                    HARVEST_LOG,
                    "day,crop,quantitySold,market,unitPrice,revenue,cost,profit\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );
        }

        String row = String.format(
                Locale.US,
                "%d,%s,%.2f,%s,%.2f,%.2f,%.2f,%.2f%n",
                sale.day(), csv(sale.cropName()), sale.quantity(), csv(sale.market()),
                sale.unitPrice(), sale.revenue(), sale.cost(), sale.profit()
        );
        Files.writeString(
                HARVEST_LOG, row, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
        );
    }

    /** Auto-save summary into data/season_report.csv (legacy compact format + extras). */
    public static void saveSeasonReport(List<SaleRecord> sales) throws IOException {
        AppSession session = AppSession.getInstance();
        writeFullSeasonReport(SEASON_REPORT, session, sales);
        writeActivityLog(ACTIVITY_LOG, session.getDayLogs());
    }

    /**
     * Writes a downloadable full season report (summary, grades, sales, activity).
     * Returns the path written.
     */
    public static Path exportSeasonReport(Path targetFile, AppSession session) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        writeFullSeasonReport(targetFile, session, session.getSales());
        return targetFile.toAbsolutePath().normalize();
    }

    /**
     * Saves a timestamped copy under data/downloads/ and returns that path.
     */
    public static Path downloadSeasonReportCopy(AppSession session) throws IOException {
        Files.createDirectories(DOWNLOADS);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String farm = session.getFarmName() == null ? "farm" : session.getFarmName().replaceAll("[^a-zA-Z0-9_-]+", "_");
        Path target = DOWNLOADS.resolve("season_report_" + farm + "_" + stamp + ".csv");
        return exportSeasonReport(target, session);
    }

    public static Path exportActivityLog(Path targetFile, List<SimDayLog> logs) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        writeActivityLog(targetFile, logs);
        return targetFile.toAbsolutePath().normalize();
    }

    public static Path exportHarvestLog(Path targetFile, List<SaleRecord> sales) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        StringBuilder builder = new StringBuilder();
        builder.append("day,crop,quantitySold,market,unitPrice,revenue,cost,profit\n");
        for (SaleRecord sale : sales) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%s,%.2f,%s,%.2f,%.2f,%.2f,%.2f%n",
                    sale.day(), csv(sale.cropName()), sale.quantity(), csv(sale.market()),
                    sale.unitPrice(), sale.revenue(), sale.cost(), sale.profit()
            ));
        }
        Files.writeString(targetFile, builder.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return targetFile.toAbsolutePath().normalize();
    }

    public static boolean openInFileManager(Path path) {
        try {
            Path openPath = Files.isDirectory(path) ? path : path.getParent();
            if (openPath == null || !Files.exists(openPath)) {
                return false;
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(openPath.toFile());
                return true;
            }
        } catch (Exception ignored) {
            // Fall through — caller shows path text instead.
        }
        return false;
    }

    private static void writeFullSeasonReport(
            Path target,
            AppSession session,
            List<SaleRecord> sales
    ) throws IOException {
        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;
        double adjustedRoi = GradePredictor.careAdjustedRoi(roi, session.getCareScore());
        String predicted = "-";
        AdvisorResult advice = session.getAdvisorResult();
        if (advice != null && advice.getPredictedGrade() != null) {
            predicted = advice.getPredictedGrade();
        }
        String actual = GradePredictor.gradeFromRoi(adjustedRoi, profit);
        if (profit >= 0 && session.getCareScore() >= 80) {
            actual = GradePredictor.nudgeGrade(actual, Math.min(100, session.getCareScore()));
        }

        FarmProfile profile = session.getFarmProfile();
        String location = profile == null ? "" : profile.getLocation();
        String soil = profile == null ? "" : profile.getSoilType();
        String crop = session.getActiveCrop() == null ? "" : session.getActiveCrop().getName();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder out = new StringBuilder();
        // BOM helps Excel open UTF-8 correctly on Windows.
        out.append('\uFEFF');
        out.append("section,key,value\n");
        out.append(row("summary", "generatedAt", stamp));
        out.append(row("summary", "farmName", nullToEmpty(session.getFarmName())));
        out.append(row("summary", "location", location));
        out.append(row("summary", "soil", soil));
        out.append(row("summary", "activeCrop", crop));
        out.append(row("summary", "careScore", String.valueOf(session.getCareScore())));
        out.append(row("summary", "totalRevenue", fmt(revenue)));
        out.append(row("summary", "totalCost", fmt(cost)));
        out.append(row("summary", "totalProfit", fmt(profit)));
        out.append(row("summary", "roiPercent", fmt(roi)));
        out.append(row("summary", "predictedGrade", predicted));
        out.append(row("summary", "actualGrade", actual));
        if (advice != null) {
            out.append(row("advisor", "fertilizerPlan", advice.getFertilizerPlan()));
            out.append(row("advisor", "fertilizerTip", advice.getFertilizerKgTip()));
            out.append(row("advisor", "rationale", advice.getRationale()));
        }

        out.append("\n");
        out.append("sales_day,crop,quantity,market,unitPrice,revenue,cost,profit\n");
        for (SaleRecord sale : sales) {
            out.append(String.format(
                    Locale.US,
                    "%d,%s,%.2f,%s,%.2f,%.2f,%.2f,%.2f%n",
                    sale.day(), csv(sale.cropName()), sale.quantity(), csv(sale.market()),
                    sale.unitPrice(), sale.revenue(), sale.cost(), sale.profit()
            ));
        }

        out.append("\n");
        out.append("activity_day,weather,action,waterUsed,fertilizerUsed,status,growthPercent\n");
        for (SimDayLog log : session.getDayLogs()) {
            out.append(String.format(
                    Locale.US,
                    "%d,%s,%s,%.2f,%.2f,%s,%d%n",
                    log.getDay(),
                    csv(log.getWeather()),
                    csv(log.getAction()),
                    log.getWaterUsed(),
                    log.getFertilizerUsed(),
                    csv(log.getStatus()),
                    log.getGrowthPercent()
            ));
        }

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        } else {
            Files.createDirectories(DATA_DIRECTORY);
        }
        Files.writeString(
                target, out.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static void writeActivityLog(Path target, List<SimDayLog> logs) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("day,weather,action,waterUsed,fertilizerUsed,status,growthPercent\n");
        for (SimDayLog log : logs) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%s,%s,%.2f,%.2f,%s,%d%n",
                    log.getDay(),
                    csv(log.getWeather()),
                    csv(log.getAction()),
                    log.getWaterUsed(),
                    log.getFertilizerUsed(),
                    csv(log.getStatus()),
                    log.getGrowthPercent()
            ));
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, builder.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String row(String section, String key, String value) {
        return section + "," + key + "," + csv(value == null ? "" : value) + "\n";
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String csv(String value) {
        String safe = asciiSafe(value);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    /** Replace fancy punctuation that breaks in Excel/ANSI viewers. */
    private static String asciiSafe(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00B7', '-')
                .replace('\u2022', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace("Â·", "-")
                .replace("â€”", "-")
                .replace("â€“", "-")
                .replace("â€™", "'");
    }
}
