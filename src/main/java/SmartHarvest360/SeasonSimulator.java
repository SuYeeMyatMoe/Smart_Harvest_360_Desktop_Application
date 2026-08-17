package SmartHarvest360;

import SmartHarvest360.weather.NasaPowerClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs a complete season across all crops planted on the farm. */
public class SeasonSimulator {
    public record DaySnapshot(int day, Map<String, Map<String, Double>> pricesByCrop) { }
    public record SeasonDayResult(int day, Weather weather,
                                  List<SimulationEngine.DayResult> results,
                                  List<String> logLines) { }
    public record SeasonEvent(int day, String cropName, RandomEvent event) { }

    private final Farm farm;
    private final List<SimulationEngine> engines = new ArrayList<>();
    private final Market market;
    private final List<String> log = new ArrayList<>();
    private final List<SeasonEvent> events = new ArrayList<>();
    private final Map<String, Boolean> sold = new HashMap<>();
    private final Map<String, Map<String, Double>> priceState = new HashMap<>();
    private final List<DaySnapshot> priceHistory = new ArrayList<>();
    private final List<NasaPowerClient.DailyWeather> weatherPlan;
    private int currentDay;
    private int waterShortageDays;
    private int weatherPlanIndex;

    public SeasonSimulator(Farm farm, long seed) { this(farm, seed, null); }

    public SeasonSimulator(Farm farm, long seed, List<NasaPowerClient.DailyWeather> weatherPlan) {
        if (farm == null) throw new IllegalArgumentException("Farm is required");
        this.farm = farm;
        this.market = new Market(seed);
        this.weatherPlan = weatherPlan == null ? List.of() : List.copyOf(weatherPlan);
        int index = 0;
        for (Crop crop : farm.getCrops()) {
            engines.add(SimulationEngine.startSimulation(farm, crop,
                    seed + index * 7919L + crop.getName().hashCode()));
            priceState.put(crop.getName(), market.getMarketPrices(crop));
            index++;
        }
    }

    public boolean usesRealWeather() { return !weatherPlan.isEmpty(); }
    public Farm getFarm() { return farm; }
    public List<SimulationEngine> getEngines() { return List.copyOf(engines); }
    public List<String> getLog() { return List.copyOf(log); }
    public List<SeasonEvent> getEvents() { return List.copyOf(events); }
    public int getWaterShortageDays() { return waterShortageDays; }
    public int getCurrentDay() { return currentDay; }
    public Market getMarket() { return market; }
    public boolean isSold(String cropName) { return sold.getOrDefault(cropName, false); }
    public void markSold(String cropName) { sold.put(cropName, true); }

    public SimulationEngine findEngine(Crop crop) {
        for (SimulationEngine engine : engines) if (engine.getCrop() == crop) return engine;
        return null;
    }

    public double progressFraction(Crop crop) {
        SimulationEngine engine = findEngine(crop);
        return engine == null ? 0.0 : engine.getGrowthProgress();
    }

    public List<SimulationEngine> getReadyCrops() {
        return engines.stream().filter(SimulationEngine::isReady).toList();
    }

    public List<SimulationEngine> getUnsoldReadyCrops() {
        return engines.stream().filter(SimulationEngine::isReady)
                .filter(engine -> !isSold(engine.getCropName())).toList();
    }

    public boolean allReady() {
        return !engines.isEmpty() && engines.stream().allMatch(SimulationEngine::isReady);
    }

    public Map<String, Double> getCurrentPrices(String cropName) { return priceState.get(cropName); }
    public List<DaySnapshot> getPriceHistory() { return List.copyOf(priceHistory); }

    public SeasonDayResult advanceDay() {
        if (allReady()) return new SeasonDayResult(currentDay, Weather.CLOUDY, List.of(), List.of());
        currentDay++;
        NasaPowerClient.DailyWeather daily = nextDailyWeather();
        Weather sharedWeather = daily == null ? null : daily.weather();
        RandomEvent forcedEvent = daily != null && daily.frost() ? RandomEvent.FROST : null;
        Weather primaryWeather = null;
        List<SimulationEngine.DayResult> results = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        Map<String, Map<String, Double>> prices = new LinkedHashMap<>();

        for (SimulationEngine engine : engines) {
            SimulationEngine.DayResult result = engine.advanceDay(sharedWeather, forcedEvent);
            results.add(result);
            if (primaryWeather == null) primaryWeather = result.weather;
            Map<String, Double> next = market.nextDayPrices(priceState.get(engine.getCropName()));
            priceState.put(engine.getCropName(), next);
            prices.put(engine.getCropName(), next);
            String line = String.format(java.util.Locale.US, "Day %d | %s | %s %d%%",
                    currentDay, result.weather.getLabel(), engine.getCropName(),
                    Math.round(result.growthProgress * 100));
            if (result.event != RandomEvent.NONE) {
                line += " | " + result.event.getLabel();
                events.add(new SeasonEvent(currentDay, engine.getCropName(), result.event));
            }
            if (result.waterUsed == 0.0) waterShortageDays++;
            lines.add(line);
        }
        priceHistory.add(new DaySnapshot(currentDay, prices));
        log.addAll(lines);
        return new SeasonDayResult(currentDay, primaryWeather, results, lines);
    }

    private NasaPowerClient.DailyWeather nextDailyWeather() {
        return weatherPlanIndex < weatherPlan.size() ? weatherPlan.get(weatherPlanIndex++) : null;
    }
}
