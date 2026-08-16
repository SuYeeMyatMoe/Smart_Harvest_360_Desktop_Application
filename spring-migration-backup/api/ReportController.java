package SmartHarvest360.api;

import SmartHarvest360.api.dto.HistoryResponse;
import SmartHarvest360.api.dto.SeasonReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Season report endpoints (wrap SeasonReportController's aggregation). */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final SessionService service;

    public ReportController(SessionService service) {
        this.service = service;
    }

    /** Structured totals, ROI, chart data, event counts, and past-season comparison. */
    @GetMapping("/season")
    public SeasonReportResponse season() {
        return service.report();
    }

    /** All stored seasons, oldest first. */
    @GetMapping("/history")
    public HistoryResponse history() {
        return service.history();
    }
}
