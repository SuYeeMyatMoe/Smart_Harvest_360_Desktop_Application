package SmartHarvest360.api.dto;

import SmartHarvest360.model.SaleRecord;

/** One completed sale, matching the desktop harvest log columns. */
public record SaleDto(
        int day,
        String cropName,
        double quantity,
        String market,
        double unitPrice,
        double revenue,
        double cost,
        double profit
) {
    public static SaleDto from(SaleRecord sale) {
        return new SaleDto(sale.day(), sale.cropName(), sale.quantity(), sale.market(),
                sale.unitPrice(), sale.revenue(), sale.cost(), sale.profit());
    }
}
