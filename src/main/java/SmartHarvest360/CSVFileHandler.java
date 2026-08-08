package SmartHarvest360;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.List;

/**
 * the harvest/sale CSV logs.
 * Columns match CsvDataStore output:
 * day, crop, quantitySold, market, revenue, cost, profit
 */
public class CSVFileHandler {

    private static final String HEADER = "day,crop,quantitySold,market,revenue,cost,profit";

    
    public void appendSale(String filePath, SaleRecord sale) throws IOException {

        File file = new File(filePath);
        boolean isNew = !file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (isNew) {
                writer.write(HEADER + "\n");
            }
            writer.write(String.format("%d,%s,%.2f,%s,%.2f,%.2f,%.2f%n",
                    sale.getDay(),
                    sale.getCropName(),
                    sale.getQuantity(),
                    sale.getMarket(),
                    sale.getRevenue(),
                    sale.getCost(),
                    sale.getProfit()));
        }
    }

    /**
      A full season report (all sales at once)
     */
    public void writeSeasonReport(String filePath, List<SaleRecord> sales) throws IOException {

        try (FileWriter writer = new FileWriter(filePath, false)) {
            writer.write(HEADER + "\n");
            for (SaleRecord sale : sales) {
                writer.write(String.format("%d,%s,%.2f,%s,%.2f,%.2f,%.2f%n",
                        sale.getDay(),
                        sale.getCropName(),
                        sale.getQuantity(),
                        sale.getMarket(),
                        sale.getRevenue(),
                        sale.getCost(),
                        sale.getProfit()));
            }
        }
    }

    
}
