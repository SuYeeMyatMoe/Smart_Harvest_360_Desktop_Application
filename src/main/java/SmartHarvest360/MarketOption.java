package SmartHarvest360;

/**
 * Represents a market option displayed to the farmer.
 */
public class MarketOption {

    private final String marketName;
    private final String buyerType;
    private final double pricePerKg;
    private final String demand;
    private final double distanceKm;
    private final String logoPath;

    public MarketOption(
            String marketName,
            String buyerType,
            double pricePerKg,
            String demand,
            double distanceKm,
            String logoPath) {

        this.marketName = marketName;
        this.buyerType = buyerType;
        this.pricePerKg = pricePerKg;
        this.demand = demand;
        this.distanceKm = distanceKm;
        this.logoPath = logoPath;
    }

    public String getMarketName() {
        return marketName;
    }

    public String getBuyerType() {
        return buyerType;
    }

    public double getPricePerKg() {
        return pricePerKg;
    }

    public String getDemand() {
        return demand;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public String getLogoPath() {
        return logoPath;
    }

    @Override
    public String toString() {
        return marketName
                + " | "
                + buyerType
                + " | RM"
                + String.format("%.2f", pricePerKg)
                + "/kg"
                + " | Demand: "
                + demand
                + " | "
                + String.format("%.1f km", distanceKm);
    }
}