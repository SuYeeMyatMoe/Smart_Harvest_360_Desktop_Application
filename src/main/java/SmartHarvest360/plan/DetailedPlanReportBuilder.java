package SmartHarvest360.plan;

import SmartHarvest360.Crop;
import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.ml.GradePredictor;
import SmartHarvest360.model.SimDayLog;
import SmartHarvest360.session.AppSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a {@link DetailedPlanReport} from session simulation evidence.
 * OOP: Single responsibility — turns AppSession state into a structured plan object.
 */
public final class DetailedPlanReportBuilder {
    private DetailedPlanReportBuilder() {
    }

    public static DetailedPlanReport fromSession(AppSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required");
        }

        FarmProfile profile = session.getFarmProfile();
        AdvisorResult advice = session.getAdvisorResult();
        Crop crop = session.getActiveCrop();

        String location = profile == null ? "-" : profile.getLocation();
        String soil = profile == null ? "-" : profile.getSoilType();
        String cropName = crop == null ? "-" : crop.getName();
        String plantGrade = advice == null || advice.getPredictedGrade() == null
                ? "C" : advice.getPredictedGrade();
        String liveGrade = GradePredictor.nudgeGrade(plantGrade, session.getCareScore());
        String fertPlan = advice == null ? "Medium" : advice.getFertilizerPlan();

        List<PlanStep> steps = buildSteps(session.getDayLogs());
        List<PlanRecommendation> recommendations = buildRecommendations(
                session, advice, fertPlan, plantGrade, liveGrade);

        String summary = String.format(Locale.US,
                "%s on %s/%s finished care %d with live grade path %s (plant-time %s). "
                        + "Review %d field steps and %d recommendations before market.",
                cropName, location, soil, session.getCareScore(), liveGrade, plantGrade,
                steps.size(), recommendations.size());

        return new DetailedPlanReport(
                session.getFarmName() == null ? "Farm" : session.getFarmName(),
                location,
                soil,
                cropName,
                session.getCareScore(),
                plantGrade,
                liveGrade,
                fertPlan,
                summary,
                steps,
                recommendations
        );
    }

    private static List<PlanStep> buildSteps(List<SimDayLog> logs) {
        List<PlanStep> steps = new ArrayList<>();
        if (logs == null || logs.isEmpty()) {
            steps.add(new PlanStep(1, "Setup", "-", "No activity logged",
                    "Run at least one simulation day before reviewing the plan."));
            return steps;
        }

        int number = 1;
        for (SimDayLog log : logs) {
            if ("Setup".equalsIgnoreCase(log.getAction())) {
                steps.add(new PlanStep(
                        number++,
                        "Setup",
                        log.getWeather(),
                        "Season started at " + log.getGrowthPercent() + "%",
                        "Baseline day - later actions move care and grade."
                ));
                continue;
            }

            String note = coachingForDay(log);
            steps.add(new PlanStep(
                    number++,
                    log.getAction(),
                    log.getWeather(),
                    log.getStatus() + " - growth " + log.getGrowthPercent() + "%",
                    note
            ));
        }
        return steps;
    }

    private static String coachingForDay(SimDayLog log) {
        String weather = log.getWeather() == null ? "" : log.getWeather();
        String action = log.getAction() == null ? "" : log.getAction();
        String status = log.getStatus() == null ? "" : log.getStatus().toLowerCase(Locale.ROOT);

        if (status.contains("paused")) {
            return "Paused day hurt progress - Conserve earlier when water is low.";
        }
        if (("Storm".equals(weather) || "Heat".equals(weather)) && "Protect".equals(action)) {
            return "Good call - Protect matched harsh weather.";
        }
        if (("Storm".equals(weather) || "Heat".equals(weather)) && !"Protect".equals(action)) {
            return "Missed Protect on harsh weather - grade care usually drops.";
        }
        if ("Fertilize".equals(action)) {
            return "Fertilizer day - keep stock aligned with the ML plan band.";
        }
        if ("Conserve".equals(action)) {
            return "Water saved for later growth days.";
        }
        if ("Irrigate".equals(action)) {
            return "Irrigation supports growth on dry/sunny days.";
        }
        return "Logged field decision.";
    }

    private static List<PlanRecommendation> buildRecommendations(
            AppSession session,
            AdvisorResult advice,
            String fertPlan,
            String plantGrade,
            String liveGrade
    ) {
        List<PlanRecommendation> list = new ArrayList<>();
        int care = session.getCareScore();

        list.add(new PlanRecommendation(
                "High",
                "Next action habit",
                "On Storm/Heat days always choose Protect first - that is the fastest grade saver."
        ));

        list.add(new PlanRecommendation(
                "High",
                "Fertilizer plan",
                "Follow " + fertPlan + " plan"
                        + (advice == null ? "." : ": " + advice.getFertilizerKgTip())
        ));

        if (care < 45) {
            list.add(new PlanRecommendation(
                    "High",
                    "Care recovery",
                    "Care score is low (" + care + "). Match the Grade Coach tip each day instead of random actions."
            ));
        } else if (care >= 70) {
            list.add(new PlanRecommendation(
                    "Medium",
                    "Keep momentum",
                    "Care " + care + " supports live grade " + liveGrade
                            + ". Keep following coach tips through harvest."
            ));
        } else {
            list.add(new PlanRecommendation(
                    "Medium",
                    "Care lift",
                    "Care " + care + " can still rise - use Do suggested action now when unsure."
            ));
        }

        long pauses = session.getDayLogs().stream()
                .filter(log -> log.getStatus() != null && log.getStatus().toLowerCase(Locale.ROOT).contains("paused"))
                .count();
        if (pauses > 0) {
            list.add(new PlanRecommendation(
                    "High",
                    "Resource buffer",
                    pauses + " paused day(s) detected. Raise starting water/fertilizer on Farm Setup or Conserve earlier."
            ));
        }

        if (GradePredictor.indexOfGrade(liveGrade) < GradePredictor.indexOfGrade(plantGrade)) {
            list.add(new PlanRecommendation(
                    "High",
                    "Grade gap",
                    "Live path " + liveGrade + " is below plant-time " + plantGrade
                            + ". Prioritize Protect + fertilizer plan to close the gap before market."
            ));
        } else if (GradePredictor.indexOfGrade(liveGrade) > GradePredictor.indexOfGrade(plantGrade)) {
            list.add(new PlanRecommendation(
                    "Low",
                    "Grade upside",
                    "You improved the path to " + liveGrade + " from " + plantGrade
                            + ". Sell promptly at the best market to lock results."
            ));
        }

        list.add(new PlanRecommendation(
                "Medium",
                "Market next",
                "Continue to Harvest & Market, sell at the best buyer, then download the season CSV on Report."
        ));

        return list;
    }
}
