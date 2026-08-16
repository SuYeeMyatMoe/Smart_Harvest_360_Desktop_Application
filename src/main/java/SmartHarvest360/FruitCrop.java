
package SmartHarvest360;

public class FruitCrop extends Crop {

    // Constructor
    public FruitCrop(String name, int growthDays, double waterNeed, double fertilizerNeed, double yieldAmount, double costPerKg, double marketPrice) {

        super(name, "Fruit", growthDays, waterNeed, fertilizerNeed, yieldAmount, costPerKg, marketPrice);
    }

    // Growth bonus for fruit crops
    @Override
    public double calculateGrowthBonus() {
        return 1.30;
    }
}
