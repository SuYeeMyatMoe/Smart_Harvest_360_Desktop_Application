package SmartHarvest360.data;

import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SeasonHistory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Persists harvest transactions and final season summaries as CSV files. */
public final class CsvDataStore {
    private static final Path DATA_DIRECTORY = DataPaths.dataDirectory();
    private static final Path HARVEST_LOG = DATA_DIRECTORY.resolve("harvest_log.csv");
    private static final Path SEASON_REPORT = DATA_DIRECTORY.resolve("season_report.csv");
    private static final Path SEASON_HISTORY = DATA_DIRECTORY.resolve("season_history.csv");
    private static final String SEASON_HISTORY_HEADER =
            "goal,revenue,cost,profit,roi,yieldKg,waterShortageDays,pestDays,"
                    + "droughtDays,frostDays,endingWater,endingFertilizer,endingBudget,plantedCrops";

    private CsvDataStore() {
    }

    public static void appendHarvest(SaleRecord sale) throws IOException {
        Files.createDirectories(DATA_DIRECTORY);
        if (Files.notExists(HARVEST_LOG)) {
            Files.writeString(
                    HARVEST_LOG,
                    "day,crop,quantitySold,market,revenue,cost,profit\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );
        }

        String row = String.format(
                Locale.US,
                "%d,%s,%.2f,%s,%.2f,%.2f,%.2f%n",
                sale.day(), csv(sale.cropName()), sale.quantity(), csv(sale.market()),
                sale.revenue(), sale.cost(), sale.profit()
        );
        Files.writeString(
                HARVEST_LOG, row, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
        );
    }

    public static void saveSeasonReport(List<SaleRecord> sales) throws IOException {
        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;

        Files.createDirectories(DATA_DIRECTORY);
        String report = String.format(
                Locale.US,
                "metric,value%ntotalRevenue,%.2f%ntotalCost,%.2f%ntotalProfit,%.2f%nroi,%.2f%%%n",
                revenue, cost, profit, roi
        );
        Files.writeString(
                SEASON_REPORT, report, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Appends one finished season to the append-only history file. The file is
     * never truncated, so every season's row survives for season-over-season
     * comparison. Returns the history list in file order (oldest first); the
     * season number of any row is its index + 1.
     */
    public static void appendSeasonHistory(SeasonHistory history) throws IOException {
        Files.createDirectories(DATA_DIRECTORY);
        if (Files.notExists(SEASON_HISTORY)) {
            Files.writeString(
                    SEASON_HISTORY, SEASON_HISTORY_HEADER + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE
            );
        }

        String row = String.format(
                Locale.US,
                "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%.2f,%.2f,%.2f,%s%n",
                csv(history.goal()),
                history.revenue(), history.cost(), history.profit(), history.roi(),
                history.yieldKg(),
                history.waterShortageDays(), history.pestDays(),
                history.droughtDays(), history.frostDays(),
                history.endingWater(), history.endingFertilizer(), history.endingBudget(),
                csv(history.plantedCrops())
        );
        Files.writeString(
                SEASON_HISTORY, row, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
        );
    }

    /** Reads every stored season, oldest first. An empty file yields an empty list. */
    public static List<SeasonHistory> loadSeasonHistory() throws IOException {
        if (Files.notExists(SEASON_HISTORY)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(SEASON_HISTORY, StandardCharsets.UTF_8);
        List<SeasonHistory> seasons = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> field = splitCsvLine(lines.get(i));
            if (field.size() < 14) {
                continue;
            }
            seasons.add(new SeasonHistory(
                    field.get(0),
                    parseDouble(field.get(1)),
                    parseDouble(field.get(2)),
                    parseDouble(field.get(3)),
                    parseDouble(field.get(4)),
                    parseDouble(field.get(5)),
                    parseInt(field.get(6)),
                    parseInt(field.get(7)),
                    parseInt(field.get(8)),
                    parseInt(field.get(9)),
                    parseDouble(field.get(10)),
                    parseDouble(field.get(11)),
                    parseDouble(field.get(12)),
                    field.get(13)
            ));
        }
        return seasons;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    /** Minimal CSV line splitter that honours double-quoted fields (e.g. "Tomato, Chili"). */
    private static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result;
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
