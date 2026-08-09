package SmartHarvest360.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

/** Connection factory for optional MySQL persistence. */
public final class Database {
    private static final Properties PROPERTIES = loadProperties();
    private static Boolean available;
    private static boolean schemaReady;

    private Database() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = openConnection().isPresent();
        }
        return available;
    }

    public static Optional<Connection> openConnection() {
        String url = PROPERTIES.getProperty("jdbc.url", "").trim();
        String user = PROPERTIES.getProperty("jdbc.user", "root").trim();
        String password = PROPERTIES.getProperty("jdbc.password", "");
        if (url.isEmpty()) {
            return Optional.empty();
        }

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            ensureSchema(connection);
            available = true;
            return Optional.of(connection);
        } catch (SQLException exception) {
            available = false;
            System.err.println("MySQL unavailable: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public static String statusLabel() {
        return isAvailable() ? "MySQL connected" : "MySQL offline";
    }

    private static void ensureSchema(Connection connection) throws SQLException {
        if (schemaReady) {
            return;
        }
        SchemaInitializer.initialize(connection);
        schemaReady = true;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = Database.class.getResourceAsStream("/db.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException exception) {
            System.err.println("Could not load db.properties: " + exception.getMessage());
        }
        return properties;
    }
}
