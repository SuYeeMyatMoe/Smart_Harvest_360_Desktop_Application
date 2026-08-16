package SmartHarvest360.api;

import SmartHarvest360.CSVFileHandler;
import SmartHarvest360.Crop;
import SmartHarvest360.Farm;
import SmartHarvest360.Market;
import SmartHarvest360.RandomEvent;
import SmartHarvest360.Resource;
import SmartHarvest360.SeasonSimulator;
import SmartHarvest360.SimulationEngine;
import SmartHarvest360.Weather;
import SmartHarvest360.api.dto.AdvanceDayResponse;
import SmartHarvest360.api.dto.BuyersResponse;
import SmartHarvest360.api.dto.CatalogResponse;
import SmartHarvest360.api.dto.ChartPointDto;
import SmartHarvest360.api.dto.CropDto;
import SmartHarvest360.api.dto.CropProgressDto;
import SmartHarvest360.api.dto.FarmSetupRequest;
import SmartHarvest360.api.dto.FarmSetupResponse;
import SmartHarvest360.api.dto.HistoryResponse;
import SmartHarvest360.api.dto.MarketBuyerDto;
import SmartHarvest360.api.dto.PlantRequest;
import SmartHarvest360.api.dto.PlantResponse;
import SmartHarvest360.api.dto.PricePointDto;
import SmartHarvest360.api.dto.ReadyCropMarketDto;
import SmartHarvest360.api.dto.ResourceDto;
import SmartHarvest360.api.dto.SaleDto;
import SmartHarvest360.api.dto.SeasonHistoryDto;
import SmartHarvest360.api.dto.SeasonReportResponse;
import SmartHarvest360.api.dto.SellRequest;
import SmartHarvest360.api.dto.SellResponse;
import SmartHarvest360.api.dto.SessionStartResponse;
import SmartHarvest360.api.dto.SimulationStateResponse;
import SmartHarvest360.data.CsvDataStore;
import SmartHarvest360.data.DataPaths;
import SmartHarvest360.db.CropRepository;
import SmartHarvest360.db.FarmRepository;
import SmartHarvest360.db.SaleRepository;
import SmartHarvest360.model.SaleRecord;
import SmartHarvest360.model.SeasonHistory;
import SmartHarvest360.session.AppSession;
import SmartHarvest360.weather.NasaPowerClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wraps the existing (unchanged) {@link AppSession} singleton and the original
 * simulation/data logic for the REST layer. Implements the decision from the
 * migration plan: the API hosts a single shared farm for now, so every request
 * operates on {@link AppSession#getInstance()} and the session id is the
 * constant "1".
 *
 * <p>Unlike the desktop flow, this service never triggers the demo-data
 * fallback ({@code AppSession.ensureDemoData()}); a request on an unset farm
 * fails with a clear 400 instead of silently creating a demo farm.
 */
@Service
public class SessionService {

    /** Fixed session identifier while the API hosts a single shared farm. */
    public static final String SESSION_ID = "1";

    private final AppSession session = AppSession.getInstance();
    private final CSVFileHandler csvFileHandler = new CSVFileHandler();

    private List<Crop> catalog;
    private Weather lastWeather;
    private boolean seasonRecorded;

    /* ------------------------------------------------------------------ */
    /* Session                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * The shared singleton is a single logical actor, so every service method is
     * synchronized to keep concurrent HTTP requests (e.g. a React auto-run loop)
     * from interleaving day advances, resource consumption, and CSV writes.
     */
    public synchronized SessionStartResponse startSession(String farmerName) {
        if (farmerName != null && !farmerName.isBlank()) {
            session.setFarmerName(farmerName.trim());
        }
        seasonRecorded = false;
        return new SessionStartResponse(SESSION_ID, session.getFarmerName());
    }

    /* ------------------------------------------------------------------ */
    /* Farm setup (wraps FarmSetupController's setup logic)                */
    /* ------------------------------------------------------------------ */

    public synchronized FarmSetupResponse setupFarm(FarmSetupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        String farmName = request.farmName() == null ? "" : request.farmName().trim();
        if (farmName.isEmpty()) {
            throw new IllegalArgumentException("Farm name is required.");
        }
        double budget = positive(request.budget(), "Budget must be greater than zero.");
        double water = positive(request.water(), "Water must be greater than zero.");
        double fertilizer = positive(request.fertilizer(), "Fertilizer must be greater than zero.");
        double land = positive(request.land(), "Land must be greater than zero.");
        NasaPowerClient.Location location = parseLocation(request.latitude(), request.longitude());

        Resource resource = new Resource(water, fertilizer, budget, land);
        Farm farm = new Farm(resource);
        session.prepareFarm(farmName, farm);
        session.setLocation(location);
        session.setSeasonGoal(request.seasonGoal());
        seasonRecorded = false;
        lastWeather = null;

        Optional<Long> farmId = FarmRepository.insert(farmName, farm);
        farmId.ifPresent(session::setFarmId);

        String status = "Farm ready." + (location != null
                ? " Real NASA POWER weather enabled for the season."
                : " Using generated weather.");
        return new FarmSetupResponse(farmId.orElse(null), farmName, ResourceDto.from(resource), status);
    }

    private static double positive(Double value, String message) {
        if (value == null || value <= 0.0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static NasaPowerClient.Location parseLocation(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return null;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Provide both latitude and longitude to enable live weather.");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
        return new NasaPowerClient.Location(latitude, longitude);
    }

    /* ------------------------------------------------------------------ */
    /* Crop catalog (wraps CropRepository / CSVFileHandler loading)        */
    /* ------------------------------------------------------------------ */

    public synchronized CatalogResponse catalog() {
        List<Crop> crops = loadCatalog();
        CropRepository.upsertAll(crops);
        String best = bestRoiCrop(crops, session.getFarm());
        List<CropDto> dtos = crops.stream()
                .map(crop -> CropDto.from(crop, crop.getName().equals(best)))
                .toList();
        return CatalogResponse.of(dtos, best);
    }

    private List<Crop> loadCatalog() {
        if (catalog == null) {
            try {
                catalog = csvFileHandler.loadCrops(DataPaths.cropsFile().toString());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load crops.csv: " + exception.getMessage());
            }
        }
        return catalog;
    }

    /** Best-ROI recommendation, mirroring CropSelectionController.computeBestCrop(). */
    private static String bestRoiCrop(List<Crop> crops, Farm farm) {
        double budget = farm == null ? 0.0 : farm.getResource().getBudget();
        String best = null;
        double bestRatio = Double.NEGATIVE_INFINITY;
        for (Crop crop : crops) {
            double cost = crop.getPlantingCost();
            if (cost <= 0.0 || cost > budget) {
                continue;
            }
            double ratio = CropDto.expectedProfit(crop) / cost;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = crop.getName();
            }
        }
        return bestRatio <= 0.0 ? null : best;
    }

    /* ------------------------------------------------------------------ */
    /* Planting (wraps CropSelectionController's plantCrop flow)           */
    /* ------------------------------------------------------------------ */

    public synchronized PlantResponse plant(PlantRequest request) {
        if (request == null || request.cropName() == null || request.cropName().isBlank()) {
            throw new IllegalArgumentException("cropName is required.");
        }
        if (session.getSeason() != null) {
            throw new IllegalArgumentException("The season has already started — plant crops before the simulation begins.");
        }
        Farm farm = requireFarm();
        Crop selected = loadCatalog().stream()
                .filter(crop -> crop.getName().equalsIgnoreCase(request.cropName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown crop: " + request.cropName()));

        boolean alreadyPlanted = farm.getCrops().stream()
                .anyMatch(crop -> crop.getName().equalsIgnoreCase(request.cropName()));
        if (alreadyPlanted) {
            throw new IllegalArgumentException(request.cropName() + " is already on the farm.");
        }

        boolean planted = farm.plantCrop(selected);
        return new PlantResponse(
                planted,
                planted ? "Crop added to the farm."
                        : "Not enough budget or land to plant " + selected.getName() + ".",
                selected.getName(),
                ResourceDto.from(farm.getResource()),
                plantedCropNameList(farm)
        );
    }

    /* ------------------------------------------------------------------ */
    /* Simulation (wraps SimulationEngine / SeasonSimulator)               */
    /* ------------------------------------------------------------------ */

    public synchronized AdvanceDayResponse advanceDay() {
        SeasonSimulator season = requireSeason();
        if (session.isCropReady()) {
            return buildAdvanceResponse(season);
        }
        SeasonSimulator.SeasonDayResult result = season.advanceDay();
        lastWeather = result.weather();
        return buildAdvanceResponse(season);
    }

    public synchronized SimulationStateResponse state() {
        SeasonSimulator season = session.getSeason();
        if (season == null) {
            return new SimulationStateResponse(false, null, null, null,
                    false, false, List.of(), resourcesOrNull(), List.of());
        }
        return new SimulationStateResponse(true, season.getCurrentDay(),
                weatherLabel(), weatherIcon(), season.usesRealWeather(), season.allReady(),
                cropProgresses(season), resourcesOrNull(), session.getSimulationLog());
    }

    private AdvanceDayResponse buildAdvanceResponse(SeasonSimulator season) {
        return new AdvanceDayResponse(season.getCurrentDay(),
                weatherLabel(), weatherIcon(), season.usesRealWeather(), season.allReady(),
                cropProgresses(season), resourcesOrNull(), session.getSimulationLog());
    }

    /**
     * Starts the season exactly like CropSelectionController's "start simulation"
     * action when it has not been started yet. Never falls back to demo data.
     */
    private void ensureSeasonStarted() {
        if (session.getSeason() != null) {
            return;
        }
        Farm farm = session.getFarm();
        if (farm == null || farm.getCrops().isEmpty()) {
            return;
        }
        session.startSimulation(farm, farm.getCrops().get(0));
    }

    private SeasonSimulator requireSeason() {
        ensureSeasonStarted();
        if (session.getSeason() == null) {
            throw new IllegalArgumentException("Farm not set up yet. Call POST /api/farm/setup and plant a crop first.");
        }
        return session.getSeason();
    }

    private List<CropProgressDto> cropProgresses(SeasonSimulator season) {
        return season.getEngines().stream()
                .map(engine -> new CropProgressDto(
                        engine.getCropName(),
                        engine.getGrowthProgress(),
                        (int) Math.round(engine.getGrowthProgress() * 100),
                        engine.isReady(),
                        season.isSold(engine.getCropName())))
                .toList();
    }

    private String weatherLabel() {
        return lastWeather == null ? null : lastWeather.getLabel();
    }

    private String weatherIcon() {
        return lastWeather == null ? null : lastWeather.getIcon();
    }

    /* ------------------------------------------------------------------ */
    /* Market (wraps HarvestMarketController's buyer comparison & selling) */
    /* ------------------------------------------------------------------ */

    public synchronized BuyersResponse buyers() {
        SeasonSimulator season = session.getSeason();
        if (season == null) {
            return new BuyersResponse(List.of());
        }
        List<ReadyCropMarketDto> result = new ArrayList<>();
        for (SimulationEngine engine : season.getUnsoldReadyCrops()) {
            Crop crop = engine.getCrop();
            Map<String, Double> prices = currentPrices(season, crop);
            String best = bestMarket(prices);
            double bestPrice = prices.get(best);
            List<MarketBuyerDto> buyers = prices.entrySet().stream()
                    .map(entry -> new MarketBuyerDto(entry.getKey(), entry.getValue(),
                            entry.getKey().equals(best)))
                    .toList();
            result.add(new ReadyCropMarketDto(crop.getName(), crop.getYieldAmount(), best, bestPrice,
                    buyers, priceHistory(season, crop.getName())));
        }
        return new BuyersResponse(result);
    }

    public synchronized SellResponse sell(SellRequest request) {
        if (request == null || request.cropName() == null || request.cropName().isBlank()) {
            throw new IllegalArgumentException("cropName is required.");
        }
        SeasonSimulator season = requireSeason();
        Farm farm = requireFarm();
        Crop crop = farm.getCrops().stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.cropName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown crop: " + request.cropName()));

        SimulationEngine engine = season.findEngine(crop);
        if (engine == null || !engine.isReady()) {
            throw new IllegalArgumentException(request.cropName() + " is not ready to sell yet.");
        }
        if (season.isSold(request.cropName())) {
            throw new IllegalArgumentException(request.cropName() + " has already been sold.");
        }

        double quantity = request.quantity() == null ? crop.getYieldAmount() : request.quantity();
        if (quantity <= 0.0 || quantity > crop.getYieldAmount()) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0 and at most " + crop.getYieldAmount() + " kg.");
        }

        Map<String, Double> prices = currentPrices(season, crop);
        String best = bestMarket(prices);
        double bestPrice = prices.get(best);
        double revenue = quantity * bestPrice;
        double cost = quantity * crop.getCostPerKg();
        SaleRecord sale = new SaleRecord(session.getCurrentDay(), crop.getName(), quantity,
                best, bestPrice, revenue, cost);

        try {
            CsvDataStore.appendHarvest(sale);
        } catch (IOException exception) {
            throw new IllegalStateException("The sale could not be saved: " + exception.getMessage());
        }
        session.addSale(sale);
        SaleRepository.insert(session.getFarmId(), sale);
        session.markSold(crop.getName());

        List<String> remaining = season.getUnsoldReadyCrops().stream()
                .map(SimulationEngine::getCropName)
                .toList();
        return new SellResponse(SaleDto.from(sale), remaining, remaining.isEmpty());
    }

    private static Map<String, Double> currentPrices(SeasonSimulator season, Crop crop) {
        Map<String, Double> prices = season.getCurrentPrices(crop.getName());
        return prices != null ? prices : new Market().getMarketPrices(crop);
    }

    private static String bestMarket(Map<String, Double> prices) {
        return prices.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Local Market");
    }

    private static List<PricePointDto> priceHistory(SeasonSimulator season, String cropName) {
        List<PricePointDto> history = new ArrayList<>();
        for (SeasonSimulator.DaySnapshot snapshot : season.getPriceHistory()) {
            Map<String, Double> prices = snapshot.pricesByCrop().get(cropName);
            if (prices != null) {
                history.add(new PricePointDto(snapshot.day(), prices));
            }
        }
        return history;
    }

    /* ------------------------------------------------------------------ */
    /* Season report (wraps SeasonReportController's aggregation)          */
    /* ------------------------------------------------------------------ */

    public synchronized SeasonReportResponse report() {
        List<SaleRecord> sales = session.getSales();
        double revenue = sales.stream().mapToDouble(SaleRecord::revenue).sum();
        double cost = sales.stream().mapToDouble(SaleRecord::cost).sum();
        double profit = revenue - cost;
        double roi = cost == 0.0 ? 0.0 : profit / cost * 100.0;

        List<SeasonHistory> past = loadHistory();
        double pastAverageRoi = past.isEmpty() ? 0.0
                : past.stream().mapToDouble(SeasonHistory::roi).average().orElse(0.0);
        Double lastSeasonRoi = past.isEmpty() ? null : past.get(past.size() - 1).roi();

        try {
            CsvDataStore.saveSeasonReport(sales);
            if (!sales.isEmpty() && !seasonRecorded) {
                CsvDataStore.appendSeasonHistory(buildSeasonHistory(sales, revenue, cost, profit, roi));
                seasonRecorded = true;
            }
        } catch (IOException exception) {
            // The report is still returned; only CSV persistence failed.
        }

        SeasonSimulator season = session.getSeason();
        Map<RandomEvent, Long> eventCounts = season == null
                ? Map.of()
                : season.getEvents().stream()
                        .collect(Collectors.groupingBy(SeasonSimulator.SeasonEvent::event, Collectors.counting()));
        Resource resource = session.getFarm() == null ? null : session.getFarm().getResource();

        Map<String, Double> revenueByCrop = new LinkedHashMap<>();
        for (SaleRecord sale : sales) {
            if (sale.revenue() > 0.0) {
                revenueByCrop.merge(sale.cropName(), sale.revenue(), Double::sum);
            }
        }
        List<ChartPointDto> chart = revenueByCrop.entrySet().stream()
                .map(entry -> new ChartPointDto(entry.getKey(), entry.getValue()))
                .toList();

        return new SeasonReportResponse(
                session.getSeasonGoal().name(),
                revenue, cost, profit, roi,
                sales.stream().mapToDouble(SaleRecord::quantity).sum(),
                season == null ? 0 : season.getWaterShortageDays(),
                eventCounts.getOrDefault(RandomEvent.PEST, 0L),
                eventCounts.getOrDefault(RandomEvent.DROUGHT, 0L),
                eventCounts.getOrDefault(RandomEvent.FROST, 0L),
                resource == null ? 0.0 : resource.getWater(),
                resource == null ? 0.0 : resource.getFertilizer(),
                resource == null ? 0.0 : resource.getBudget(),
                plantedCropsString(),
                chart,
                past.size(), pastAverageRoi, lastSeasonRoi, seasonRecorded
        );
    }

    public synchronized HistoryResponse history() {
        List<SeasonHistoryDto> seasons = new ArrayList<>();
        int number = 1;
        for (SeasonHistory history : loadHistory()) {
            seasons.add(SeasonHistoryDto.from(history, number++));
        }
        return new HistoryResponse(seasons);
    }

    /** Mirrors SeasonReportController.buildSeasonHistory(). */
    private SeasonHistory buildSeasonHistory(List<SaleRecord> sales, double revenue,
                                             double cost, double profit, double roi) {
        SeasonSimulator season = session.getSeason();
        Map<RandomEvent, Long> eventCounts = season == null
                ? Map.of()
                : season.getEvents().stream()
                        .collect(Collectors.groupingBy(SeasonSimulator.SeasonEvent::event, Collectors.counting()));

        double endingWater = 0.0;
        double endingFertilizer = 0.0;
        double endingBudget = 0.0;
        if (session.getFarm() != null) {
            Resource resource = session.getFarm().getResource();
            endingWater = resource.getWater();
            endingFertilizer = resource.getFertilizer();
            endingBudget = resource.getBudget();
        }

        return new SeasonHistory(
                session.getSeasonGoal().name(),
                revenue, cost, profit, roi,
                sales.stream().mapToDouble(SaleRecord::quantity).sum(),
                season == null ? 0 : season.getWaterShortageDays(),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.PEST, 0L)),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.DROUGHT, 0L)),
                Math.toIntExact(eventCounts.getOrDefault(RandomEvent.FROST, 0L)),
                endingWater, endingFertilizer, endingBudget,
                plantedCropsString()
        );
    }

    private static List<SeasonHistory> loadHistory() {
        try {
            return CsvDataStore.loadSeasonHistory();
        } catch (IOException exception) {
            return new ArrayList<>();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private Farm requireFarm() {
        Farm farm = session.getFarm();
        if (farm == null) {
            throw new IllegalArgumentException("Farm not set up yet. Call POST /api/farm/setup first.");
        }
        return farm;
    }

    private ResourceDto resourcesOrNull() {
        Farm farm = session.getFarm();
        return farm == null ? null : ResourceDto.from(farm.getResource());
    }

    private List<String> plantedCropNameList(Farm farm) {
        return farm.getCrops().stream().map(Crop::getName).toList();
    }

    private String plantedCropsString() {
        Farm farm = session.getFarm();
        if (farm == null) {
            return "";
        }
        return farm.getCrops().stream().map(Crop::getName).collect(Collectors.joining(", "));
    }
}
