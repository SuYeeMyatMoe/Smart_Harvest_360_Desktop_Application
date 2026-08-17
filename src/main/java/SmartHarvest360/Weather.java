package SmartHarvest360;

/** Weather conditions and their effects on water use and crop growth. */
public enum Weather {
    SUNNY("Sunny", 1.20, 1.05, 0.40),
    CLOUDY("Cloudy", 1.00, 1.00, 0.35),
    RAIN("Rain", 0.60, 1.10, 0.25);

    private final String label;
    private final double waterFactor;
    private final double growthFactor;
    private final double probability;

    Weather(String label, double waterFactor, double growthFactor, double probability) {
        this.label = label;
        this.waterFactor = waterFactor;
        this.growthFactor = growthFactor;
        this.probability = probability;
    }

    public String getLabel() { return label; }
    public double getWaterFactor() { return waterFactor; }
    public double getGrowthFactor() { return growthFactor; }
    public double getProbability() { return probability; }
}
