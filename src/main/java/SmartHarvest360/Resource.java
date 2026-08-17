package SmartHarvest360;

 // Stores and manages farm resources.This OOP Concept represents Encapsulation

public class Resource {

    private double water;
    private double fertilizer;
    private double budget;
    private double land;

    public Resource(double water, double fertilizer, double budget, double land) {
        this.water = water;
        this.fertilizer = fertilizer;
        this.budget = budget;
        this.land = land;
    }

    // Getters

    public double getWater() {
        return water;
    }

    public double getFertilizer() {
        return fertilizer;
    }

    public double getBudget() {
        return budget;
    }

    public double getLand() {
        return land;
    }

    // Setters

    public void setWater(double water) {
        this.water = water;
    }

    public void setFertilizer(double fertilizer) {
        this.fertilizer = fertilizer;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public void setLand(double land) {
        this.land = land;
    }

    
     //It checks if enough resources are available.

    public boolean isAvailable(String type, double amount) {

        switch (type.toLowerCase()) {

            case "water":
                return water >= amount;

            case "fertilizer":
                return fertilizer >= amount;

            case "budget":
                return budget >= amount;

            case "land":
                return land >= amount;

            default:
                return false;
        }
    }

    /** Type-safe overload used by the integrated season simulation backend. */
    public boolean isAvailable(ResourceType type, double amount) {
        return type != null && isAvailable(type.name(), amount);
    }

    public boolean consume(String type, double amount) {

        if (!isAvailable(type, amount))
            return false;

        switch (type.toLowerCase()) {

            case "water":
                water -= amount;
                break;

            case "fertilizer":
                fertilizer -= amount;
                break;

            case "budget":
                budget -= amount;
                break;

            case "land":
                land -= amount;
                break;
        }

        return true;
    }

    /** Type-safe overload used by the integrated season simulation backend. */
    public boolean consume(ResourceType type, double amount) {
        return type != null && consume(type.name(), amount);
    }

    /** Restores or increases a resource, for example after a failed planting transaction. */
    public void add(ResourceType type, double amount) {
        if (type == null || amount < 0) {
            throw new IllegalArgumentException("Resource type and non-negative amount are required");
        }
        switch (type) {
            case WATER -> water += amount;
            case FERTILIZER -> fertilizer += amount;
            case BUDGET -> budget += amount;
            case LAND -> land += amount;
        }
    }
}
