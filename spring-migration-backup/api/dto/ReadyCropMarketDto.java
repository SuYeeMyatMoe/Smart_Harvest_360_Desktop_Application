package SmartHarvest360.api.dto;

import java.util.List;

/** Market comparison for one ready (unsold) crop, plus its price history for charts. */
public record ReadyCropMarketDto(
        String cropName,
        double yieldAmount,
        String bestMarket,
        double bestPrice,
        List<MarketBuyerDto> buyers,
        List<PricePointDto> priceHistory
) {
}
