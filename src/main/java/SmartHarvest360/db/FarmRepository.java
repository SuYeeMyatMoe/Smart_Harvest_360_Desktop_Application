package SmartHarvest360.db;

import SmartHarvest360.Farm;
import SmartHarvest360.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** Persists farm setup rows to MySQL. */
public final class FarmRepository {
    private FarmRepository() {
    }

    public static Optional<Long> insert(String farmName, Farm farm) {
        if (farm == null || farmName == null || farmName.isBlank()) {
            return Optional.empty();
        }

        Optional<Connection> optional = Database.openConnection();
        if (optional.isEmpty()) {
            return Optional.empty();
        }

        Resource resource = farm.getResource();
        String sql = """
                INSERT INTO farms (name, budget, water, fertilizer, land)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = optional.get();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, farmName.trim());
            statement.setDouble(2, resource.getBudget());
            statement.setDouble(3, resource.getWater());
            statement.setDouble(4, resource.getFertilizer());
            statement.setDouble(5, resource.getLand());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return Optional.of(keys.getLong(1));
                }
            }
        } catch (SQLException exception) {
            System.err.println("Could not save farm to MySQL: " + exception.getMessage());
        }
        return Optional.empty();
    }
}
