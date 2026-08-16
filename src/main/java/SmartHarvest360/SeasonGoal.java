package SmartHarvest360;

/**
 * The farmer's stated objective for a season, chosen at farm setup and graded
 * on the season report. Defaults to MAXIMIZE_ROI so skipping the picker never
 * breaks a flow.
 */
public enum SeasonGoal {
    MAXIMIZE_ROI("Maximize ROI", "Aim for the highest return on every ringgit invested."),
    MAXIMIZE_YIELD("Maximize Total Yield (kg)", "Aim for the largest total harvest in kilograms."),
    CONSERVE_RESOURCES("Conserve Resources", "Finish the season with water, fertilizer, and budget to spare.");

    private final String label;
    private final String description;

    SeasonGoal(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
