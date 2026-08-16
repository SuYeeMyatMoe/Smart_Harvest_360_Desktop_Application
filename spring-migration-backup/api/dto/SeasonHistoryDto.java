package SmartHarvest360.api.dto;

import SmartHarvest360.model.SeasonHistory;

/** One stored season row from data/season_history.csv. */
public record SeasonHistoryDto(
        int seasonNumber,
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
    public static SeasonHistoryDto from(SeasonHistory history, int seasonNumber) {
        return new SeasonHistoryDto(
                seasonNumber, history.goal(), history.revenue(), history.cost(),
                history.profit(), history.roi(), history.yieldKg(),
                history.waterShortageDays(), history.pestDays(), history.droughtDays(),
                history.frostDays(), history.endingWater(), history.endingFertilizer(),
                history.endingBudget(), history.plantedCrops()
        );
    }
}
