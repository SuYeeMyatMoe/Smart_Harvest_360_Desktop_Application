package SmartHarvest360;

import java.util.List;

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
            total += sale.getQuantity();
        }

        return total;
    }

    /**
     * Calculates the total revenue from all sales.
     */
    public double getTotalRevenue(List<SaleRecord> sales) {

        double total = 0;

        for (SaleRecord sale : sales) {
            total += sale.getRevenue();
        }

        return total;
    }

    /**
     * Calculates the total cost from all sales.
     */
    public double getTotalCost(List<SaleRecord> sales) {

        double total = 0;

        for (SaleRecord sale : sales) {
            total += sale.getCost();
        }

        return total;
    }

    /**
     * Calculates the total profit from all sales.
     */
    public double getTotalProfit(List<SaleRecord> sales) {

        double total = 0;

        for (SaleRecord sale : sales) {
            total += sale.getProfit();
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

        String bestMarket = sales.get(0).getMarket();
        double highestRevenue = 0;

        for (SaleRecord sale : sales) {

            if (sale.getRevenue() > highestRevenue) {
                highestRevenue = sale.getRevenue();
                bestMarket = sale.getMarket();
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
                + String.format("%.2f", quantity) + " kg\n"
                + "Total Revenue: RM"
                + String.format("%.2f", revenue) + "\n"
                + "Total Cost: RM"
                + String.format("%.2f", cost) + "\n"
                + "Total Profit: RM"
                + String.format("%.2f", profit) + "\n"
                + "Best Selling Market: "
                + getBestSellingMarket(sales) + "\n"
                + "=================================";
    }
}