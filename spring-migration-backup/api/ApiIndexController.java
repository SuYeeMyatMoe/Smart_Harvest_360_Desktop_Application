package SmartHarvest360.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON endpoint index served at /api. The visual landing page at "/" is the
 * static index.html in src/main/resources/static (see welcome-page handling),
 * which calls this endpoint to render the endpoint table.
 */
@RestController
public class ApiIndexController {

    @GetMapping("/api")
    public Map<String, Object> index() {
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("service", "SmartHarvest 360 API");
        index.put("version", "1.0-SNAPSHOT");
        index.put("status", "running");
        index.put("baseUrl", "http://localhost:8080/api");
        index.put("sessionId", SessionService.SESSION_ID);
        index.put("endpoints", List.of(
                Map.of("method", "POST", "path", "/api/session/start", "purpose", "Initialize the farm session (returns sessionId)"),
                Map.of("method", "POST", "path", "/api/farm/setup", "purpose", "Create the farm with starting resources"),
                Map.of("method", "GET", "path", "/api/crops/catalog", "purpose", "List the crop catalog with economics"),
                Map.of("method", "POST", "path", "/api/farm/plant", "purpose", "Plant a catalog crop (spends budget + land)"),
                Map.of("method", "POST", "path", "/api/simulation/advance-day", "purpose", "Advance the season by one day"),
                Map.of("method", "GET", "path", "/api/simulation/state", "purpose", "Current season state (day, weather, growth, log)"),
                Map.of("method", "GET", "path", "/api/market/buyers", "purpose", "Market comparison for ready crops"),
                Map.of("method", "POST", "path", "/api/market/sell", "purpose", "Sell a ready crop at the best price"),
                Map.of("method", "GET", "path", "/api/report/season", "purpose", "Season totals, ROI, chart, event counts"),
                Map.of("method", "GET", "path", "/api/report/history", "purpose", "All stored seasons, oldest first")
        ));
        return index;
    }
}
