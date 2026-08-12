package SmartHarvest360;

import SmartHarvest360.model.SaleRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles market prices and market information for selling crops.
 */
public class Market {

    /**
     * Returns the market prices that are available for a crop.
     * The crop's existing market price is used as the reference price.
     */
    public Map<String, Double> getMarketPrices(Crop crop) {
        double basePrice = crop.getMarketPrice();
        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Local Market", round2(basePrice * 1.00));
        prices.put("Farm Cooperative", round2(basePrice * 0.97));
        prices.put("Wholesale Buyer", round2(basePrice * 0.94));
        return prices;
    }

    /**
     * Returns market options including price and demand.
     */
    public List<MarketOption> getMarketOptions(Crop crop) {
        double basePrice = crop.getMarketPrice();
        List<MarketOption> markets = new ArrayList<>();
        markets.add(new MarketOption("Local Market", round2(basePrice * 1.00), "High"));
        markets.add(new MarketOption("Farm Cooperative", round2(basePrice * 0.97), "Medium"));
        markets.add(new MarketOption("Wholesale Buyer", round2(basePrice * 0.94), "Low"));
        return markets;
    }

    /**
     * Returns the market with the highest price.
     * Kept for compatibility with the existing system.
     */
    public String getBestMarket(Map<String, Double> marketPrices) {
        return marketPrices.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Executes a sale and creates a SaleRecord.
     */
    public SaleRecord sell(int day, Crop crop, double quantity, String market, double unitPrice) {
        double revenue = quantity * unitPrice;
        double cost = quantity * crop.getCostPerKg();
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

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
