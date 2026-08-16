package SmartHarvest360.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the project's data directory without depending on the JVM working
 * directory. Walks up from the current directory (also checking one level of
 * subfolders, for nested unzipped projects) until it finds the project root,
 * i.e. a folder containing pom.xml, and then returns &lt;root&gt;/data.
 * Falls back to a relative "data" path if the project root cannot be found.
 */
public final class DataPaths {

    private static final int MAX_LEVELS = 8;

    private DataPaths() {
    }

    public static Path dataDirectory() {
        Path root = findProjectRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
        return root == null ? Path.of("data") : root.resolve("data");
    }

    public static Path cropsFile() {
        return dataDirectory().resolve("crops.csv");
    }

    private static Path findProjectRoot(Path start) {
        Path dir = start;
        for (int level = 0; level < MAX_LEVELS; level++) {
            if (Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }
            try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                for (Path child : children) {
                    if (Files.isDirectory(child) && Files.exists(child.resolve("pom.xml"))) {
                        return child;
                    }
                }
            } catch (IOException ignored) {
                // skip unreadable directories and keep walking up
            }
            Path parent = dir.getParent();
            if (parent == null) {
                break;
            }
            dir = parent;
        }
        return null;
    }
}
