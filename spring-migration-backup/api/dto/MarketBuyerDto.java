package SmartHarvest360.api.dto;

/** One buyer (market) and the price it is offering. */
public record MarketBuyerDto(
        String market,
        double pricePerKg,
        boolean best
) {
}
