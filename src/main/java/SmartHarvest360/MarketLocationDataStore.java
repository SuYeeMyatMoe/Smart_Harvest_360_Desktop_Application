package SmartHarvest360;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads buyer/shop information from market_locations.csv.
 *
 * The CSV is stored in the project's external data folder:
 *
 * data/market_locations.csv
 */
public final class MarketLocationDataStore {

    private static final Path CSV_PATH =
            Path.of("data", "market_locations.csv");

    private MarketLocationDataStore() {
    }

    /**
     * Loads all market locations from the CSV file.
     */
    public static List<MarketLocation> loadAll() {

        if (!Files.exists(CSV_PATH)) {

            System.err.println(
                    "Could not find market CSV: "
                            + CSV_PATH.toAbsolutePath()
            );

            return Collections.emptyList();
        }

        List<MarketLocation> markets =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                CSV_PATH,
                                StandardCharsets.UTF_8
                        )
        ) {

            String header = reader.readLine();

            if (header == null) {
                return markets;
            }

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String[] values = parseCsvLine(line);

                if (values.length < 7) {

                    System.err.println(
                            "Skipping invalid market row: "
                                    + line
                    );

                    continue;
                }

                try {

                    String location =
                            values[0].trim();

                    String marketName =
                            values[1].trim();

                    String buyerType =
                            values[2].trim();

                    double distanceKm =
                            Double.parseDouble(
                                    values[3].trim()
                            );

                    double priceMultiplier =
                            Double.parseDouble(
                                    values[4].trim()
                            );

                    String demand =
                            values[5].trim();

                    String logoPath =
                            values[6].trim();

                    markets.add(
                            new MarketLocation(
                                    location,
                                    marketName,
                                    buyerType,
                                    distanceKm,
                                    priceMultiplier,
                                    demand,
                                    logoPath
                            )
                    );

                } catch (NumberFormatException exception) {

                    System.err.println(
                            "Skipping invalid market row: "
                                    + line
                    );
                }
            }

        } catch (IOException exception) {

            System.err.println(
                    "Could not read market locations: "
                            + exception.getMessage()
            );
        }

        return markets;
    }

    /**
     * Returns buyers available in the selected Malaysian state.
     */
    public static List<MarketLocation> getMarketsForLocation(
            String location) {

        if (location == null || location.isBlank()) {
            return Collections.emptyList();
        }

        List<MarketLocation> results =
                new ArrayList<>();

        for (MarketLocation market : loadAll()) {

            if (market.getLocation()
                    .equalsIgnoreCase(location.trim())) {

                results.add(market);
            }
        }

        return results;
    }

    /**
     * Simple CSV parser supporting quoted fields.
     */
    private static String[] parseCsvLine(String line) {

        List<String> values =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char character = line.charAt(i);

            if (character == '"') {

                insideQuotes = !insideQuotes;

            } else if (
                    character == ','
                            && !insideQuotes) {

                values.add(
                        current.toString()
                );

                current.setLength(0);

            } else {

                current.append(character);
            }
        }

        values.add(
                current.toString()
        );

        return values.toArray(
                new String[0]
        );
    }
}