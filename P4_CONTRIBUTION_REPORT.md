# P4 Contribution Report — SmartHarvest 360

**Role:** Person 4 (Frontend A — Setup & Onboarding)  
**Focus:** Farm Setup, Crop Selection, app launch flow, CSV crop loading, hybrid MySQL

---

## What I Did

### 1. Farm Setup screen
- Built `FarmSetupScreen.fxml` and `FarmSetupController.java`
- Collects farm name, budget, water, fertilizer, and land
- Creates `Farm` + `Resource`, stores them in `AppSession`, then navigates to Crop Selection

### 2. Crop Selection screen
- Built `CropSelectionScreen.fxml` and `CropSelectionController.java`
- Loads the crop catalog from `data/crops.csv`
- Shows growth bonus and expected profit (uses `VegetableCrop` / `FruitCrop`)
- **Add to Farm** and **Start Simulation** wired to `Farm` and `AppSession`

### 3. App flow & navigation
- App now starts on Farm Setup (not Simulation)
- Connected Setup → Crops → Simulation → Market → Report
- **New Season** returns to Farm Setup with a clean session
- Updated the step indicator to the full 5-screen flow

### 4. CSV + MySQL (hybrid)
- Added `data/crops.csv` and `CSVFileHandler.loadCrops()`
- Kept CSV for crops in / harvest log / season report (assignment requirement)
- Added optional MySQL layer for farms, crops, and sales (app still runs if MySQL is offline)

### 5. Docs & launch
- Updated `README.md` and wrote `report.md` (how to run)
- Fixed VS Code launch config to use `SmartHarvest360.Launcher`
- Updated smoke / layout tests for the new screens

---

## Files I Own / Added

| Area | Files |
|---|---|
| Screens | `FarmSetupScreen.fxml`, `FarmSetupController.java`, `CropSelectionScreen.fxml`, `CropSelectionController.java` |
| Data | `data/crops.csv`, `CSVFileHandler.loadCrops()` |
| DB | `db.properties`, `SmartHarvest360.db.*` |
| Flow | `SmartHarvestApp.java`, `AppSession.java` (farm name / new season), step bars on later screens |
| Docs | `report.md`, this contribution report |

---

## Demo Path

Farm Setup → Crop Selection → Simulation → Harvest & Market → Season Report
