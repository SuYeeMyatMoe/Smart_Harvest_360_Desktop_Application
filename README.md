# SmartHarvest 360

SmartHarvest 360 is a JavaFX desktop application that simulates crop growth, compares market
prices, records harvest sales, and displays a final season report.

## Run in VS Code

1. Open the folder containing `pom.xml`.
2. Install Microsoft's **Extension Pack for Java** and wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`.

Use `Launcher` in VS Code. Running the JavaFX `SmartHarvestApp` class directly can trigger the
JDK's "JavaFX runtime components are missing" launcher check.

See [`report.md`](report.md) for MySQL setup, the full 5-screen walkthrough, and CSV locations.

## Application screens

1. `FarmSetupScreen.fxml` / `FarmSetupController.java`
2. `CropSelectionScreen.fxml` / `CropSelectionController.java`
3. `SimulationScreen.fxml` / `SimulationController.java`
4. `HarvestMarketScreen.fxml` / `HarvestMarketController.java`
5. `SeasonReportScreen.fxml` / `SeasonReportController.java`

## Supporting components

- `AppSession` shares the selected farm, crop, simulation progress, and sales between screens.
- `SceneNavigator` centralizes JavaFX scene navigation.
- `CSVFileHandler` loads `data/crops.csv`.
- `CsvDataStore` writes `harvest_log.csv` and `season_report.csv`.
- `SmartHarvest360.db` optionally mirrors farms, crops, and sales in MySQL.
- `SaleRecord` stores the details of a completed sale.

Farm Setup prepares the session; Crop Selection starts the simulation with:

```java
AppSession.getInstance().startSimulation(farm, selectedCrop);
```

Runtime CSV files are created in the project's `data` directory.
