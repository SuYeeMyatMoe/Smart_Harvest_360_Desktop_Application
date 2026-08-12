package SmartHarvest360.ml;

import SmartHarvest360.Crop;
import SmartHarvest360.Resource;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Locale;

/**
 * J48 fertilizer plan: Low|Medium|High plus kg guidance from crop.fertilizerNeed.
 */
public final class FertilizerAdvisor {
    private final Classifier model;
    private final Instances template;

    public FertilizerAdvisor(Classifier model, Instances template) {
        this.model = model;
        this.template = template;
        this.template.setClassIndex(this.template.numAttributes() - 1);
    }

    public String predictPlan(FarmProfile profile, String cropName, Resource resource) throws Exception {
        Instance row = new DenseInstance(template.numAttributes());
        row.setDataset(template);
        row.setValue(template.attribute("location"), profile.getLocation());
        row.setValue(template.attribute("soil"), profile.getSoilType());
        row.setValue(template.attribute("crop"), cropName);
        row.setValue(template.attribute("water"), resource.getWater());
        row.setValue(template.attribute("fertilizerStock"), resource.getFertilizer());
        double cls = model.classifyInstance(row);
        return template.classAttribute().value((int) cls);
    }

    public static String kgTip(String plan, Crop crop) {
        double need = crop == null ? 10.0 : crop.getFertilizerNeed();
        double factor = switch (plan == null ? "Medium" : plan) {
            case "Low" -> 0.5;
            case "High" -> 1.5;
            default -> 1.0;
        };
        double kg = need * factor;
        return switch (plan == null ? "Medium" : plan) {
            case "Low" -> String.format(Locale.US,
                    "Apply about %.1f kg early season (0.5× need).", kg);
            case "High" -> String.format(Locale.US,
                    "Apply about %.1f kg with extra mid-season top-up (1.5× need).", kg);
            default -> String.format(Locale.US,
                    "Apply about %.1f kg split early/mid season (1.0× need).", kg);
        };
    }
}
