package SmartHarvest360.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the data directory even when VS Code starts Java from a nested folder. */
public final class DataPaths {
    private static final int MAX_LEVELS = 8;

    private DataPaths() { }

    public static Path dataDirectory() {
        Path root = findProjectRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
        return root == null ? Path.of("data") : root.resolve("data");
    }

    public static Path cropsFile() { return dataDirectory().resolve("crops.csv"); }

    private static Path findProjectRoot(Path start) {
        Path dir = start;
        for (int level = 0; level < MAX_LEVELS; level++) {
            if (Files.exists(dir.resolve("pom.xml"))) return dir;
            try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                for (Path child : children) {
                    if (Files.isDirectory(child) && Files.exists(child.resolve("pom.xml"))) return child;
                }
            } catch (IOException ignored) {
                // Keep walking upward if a directory cannot be read.
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return null;
    }
}
