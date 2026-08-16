package SmartHarvest360.api.dto;

import java.util.Map;

/** Market prices offered on a single simulated day, keyed by market name. */
public record PricePointDto(
        int day,
        Map<String, Double> prices
) {
}
