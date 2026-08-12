package SmartHarvest360.ml;

import SmartHarvest360.Crop;
import SmartHarvest360.Resource;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * J48 grade predictor: S–F aligned with Season Report ROI bands.
 */
public final class GradePredictor {
    private final Classifier model;
    private final Instances template;

    public GradePredictor(Classifier model, Instances template) {
        this.model = model;
        this.template = template;
        this.template.setClassIndex(this.template.numAttributes() - 1);
    }

    public String predict(
            FarmProfile profile,
            String cropName,
            Resource resource,
            String expectedProfitBand
    ) throws Exception {
        Instance row = new DenseInstance(template.numAttributes());
        row.setDataset(template);
        row.setValue(template.attribute("location"), profile.getLocation());
        row.setValue(template.attribute("soil"), profile.getSoilType());
        row.setValue(template.attribute("crop"), cropName);
        row.setValue(template.attribute("water"), resource.getWater());
        row.setValue(template.attribute("fertilizerStock"), resource.getFertilizer());
        row.setValue(template.attribute("budget"), resource.getBudget());
        row.setValue(template.attribute("expectedProfitBand"), expectedProfitBand);
        double cls = model.classifyInstance(row);
        return template.classAttribute().value((int) cls);
    }

    /** Maps expected crop profit into Low|Medium|High using ROI so catalog yields work. */
    public static String profitBand(Crop crop) {
        if (crop == null) {
            return "Medium";
        }
        double bonus = crop.calculateGrowthBonus();
        double revenue = crop.getYieldAmount() * crop.getMarketPrice() * bonus;
        double cost = crop.getYieldAmount() * crop.getCostPerKg();
        if (cost <= 0) {
            return "Medium";
        }
        double roi = (revenue - cost) / cost * 100.0;
        if (roi < 40) {
            return "Low";
        }
        if (roi < 100) {
            return "Medium";
        }
        return "High";
    }

    /** Actual grade from Season Report ROI logic. */
    public static String gradeFromRoi(double roiPercent, double profit) {
        if (profit < 0) {
            return "F";
        }
        if (roiPercent >= 120) {
            return "S";
        }
        if (roiPercent >= 80) {
            return "A";
        }
        if (roiPercent >= 40) {
            return "B";
        }
        if (roiPercent >= 10) {
            return "C";
        }
        return "D";
    }

    private static final String[] GRADE_LADDER = {"F", "D", "C", "B", "A", "S"};

    /** Nudge a base grade up/down using in-season care quality (0–100). */
    public static String nudgeGrade(String baseGrade, int careScore) {
        int index = indexOfGrade(baseGrade);
        int steps = (careScore - 50) / 18; // ~±2 letters across full care range
        int nudged = Math.max(0, Math.min(GRADE_LADDER.length - 1, index + steps));
        return GRADE_LADDER[nudged];
    }

    public static int indexOfGrade(String grade) {
        if (grade == null) {
            return 2;
        }
        for (int i = 0; i < GRADE_LADDER.length; i++) {
            if (GRADE_LADDER[i].equalsIgnoreCase(grade.trim())) {
                return i;
            }
        }
        return 2;
    }

    /** Soft ROI boost from care so better field decisions improve actual grade. */
    public static double careAdjustedRoi(double roiPercent, int careScore) {
        return roiPercent + (careScore - 50) * 0.55;
    }
}
