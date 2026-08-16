package SmartHarvest360.session;

import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.SeasonGoal;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.SimulationEngine;
import SmartHarvest360.VegetableCrop;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.weather.NasaPowerClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps shared farm, crop, season, and sale state between application screens.
 */
public final class AppSession {
    private static final AppSession INSTANCE = new AppSession();

    private String farmName;
    private String farmerName;
    private Long farmId;
    private Farm farm;
    private Crop activeCrop;
    private SeasonSimulator season;
    private NasaPowerClient.Location location;
    private SeasonGoal seasonGoal = SeasonGoal.MAXIMIZE_ROI;
    private final List<SaleRecord> sales = new ArrayList<>();

    private AppSession() {
    }

    public static AppSession getInstance() {
        return INSTANCE;
    }

    public void prepareFarm(String name, Farm selectedFarm) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Farm name is required");
        }
        if (selectedFarm == null) {
            throw new IllegalArgumentException("Farm is required");
        }
        farmName = name.trim();
        farm = selectedFarm;
        farmId = null;
        activeCrop = null;
        season = null;
        sales.clear();
    }

    public void ensureDemoData() {
        if (farm != null && season != null) {
            return;
        }
        Resource resource = new Resource(200.0, 20.0, 10_000.0, 5.0);
        Farm demoFarm = new Farm(resource);
        demoFarm.addCrop(new VegetableCrop("Tomato", 90, 50, 2, 500, 2.2, 5.5));
        startSimulation(demoFarm, demoFarm.getCrops().get(0));
    }

    public void startSimulation(Farm farm, Crop crop) {
        this.farm = farm;
        this.activeCrop = crop;
        this.season = buildSeason(farm);
        this.sales.clear();
    }

    public void resetDemoSeason() {
        ensureDemoData();
        beginNewSeason();
    }

    public void beginNewSeason() {
        if (farm != null) {
            season = buildSeason(farm);
        } else {
            season = null;
        }
        sales.clear();
    }

    private SeasonSimulator buildSeason(Farm farm) {
        long seed = System.nanoTime();
        List<NasaPowerClient.DailyWeather> weatherPlan = null;
        if (location != null) {
            int days = 0;
            for (Crop crop : farm.getCrops()) {
                days = Math.max(days, crop.getGrowthDays());
            }
            weatherPlan = NasaPowerClient.fetchSeason(location, days + 5);
        }
        if (weatherPlan == null || weatherPlan.isEmpty()) {
            return new SeasonSimulator(farm, seed);
        }
        return new SeasonSimulator(farm, seed, weatherPlan);
    }

    public void setLocation(NasaPowerClient.Location location) {
        this.location = location;
    }

    public NasaPowerClient.Location getLocation() {
        return location;
    }

    public SeasonSimulator getSeason() {
        return season;
    }

    public void ensureSeasonReady() {
        if (season == null) {
            ensureDemoData();
        }
    }

    public boolean isCropReady() {
        return season != null && season.allReady();
    }

    public void markSold(String cropName) {
        if (season != null) {
            season.markSold(cropName);
        }
    }

    public boolean isSold(String cropName) {
        return season != null && season.isSold(cropName);
    }

    public List<SimulationEngine> getUnsoldReadyCrops() {
        ensureSeasonReady();
        return season.getUnsoldReadyCrops();
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmerName(String name) {
        farmerName = name == null ? null : name.trim();
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setSeasonGoal(SeasonGoal goal) {
        seasonGoal = goal == null ? SeasonGoal.MAXIMIZE_ROI : goal;
    }

    public SeasonGoal getSeasonGoal() {
        return seasonGoal;
    }

    public Farm getFarm() {
        return farm;
    }

    public List<String> getSimulationLog() {
        if (season == null) {
            return new ArrayList<>();
        }
        return season.getLog();
    }

    public List<SaleRecord> getSales() {
        return sales;
    }

    public int getCurrentDay() {
        if (season == null) {
            return 0;
        }
        return season.getCurrentDay();
    }

    public void addSale(SaleRecord sale) {
        sales.add(sale);
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public Long getFarmId() {
        return farmId;
    }

    public int getProgressPercent() {
        return (int) Math.round(getProgressFraction() * 100);
    }

    public double getProgressFraction() {
        if (season == null || activeCrop == null) {
            return 0.0;
        }
        return season.progressFraction(activeCrop);
    }
}
