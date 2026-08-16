package SmartHarvest360;

/**
 * Represents a physical buyer/shop available near a farm.
 */
public class MarketLocation {

    private final String location;
    private final String marketName;
    private final String buyerType;
    private final double distanceKm;
    private final double priceMultiplier;
    private final String demand;
    private final String logoPath;

    public MarketLocation(
            String location,
            String marketName,
            String buyerType,
            double distanceKm,
            double priceMultiplier,
            String demand,
            String logoPath) {

        this.location = location;
        this.marketName = marketName;
        this.buyerType = buyerType;
        this.distanceKm = distanceKm;
        this.priceMultiplier = priceMultiplier;
        this.demand = demand;
        this.logoPath = logoPath;
    }

    public String getLocation() {
        return location;
    }

    public String getMarketName() {
        return marketName;
    }

    public String getBuyerType() {
        return buyerType;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public String getDemand() {
        return demand;
    }

    public String getLogoPath() {
        return logoPath;
    }

    @Override
    public String toString() {
        return marketName
                + " | "
                + buyerType
                + " | "
                + distanceKm
                + " km";
    }
}