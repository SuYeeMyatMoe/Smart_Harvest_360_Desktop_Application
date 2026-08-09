# SmartHarvest 360 — Run Report (P4 Hybrid)

This guide covers how to run the full five-screen flow with CSV persistence and optional MySQL storage.

## Prerequisites

- JDK compatible with the project (`pom.xml` targets Java 25)
- Maven 3.9+ (or VS Code **Extension Pack for Java**, which imports Maven automatically)
- MySQL Server **optional** — the app still runs if MySQL is offline

## 1. Create the MySQL database (optional)

```sql
CREATE DATABASE smartharvest;
```

Edit [`src/main/resources/db.properties`](src/main/resources/db.properties):

```properties
jdbc.url=jdbc:mysql://localhost:3306/smartharvest?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
jdbc.user=root
jdbc.password=YOUR_PASSWORD
```

Do not commit real passwords. Leave `jdbc.password` empty if your local root has no password.

On first successful connection the app creates:

- `farms`
- `crops`
- `sales`

## 2. Run in VS Code

1. Open the folder that contains `pom.xml`.
2. Wait for Maven import to finish.
3. Open `SmartHarvest360.Launcher` and click **Run** above `main`, **or** use the **SmartHarvest360** launch configuration.

Use `Launcher`, not `SmartHarvestApp` directly — that avoids the JDK “JavaFX runtime components are missing” check.

## 3. Run with Maven

From the project root:

```bash
mvn -q compile exec:java -Dexec.mainClass=SmartHarvest360.Launcher
```

## 4. Demo walkthrough (all 5 screens)

1. **Farm Setup** — enter farm name, budget, water, fertilizer, land → **Next**
2. **Crop Selection** — browse `data/crops.csv`, **Add to Farm**, then **Start Simulation**
3. **Simulation** — click **Advance to Next Day** until harvest unlocks → **Continue to Market**
4. **Harvest & Market** — review best price → **Sell**
5. **Season Report** — view totals, ROI, pie chart → **New Season** (returns to Farm Setup) or **Exit**

## 5. CSV files (assignment requirement)

| File | Role |
|---|---|
| `data/crops.csv` | Input crop catalog (loaded at Crop Selection) |
| `data/harvest_log.csv` | Appended on each sale |
| `data/season_report.csv` | Written when Season Report opens |

## 6. MySQL verification (when connected)

```sql
USE smartharvest;
SELECT * FROM farms;
SELECT * FROM crops;
SELECT * FROM sales;
```

Hybrid behaviour:

- CSV always remains the source of truth for crop loading and report files
- Farms are inserted on Farm Setup **Next**
- Crops are upserted when Crop Selection loads the CSV
- Sales are written to CSV first, then best-effort to MySQL

Farm Setup shows a status chip: `CSV ready · MySQL connected` or `CSV ready · MySQL offline`.

## 7. Smoke checks

```bash
mvn -q compile
```

Optional UI smoke (loads all FXML screens):

```bash
mvn -q exec:java -Dexec.mainClass=SmartHarvest360.FxmlSmokeTest
```

## Troubleshooting

- **crops.csv not found** — run the app from the project root so `data/crops.csv` resolves correctly
- **MySQL offline** — safe; continue with CSV only
- **JavaFX missing modules** — always start via `SmartHarvest360.Launcher`
