package SmartHarvest360;

import SmartHarvest360.model.SaleRecord;

import java.util.List;
import java.util.Locale;

/**
 * Generates a summary report from completed sales.
 */
public class SalesReport {

    /**
     * Calculates the total quantity that has been sold.
     */
    public double getTotalQuantity(List<SaleRecord> sales) {
        double total = 0;
        for (SaleRecord sale : sales) {
            total += sale.quantity();
        }
        return total;
    }

    /**
     * Calculates the total revenue from all sales.
     */
    public double getTotalRevenue(List<SaleRecord> sales) {
        double total = 0;
        for (SaleRecord sale : sales) {
            total += sale.revenue();
        }
        return total;
    }

    /**
     * Calculates the total cost from all sales.
     */
    public double getTotalCost(List<SaleRecord> sales) {
        double total = 0;
        for (SaleRecord sale : sales) {
            total += sale.cost();
        }
        return total;
    }

    /**
     * Calculates the total profit from all sales.
     */
    public double getTotalProfit(List<SaleRecord> sales) {
        double total = 0;
        for (SaleRecord sale : sales) {
            total += sale.profit();
        }
        return total;
    }

    /**
     * Finds the market where the highest total revenue was generated.
     */
    public String getBestSellingMarket(List<SaleRecord> sales) {
        if (sales == null || sales.isEmpty()) {
            return "No sales recorded";
        }

        String bestMarket = sales.get(0).market();
        double highestRevenue = 0;

        for (SaleRecord sale : sales) {
            if (sale.revenue() > highestRevenue) {
                highestRevenue = sale.revenue();
                bestMarket = sale.market();
            }
        }
        return bestMarket;
    }

    /**
     * Creates a simple sales report as text.
     */
    public String generateReport(List<SaleRecord> sales) {
        if (sales == null || sales.isEmpty()) {
            return "No sales recorded.";
        }

        double quantity = getTotalQuantity(sales);
        double revenue = getTotalRevenue(sales);
        double cost = getTotalCost(sales);
        double profit = getTotalProfit(sales);

        return "========== SALES REPORT ==========\n"
                + "Total Quantity Sold: "
                + String.format(Locale.US, "%.2f", quantity) + " kg\n"
                + "Total Revenue: RM"
                + String.format(Locale.US, "%.2f", revenue) + "\n"
                + "Total Cost: RM"
                + String.format(Locale.US, "%.2f", cost) + "\n"
                + "Total Profit: RM"
                + String.format(Locale.US, "%.2f", profit) + "\n"
                + "Best Selling Market: "
                + getBestSellingMarket(sales) + "\n"
                + "=================================";
    }
}
