package SmartHarvest360.data;

import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.ml.GradePredictor;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SeasonHistory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CSV file handling for harvest logs, season reports, and user downloads.
 */
public final class CsvDataStore {
    private static final Path DATA_DIRECTORY = DataPaths.dataDirectory();
    private static final Path HARVEST_LOG = DATA_DIRECTORY.resolve("harvest_log.csv");
    private static final Path SEASON_REPORT = DATA_DIRECTORY.resolve("season_report.csv");
    private static final Path ACTIVITY_LOG = DATA_DIRECTORY.resolve("activity_log.csv");
    private static final Path DOWNLOADS = DATA_DIRECTORY.resolve("downloads");
    private static final Path SEASON_HISTORY = DATA_DIRECTORY.resolve("season_history.csv");
    private static final String SEASON_HISTORY_HEADER =
            "goal,revenue,cost,profit,roi,yieldKg,waterShortageDays,pestDays,"
                    + "droughtDays,frostDays,endingWater,endingFertilizer,endingBudget,plantedCrops";

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
        if (!session.isSeasonHistorySaved()) {
            appendSeasonHistory(buildSeasonHistory(session, sales));
            session.markSeasonHistorySaved();
        }
    }

    private static SeasonHistory buildSeasonHistory(AppSession session, List<SaleRecord> sales) {
        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;
        double yield = sales.stream().mapToDouble(SaleRecord::quantity).sum();
        int shortage = (int) session.getDayLogs().stream().filter(log -> log.getWaterUsed() == 0.0).count();
        int pest = countLogText(session, "pest");
        int drought = countLogText(session, "drought");
        int frost = countLogText(session, "frost");
        double water = session.getFarm() == null ? 0.0 : session.getFarm().getResource().getWater();
        double fertilizer = session.getFarm() == null ? 0.0 : session.getFarm().getResource().getFertilizer();
        double budget = session.getFarm() == null ? 0.0 : session.getFarm().getResource().getBudget();
        String crops = session.getFarm() == null ? "" : session.getFarm().getCrops().stream()
                .map(crop -> crop.getName()).distinct().reduce((left, right) -> left + ", " + right).orElse("");
        return new SeasonHistory(session.getSeasonGoal().getLabel(), revenue, cost, profit, roi,
                yield, shortage, pest, drought, frost, water, fertilizer, budget, crops);
    }

    private static int countLogText(AppSession session, String wanted) {
        return (int) session.getDayLogs().stream()
                .filter(log -> (log.getAction() + " " + log.getStatus())
                        .toLowerCase(Locale.ROOT).contains(wanted))
                .count();
    }

    public static void appendSeasonHistory(SeasonHistory history) throws IOException {
        Files.createDirectories(DATA_DIRECTORY);
        if (Files.notExists(SEASON_HISTORY)) {
            Files.writeString(SEASON_HISTORY, SEASON_HISTORY_HEADER + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        String row = String.format(Locale.US,
                "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%.2f,%.2f,%.2f,%s%n",
                csv(history.goal()), history.revenue(), history.cost(), history.profit(), history.roi(),
                history.yieldKg(), history.waterShortageDays(), history.pestDays(),
                history.droughtDays(), history.frostDays(), history.endingWater(),
                history.endingFertilizer(), history.endingBudget(), csv(history.plantedCrops()));
        Files.writeString(SEASON_HISTORY, row, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static List<SeasonHistory> loadSeasonHistory() throws IOException {
        if (Files.notExists(SEASON_HISTORY)) return new ArrayList<>();
        List<String> lines = Files.readAllLines(SEASON_HISTORY, StandardCharsets.UTF_8);
        List<SeasonHistory> history = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> field = splitCsvLine(lines.get(index));
            if (field.size() < 14) continue;
            history.add(new SeasonHistory(field.get(0), number(field.get(1)), number(field.get(2)),
                    number(field.get(3)), number(field.get(4)), number(field.get(5)), integer(field.get(6)),
                    integer(field.get(7)), integer(field.get(8)), integer(field.get(9)), number(field.get(10)),
                    number(field.get(11)), number(field.get(12)), field.get(13)));
        }
        return history;
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

    private static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        result.add(current.toString());
        return result;
    }

    private static double number(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) { return 0.0; }
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
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
