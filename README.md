# SmartHarvest 360

SmartHarvest 360 is a JavaFX desktop application that simulates crop growth, compares market
prices, records harvest sales, and displays a final season report.

## Run in VS Code

1. Open the folder containing `pom.xml`.
2. Install Microsoft's **Extension Pack for Java** and wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`.

Use `Launcher` in VS Code. Running the JavaFX `SmartHarvestApp` class directly can trigger the
JDK's "JavaFX runtime components are missing" launcher check.

MySQL is optional: if a MySQL server is reachable (see `src/main/resources/db.properties`), farms,
crops, and sales are mirrored there; otherwise the app runs entirely on CSV files in the `data` directory.

## Application screens

0. `WelcomeScreen.fxml` / `WelcomeController.java` — captures the farmer's name
1. `FarmSetupScreen.fxml` / `FarmSetupController.java`
2. `CropSelectionScreen.fxml` / `CropSelectionController.java`
3. `SimulationScreen.fxml` / `SimulationController.java`
4. `HarvestMarketScreen.fxml` / `HarvestMarketController.java`
5. `SeasonReportScreen.fxml` / `SeasonReportController.java`

`FieldsOverviewScreen.fxml` / `FieldsOverviewController.java` is a dashboard (field cards, task list,
calendar) reachable from a button on the other screens.

The app opens on the Welcome screen; the captured farmer name is shown later in the
Crop Selection summary greeting.

## Supporting components

- `AppSession` shares the selected farm, crop, simulation progress, and sales between screens.
- `SceneNavigator` centralizes JavaFX scene navigation.
- `CSVFileHandler` loads `data/crops.csv`.
- `CsvDataStore` writes `harvest_log.csv`, `season_report.csv`, and `season_history.csv`.
- `SmartHarvest360.db` optionally mirrors farms, crops, and sales in MySQL.
- `model.SaleRecord` stores the details of a completed sale.

Farm Setup prepares the session; Crop Selection starts the simulation with:

```java
AppSession.getInstance().startSimulation(farm, selectedCrop);
```

Runtime CSV files are created in the project's `data` directory.
