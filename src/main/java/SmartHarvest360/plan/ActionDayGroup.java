package SmartHarvest360.plan;

/**
 * Action totals for a stretch of simulation days (week-style groups).
 */
public final class ActionDayGroup {
    private final int fromDay;
    private final int toDay;
    private final String phase;
    private final String dominantAction;
    private final String mainWeather;
    private final int irrigateCount;
    private final int conserveCount;
    private final int fertilizeCount;
    private final int protectCount;
    private final int otherCount;
    private final double waterUsed;
    private final double fertilizerUsed;
    private final int startGrowth;
    private final int endGrowth;
    private final String actionMix;
    private final String summary;

    public ActionDayGroup(
            int fromDay,
            int toDay,
            String phase,
            String dominantAction,
            String mainWeather,
            int irrigateCount,
            int conserveCount,
            int fertilizeCount,
            int protectCount,
            int otherCount,
            double waterUsed,
            double fertilizerUsed,
            int startGrowth,
            int endGrowth,
            String actionMix,
            String summary
    ) {
        this.fromDay = fromDay;
        this.toDay = toDay;
        this.phase = phase == null ? "-" : phase;
        this.dominantAction = dominantAction == null ? "-" : dominantAction;
        this.mainWeather = mainWeather == null ? "-" : mainWeather;
        this.irrigateCount = irrigateCount;
        this.conserveCount = conserveCount;
        this.fertilizeCount = fertilizeCount;
        this.protectCount = protectCount;
        this.otherCount = otherCount;
        this.waterUsed = waterUsed;
        this.fertilizerUsed = fertilizerUsed;
        this.startGrowth = startGrowth;
        this.endGrowth = endGrowth;
        this.actionMix = actionMix == null ? "-" : actionMix;
        this.summary = summary == null ? "" : summary;
    }

    public int getFromDay() {
        return fromDay;
    }

    public int getToDay() {
        return toDay;
    }

    public String getDaysLabel() {
        return fromDay == toDay ? "Day " + fromDay : "Days " + fromDay + "-" + toDay;
    }

    public String getPhase() {
        return phase;
    }

    public String getDominantAction() {
        return dominantAction;
    }

    public String getMainWeather() {
        return mainWeather;
    }

    public int getIrrigateCount() {
        return irrigateCount;
    }

    public int getConserveCount() {
        return conserveCount;
    }

    public int getFertilizeCount() {
        return fertilizeCount;
    }

    public int getProtectCount() {
        return protectCount;
    }

    public int getOtherCount() {
        return otherCount;
    }

    public double getWaterUsed() {
        return waterUsed;
    }

    public double getFertilizerUsed() {
        return fertilizerUsed;
    }

    public String getResourcesLabel() {
        return String.format(java.util.Locale.US, "%.1f L / %.1f kg", waterUsed, fertilizerUsed);
    }

    public int getStartGrowth() {
        return startGrowth;
    }

    public int getEndGrowth() {
        return endGrowth;
    }

    public String getGrowthLabel() {
        return startGrowth + "% -> " + endGrowth + "%";
    }

    public String getActionMix() {
        return actionMix;
    }

    public String getSummary() {
        return summary;
    }
}
