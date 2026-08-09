package SmartHarvest360.db;

import SmartHarvest360.model.SaleRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Best-effort sale persistence for the hybrid MySQL layer. */
public final class SaleRepository {
    private SaleRepository() {
    }

    public static void insert(Long farmId, SaleRecord sale) {
        if (sale == null) {
            return;
        }

        var optional = Database.openConnection();
        if (optional.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO sales (
                    farm_id, day, crop_name, quantity, market, revenue, cost, profit
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = optional.get();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (farmId == null) {
                statement.setObject(1, null);
            } else {
                statement.setLong(1, farmId);
            }
            statement.setInt(2, sale.day());
            statement.setString(3, sale.cropName());
            statement.setDouble(4, sale.quantity());
            statement.setString(5, sale.market());
            statement.setDouble(6, sale.revenue());
            statement.setDouble(7, sale.cost());
            statement.setDouble(8, sale.profit());
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("Could not save sale to MySQL: " + exception.getMessage());
        }
    }
}
