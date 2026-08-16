package SmartHarvest360;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads the crop catalog from the crops CSV file. */
public class CSVFileHandler {

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
                } else if (type.equalsIgnoreCase("Grain")) {
                    crops.add(new GrainCrop(
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
}
