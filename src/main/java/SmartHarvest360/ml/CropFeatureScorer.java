package SmartHarvest360.ml;

import SmartHarvest360.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Transparent multi-factor crop scoring used to label training data and keep
 * live advice responsive. Keep rules in sync with scripts/generate_ml_arff.py.
 */
public final class CropFeatureScorer {
    public static final String[] CROPS = {
            "Tomato", "Lettuce", "Chili", "Corn", "Paddy", "Papaya", "Durian"
    };

    private CropFeatureScorer() {
    }

    public static String recommend(FarmProfile profile, Resource resource) {
        return rank(profile, resource).get(0).crop();
    }

    public static List<ScoredCrop> rank(FarmProfile profile, Resource resource) {
        String soil = profile.getSoilType();
        String state = profile.getLocation();
        double water = resource.getWater();
        double fertilizer = resource.getFertilizer();
        double budget = resource.getBudget();
        double land = resource.getLand();

        List<ScoredCrop> scores = new ArrayList<>();
        scores.add(new ScoredCrop("Paddy", paddyScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Durian", durianScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Papaya", papayaScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Corn", cornScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Chili", chiliScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Lettuce", lettuceScore(soil, water, fertilizer, budget, land, state)));
        scores.add(new ScoredCrop("Tomato", tomatoScore(soil, water, fertilizer, budget, land, state)));
        scores.sort(Comparator.comparingDouble(ScoredCrop::score).reversed());
        return scores;
    }

    /** Hard exclusive label used for ARFF generation / live recommendation. */
    public static String ruleLabel(FarmProfile profile, Resource resource) {
        String soil = profile.getSoilType();
        String state = profile.getLocation() == null ? "" : profile.getLocation();
        double water = resource.getWater();
        double fertilizer = resource.getFertilizer();
        double budget = resource.getBudget();
        double land = resource.getLand();

        // 1) Flooded / high-water plots → Paddy
        if (water >= 220 && land >= 4.0 && ("Clay".equals(soil) || "Loam".equals(soil))) {
            return "Paddy";
        }
        // 2) Large irrigated field → Corn (before Durian so grain farms are not stolen)
        if (land >= 6.5 && water >= 160 && water < 220 && fertilizer >= 14) {
            return "Corn";
        }
        // 3) High-investment orchard → Durian
        if (budget >= 12000 && land >= 5.0 && fertilizer >= 15
                && water >= 100 && water < 160
                && ("Loam".equals(soil) || "Clay".equals(soil))) {
            return "Durian";
        }
        // 4) Sandy beds → Chili
        if ("Sandy".equals(soil) && budget >= 4500 && water >= 85 && water < 210) {
            return "Chili";
        }
        // 5) Warm-state fruit niche → Papaya
        if (state.matches("Johor|Kelantan|Terengganu|Sabah|Sarawak|Pahang")
                && water >= 110 && water <= 200
                && budget >= 6000 && land >= 2.5 && land <= 7.5
                && !"Silty".equals(soil)) {
            return "Papaya";
        }
        // 6) Limited water / silty → Lettuce
        if ("Silty".equals(soil) || water < 125) {
            return "Lettuce";
        }
        if (land <= 3.5 && water <= 180) {
            return "Lettuce";
        }
        // 7) Chili coastal states
        if (state.matches("Kelantan|Terengganu|Johor|Perlis")
                && water >= 100 && water <= 210 && budget >= 5000) {
            return "Chili";
        }
        // 8) Corn grain states
        if (state.matches("Kedah|Perak|Pahang|Sarawak") && land >= 5.5 && water >= 140 && water < 220) {
            return "Corn";
        }
        // 9) Productive mid farm → Tomato
        if (("Loam".equals(soil) || "Clay".equals(soil)) && water >= 140 && fertilizer >= 12) {
            return "Tomato";
        }
        if (budget >= 9000 && water >= 150 && water < 220) {
            return "Tomato";
        }
        if (fertilizer < 10) {
            return "Lettuce";
        }
        return "Tomato";
    }

    public static String explain(FarmProfile profile, Resource resource, String crop) {
        return String.format(Locale.US,
                "Scored from %s / %s, water %.0f L, fertilizer %.0f kg, land %.1f ha, budget RM %,.0f.",
                profile.getLocation(),
                profile.getSoilType(),
                resource.getWater(),
                resource.getFertilizer(),
                resource.getLand(),
                resource.getBudget());
    }

    private static double paddyScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if (water >= 220) {
            score += 5.0;
        } else if (water >= 180) {
            score += 2.0;
        } else {
            score -= 3.0;
        }
        if ("Clay".equals(soil) || "Loam".equals(soil)) {
            score += 2.5;
        } else {
            score -= 2.0;
        }
        if (land >= 4) {
            score += 2.0;
        }
        if (state != null && state.matches("Kedah|Perlis|Kelantan|Terengganu|Perak")) {
            score += 1.5;
        }
        if (fertilizer >= 12) {
            score += 0.8;
        }
        return score;
    }

    private static double durianScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if (budget >= 12000) {
            score += 4.5;
        } else if (budget >= 9000) {
            score += 1.5;
        } else {
            score -= 2.5;
        }
        if (land >= 5) {
            score += 2.5;
        } else {
            score -= 1.5;
        }
        if (fertilizer >= 15) {
            score += 2.0;
        }
        if ("Loam".equals(soil) || "Clay".equals(soil)) {
            score += 1.5;
        }
        if (state != null && state.matches("Pahang|Johor|Negeri Sembilan|Kelantan|Terengganu")) {
            score += 1.2;
        }
        if (water >= 100 && water <= 200) {
            score += 1.0;
        }
        return score;
    }

    private static double papayaScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if (water >= 110 && water <= 200) {
            score += 2.5;
        } else {
            score -= 1.0;
        }
        if ("Sandy".equals(soil) || "Loam".equals(soil)) {
            score += 2.0;
        }
        if (budget >= 6000) {
            score += 1.5;
        }
        if (land >= 2.5 && land <= 7.5) {
            score += 1.5;
        }
        if (state != null && state.matches("Johor|Kelantan|Terengganu|Sabah|Sarawak|Pahang")) {
            score += 2.0;
        }
        if (fertilizer >= 10) {
            score += 0.8;
        }
        return score;
    }

    private static double cornScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if (land >= 6.5) {
            score += 4.0;
        } else if (land >= 5.5) {
            score += 2.0;
        } else if (land < 4) {
            score -= 2.5;
        }
        if (water >= 160 && water < 220) {
            score += 2.5;
        } else if (water < 130) {
            score -= 2.0;
        }
        if (fertilizer >= 14) {
            score += 1.5;
        }
        if ("Loam".equals(soil) || "Clay".equals(soil)) {
            score += 1.0;
        }
        if (state != null && state.matches("Kedah|Perak|Pahang|Sarawak")) {
            score += 1.2;
        }
        if (budget >= 8000) {
            score += 0.8;
        }
        return score;
    }

    private static double chiliScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if ("Sandy".equals(soil)) {
            score += 4.0;
        } else if ("Loam".equals(soil)) {
            score += 1.2;
        }
        if (water >= 85 && water <= 210) {
            score += 2.0;
        } else {
            score -= 1.0;
        }
        if (budget >= 4500) {
            score += 1.5;
        } else {
            score -= 1.5;
        }
        if (fertilizer >= 10) {
            score += 0.8;
        }
        if (state != null && state.matches("Kelantan|Terengganu|Johor|Perlis")) {
            score += 1.5;
        }
        if (land >= 2 && land <= 7) {
            score += 0.5;
        }
        return score;
    }

    private static double lettuceScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if ("Silty".equals(soil)) {
            score += 3.5;
        } else if ("Loam".equals(soil)) {
            score += 1.0;
        }
        if (water < 125) {
            score += 3.5;
        } else if (water > 200) {
            score -= 1.5;
        }
        if (land <= 3.5) {
            score += 2.0;
        } else if (land > 7) {
            score -= 1.0;
        }
        if (fertilizer < 12) {
            score += 1.0;
        }
        if (budget <= 11000) {
            score += 0.5;
        }
        return score;
    }

    private static double tomatoScore(
            String soil, double water, double fertilizer, double budget, double land, String state
    ) {
        double score = 0;
        if ("Loam".equals(soil) || "Clay".equals(soil)) {
            score += 2.5;
        }
        if (water >= 140 && water <= 210) {
            score += 2.5;
        } else if (water < 120) {
            score -= 2.0;
        }
        if (fertilizer >= 12) {
            score += 1.5;
        }
        if (land >= 3.5 && land <= 6.5) {
            score += 1.5;
        } else if (land >= 6.5) {
            score -= 0.5;
        }
        if (budget >= 7000 && budget < 12000) {
            score += 1.0;
        }
        if ("Sandy".equals(soil)) {
            score -= 1.5;
        }
        return score;
    }

    public record ScoredCrop(String crop, double score) {
    }
}
