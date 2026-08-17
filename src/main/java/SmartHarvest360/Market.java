package SmartHarvest360;

import SmartHarvest360.model.SaleRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles market prices and buyer information for selling crops.
 *
 * Buyer information is loaded from market_locations.csv based
 * on the farm's selected Malaysia state.
 */
public class Market {

    private final Random random;

    public Market() {
        this(System.nanoTime());
    }

    public Market(long seed) {
        random = new Random(seed);
    }

    /**
     * Returns market prices for the selected location.
     *
     * The prices are based on the crop's existing market price
     * multiplied by the buyer's location-specific price multiplier.
     */
    public Map<String, Double> getMarketPrices(
            Crop crop,
            String location) {

        Map<String, Double> prices =
                new LinkedHashMap<>();

        if (crop == null) {
            return prices;
        }

        double basePrice =
                crop.getMarketPrice();

        List<MarketLocation> locations =
                MarketLocationDataStore
                        .getMarketsForLocation(location);

        for (MarketLocation market : locations) {

            double price =
                    basePrice
                            * market.getPriceMultiplier();

            prices.put(
                    market.getMarketName(),
                    round2(price)
            );
        }

        return prices;
    }

    /**
     * Compatibility method.
     *
     * If older parts of the project call getMarketPrices(crop),
     * this still works using the default local market structure.
     */
    public Map<String, Double> getMarketPrices(
            Crop crop) {

        double basePrice =
                crop.getMarketPrice();

        Map<String, Double> prices =
                new LinkedHashMap<>();

        prices.put(
                "Local Market",
                round2(basePrice * 1.00)
        );

        prices.put(
                "Farm Cooperative",
                round2(basePrice * 0.97)
        );

        prices.put(
                "Wholesale Buyer",
                round2(basePrice * 0.94)
        );

        return prices;
    }

    /** Applies a small seeded daily price drift for reproducible simulations. */
    public Map<String, Double> nextDayPrices(Map<String, Double> previous) {
        Map<String, Double> next = new LinkedHashMap<>();
        if (previous == null) return next;
        for (Map.Entry<String, Double> entry : previous.entrySet()) {
            double factor = 0.95 + random.nextDouble() * 0.10;
            next.put(entry.getKey(), round2(entry.getValue() * factor));
        }
        return next;
    }

    /**
     * Returns market options for the selected farm location.
     */
    public List<MarketOption> getMarketOptions(
            Crop crop,
            String location) {

        List<MarketOption> markets =
                new ArrayList<>();

        if (crop == null) {
            return markets;
        }

        double basePrice =
                crop.getMarketPrice();

        List<MarketLocation> locations =
                MarketLocationDataStore
                        .getMarketsForLocation(location);

        for (MarketLocation market : locations) {

            double price =
                    basePrice
                            * market.getPriceMultiplier();

            markets.add(
                    new MarketOption(
                            market.getMarketName(),
                            market.getBuyerType(),
                            round2(price),
                            market.getDemand(),
                            market.getDistanceKm(),
                            market.getLogoPath()
                    )
            );
        }

        /*
         * If the CSV has no buyers for the selected location,
         * provide safe fallback buyers so the application
         * does not become unusable.
         */
        if (markets.isEmpty()) {

            markets.add(
                    new MarketOption(
                            "Local Market",
                            "Wet Market",
                            round2(basePrice),
                            "High",
                            2.0,
                            "/images/shops/local-market.png"
                    )
            );

            markets.add(
                    new MarketOption(
                            "Wholesale Buyer",
                            "Wholesale",
                            round2(basePrice * 0.94),
                            "Medium",
                            5.0,
                            "/images/shops/wholesale.png"
                    )
            );
        }

        return markets;
    }

    /**
     * Compatibility method for existing code.
     *
     * This keeps the previous application behaviour if another
     * screen still calls getMarketOptions(crop).
     */
    public List<MarketOption> getMarketOptions(
            Crop crop) {

        double basePrice =
                crop.getMarketPrice();

        List<MarketOption> markets =
                new ArrayList<>();

        markets.add(
                new MarketOption(
                        "Local Market",
                        "Wet Market",
                        round2(basePrice),
                        "High",
                        2.0,
                        "/images/shops/local-market.png"
                )
        );

        markets.add(
                new MarketOption(
                        "Farm Cooperative",
                        "Cooperative",
                        round2(basePrice * 0.97),
                        "Medium",
                        4.0,
                        "/images/shops/farm-cooperative.png"
                )
        );

        markets.add(
                new MarketOption(
                        "Wholesale Buyer",
                        "Wholesale",
                        round2(basePrice * 0.94),
                        "Low",
                        7.0,
                        "/images/shops/wholesale.png"
                )
        );

        return markets;
    }

    /**
     * Returns the market with the highest price.
     */
    public String getBestMarket(
            Map<String, Double> marketPrices) {

        return marketPrices.entrySet()
                .stream()
                .max(
                        Map.Entry.comparingByValue()
                )
                .map(
                        Map.Entry::getKey
                )
                .orElse(null);
    }

    /**
     * Executes a sale and creates a SaleRecord.
     */
    public SaleRecord sell(
            int day,
            Crop crop,
            double quantity,
            String market,
            double unitPrice) {

        double revenue =
                quantity * unitPrice;

        double cost =
                quantity
                        * crop.getCostPerKg();

        return new SaleRecord(
                day,
                crop.getName(),
                quantity,
                market,
                unitPrice,
                revenue,
                cost
        );
    }

    private double round2(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}
