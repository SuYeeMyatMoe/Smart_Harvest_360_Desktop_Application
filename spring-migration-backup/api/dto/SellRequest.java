package SmartHarvest360.api.dto;

/** Body for POST /api/market/sell. quantity defaults to the full yield when omitted. */
public record SellRequest(String cropName, Double quantity) {
}
