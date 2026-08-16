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
     * Plants a crop only if the farm has enough budget for the planting cost
     * and enough land. Both are consumed on a successful plant.
     */
    public boolean plantCrop(Crop crop) {
        double cost = crop.getPlantingCost();
        if (!resource.consume(ResourceType.BUDGET, cost)) {
            return false;
        }
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
    }}