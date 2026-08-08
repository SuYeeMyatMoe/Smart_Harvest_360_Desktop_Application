package SmartHarvest360;

/**
 * This is a  completed sale that matches the fields that's been  built the
 * simulation screens is already using:
 * day, cropName, quantity, market, unitPrice, revenue, cost.
 *
 * TODO: check whether a SaleRecord class already exists on their branch.
 * If it does, we should only keep one version before merging — otherwise
 * Market.java and CSVFileHandler.java will be built against a different
 * SaleRecord than what the screens actually expect.
 *
 * Profit is calculated as revenue minus cost, rather than stored as its
 * own field, so the two numbers can't accidentally go out of sync.
 */




public class SaleRecord {

    private final int day;
    private final String cropName;
    private final double quantity;
    private final String market;
    private final double unitPrice;
    private final double revenue;
    private final double cost;

    public SaleRecord(int day, String cropName, double quantity, String market,
                       double unitPrice, double revenue, double cost) {
        this.day = day;
        this.cropName = cropName;
        this.quantity = quantity;
        this.market = market;
        this.unitPrice = unitPrice;
        this.revenue = revenue;
        this.cost = cost;
    }

    public int getDay() { return day; }
    public String getCropName() { return cropName; }
    public double getQuantity() { return quantity; }
    public String getMarket() { return market; }
    public double getUnitPrice() { return unitPrice; }
    public double getRevenue() { return revenue; }
    public double getCost() { return cost; }
    public double getProfit() { return revenue - cost; }
}
