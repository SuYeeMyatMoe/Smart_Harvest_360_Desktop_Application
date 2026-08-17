package SmartHarvest360.model;

/** One finished season's summary, stored in data/season_history.csv. */
public record SeasonHistory(
        String goal,
        double revenue,
        double cost,
        double profit,
        double roi,
        double yieldKg,
        int waterShortageDays,
        int pestDays,
        int droughtDays,
        int frostDays,
        double endingWater,
        double endingFertilizer,
        double endingBudget,
        String plantedCrops
) { }
