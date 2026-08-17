package SmartHarvest360;

/** Random events that can affect a simulated crop day. */
public enum RandomEvent {
    NONE(""),
    PEST("Pest outbreak - growth slowed today"),
    DROUGHT("Drought - crop needs 50% more water today"),
    FROST("Frost - growth slowed today");

    private final String label;

    RandomEvent(String label) { this.label = label; }
    public String getLabel() { return label; }
}
