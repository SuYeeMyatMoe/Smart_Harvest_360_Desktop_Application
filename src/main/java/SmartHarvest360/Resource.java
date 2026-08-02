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
}