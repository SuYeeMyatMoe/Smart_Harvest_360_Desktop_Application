package SmartHarvest360.data;

import SmartHarvest360.model.SaleRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

/** Persists harvest transactions and final season summaries as CSV files. */
public final class CsvDataStore {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path HARVEST_LOG = DATA_DIRECTORY.resolve("harvest_log.csv");
    private static final Path SEASON_REPORT = DATA_DIRECTORY.resolve("season_report.csv");

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

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
