package SmartHarvest360.api.dto;

import java.util.List;

/** All ready, unsold crops with their market comparisons. */
public record BuyersResponse(List<ReadyCropMarketDto> crops) {
}
