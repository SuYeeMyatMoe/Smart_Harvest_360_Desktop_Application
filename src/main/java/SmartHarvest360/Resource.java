package SmartHarvest360;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stores and manages farm resources.
 * OOP Concept: Encapsulation.
 */
public class Resource {

    private final Map<ResourceType, Double> amounts = new EnumMap<>(ResourceType.class);

    public Resource(double water, double fertilizer, double budget, double land) {
        amounts.put(ResourceType.WATER, water);
        amounts.put(ResourceType.FERTILIZER, fertilizer);
        amounts.put(ResourceType.BUDGET, budget);
        amounts.put(ResourceType.LAND, land);
    }

    public double get(ResourceType type) {
        Double value = amounts.get(type);
        return value == null ? 0.0 : value;
    }

    public boolean isAvailable(ResourceType type, double amount) {
        return get(type) >= amount;
    }

    public boolean consume(ResourceType type, double amount) {
        if (!isAvailable(type, amount)) {
            return false;
        }
        amounts.put(type, get(type) - amount);
        return true;
    }

    public void add(ResourceType type, double amount) {
        amounts.put(type, get(type) + amount);
    }

    public double getWater() {
        return get(ResourceType.WATER);
    }

    public double getFertilizer() {
        return get(ResourceType.FERTILIZER);
    }

    public double getBudget() {
        return get(ResourceType.BUDGET);
    }

    public double getLand() {
        return get(ResourceType.LAND);
    }

    public void setWater(double water) {
        amounts.put(ResourceType.WATER, water);
    }

    public void setFertilizer(double fertilizer) {
        amounts.put(ResourceType.FERTILIZER, fertilizer);
    }

    public void setBudget(double budget) {
        amounts.put(ResourceType.BUDGET, budget);
    }

    public void setLand(double land) {
        amounts.put(ResourceType.LAND, land);
    }
}
