
package SmartHarvest360;

public class VegetableCrop extends Crop {

    // Constructor
    public VegetableCrop(String name, int growthDays, double waterNeed, double fertilizerNeed, double yieldAmount, double costPerKg, double marketPrice) {

        super(name, "Vegetable", growthDays, waterNeed, fertilizerNeed, yieldAmount, costPerKg, marketPrice);
    }

    // Growth bonus for vegetable crops
    @Override
    public double calculateGrowthBonus() {
        return 1.10;
    }
}

