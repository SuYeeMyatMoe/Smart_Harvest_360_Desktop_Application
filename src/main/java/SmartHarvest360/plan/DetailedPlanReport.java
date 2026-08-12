package SmartHarvest360.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated post-simulation plan: summary + steps + recommendations.
 * OOP: Composition — a report owns collections of {@link PlanStep} and {@link PlanRecommendation}.
 */
public final class DetailedPlanReport {
    private final String farmName;
    private final String location;
    private final String soil;
    private final String cropName;
    private final int careScore;
    private final String plantGrade;
    private final String liveGrade;
    private final String fertilizerPlan;
    private final String summary;
    private final List<PlanStep> steps;
    private final List<PlanRecommendation> recommendations;

    public DetailedPlanReport(
            String farmName,
            String location,
            String soil,
            String cropName,
            int careScore,
            String plantGrade,
            String liveGrade,
            String fertilizerPlan,
            String summary,
            List<PlanStep> steps,
            List<PlanRecommendation> recommendations
    ) {
        this.farmName = farmName;
        this.location = location;
        this.soil = soil;
        this.cropName = cropName;
        this.careScore = careScore;
        this.plantGrade = plantGrade;
        this.liveGrade = liveGrade;
        this.fertilizerPlan = fertilizerPlan;
        this.summary = summary;
        this.steps = List.copyOf(steps == null ? List.of() : steps);
        this.recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }

    public String getFarmName() {
        return farmName;
    }

    public String getLocation() {
        return location;
    }

    public String getSoil() {
        return soil;
    }

    public String getCropName() {
        return cropName;
    }

    public int getCareScore() {
        return careScore;
    }

    public String getPlantGrade() {
        return plantGrade;
    }

    public String getLiveGrade() {
        return liveGrade;
    }

    public String getFertilizerPlan() {
        return fertilizerPlan;
    }

    public String getSummary() {
        return summary;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public List<PlanRecommendation> getRecommendations() {
        return recommendations;
    }

    /** Polymorphic view of all plan rows for export/UI. */
    public List<PlanItem> allItems() {
        List<PlanItem> items = new ArrayList<>(steps.size() + recommendations.size());
        items.addAll(steps);
        items.addAll(recommendations);
        return Collections.unmodifiableList(items);
    }
}
