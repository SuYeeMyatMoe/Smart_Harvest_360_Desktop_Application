package SmartHarvest360.session;

import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.VegetableCrop;
import SmartHarvest360.ml.AdvisorResult;
import SmartHarvest360.ml.FarmProfile;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SimDayLog;
import SmartHarvest360.plan.DetailedPlanReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps shared farm, crop, simulation, location,
 * and sale state between application screens.
 */
public final class AppSession {

    private static final AppSession INSTANCE =
            new AppSession();

    private String farmName;
    private Long farmId;

    private Farm farm;

    private FarmProfile farmProfile;

    private AdvisorResult advisorResult;

    private DetailedPlanReport detailedPlanReport;

    private Crop activeCrop;

    private int currentDay;

    private int completedGrowthDays;

    private int careScore;

    private final List<String> simulationLog =
            new ArrayList<>();

    private final List<SimDayLog> dayLogs =
            new ArrayList<>();

    private final List<SaleRecord> sales =
            new ArrayList<>();

    private AppSession() {
    }

    public static AppSession getInstance() {
        return INSTANCE;
    }

    /**
     * Prepares a new farm.
     */
    public void prepareFarm(
            String name,
            Farm selectedFarm) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Farm name is required"
            );
        }

        if (selectedFarm == null) {

            throw new IllegalArgumentException(
                    "Farm is required"
            );
        }

        farmName =
                name.trim();

        farm =
                selectedFarm;

        farmId =
                null;

        activeCrop =
                null;

        advisorResult =
                null;

        detailedPlanReport =
                null;

        currentDay =
                0;

        completedGrowthDays =
                0;

        careScore =
                50;

        simulationLog.clear();

        dayLogs.clear();

        sales.clear();
    }

    /**
     * Stores the selected farm location and soil profile.
     */
    public void setFarmProfile(
            FarmProfile profile) {

        farmProfile =
                profile;
    }

    public FarmProfile getFarmProfile() {

        return farmProfile;
    }

    /**
     * Convenience method for retrieving the selected state.
     */
    public String getFarmLocation() {

        if (farmProfile == null) {
            return null;
        }

        return farmProfile.getLocation();
    }

    /**
     * Convenience method for retrieving the selected soil.
     */
    public String getSoilType() {

        if (farmProfile == null) {
            return null;
        }

        return farmProfile.getSoilType();
    }

    public void setAdvisorResult(
            AdvisorResult result) {

        advisorResult =
                result;
    }

    public AdvisorResult getAdvisorResult() {

        return advisorResult;
    }

    public void setDetailedPlanReport(
            DetailedPlanReport report) {

        detailedPlanReport =
                report;
    }

    public DetailedPlanReport getDetailedPlanReport() {

        return detailedPlanReport;
    }

    /**
     * Creates demo data if a screen requires it.
     */
    public void ensureDemoData() {

        if (farm != null
                && activeCrop != null) {

            return;
        }

        Resource resource =
                new Resource(
                        200.0,
                        20.0,
                        10_000.0,
                        5.0
                );

        Farm demoFarm =
                new Farm(resource);

        Crop tomato =
                new VegetableCrop(
                        "Tomato",
                        90,
                        50.0,
                        2.0,
                        500.0,
                        2.20,
                        5.50
                );

        demoFarm.addCrop(
                tomato
        );

        farmName =
                "Demo Farm";

        farmId =
                null;

        /*
         * Give the demo farm a location so that
         * the market screen can still demonstrate
         * location-based buyers.
         */
        farmProfile =
                new FarmProfile(
                        "Selangor",
                        "Loam"
                );

        startSimulation(
                demoFarm,
                tomato
        );
    }

    /**
     * Starts the simulation.
     */
    public void startSimulation(
            Farm selectedFarm,
            Crop selectedCrop) {

        if (selectedFarm == null
                || selectedCrop == null) {

            throw new IllegalArgumentException(
                    "Farm and crop are required"
            );
        }

        farm =
                selectedFarm;

        activeCrop =
                selectedCrop;

        currentDay =
                1;

        completedGrowthDays =
                1;

        careScore =
                50;

        simulationLog.clear();

        dayLogs.clear();

        detailedPlanReport =
                null;
    }

    /**
     * Clears the current season.
     */
    public void beginNewSeason() {

        farmName =
                null;

        farmId =
                null;

        farm =
                null;

        farmProfile =
                null;

        advisorResult =
                null;

        detailedPlanReport =
                null;

        activeCrop =
                null;

        currentDay =
                0;

        completedGrowthDays =
                0;

        careScore =
                50;

        simulationLog.clear();

        dayLogs.clear();

        sales.clear();
    }

    public void resetDemoSeason() {

        beginNewSeason();

        ensureDemoData();
    }

    public String getFarmName() {

        return farmName;
    }

    public void setFarmId(
            Long farmId) {

        this.farmId =
                farmId;
    }

    public Long getFarmId() {

        return farmId;
    }

    public Farm getFarm() {

        return farm;
    }

    public Crop getActiveCrop() {

        return activeCrop;
    }

    public int getCurrentDay() {

        return currentDay;
    }

    public void advanceDay() {

        currentDay++;
    }

    public int getCompletedGrowthDays() {

        return completedGrowthDays;
    }

    public void addGrowthDay() {

        completedGrowthDays++;
    }

    public boolean isCropReady() {

        return activeCrop != null
                && completedGrowthDays
                >= activeCrop.getGrowthDays();
    }

    public List<String> getSimulationLog() {

        return simulationLog;
    }

    public List<SimDayLog> getDayLogs() {

        return dayLogs;
    }

    public void addDayLog(
            SimDayLog log) {

        dayLogs.add(log);

        simulationLog.add(
                String.format(
                        java.util.Locale.US,
                        "Day %d | %s | %s | Water %.2fL | Fert %.2fkg | %s | Growth %d%%",
                        log.getDay(),
                        log.getWeather(),
                        log.getAction(),
                        log.getWaterUsed(),
                        log.getFertilizerUsed(),
                        log.getStatus(),
                        log.getGrowthPercent()
                )
        );
    }

    public int getCareScore() {

        return careScore;
    }

    public void adjustCareScore(
            int delta) {

        careScore =
                Math.max(
                        0,
                        Math.min(
                                100,
                                careScore + delta
                        )
                );
    }

    public List<SaleRecord> getSales() {

        return List.copyOf(
                sales
        );
    }

    public void addSale(
            SaleRecord sale) {

        sales.add(
                sale
        );
    }
}