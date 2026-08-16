package SmartHarvest360;

/**
 * Weather conditions that influence the daily simulation.
 * Each weather type has a water-consumption factor, a growth factor,
 * and a probability of occurring on any given day.
 */
public enum Weather {
    SUNNY("Sunny", "sun", 1.20, 1.05, 0.40),
    CLOUDY("Cloudy", "cloud", 1.00, 1.00, 0.35),
    RAIN("Rain", "rain", 0.60, 1.10, 0.25);

    private final String label;
    private final String icon;
    private final double waterFactor;
    private final double growthFactor;
    private final double probability;

    Weather(String label, String icon, double waterFactor, double growthFactor, double probability) {
        this.label = label;
        this.icon = icon;
        this.waterFactor = waterFactor;
        this.growthFactor = growthFactor;
        this.probability = probability;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Semantic icon key ("sun" / "cloud" / "rain"). Not a display glyph:
     * render it through {@code SmartHarvest360.ui.IconFactory.weatherIcon(Weather)}.
     */
    public String getIcon() {
        return icon;
    }

    public double getWaterFactor() {
        return waterFactor;
    }

    public double getGrowthFactor() {
        return growthFactor;
    }

    public double getProbability() {
        return probability;
    }
}
