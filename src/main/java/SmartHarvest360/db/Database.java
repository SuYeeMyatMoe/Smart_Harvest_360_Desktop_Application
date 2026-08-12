package SmartHarvest360.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        applyEnvFile(properties);
        applyProcessEnv(properties);
        return properties;
    }

    /** Loads project-root .env and overrides jdbc.* keys. */
    private static void applyEnvFile(Properties properties) {
        Path envPath = findEnvFile();
        if (envPath == null) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = stripQuotes(line.substring(eq + 1).trim());
                mapEnvKey(properties, key, value);
            }
        } catch (IOException exception) {
            System.err.println("Could not load .env: " + exception.getMessage());
        }
    }

    private static void applyProcessEnv(Properties properties) {
        mapEnvKey(properties, "DB_URL", System.getenv("DB_URL"));
        mapEnvKey(properties, "DB_USER", System.getenv("DB_USER"));
        mapEnvKey(properties, "DB_PASSWORD", System.getenv("DB_PASSWORD"));
        mapEnvKey(properties, "JDBC_URL", System.getenv("JDBC_URL"));
        mapEnvKey(properties, "JDBC_USER", System.getenv("JDBC_USER"));
        mapEnvKey(properties, "JDBC_PASSWORD", System.getenv("JDBC_PASSWORD"));
    }

    private static void mapEnvKey(Properties properties, String key, String value) {
        if (key == null || value == null || value.isBlank()) {
            return;
        }
        switch (key) {
            case "DB_URL", "JDBC_URL" -> properties.setProperty("jdbc.url", value);
            case "DB_USER", "JDBC_USER" -> properties.setProperty("jdbc.user", value);
            case "DB_PASSWORD", "JDBC_PASSWORD", "MYSQL_PASSWORD" ->
                    properties.setProperty("jdbc.password", value);
            default -> {
                // ignore unknown keys
            }
        }
    }

    private static Path findEnvFile() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path candidate = cwd.resolve(".env");
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        // Walk up a few levels (useful if launched from a subfolder).
        Path parent = cwd;
        for (int i = 0; i < 3; i++) {
            parent = parent.getParent();
            if (parent == null) {
                break;
            }
            candidate = parent.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
