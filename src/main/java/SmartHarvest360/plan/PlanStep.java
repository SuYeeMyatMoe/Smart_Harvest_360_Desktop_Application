package SmartHarvest360.plan;

/**
 * One concrete field step taken (or missed) during the season.
 * OOP: Encapsulation — immutable step data with controlled access.
 */
public final class PlanStep implements PlanItem {
    private final int stepNumber;
    private final String action;
    private final String weather;
    private final String outcome;
    private final String coachingNote;

    public PlanStep(int stepNumber, String action, String weather, String outcome, String coachingNote) {
        this.stepNumber = stepNumber;
        this.action = action == null ? "-" : action;
        this.weather = weather == null ? "-" : weather;
        this.outcome = outcome == null ? "-" : outcome;
        this.coachingNote = coachingNote == null ? "" : coachingNote;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getAction() {
        return action;
    }

    public String getWeather() {
        return weather;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getCoachingNote() {
        return coachingNote;
    }

    @Override
    public String getCategory() {
        return "STEP";
    }

    @Override
    public String getTitle() {
        return "Day step " + stepNumber + ": " + action;
    }

    @Override
    public String getDetail() {
        return weather + " - " + outcome
                + (coachingNote.isBlank() ? "" : " - " + coachingNote);
    }
}
