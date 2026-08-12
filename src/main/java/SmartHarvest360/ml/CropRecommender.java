package SmartHarvest360.ml;

import SmartHarvest360.Resource;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Crop recommender: feature scorer (responsive) + J48 confirmation from ARFF.
 */
public final class CropRecommender {
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
        // Primary: transparent feature rules so advice changes with Farm Setup inputs.
        String scored = CropFeatureScorer.ruleLabel(profile, resource);
        var ranked = CropFeatureScorer.rank(profile, resource);

        double j48Confidence = 0;
        String j48Crop = scored;
        try {
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
            int bestIdx = 0;
            for (int i = 1; i < dist.length; i++) {
                if (dist[i] > dist[bestIdx]) {
                    bestIdx = i;
                }
            }
            j48Crop = template.classAttribute().value(bestIdx);
            j48Confidence = dist[bestIdx];
        } catch (Exception ignored) {
            // Scorer still works if J48 row fails.
        }

        // Prefer feature rule (keeps UI responsive). Use J48 confidence when it agrees.
        String crop = scored;
        double confidence = j48Crop.equalsIgnoreCase(scored)
                ? Math.max(0.55, j48Confidence)
                : 0.70;

        List<String> alternatives = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            String name = ranked.get(i).crop();
            if (!name.equalsIgnoreCase(crop)) {
                alternatives.add(name);
            }
            if (alternatives.size() == 2) {
                break;
            }
        }
        if (!j48Crop.equalsIgnoreCase(crop)) {
            alternatives.add(0, String.format(Locale.US, "J48:%s", j48Crop));
            if (alternatives.size() > 2) {
                alternatives = alternatives.subList(0, 2);
            }
        }

        return new Recommendation(crop, confidence, List.copyOf(alternatives));
    }

    public record Recommendation(String crop, double confidence, List<String> alternatives) {
    }
}
