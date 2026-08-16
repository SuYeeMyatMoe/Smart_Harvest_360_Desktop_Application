package SmartHarvest360;

import SmartHarvest360.weather.NasaPowerClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs one season across every crop planted on the farm.
 * Each simulated day advances every crop, reports a line per crop,
 * drifts market prices, and tracks which crops have been sold.
 */
public class SeasonSimulator {

    public record DaySnapshot(int day, Map<String, Map<String, Double>> pricesByCrop) {}
    public record SeasonDayResult(int day, Weather weather,
                                  List<SimulationEngine.DayResult> results,
                                  List<String> logLines) {}
    /** A structured record of one random event, for the season report narrative. */
    public record SeasonEvent(int day, String cropName, RandomEvent event) {}

    private final Farm farm;
    private final List<SimulationEngine> engines;
    private final Market market;
    private final List<String> log;
    private final List<SeasonEvent> events;
    private final Map<String, Boolean> sold;
    private final Map<String, Map<String, Double>> priceState;
    private final List<DaySnapshot> priceHistory;
    private final List<NasaPowerClient.DailyWeather> weatherPlan;
    private int waterShortageDays;
    private int weatherPlanIndex;
    private int currentDay;

    public SeasonSimulator(Farm farm, long seed) {
        this(farm, seed, null);
    }

    public SeasonSimulator(Farm farm, long seed, List<NasaPowerClient.DailyWeather> weatherPlan) {
        this.farm = farm;
        this.market = new Market(seed);
        this.engines = new ArrayList<>();
        this.log = new ArrayList<>();
        this.events = new ArrayList<>();
        this.sold = new HashMap<>();
        this.priceState = new HashMap<>();
        this.priceHistory = new ArrayList<>();
        this.weatherPlan = weatherPlan;
        this.weatherPlanIndex = 0;
        this.currentDay = 0;
        this.waterShortageDays = 0;
        int i = 0;
        for (Crop crop : farm.getCrops()) {
            engines.add(SimulationEngine.startSimulation(
                    farm, crop, seed + i * 7919L + crop.getName().hashCode()));
            priceState.put(crop.getName(), market.getMarketPrices(crop));
            i++;
        }
    }

    /** True when this season is driven by real NASA POWER weather instead of random generation. */
    public boolean usesRealWeather() {
        return weatherPlan != null && !weatherPlan.isEmpty();
    }

    public Farm getFarm() {
        return farm;
    }

    public List<SimulationEngine> getEngines() {
        return engines;
    }

    public List<String> getLog() {
        return log;
    }

    /** Structured random-event records for the whole season, in day order. */
    public List<SeasonEvent> getEvents() {
        return events;
    }

    /** Number of days any crop ran out of water this season. */
    public int getWaterShortageDays() {
        return waterShortageDays;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public Market getMarket() {
        return market;
    }

    public boolean isSold(String cropName) {
        return sold.getOrDefault(cropName, false);
    }

    public void markSold(String cropName) {
        sold.put(cropName, true);
    }

    public SimulationEngine findEngine(Crop crop) {
        for (SimulationEngine engine : engines) {
            if (engine.getCrop() == crop) {
                return engine;
            }
        }
        return null;
    }

    public double progressFraction(Crop crop) {
        SimulationEngine engine = findEngine(crop);
        return engine == null ? 0.0 : engine.getGrowthProgress();
    }

    public List<SimulationEngine> getReadyCrops() {
        List<SimulationEngine> ready = new ArrayList<>();
        for (SimulationEngine engine : engines) {
            if (engine.isReady()) {
                ready.add(engine);
            }
        }
        return ready;
    }

    public List<SimulationEngine> getUnsoldReadyCrops() {
        List<SimulationEngine> result = new ArrayList<>();
        for (SimulationEngine engine : getReadyCrops()) {
            if (!isSold(engine.getCropName())) {
                result.add(engine);
            }
        }
        return result;
    }

    public boolean allReady() {
        if (engines.isEmpty()) {
            return false;
        }
        for (SimulationEngine engine : engines) {
            if (!engine.isReady()) {
                return false;
            }
        }
        return true;
    }

    /** Latest offered prices for a crop at harvest time. */
    public Map<String, Double> getCurrentPrices(String cropName) {
        return priceState.get(cropName);
    }

    /** Per-day snapshot of every crop's offered prices. */
    public List<DaySnapshot> getPriceHistory() {
        return priceHistory;
    }

    public SeasonDayResult advanceDay() {
        if (allReady()) {
            return new SeasonDayResult(currentDay, Weather.CLOUDY, List.of(), List.of());
        }
        currentDay++;

        NasaPowerClient.DailyWeather daily = nextDailyWeather();
        Weather sharedWeather = daily == null ? null : daily.weather();
        RandomEvent sharedEvent = daily != null && daily.frost() ? RandomEvent.FROST : null;

        Weather primaryWeather = null;
        List<SimulationEngine.DayResult> results = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        Map<String, Map<String, Double>> pricesByCrop = new LinkedHashMap<>();

        for (SimulationEngine engine : engines) {
            SimulationEngine.DayResult result = engine.advanceDay(sharedWeather, sharedEvent);
            results.add(result);
            if (primaryWeather == null) {
                primaryWeather = result.weather;
            }

            Map<String, Double> next = market.nextDayPrices(priceState.get(engine.getCropName()));
            priceState.put(engine.getCropName(), next);
            pricesByCrop.put(engine.getCropName(), next);

            StringBuilder line = new StringBuilder();
            line.append(String.format(java.util.Locale.US,
                    "Day %d | %s | %s %d%%",
                    currentDay, result.weather.getLabel(),
                    engine.getCropName(), Math.round(result.growthProgress * 100)));
            if (result.event != RandomEvent.NONE) {
                line.append(" | ").append(result.event.getLabel());
                events.add(new SeasonEvent(currentDay, engine.getCropName(), result.event));
            }
            if (result.waterUsed == 0.0) {
                waterShortageDays++;
            }
            lines.add(line.toString());
        }
        priceHistory.add(new DaySnapshot(currentDay, pricesByCrop));
        log.addAll(lines);
        return new SeasonDayResult(currentDay, primaryWeather, results, lines);
    }

    private NasaPowerClient.DailyWeather nextDailyWeather() {
        if (weatherPlan == null || weatherPlanIndex >= weatherPlan.size()) {
            return null;
        }
        return weatherPlan.get(weatherPlanIndex++);
    }
}
