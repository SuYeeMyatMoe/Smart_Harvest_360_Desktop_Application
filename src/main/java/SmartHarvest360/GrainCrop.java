package SmartHarvest360;

/** Grain-specific Crop subtype, demonstrating inheritance and polymorphism. */
public class GrainCrop extends Crop {
    public GrainCrop(String name, int growthDays, double waterNeed, double fertilizerNeed,
                     double yieldAmount, double costPerKg, double marketPrice) {
        super(name, "Grain", growthDays, waterNeed, fertilizerNeed,
                yieldAmount, costPerKg, marketPrice);
    }

    @Override
    public double calculateGrowthBonus() {
        return 1.20;
    }
}
