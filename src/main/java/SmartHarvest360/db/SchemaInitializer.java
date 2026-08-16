package SmartHarvest360.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates SmartHarvest tables when they do not already exist. */
public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS farms (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(120) NOT NULL,
                        budget DOUBLE NOT NULL,
                        water DOUBLE NOT NULL,
                        fertilizer DOUBLE NOT NULL,
                        land DOUBLE NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS crops (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(80) NOT NULL UNIQUE,
                        type VARCHAR(40) NOT NULL,
                        growth_days INT NOT NULL,
                        water_need DOUBLE NOT NULL,
                        fertilizer_need DOUBLE NOT NULL,
                        yield_amount DOUBLE NOT NULL,
                        cost_per_kg DOUBLE NOT NULL,
                        market_price DOUBLE NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sales (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        farm_id BIGINT NULL,
                        day INT NOT NULL,
                        crop_name VARCHAR(80) NOT NULL,
                        quantity DOUBLE NOT NULL,
                        market VARCHAR(80) NOT NULL,
                        revenue DOUBLE NOT NULL,
                        cost DOUBLE NOT NULL,
                        profit DOUBLE NOT NULL,
                        sold_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_sales_farm
                            FOREIGN KEY (farm_id) REFERENCES farms(id)
                            ON DELETE SET NULL
                    )
                    """);
        }
    }
}
