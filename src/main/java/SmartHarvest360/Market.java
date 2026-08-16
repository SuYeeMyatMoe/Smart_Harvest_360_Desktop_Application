package SmartHarvest360;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Displays market prices for a crop.
 * Prices drift day to day, so the best market can change across a season.
 */
public class Market {

    private Random random;

    public Market() {
        this(System.nanoTime());
    }

    public Market(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Returns a market-to-price map for the crop, based on its reference
     * market price. The UI reads this to build the market comparison list.
     */
    public Map<String, Double> getMarketPrices(Crop crop) {
        double basePrice = crop.getMarketPrice();

        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Local Market", round2(basePrice * (0.9 + random.nextDouble() * 0.2)));
        prices.put("Farm Cooperative", round2(basePrice * (0.95 + random.nextDouble() * 0.15)));
        prices.put("Wholesale Buyer", round2(basePrice * (0.85 + random.nextDouble() * 0.25)));

        return prices;
    }

    /** Applies a small random walk to the previous day's prices. */
    public Map<String, Double> nextDayPrices(Map<String, Double> previous) {
        Map<String, Double> next = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : previous.entrySet()) {
            double factor = 0.95 + random.nextDouble() * 0.10;
            next.put(entry.getKey(), round2(entry.getValue() * factor));
        }
        return next;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
