
package SmartHarvest360;

public class GrainCrop extends Crop {

    // Constructor
    public GrainCrop(String name, int growthDays, double waterNeed, double fertilizerNeed, double yieldAmount, double costPerKg, double marketPrice) {

        super(name, "Grain", growthDays, waterNeed, fertilizerNeed, yieldAmount, costPerKg, marketPrice);
    }

    // Growth bonus for grain crops
    @Override
    public double calculateGrowthBonus() {
        return 1.20;
    }
}
