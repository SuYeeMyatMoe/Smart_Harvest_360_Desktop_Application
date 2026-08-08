package SmartHarvest360;

import java.util.Random;

/**
 * Drives the day-by-day crop simulation.
 * Simulation, Harvest & Market, and Season Report screens  calls startSimulation() once, then advanceDay() once per simulated day.
 */
public class SimulationEngine {

    private final Farm farm;
    private final Crop crop;

    private int currentDay;
    private double growthProgress; // 0.0 -> 1.0
    private boolean ready;

    private final Random random = new Random();

    public SimulationEngine(Farm farm, Crop crop) {
        this.farm = farm;
        this.crop = crop;
        this.currentDay = 0;
        this.growthProgress = 0.0;
        this.ready = false;
    }

    /**
     * Call once at the start of a simulation run.
     * Mirrors AppSession.getInstance().startSimulation(farm, selectedCrop)
     */
    public static SimulationEngine startSimulation(Farm farm, Crop crop) {
        return new SimulationEngine(farm, crop);
    }

    /**
     * Advances the simulation by one day: generates weather, consumes
     * resources, updates growth progress, and reports the day's result.
     * Returns null if resources ran out and the day could not be simulated.
     */
    public DayResult advanceDay() {

        currentDay++;

        String weather = generateWeather();

        // Water/fertilizer used this day, scaled by crop need and weather.
        double waterUsed = crop.getWaterNeed() / crop.getGrowthDays() * weatherWaterFactor(weather);
        double fertilizerUsed = crop.getFertilizerNeed() / crop.getGrowthDays();

        Resource resource = farm.getResource();

        boolean gotWater = resource.consume("water", waterUsed);
        boolean gotFertilizer = resource.consume("fertilizer", fertilizerUsed);

        if (!gotWater || !gotFertilizer) {
            // Not enough resources today; still report state, but growth stalls.
            return new DayResult(currentDay, weather, 0, 0, growthProgress, ready, resource);
        }

        growthProgress = Math.min(1.0, (double) currentDay / crop.getGrowthDays());
        ready = growthProgress >= 1.0;

        return new DayResult(currentDay, weather, waterUsed, fertilizerUsed, growthProgress, ready, resource);
    }

    public boolean isReady() {
        return ready;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    private String generateWeather() {
        String[] options = {"Sunny", "Rain", "Cloudy"};
        return options[random.nextInt(options.length)];
    }

    private double weatherWaterFactor(String weather) {
        switch (weather) {
            case "Sunny": return 1.2;
            case "Rain": return 0.6;
            default: return 1.0; // Cloudy
        }
    }

    /**
     * Snapshot of one simulated day, for the UI to render directly.
     */
    public static class DayResult {
        public final int day;
        public final String weather;
        public final double waterUsed;
        public final double fertilizerUsed;
        public final double growthProgress; // 0.0 -> 1.0
        public final boolean ready;
        public final Resource updatedResource;

        public DayResult(int day, String weather, double waterUsed, double fertilizerUsed,
                          double growthProgress, boolean ready, Resource updatedResource) {
            this.day = day;
            this.weather = weather;
            this.waterUsed = waterUsed;
            this.fertilizerUsed = fertilizerUsed;
            this.growthProgress = growthProgress;
            this.ready = ready;
            this.updatedResource = updatedResource;
        }
    }
}
