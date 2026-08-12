package SmartHarvest360.ml;

import SmartHarvest360.Resource;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * J48 crop classifier trained on DOSM state production-informed ARFF.
 * Falls back to {@link CropFeatureScorer} only when model confidence is very low.
 */
public final class CropRecommender {
    private static final double MIN_CONFIDENCE = 0.28;

    private final Classifier model;
    private final Instances template;

    public CropRecommender(Classifier model, Instances template) {
        this.model = model;
        this.template = template;
        this.template.setClassIndex(this.template.numAttributes() - 1);
    }

    public String recommend(FarmProfile profile, Resource resource) throws Exception {
        return recommendDetailed(profile, resource).crop();
    }

    public Recommendation recommendDetailed(FarmProfile profile, Resource resource) throws Exception {
        Instance row = new DenseInstance(template.numAttributes());
        row.setDataset(template);
        row.setValue(template.attribute("location"), profile.getLocation());
        row.setValue(template.attribute("soil"), profile.getSoilType());
        row.setValue(template.attribute("water"), resource.getWater());
        row.setValue(template.attribute("fertilizerStock"), resource.getFertilizer());
        row.setValue(template.attribute("budget"), resource.getBudget());
        row.setValue(template.attribute("land"), resource.getLand());
        row.setValue(template.attribute("npkBand"), profile.npkBand());

        double[] dist = model.distributionForInstance(row);
        List<Scored> ranked = new ArrayList<>();
        for (int i = 0; i < dist.length; i++) {
            ranked.add(new Scored(template.classAttribute().value(i), dist[i]));
        }
        ranked.sort(Comparator.comparingDouble(Scored::score).reversed());

        String crop = ranked.get(0).crop();
        double confidence = ranked.get(0).score();

        // Low-confidence leaf → blend with feature scorer (still classification-first).
        if (confidence < MIN_CONFIDENCE) {
            String scored = CropFeatureScorer.ruleLabel(profile, resource);
            crop = scored;
            confidence = Math.max(confidence, 0.45);
        }

        List<String> alternatives = new ArrayList<>();
        for (int i = 1; i < ranked.size() && alternatives.size() < 2; i++) {
            Scored next = ranked.get(i);
            if (next.score() >= 0.08 && !next.crop().equalsIgnoreCase(crop)) {
                alternatives.add(String.format(Locale.US, "%s %.0f%%",
                        next.crop(), next.score() * 100.0));
            }
        }

        return new Recommendation(crop, confidence, List.copyOf(alternatives));
    }

    public record Recommendation(String crop, double confidence, List<String> alternatives) {
    }

    private record Scored(String crop, double score) {
    }
}
