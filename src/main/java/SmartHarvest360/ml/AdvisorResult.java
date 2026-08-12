package SmartHarvest360.ml;

/**
 * Bundle of Weka (or heuristic fallback) advice for the current farm/crop context.
 */
public final class AdvisorResult {
    private final String recommendedCrop;
    private final String fertilizerPlan;
    private final String fertilizerKgTip;
    private final String predictedGrade;
    private final String rationale;
    private final boolean fromWeka;

    public AdvisorResult(
            String recommendedCrop,
            String fertilizerPlan,
            String fertilizerKgTip,
            String predictedGrade,
            String rationale,
            boolean fromWeka
    ) {
        this.recommendedCrop = recommendedCrop;
        this.fertilizerPlan = fertilizerPlan;
        this.fertilizerKgTip = fertilizerKgTip;
        this.predictedGrade = predictedGrade;
        this.rationale = rationale;
        this.fromWeka = fromWeka;
    }

    public String getRecommendedCrop() {
        return recommendedCrop;
    }

    public String getFertilizerPlan() {
        return fertilizerPlan;
    }

    public String getFertilizerKgTip() {
        return fertilizerKgTip;
    }

    public String getPredictedGrade() {
        return predictedGrade;
    }

    public String getRationale() {
        return rationale;
    }

    public boolean isFromWeka() {
        return fromWeka;
    }
}
