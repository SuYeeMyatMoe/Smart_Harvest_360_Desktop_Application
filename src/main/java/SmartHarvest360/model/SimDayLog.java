package SmartHarvest360.model;

/**
 * One simulated day row for the activity log table.
 */
public final class SimDayLog {
    private final int day;
    private final String weather;
    private final String action;
    private final double waterUsed;
    private final double fertilizerUsed;
    private final String status;
    private final int growthPercent;

    public SimDayLog(
            int day,
            String weather,
            String action,
            double waterUsed,
            double fertilizerUsed,
            String status,
            int growthPercent
    ) {
        this.day = day;
        this.weather = weather;
        this.action = action;
        this.waterUsed = waterUsed;
        this.fertilizerUsed = fertilizerUsed;
        this.status = status;
        this.growthPercent = growthPercent;
    }

    public int getDay() {
        return day;
    }

    public String getWeather() {
        return weather;
    }

    public String getAction() {
        return action;
    }

    public double getWaterUsed() {
        return waterUsed;
    }

    public double getFertilizerUsed() {
        return fertilizerUsed;
    }

    public String getStatus() {
        return status;
    }

    public int getGrowthPercent() {
        return growthPercent;
    }
}
