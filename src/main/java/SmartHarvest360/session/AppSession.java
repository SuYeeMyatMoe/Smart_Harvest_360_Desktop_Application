package SmartHarvest360.session;

import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Resource;
import SmartHarvest360.VegetableCrop;
import SmartHarvest360.model.SaleRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps shared farm, crop, simulation, and sale state between application screens.
 */
public final class AppSession {
    private static final AppSession INSTANCE = new AppSession();

    private Farm farm;
    private Crop activeCrop;
    private int currentDay;
    private int completedGrowthDays;
    private final List<String> simulationLog = new ArrayList<>();
    private final List<SaleRecord> sales = new ArrayList<>();

    private AppSession() {
    }

    public static AppSession getInstance() {
        return INSTANCE;
    }

    public void ensureDemoData() {
        if (farm != null && activeCrop != null) {
            return;
        }

        Resource resource = new Resource(200.0, 20.0, 10_000.0, 5.0);
        Farm demoFarm = new Farm(resource);
        Crop tomato = new VegetableCrop(
                "Tomato", 90, 50.0, 2.0, 500.0, 2.20, 5.50
        );
        demoFarm.addCrop(tomato);
        startSimulation(demoFarm, tomato);
    }

    public void startSimulation(Farm selectedFarm, Crop selectedCrop) {
        if (selectedFarm == null || selectedCrop == null) {
            throw new IllegalArgumentException("Farm and crop are required");
        }
        farm = selectedFarm;
        activeCrop = selectedCrop;
        currentDay = 1;
        completedGrowthDays = 1;
        simulationLog.clear();
    }

    public void resetDemoSeason() {
        farm = null;
        activeCrop = null;
        currentDay = 0;
        completedGrowthDays = 0;
        simulationLog.clear();
        sales.clear();
        ensureDemoData();
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
        return activeCrop != null && completedGrowthDays >= activeCrop.getGrowthDays();
    }

    public List<String> getSimulationLog() {
        return simulationLog;
    }

    public List<SaleRecord> getSales() {
        return List.copyOf(sales);
    }

    public void addSale(SaleRecord sale) {
        sales.add(sale);
    }
}
