# SmartHarvest 360

SmartHarvest 360 is a JavaFX desktop application that simulates crop growth, compares market
prices, records harvest sales, and displays a final season report.

## Run in VS Code

1. Open the folder containing `pom.xml`.
2. Install Microsoft's **Extension Pack for Java** and wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`.

Use `Launcher` in VS Code. Running the JavaFX `SmartHarvestApp` class directly can trigger the
JDK's "JavaFX runtime components are missing" launcher check.

## Application screens

- `SimulationScreen.fxml` / `SimulationController.java`
- `HarvestMarketScreen.fxml` / `HarvestMarketController.java`
- `SeasonReportScreen.fxml` / `SeasonReportController.java`

## Supporting components

- `AppSession` shares the selected farm, crop, simulation progress, and sales between screens.
- `SceneNavigator` centralizes JavaFX scene navigation.
- `CsvDataStore` writes `harvest_log.csv` and `season_report.csv`.
- `SaleRecord` stores the details of a completed sale.

The farm setup or crop selection flow can start the simulation with:

```java
AppSession.getInstance().startSimulation(farm, selectedCrop);
```

Runtime CSV files are created in the project's `data` directory.
