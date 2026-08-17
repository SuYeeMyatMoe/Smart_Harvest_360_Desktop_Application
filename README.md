# SmartHarvest 360

SmartHarvest 360 is a JavaFX desktop application that simulates crop growth with ML farm advice,
compares market prices, records harvest sales, and shows a final season report with charts and CSV export.
The simulation can use recent NASA POWER weather for the selected Malaysian state and automatically
falls back to generated offline weather when the service is unavailable.

## Run in VS Code

1. Open the folder containing `pom.xml`.
2. Install Microsoft's **Extension Pack for Java** and wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`.

Use `Launcher` in VS Code. Running the JavaFX `SmartHarvestApp` class directly can trigger the
JDK's "JavaFX runtime components are missing" launcher check.

## Application flow

1. **Farm Setup** – farm name, Malaysia state, soil, budget, water, fertilizer, land
2. **Crop Selection** – browse/edit `data/crops.csv`, Weka ML advisor, plant crops (Back returns to setup)
3. **Simulation** – day-by-day field actions, grade coach, activity log
4. **Detailed Plan** – steps and recommendations from the season so far
5. **Harvest & Market** – sell the crop
6. **Season Report** – revenue/cost/profit/ROI, two charts, one full CSV download

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
| `data/harvest_log.csv` | Sale history |
| `data/season_report.csv` | Auto-saved season summary + sales + activity |
| `data/activity_log.csv` | Simulation day log |
| `data/season_history.csv` | Append-only season results for future comparisons |
| `data/detailed_plan_report.csv` | Post-sim plan export |
| `data/ml/` | Cached Weka models |
| `data/downloads/` | Optional download folder |

## Supporting components

- `AppSession` – shared farm, crop, simulation, sales, advisor result
- `SceneNavigator` – JavaFX screen switching (keeps window size)
- `CSVFileHandler` – load/save `data/crops.csv`
- `CsvDataStore` – harvest / season / activity CSV export and season history
- `SeasonSimulator` – seeded multi-crop simulation, random events, and daily market-price drift
- `NasaPowerClient` – free NASA POWER daily weather integration with offline fallback
- `SmartHarvest360.db` – optional MySQL mirror
- `SmartHarvest360.ml` – Weka crop / fertilizer / grade advice
- `SmartHarvest360.plan` – detailed plan report after simulation

Farm Setup prepares the session; Crop Selection starts the simulation with:

```java
AppSession.getInstance().startSimulation(farm, selectedCrop);
```
