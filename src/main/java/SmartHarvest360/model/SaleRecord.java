package SmartHarvest360.model;

/** Immutable details for one completed crop sale. */
public record SaleRecord(
        int day,
        String cropName,
        double quantity,
        String market,
        double unitPrice,
        double revenue,
        double cost
) {
    public double profit() {
        return revenue - cost;
    }
}
