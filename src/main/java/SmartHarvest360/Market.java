package SmartHarvest360;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Diplay market prices for a crop and handles selling.
 */
public class Market {

    private final Random random = new Random();

    /**
     * Returns market name by price per kg, based on the crop's reference
     * market price. Then the UI reads this to build the market comparison list.
     */
    public Map<String, Double> getMarketPrices(Crop crop) {

        double basePrice = crop.getMarketPrice();

        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Local Market", round2(basePrice * (0.9 + random.nextDouble() * 0.2)));
        prices.put("Farm Cooperative", round2(basePrice * (0.95 + random.nextDouble() * 0.15)));
        prices.put("Wholesale Buyer", round2(basePrice * (0.85 + random.nextDouble() * 0.25)));

        return prices;
    }

    /** Convenience: highest-priced market from a price map. */
    public String getBestMarket(Map<String, Double> marketPrices) {
        return marketPrices.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Executes a sale and returns a filled-in SaleRecord.
     * revenue = quantity * unitPrice
     * cost    = quantity * crop.getCostPerKg()
     */
    public SaleRecord sell(int day, Crop crop, double quantity, String market, double unitPrice) {

        double revenue = quantity * unitPrice;
        double cost = quantity * crop.getCostPerKg();

        return new SaleRecord(day, crop.getName(), quantity, market, unitPrice, revenue, cost);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
