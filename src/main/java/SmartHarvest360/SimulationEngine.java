package SmartHarvest360;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * Drives the day-by-day crop simulation.
 * The Simulation, Harvest & Market, and Season Report screens call
 * startSimulation() once, then advanceDay() once per simulated day.
 *
 * Weather influences water consumption and daily growth, fertilizer
 * availability changes growth speed, and random events can help or hurt
 * the crop. The engine is seedable so runs are reproducible.
 */
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

    public SimulationEngine(Farm farm, Crop crop, long seed) {
        this.farm = farm;
        this.crop = crop;
        this.random = new Random(seed);
        this.currentDay = 0;
        this.growthProgress = 0.0;
        this.totalWaterUsed = 0.0;
        this.ready = false;
    }

    public static SimulationEngine startSimulation(Farm farm, Crop crop, long seed) {
        return new SimulationEngine(farm, crop, seed);
    }

    public String getCropName() {
        return crop.getName();
    }

    public Crop getCrop() {
        return crop;
    }

    public boolean isReady() {
        return ready;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public double getGrowthProgress() {
        return growthProgress;
    }

    /** Cumulative litres of water actually consumed by this crop so far. */
    public double getTotalWaterUsed() {
        return totalWaterUsed;
    }

    /**
     * Advances the simulation by one day: generates weather and random events,
     * consumes resources, updates growth progress, and reports the day's result.
     */
    public DayResult advanceDay() {
        return advanceDay(null, null);
    }

    /**
     * Advances one day using externally supplied weather (e.g. real data from
     * the NASA POWER service). A null weather falls back to weighted random
     * generation, and a non-null forced event overrides the random event roll.
     */
    public DayResult advanceDay(Weather externalWeather, RandomEvent forcedEvent) {
        currentDay++;

        Weather weather = externalWeather != null ? externalWeather : generateWeather();
        RandomEvent event = forcedEvent != null ? forcedEvent : generateEvent();
        Resource resource = farm.getResource();

        double waterNeed = crop.getWaterNeed() / (double) crop.getGrowthDays() * weather.getWaterFactor();
        double fertilizerNeed = crop.getFertilizerNeed() / (double) crop.getGrowthDays();
        if (event == RandomEvent.DROUGHT) {
            waterNeed *= 1.5;
        }

        boolean gotWater = resource.consume(ResourceType.WATER, waterNeed);
        boolean gotFertilizer = resource.consume(ResourceType.FERTILIZER, fertilizerNeed);

        double dayProgress = (1.0 / crop.getGrowthDays()) * weather.getGrowthFactor();
        if (!gotWater) {
            dayProgress *= WATER_SHORTAGE_PENALTY;
        }
        if (!gotFertilizer) {
            dayProgress *= NO_FERTILIZER_PENALTY;
        } else {
            dayProgress *= (1.0 + FERTILIZER_BOOST);
        }
        if (event == RandomEvent.PEST) {
            dayProgress *= 0.75;
        }
        if (event == RandomEvent.FROST) {
            dayProgress *= 0.70;
        }

        growthProgress = Math.min(1.0, growthProgress + dayProgress);
        ready = growthProgress >= 1.0;

        double waterUsed = gotWater ? waterNeed : 0.0;
        totalWaterUsed += waterUsed;

        return new DayResult(currentDay, weather, event,
                waterUsed,
                gotFertilizer ? fertilizerNeed : 0.0,
                growthProgress, ready, resource);
    }

    private Weather generateWeather() {
        NavigableMap<Double, Weather> weighted = new TreeMap<>();
        double cumulative = 0.0;
        for (Weather weather : Weather.values()) {
            cumulative += weather.getProbability();
            weighted.put(cumulative, weather);
        }
        double roll = random.nextDouble() * cumulative;
        return weighted.higherEntry(roll).getValue();
    }

    private RandomEvent generateEvent() {
        double roll = random.nextDouble();
        if (roll < 0.05) {
            return RandomEvent.PEST;
        }
        if (roll < 0.09) {
            return RandomEvent.DROUGHT;
        }
        if (roll < 0.12) {
            return RandomEvent.FROST;
        }
        return RandomEvent.NONE;
    }

    /**
     * Snapshot of one simulated day, for the UI to render directly.
     */
    public static class DayResult {
        public final int day;
        public final Weather weather;
        public final RandomEvent event;
        public final double waterUsed;
        public final double fertilizerUsed;
        public final double growthProgress;
        public final boolean ready;
        public final Resource updatedResource;

        public DayResult(int day, Weather weather, RandomEvent event,
                         double waterUsed, double fertilizerUsed,
                         double growthProgress, boolean ready, Resource updatedResource) {
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
