package SmartHarvest360;

public class MarketOption {

    private final String marketName;
    private final double pricePerKg;
    private final String demand;

    public MarketOption(String marketName, double pricePerKg, String demand) {
        this.marketName = marketName;
        this.pricePerKg = pricePerKg;
        this.demand = demand;
    }

    public String getMarketName() {
        return marketName;
    }

    public double getPricePerKg() {
        return pricePerKg;
    }

    public String getDemand() {
        return demand;
    }

    @Override
    public String toString() {
        return marketName + " | RM" + String.format("%.2f", pricePerKg) + "/kg | Demand: " + demand;
    }
}