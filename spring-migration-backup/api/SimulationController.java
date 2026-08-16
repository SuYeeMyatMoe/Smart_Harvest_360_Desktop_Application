package SmartHarvest360.api;

import SmartHarvest360.api.dto.AdvanceDayResponse;
import SmartHarvest360.api.dto.SimulationStateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Simulation endpoints (wrap SimulationEngine's day-tick logic). */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SessionService service;

    public SimulationController(SessionService service) {
        this.service = service;
    }

    /** Advances the whole season by one simulated day. */
    @PostMapping("/advance-day")
    public AdvanceDayResponse advanceDay() {
        return service.advanceDay();
    }

    /** Current day, weather, per-crop growth, resources, and activity log. */
    @GetMapping("/state")
    public SimulationStateResponse state() {
        return service.state();
    }
}
