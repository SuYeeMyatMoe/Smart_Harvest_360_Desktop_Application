package SmartHarvest360.api.dto;

import java.util.List;

/** Result of POST /api/market/sell. */
public record SellResponse(
        SaleDto sale,
        List<String> remainingReadyCrops,
        boolean allSold
) {
}
