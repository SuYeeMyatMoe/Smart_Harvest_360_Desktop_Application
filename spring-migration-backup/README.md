# Spring Boot migration — aborted, preserved here

The Spring Boot REST backend work from the (aborted) JavaFX → Spring Boot + React migration
is kept in this folder so nothing is lost. The project continues as the original JavaFX
desktop app.

Contents:

- `SmartHarvest360Application.java` — the Spring Boot entry point (`@SpringBootApplication`).
- `api/` — the REST layer: `SessionService`, 6 controllers (`SessionController`,
  `FarmController`, `CropController`, `SimulationController`, `MarketController`,
  `ReportController`), `WebConfig` (CORS), `ApiExceptionHandler`, and the `dto/` records.
  It wraps the unchanged business logic (`AppSession`, `SimulationEngine`, `SeasonSimulator`,
  `db/`, `data/`).
- `static/` — the `index.html` landing page served at `/`.

If you ever want to resume the migration:

1. Restore the files to their original locations (`src/main/java/SmartHarvest360/...`,
   `src/main/resources/static/`).
2. Swap the `pom.xml` back to the Spring Boot parent + `spring-boot-starter-web` and re-add the
   `maven-compiler-plugin` excludes for the legacy JavaFX packages.
3. Delete this folder.
