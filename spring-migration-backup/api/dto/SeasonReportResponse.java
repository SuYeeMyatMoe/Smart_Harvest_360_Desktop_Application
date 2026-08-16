package SmartHarvest360.api.dto;

import java.util.List;

/**
 * Structured season report (numbers only). The React frontend composes any
 * narrative text from these values.
 */
public record SeasonReportResponse(
        String seasonGoal,
        double revenue,
        double cost,
        double profit,
        double roi,
        double yieldKg,
        int waterShortageDays,
        long pestDays,
        long droughtDays,
        long frostDays,
        double endingWater,
        double endingFertilizer,
        double endingBudget,
        String plantedCrops,
        List<ChartPointDto> revenueByCrop,
        int pastSeasonCount,
        double pastAverageRoi,
        Double lastSeasonRoi,
        boolean recorded
) {
}
