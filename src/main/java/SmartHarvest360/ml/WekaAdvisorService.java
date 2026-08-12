package SmartHarvest360.ml;

import SmartHarvest360.Crop;
import SmartHarvest360.FruitCrop;
import SmartHarvest360.Resource;
import SmartHarvest360.VegetableCrop;

import java.util.Locale;
import java.util.Map;

/**
 * Facade over J48 crop / fertilizer / grade models with Malaysia heuristics fallback.
 */
public final class WekaAdvisorService {
    private static final WekaAdvisorService INSTANCE = new WekaAdvisorService();

    /** Catalog names used in crops.csv that differ from model labels. */
    private static final Map<String, String> CROP_ALIASES = Map.of(
            "Chili", "Green Chili",
            "Green Chili", "Chili"
    );

    private CropRecommender cropRecommender;
    private FertilizerAdvisor fertilizerAdvisor;
    private GradePredictor gradePredictor;
    private boolean ready;
    private String statusMessage = "Weka J48 · not loaded";

    private WekaAdvisorService() {
        try {
            // Retrain if ARFF changed since last cached model.
            var cropModel = ModelTrainer.loadOrTrain("crop_recommend.arff", "crop.model");
            var fertModel = ModelTrainer.loadOrTrain("fertilizer_plan.arff", "fertilizer.model");
            var gradeModel = ModelTrainer.loadOrTrain("grade_predict.arff", "grade.model");
            cropRecommender = new CropRecommender(
                    cropModel, ModelTrainer.loadArff("crop_recommend.arff"));
            fertilizerAdvisor = new FertilizerAdvisor(
                    fertModel, ModelTrainer.loadArff("fertilizer_plan.arff"));
            gradePredictor = new GradePredictor(
                    gradeModel, ModelTrainer.loadArff("grade_predict.arff"));
            ready = true;
            statusMessage = "Weka J48 classification · DOSM crop area/production by state (2017-2022)";
        } catch (Exception exception) {
            ready = false;
            statusMessage = "Heuristic fallback · " + shortMsg(exception);
        }
    }

    public static WekaAdvisorService getInstance() {
        return INSTANCE;
    }

    public boolean isReady() {
        return ready;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /** Resolve model label <-> catalog crop name (e.g. Chili / Green Chili). */
    public static String catalogNameFor(String modelCrop) {
        if (modelCrop == null || modelCrop.isBlank()) {
            return modelCrop;
        }
        return CROP_ALIASES.getOrDefault(modelCrop, modelCrop);
    }

    public static boolean namesMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equalsIgnoreCase(b)) {
            return true;
        }
        String aliasA = CROP_ALIASES.get(a);
        String aliasB = CROP_ALIASES.get(b);
        return (aliasA != null && aliasA.equalsIgnoreCase(b))
                || (aliasB != null && aliasB.equalsIgnoreCase(a));
    }

    public AdvisorResult advise(FarmProfile profile, Resource resource, Crop cropHint) {
        FarmProfile safeProfile = profile == null
                ? new FarmProfile("Selangor", "Loam") : profile;
        Resource safeResource = resource == null
                ? new Resource(200, 20, 10_000, 5) : resource;

        try {
            if (!ready) {
                return heuristic(safeProfile, safeResource, cropHint);
            }

            CropRecommender.Recommendation pick =
                    cropRecommender.recommendDetailed(safeProfile, safeResource);
            String crop = pick.crop();
            Crop tipCrop = resolveTipCrop(cropHint, crop);
            String plan = fertilizerAdvisor.predictPlan(safeProfile, crop, safeResource);
            String tip = FertilizerAdvisor.kgTip(plan, tipCrop);
            String band = GradePredictor.profitBand(tipCrop);
            String grade = gradePredictor.predict(safeProfile, crop, safeResource, band);

            String alts = pick.alternatives().isEmpty()
                    ? "none close"
                    : String.join(", ", pick.alternatives());
            String drivers = CropFeatureScorer.explain(safeProfile, safeResource, crop);
            String rationale = String.format(Locale.US,
                    "J48 classifies %s (%.0f%%) from DOSM state production priors + your farm inputs. %s Next: %s. Plan %s; band %s → grade %s.",
                    crop, pick.confidence() * 100.0, drivers, alts, plan, band, grade);
            return new AdvisorResult(crop, plan, tip, grade, rationale, true);
        } catch (Exception exception) {
            return heuristic(safeProfile, safeResource, cropHint);
        }
    }

    /** Soft daily tip from stored advice + live resources/weather (text only). */
    public String dailyActionTip(AdvisorResult advice, Resource resource, String weather) {
        return gradeImprovementTip(advice, resource, weather, 50).action();
    }

    /**
     * Grade-focused recommendation: which action lifts care/grade today, plus why.
     */
    public GradeTip gradeImprovementTip(
            AdvisorResult advice,
            Resource resource,
            String weather,
            int careScore
    ) {
        String w = weather == null ? "" : weather.toLowerCase(Locale.ROOT);
        String plantGrade = advice == null ? "C" : advice.getPredictedGrade();
        String liveGrade = GradePredictor.nudgeGrade(plantGrade, careScore);

        if (w.contains("storm") || w.contains("heat")) {
            return new GradeTip(
                    "Protect",
                    "Protect today - harsh weather risks spoilage and drops grade toward "
                            + GradePredictor.nudgeGrade(plantGrade, Math.max(0, careScore - 12))
                            + ". Shielding protects path to " + liveGrade + "/better."
            );
        }
        if (resource != null && resource.getWater() < 35) {
            return new GradeTip(
                    "Conserve",
                    "Conserve - water is critical. Stretching supply avoids growth pauses that lock a lower grade."
            );
        }
        if (advice != null && "High".equalsIgnoreCase(advice.getFertilizerPlan())
                && resource != null && resource.getFertilizer() >= 1.5) {
            return new GradeTip(
                    "Fertilize",
                    "Fertilize - High plan still needs feeding. Matching the plan lifts care score toward grade "
                            + GradePredictor.nudgeGrade(plantGrade, Math.min(100, careScore + 10)) + "."
            );
        }
        if (resource != null && resource.getWater() < 90) {
            return new GradeTip(
                    "Conserve",
                    "Conserve - keep a water buffer so later days do not pause (pauses hurt grade)."
            );
        }
        if (w.contains("sunny") || w.isBlank()) {
            return new GradeTip(
                    "Irrigate",
                    "Irrigate - sunny days grow fastest when watered. Steady growth raises care toward "
                            + GradePredictor.nudgeGrade(plantGrade, Math.min(100, careScore + 8)) + "."
            );
        }
        if (advice != null && "Medium".equalsIgnoreCase(advice.getFertilizerPlan())
                && resource != null && resource.getFertilizer() >= 1.0) {
            return new GradeTip(
                    "Fertilize",
                    "Fertilize mid-plan - a light feed today supports Medium plan and grade " + liveGrade + "."
            );
        }
        return new GradeTip(
                "Irrigate",
                "Irrigate as weather allows - follow " + (advice == null ? "Medium" : advice.getFertilizerPlan())
                        + " fertilizer plan. Live grade path: " + liveGrade + "."
        );
    }

    public record GradeTip(String action, String message) {
    }

    private static Crop resolveTipCrop(Crop cropHint, String recommendedName) {
        if (cropHint != null && namesMatch(cropHint.getName(), recommendedName)) {
            return cropHint;
        }
        return syntheticCrop(recommendedName);
    }

    private AdvisorResult heuristic(FarmProfile profile, Resource resource, Crop cropHint) {
        String crop = CropFeatureScorer.ruleLabel(profile, resource);
        Crop tipSource = resolveTipCrop(cropHint, crop);

        String plan;
        if (resource.getFertilizer() < 10 || profile.getSoilType().equals("Sandy")) {
            plan = "High";
        } else if (resource.getFertilizer() > 25 && resource.getWater() > 200) {
            plan = "Low";
        } else {
            plan = "Medium";
        }

        String tip = FertilizerAdvisor.kgTip(plan, tipSource);
        String band = GradePredictor.profitBand(tipSource);
        double score = resource.getWater() / 200.0
                + resource.getFertilizer() / 20.0
                + resource.getBudget() / 10_000.0;
        score /= 3.0;
        if ("High".equals(band)) {
            score += 0.15;
        } else if ("Low".equals(band)) {
            score -= 0.1;
        }
        String grade = score >= 0.95 ? "S"
                : score >= 0.80 ? "A"
                : score >= 0.62 ? "B"
                : score >= 0.48 ? "C"
                : score >= 0.35 ? "D" : "F";

        String rationale = String.format(Locale.US,
                "Heuristic for %s / %s with water %.0f, land %.1f: recommend %s, %s fertilizer, grade %s.",
                profile.getLocation(), profile.getSoilType(),
                resource.getWater(), resource.getLand(), crop, plan, grade);
        return new AdvisorResult(crop, plan, tip, grade, rationale, false);
    }

    private static Crop syntheticCrop(String name) {
        // Values aligned with data/crops.csv so grade bands stay realistic.
        return switch (name) {
            case "Lettuce" -> new VegetableCrop(
                    "Lettuce", 45, 30.0, 1.0, 20.0, 1.20, 4.00);
            case "Chili", "Green Chili" -> new FruitCrop(
                    "Green Chili", 120, 70.0, 3.0, 25.0, 2.50, 8.00);
            case "Corn" -> new FruitCrop(
                    "Corn", 100, 80.0, 4.0, 50.0, 1.80, 4.50);
            case "Paddy" -> new VegetableCrop(
                    "Paddy", 120, 200.0, 5.0, 80.0, 1.50, 3.20);
            case "Papaya" -> new FruitCrop(
                    "Papaya", 150, 90.0, 3.5, 40.0, 2.20, 6.50);
            case "Durian" -> new FruitCrop(
                    "Durian", 180, 110.0, 6.0, 35.0, 8.00, 22.00);
            default -> new VegetableCrop(
                    "Tomato", 90, 50.0, 2.0, 30.0, 2.00, 5.50);
        };
    }

    private static String shortMsg(Exception exception) {
        String msg = exception.getMessage();
        if (msg == null || msg.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return msg.length() > 60 ? msg.substring(0, 57) + "..." : msg;
    }
}
