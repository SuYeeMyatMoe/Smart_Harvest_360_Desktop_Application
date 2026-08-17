package SmartHarvest360;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/** Day-by-day crop simulation with weather, resources, and random events. */
public class SimulationEngine {
    private static final double WATER_SHORTAGE_PENALTY = 0.30;
    private static final double NO_FERTILIZER_PENALTY = 0.50;
    private static final double FERTILIZER_BOOST = 0.10;
    private final Farm farm;
    private final Crop crop;
    private final Random random;
    private int currentDay;
    private double growthProgress;
    private double totalWaterUsed;
    private boolean ready;

    public SimulationEngine(Farm farm, Crop crop) { this(farm, crop, System.nanoTime()); }

    public SimulationEngine(Farm farm, Crop crop, long seed) {
        if (farm == null || crop == null) throw new IllegalArgumentException("Farm and crop are required");
        this.farm = farm;
        this.crop = crop;
        this.random = new Random(seed);
    }

    public static SimulationEngine startSimulation(Farm farm, Crop crop) {
        return new SimulationEngine(farm, crop);
    }

    public static SimulationEngine startSimulation(Farm farm, Crop crop, long seed) {
        return new SimulationEngine(farm, crop, seed);
    }

    public String getCropName() { return crop.getName(); }
    public Crop getCrop() { return crop; }
    public boolean isReady() { return ready; }
    public int getCurrentDay() { return currentDay; }
    public double getGrowthProgress() { return growthProgress; }
    public double getTotalWaterUsed() { return totalWaterUsed; }

    public DayResult advanceDay() { return advanceDay(null, null); }

    /** Accepts real external weather; null preserves the offline random fallback. */
    public DayResult advanceDay(Weather externalWeather, RandomEvent forcedEvent) {
        currentDay++;
        Weather weather = externalWeather == null ? generateWeather() : externalWeather;
        RandomEvent event = forcedEvent == null ? generateEvent() : forcedEvent;
        Resource resource = farm.getResource();
        double waterNeed = crop.getWaterNeed() / crop.getGrowthDays() * weather.getWaterFactor();
        double fertilizerNeed = crop.getFertilizerNeed() / crop.getGrowthDays();
        if (event == RandomEvent.DROUGHT) waterNeed *= 1.5;
        boolean gotWater = resource.consume(ResourceType.WATER, waterNeed);
        boolean gotFertilizer = resource.consume(ResourceType.FERTILIZER, fertilizerNeed);
        double progress = (1.0 / crop.getGrowthDays()) * weather.getGrowthFactor();
        if (!gotWater) progress *= WATER_SHORTAGE_PENALTY;
        if (!gotFertilizer) progress *= NO_FERTILIZER_PENALTY;
        else progress *= 1.0 + FERTILIZER_BOOST;
        if (event == RandomEvent.PEST) progress *= 0.75;
        if (event == RandomEvent.FROST) progress *= 0.70;
        growthProgress = Math.min(1.0, growthProgress + progress);
        ready = growthProgress >= 1.0;
        double waterUsed = gotWater ? waterNeed : 0.0;
        totalWaterUsed += waterUsed;
        return new DayResult(currentDay, weather, event, waterUsed,
                gotFertilizer ? fertilizerNeed : 0.0, growthProgress, ready, resource);
    }

    private Weather generateWeather() {
        NavigableMap<Double, Weather> weighted = new TreeMap<>();
        double cumulative = 0.0;
        for (Weather weather : Weather.values()) {
            cumulative += weather.getProbability();
            weighted.put(cumulative, weather);
        }
        return weighted.higherEntry(random.nextDouble() * cumulative).getValue();
    }

    private RandomEvent generateEvent() {
        double roll = random.nextDouble();
        if (roll < 0.05) return RandomEvent.PEST;
        if (roll < 0.09) return RandomEvent.DROUGHT;
        if (roll < 0.12) return RandomEvent.FROST;
        return RandomEvent.NONE;
    }

    public static class DayResult {
        public final int day;
        public final Weather weather;
        public final RandomEvent event;
        public final double waterUsed;
        public final double fertilizerUsed;
        public final double growthProgress;
        public final boolean ready;
        public final Resource updatedResource;

        public DayResult(int day, Weather weather, RandomEvent event, double waterUsed,
                         double fertilizerUsed, double growthProgress, boolean ready,
                         Resource updatedResource) {
            this.day = day;
            this.weather = weather;
            this.event = event;
            this.waterUsed = waterUsed;
            this.fertilizerUsed = fertilizerUsed;
            this.growthProgress = growthProgress;
            this.ready = ready;
            this.updatedResource = updatedResource;
        }
    }
}
