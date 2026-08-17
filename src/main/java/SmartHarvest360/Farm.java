package SmartHarvest360;

import java.util.ArrayList;

// It manages crops and farm resources. This represents OOP Concept : Polymorphism and Collection

public class Farm {

    private ArrayList<Crop> crops;
    private Resource resource;

    public Farm(Resource resource) {
        this.resource = resource;
        this.crops = new ArrayList<>();
    }

    
     // Adds a crop to the farm.

    public void addCrop(Crop crop) {
        crops.add(crop);
    }

    /**
     * Plants atomically when budget and land are available. Existing UI code may
     * still use addCrop when it has already validated resources itself.
     */
    public boolean plantCrop(Crop crop) {
        if (crop == null) return false;
        double cost = crop.getPlantingCost();
        if (!resource.consume(ResourceType.BUDGET, cost)) return false;
        if (!resource.consume(ResourceType.LAND, 1.0)) {
            resource.add(ResourceType.BUDGET, cost);
            return false;
        }
        crops.add(crop);
        return true;
    }

    
     // Returns all planted crops.
    
    public ArrayList<Crop> getCrops() {
        return crops;
    }

    
    // Returns the farm resources.
     
    public Resource getResource() {
        return resource;
    }

   
     // Calculates total expected profit.
    
    public double totalExpectedProfit() {

        double total = 0;

        for (Crop crop : crops) {

            double revenue =
                    crop.getYieldAmount()
                    * crop.getMarketPrice()
                    * crop.calculateGrowthBonus();

            double cost =
                    crop.getYieldAmount()
                    * crop.getCostPerKg();

            total += (revenue - cost);
        }

        return total;
    }
}
