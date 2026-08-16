package SmartHarvest360.model;

/**
 * One finished season's summary row, appended to data/season_history.csv.
 * Season number is implied by the row's position in the file (index + 1).
 */
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
) {
}
