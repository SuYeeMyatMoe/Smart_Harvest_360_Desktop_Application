package SmartHarvest360.db;

import SmartHarvest360.Crop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Upserts crop catalog rows into MySQL. */
public final class CropRepository {
    private CropRepository() {
    }

    public static void upsertAll(List<Crop> crops) {
        if (crops == null || crops.isEmpty()) {
            return;
        }

        Optional<Connection> optional = Database.openConnection();
        if (optional.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO crops (
                    name, type, growth_days, water_need, fertilizer_need,
                    yield_amount, cost_per_kg, market_price
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    type = VALUES(type),
                    growth_days = VALUES(growth_days),
                    water_need = VALUES(water_need),
                    fertilizer_need = VALUES(fertilizer_need),
                    yield_amount = VALUES(yield_amount),
                    cost_per_kg = VALUES(cost_per_kg),
                    market_price = VALUES(market_price)
                """;

        try (Connection connection = optional.get();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Crop crop : crops) {
                statement.setString(1, crop.getName());
                statement.setString(2, crop.getType());
                statement.setInt(3, crop.getGrowthDays());
                statement.setDouble(4, crop.getWaterNeed());
                statement.setDouble(5, crop.getFertilizerNeed());
                statement.setDouble(6, crop.getYieldAmount());
                statement.setDouble(7, crop.getCostPerKg());
                statement.setDouble(8, crop.getMarketPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            System.err.println("Could not sync crops to MySQL: " + exception.getMessage());
        }
    }
}
