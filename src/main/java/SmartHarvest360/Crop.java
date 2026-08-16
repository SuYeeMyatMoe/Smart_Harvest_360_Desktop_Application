
package SmartHarvest360;

/**
 * Abstract parent class representing a crop in the SmartHarvest 360 system.
 * Stores information that is common to all crops.
 * OOP Concepts: Abstraction, Encapsulation.
 */
public abstract class Crop {

    // Private fields - Encapsulation
    private String name;
    private String type;
    private int growthDays;
    private double waterNeed;
    private double fertilizerNeed;
    private double yieldAmount;
    private double costPerKg;
    private double marketPrice;

    // Constructor
    public Crop(String name, String type, int growthDays, double waterNeed, double fertilizerNeed, double yieldAmount, double costPerKg, double marketPrice) {

        this.name = name;
        this.type = type;
        this.growthDays = growthDays;
        this.waterNeed = waterNeed;
        this.fertilizerNeed = fertilizerNeed;
        this.yieldAmount = yieldAmount;
        this.costPerKg = costPerKg;
        this.marketPrice = marketPrice;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getGrowthDays() {
        return growthDays;
    }

    public void setGrowthDays(int growthDays) {
        this.growthDays = growthDays;
    }

    public double getWaterNeed() {
        return waterNeed;
    }

    public void setWaterNeed(double waterNeed) {
        this.waterNeed = waterNeed;
    }

    public double getFertilizerNeed() {
        return fertilizerNeed;
    }

    public void setFertilizerNeed(double fertilizerNeed) {
        this.fertilizerNeed = fertilizerNeed;
    }

    public double getYieldAmount() {
        return yieldAmount;
    }

    public void setYieldAmount(double yieldAmount) {
        this.yieldAmount = yieldAmount;
    }

    public double getCostPerKg() {
        return costPerKg;
    }

    public void setCostPerKg(double costPerKg) {
        this.costPerKg = costPerKg;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(double marketPrice) {
        this.marketPrice = marketPrice;
    }

    // One-time planting cost charged against the farm budget when this crop is planted
    public double getPlantingCost() {
        return costPerKg * yieldAmount;
    }

    // Each subclass provides its own growth bonus
    public abstract double calculateGrowthBonus();

    // Used when displaying crop information
    @Override
    public String toString() {
        return "[" + type + "] " + name + " Growth: " + growthDays + " days" + " Yield: " + yieldAmount + "kg" + " Price: RM" + String.format("%.2f", marketPrice) + "/kg";
    }
}

