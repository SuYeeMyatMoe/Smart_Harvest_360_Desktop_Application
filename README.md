# SmartHarvest 360 Desktop Application

SmartHarvest 360 is a JavaFX desktop application that simulates crop growth with ML farm advice,
compares market prices, records harvest sales, and shows a final season report with 3D charts and CSV export.
The simulation can use recent NASA POWER weather for the selected Malaysian state and automatically
falls back to generated offline weather when the service is unavailable.

## Run in VS Code

1. Open the folder containing `pom.xml`.
2. Install Microsoft's **Extension Pack for Java** and wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`.

Use `Launcher` in VS Code. Running the JavaFX `SmartHarvestApp` class directly can trigger the
JDK's "JavaFX runtime components are missing" launcher check.

## Application flow

The app opens on a cinematic **Intro**, then follows a branded season path
(Setup → Crops → Sim → Market → Report). **Detailed Plan** sits between Simulation and Market.

**First launch** always starts at Intro. After a season finishes, **Start New Season** on the Season Report
clears the in-memory session and returns to **Farm Setup** (not Intro) so you can run another season
without restarting the app. Saved CSV files under `data/` stay on disk (`crops.csv`, harvest log,
season history).

```text
[Launch]
    │
    ▼
  Intro  ── Enter Smart Farm (first launch only)
    │
    ▼
  Farm Setup ◄────────────── Start New Season (clears live session, keeps CSV files)
    │
    ▼
  Crop Selection
    │
    ▼
  Simulation ⇄ Fields Overview → Detailed Plan → Harvest & Market → Season Report
                                                    │
                                                    ├── Start New Season → Farm Setup
                                                    └── Exit Application → close the app
```

| # | Screen | FXML | What you do |
|---|---|---|---|
| 0 | **Intro** | `IntroScreen.fxml` | Cinematic splash. **Enter Smart Farm** continues; **Watch Film** plays the bundled promo. |
| 1 | **Farm Setup** | `FarmSetupScreen.fxml` | Farm name, Malaysia state, soil, budget, water, fertilizer, land. State later drives NASA weather and nearby shops. CSV / MySQL status badge. |
| 2 | **Crop Selection** | `CropSelectionScreen.fxml` | Browse/edit `data/crops.csv`, Weka J48 advisor (recommended crop, fertilizer plan, grade), plant crops. Back returns to setup. |
| 3 | **Simulation** | `SimulationScreen.fxml` | Day-by-day field: interactive 3D crop + growth film, NASA/offline weather, Irrigate / Conserve / Fertilize / Protect, grade coach, Play + speed, activity log. **Fields Overview** opens a live command center, then returns here. |
| 3a | **Fields Overview** | `FieldsOverviewScreen.fxml` | Live KPIs, generated tasks, season calendar, and a card per planted crop with growth, water, yield, and best-buyer estimate. |
| 4 | **Detailed Plan** | `PlanReportScreen.fxml` | Field steps, coaching notes, recommendations, download plan CSV. |
| 5 | **Harvest & Market** | `HarvestMarketScreen.fxml` | Location-aware buyers with shop logos; score = 70% price + 30% demand; live sale overview; confirm sale. |
| 6 | **Season Report** | `SeasonReportScreen.fxml` | Revenue / cost / profit / ROI, predicted vs actual grade, four standard charts (crop revenue, totals, growth, water/fertilizer), full CSV download. **Start New Season** resets the live session and opens Farm Setup again; **Exit Application** closes the window. |

Season screens share the app logo header, step dots, card layout, and `style.css`. Scene changes fade in with a short leaf sweep (`SceneNavigator`).

## MySQL (optional)

The app is **CSV-first**. It runs fully without a database. MySQL only mirrors farms, crops, and sales when available.

1. Install MySQL and create an empty database:

```sql
CREATE DATABASE smartharvest;
```

2. Copy the env template and set your password in the project root:

```bash
copy .env.example .env
```

Edit `.env`:

```properties
DB_URL=jdbc:mysql://localhost:3306/smartharvest?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=YOUR_PASSWORD
```

`src/main/resources/db.properties` holds non-secret defaults. The real password comes from `.env` (gitignored).

3. Restart the app. Farm Setup / Crop Selection show **MySQL connected** or **MySQL offline**.

If MySQL is offline, crop catalog saves and reports still work through CSV under `data/`.

Schema tables are created automatically on first successful connection (`SmartHarvest360.db` package).

## ML farm advisor (Weka J48 classification)

Full details (dataset, ARFF, models, runtime behavior, retrain steps): see [`ML.md`](ML.md).

- **Model:** Weka **J48** decision-tree **classifier** (not regression).
- **Training data:** Malaysia *Crop Area and Production by State* (DOSM/DOA, 2017–2022) in
  `src/main/resources/ml/crops_state_dataset.csv`.
- **Mapping:** `paddy`→Paddy, `vegetables`→Tomato/Lettuce/Chili, `fruits`→Durian/Papaya, `cash_crops`→Corn.
- Models cache under `data/ml/`. Fertilizer plan and grade are also J48 classifiers.
- Regenerate ARFF after dataset changes:

```bash
python scripts/generate_ml_arff.py
```

Then delete `data/ml/*.model` and `data/ml/*.meta` so models retrain on next launch.

## CSV / data files

| Path | Purpose |
|---|---|
| `data/crops.csv` | Editable crop catalog |
| `data/market_locations.csv` | State-based buyers / shops (names, types, logos, price multipliers) |
| `data/harvest_log.csv` | Sale history |
| `data/season_report.csv` | Auto-saved season summary + sales + activity |
| `data/activity_log.csv` | Simulation day log |
| `data/season_history.csv` | Append-only season results for future comparisons |
| `data/detailed_plan_report.csv` | Post-sim plan export |
| `data/ml/` | Cached Weka models |
| `data/downloads/` | Optional download folder |

## Supporting components

- `AppSession` – shared farm, crop, simulation, sales, advisor result
- `SceneNavigator` – JavaFX screen switching with fade / leaf-sweep transitions (keeps window size)
- `CSVFileHandler` – load/save `data/crops.csv` (`FruitCrop` / `VegetableCrop` / `GrainCrop`)
- `CsvDataStore` – harvest / season / activity CSV export and season history
- `MarketLocationDataStore` – nearby shops from `data/market_locations.csv`
- `SeasonSimulator` – seeded multi-crop simulation, random events, and daily market-price drift
- `NasaPowerClient` – free NASA POWER daily weather integration with offline fallback
- `SmartHarvest360.ui` – intro promo player, 3D crop field, 3D revenue pie, 3D finance bars
- `SmartHarvest360.db` – optional MySQL mirror
- `SmartHarvest360.ml` – Weka crop / fertilizer / grade advice
- `SmartHarvest360.plan` – detailed plan report after simulation

Farm Setup prepares the session; Crop Selection starts the simulation with:

```java
AppSession.getInstance().startSimulation(farm, selectedCrop);
```
