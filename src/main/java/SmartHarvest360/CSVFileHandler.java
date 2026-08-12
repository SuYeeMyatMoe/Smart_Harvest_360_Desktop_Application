package SmartHarvest360;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads crop catalog data and writes harvest/sale CSV logs.
 * Harvest columns match CsvDataStore output:
 * day, crop, quantitySold, market, revenue, cost, profit
 */
public class CSVFileHandler {

    private static final String HEADER = "day,crop,quantitySold,market,revenue,cost,profit";

    /**
     * Loads crops from a CSV file.
     * Expected header:
     * name,type,growthDays,waterNeed,fertilizerNeed,yieldAmount,costPerKg,marketPrice
     */
    public List<Crop> loadCrops(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Crop database not found: " + path.toAbsolutePath());
        }

        List<Crop> crops = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (first) {
                    first = false;
                    if (line.toLowerCase().startsWith("name,")) {
                        continue;
                    }
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    throw new IOException("Invalid crop row (expected 8 columns): " + line);
                }

                String name = parts[0].trim();
                String type = parts[1].trim();
                int growthDays = Integer.parseInt(parts[2].trim());
                double waterNeed = Double.parseDouble(parts[3].trim());
                double fertilizerNeed = Double.parseDouble(parts[4].trim());
                double yieldAmount = Double.parseDouble(parts[5].trim());
                double costPerKg = Double.parseDouble(parts[6].trim());
                double marketPrice = Double.parseDouble(parts[7].trim());

                if (type.equalsIgnoreCase("Vegetable")) {
                    crops.add(new VegetableCrop(
                            name, growthDays, waterNeed, fertilizerNeed,
                            yieldAmount, costPerKg, marketPrice
                    ));
                } else if (type.equalsIgnoreCase("Fruit")) {
                    crops.add(new FruitCrop(
                            name, growthDays, waterNeed, fertilizerNeed,
                            yieldAmount, costPerKg, marketPrice
                    ));
                } else {
                    throw new IOException("Unknown crop type '" + type + "' for " + name);
                }
            }
        }
        return crops;
    }

    /**
     * Writes the crop catalog CSV (same header/columns as {@link #loadCrops(String)}).
     */
    public void saveCrops(String filePath, List<Crop> crops) throws IOException {
        Path path = Path.of(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (FileWriter writer = new FileWriter(path.toFile(), false)) {
            writer.write("name,type,growthDays,waterNeed,fertilizerNeed,yieldAmount,costPerKg,marketPrice\n");
            for (Crop crop : crops) {
                writer.write(String.format(
                        java.util.Locale.US,
                        "%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                        crop.getName().trim(),
                        crop.getType().trim(),
                        crop.getGrowthDays(),
                        crop.getWaterNeed(),
                        crop.getFertilizerNeed(),
                        crop.getYieldAmount(),
                        crop.getCostPerKg(),
                        crop.getMarketPrice()
                ));
            }
        }
    }

    /** Builds a Vegetable or Fruit crop from catalog fields. */
    public static Crop createCrop(
            String name,
            String type,
            int growthDays,
            double waterNeed,
            double fertilizerNeed,
            double yieldAmount,
            double costPerKg,
            double marketPrice
    ) {
        if (type != null && type.equalsIgnoreCase("Fruit")) {
            return new FruitCrop(
                    name, growthDays, waterNeed, fertilizerNeed,
                    yieldAmount, costPerKg, marketPrice
            );
        }
        return new VegetableCrop(
                name, growthDays, waterNeed, fertilizerNeed,
                yieldAmount, costPerKg, marketPrice
        );
    }

    /** Deep copy so planted crops are not mutated by catalog edits. */
    public static Crop copyOf(Crop crop) {
        if (crop == null) {
            return null;
        }
        return createCrop(
                crop.getName(),
                crop.getType(),
                crop.getGrowthDays(),
                crop.getWaterNeed(),
                crop.getFertilizerNeed(),
                crop.getYieldAmount(),
                crop.getCostPerKg(),
                crop.getMarketPrice()
        );
    }

    public void appendSale(String filePath, SaleRecord sale) throws IOException {
        File file = new File(filePath);
        boolean isNew = !file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (isNew) {
                writer.write(HEADER + "\n");
            }
            writer.write(String.format("%d,%s,%.2f,%s,%.2f,%.2f,%.2f%n",
                    sale.getDay(),
                    sale.getCropName(),
                    sale.getQuantity(),
                    sale.getMarket(),
                    sale.getRevenue(),
                    sale.getCost(),
                    sale.getProfit()));
        }
    }

    /** A full season report (all sales at once). */
    public void writeSeasonReport(String filePath, List<SaleRecord> sales) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, false)) {
            writer.write(HEADER + "\n");
            for (SaleRecord sale : sales) {
                writer.write(String.format("%d,%s,%.2f,%s,%.2f,%.2f,%.2f%n",
                        sale.getDay(),
                        sale.getCropName(),
                        sale.getQuantity(),
                        sale.getMarket(),
                        sale.getRevenue(),
                        sale.getCost(),
                        sale.getProfit()));
            }
        }
    }
}
