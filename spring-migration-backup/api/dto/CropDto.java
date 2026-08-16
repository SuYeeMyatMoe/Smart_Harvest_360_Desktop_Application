package SmartHarvest360.api.dto;

import SmartHarvest360.Crop;

/** One crop from the catalog, with the economics the desktop crop screen displayed. */
public record CropDto(
        String name,
        String type,
        int growthDays,
        double waterNeed,
        double fertilizerNeed,
        double yieldAmount,
        double costPerKg,
        double marketPrice,
        double plantingCost,
        double growthBonus,
        double expectedProfit,
        boolean bestRoi
) {
    /** Shared expected-profit formula, also used by the best-ROI recommendation. */
    public static double expectedProfit(Crop crop) {
        return crop.getYieldAmount() * crop.getMarketPrice() * crop.calculateGrowthBonus()
                - crop.getYieldAmount() * crop.getCostPerKg();
    }

    public static CropDto from(Crop crop, boolean bestRoi) {
        double bonus = crop.calculateGrowthBonus();
        double expectedProfit = expectedProfit(crop);
        return new CropDto(
                crop.getName(), crop.getType(), crop.getGrowthDays(),
                crop.getWaterNeed(), crop.getFertilizerNeed(), crop.getYieldAmount(),
                crop.getCostPerKg(), crop.getMarketPrice(), crop.getPlantingCost(),
                bonus, expectedProfit, bestRoi
        );
    }
}
