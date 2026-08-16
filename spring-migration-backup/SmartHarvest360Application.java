package SmartHarvest360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for SmartHarvest 360.
 *
 * <p>Replaces the JavaFX {@code Launcher}/{@code SmartHarvestApp} entry points: this
 * process starts a pure REST API server with no UI window. All original simulation
 * business logic (Crop/Farm/SimulationEngine/SeasonSimulator, db/, data/, session/)
 * is reused as-is; the JavaFX screens were replaced by the endpoints in
 * {@code SmartHarvest360.api}.
 */
@SpringBootApplication
public class SmartHarvest360Application {

    public static void main(String[] args) {
        SpringApplication.run(SmartHarvest360Application.class, args);
    }
}
