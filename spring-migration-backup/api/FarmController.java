package SmartHarvest360.api;

import SmartHarvest360.api.dto.FarmSetupRequest;
import SmartHarvest360.api.dto.FarmSetupResponse;
import SmartHarvest360.api.dto.PlantRequest;
import SmartHarvest360.api.dto.PlantResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Farm setup and planting endpoints (wraps FarmSetupController/CropSelectionController logic). */
@RestController
@RequestMapping("/api/farm")
public class FarmController {

    private final SessionService service;

    public FarmController(SessionService service) {
        this.service = service;
    }

    /** Creates the farm with the given starting resources (and optional live-weather location). */
    @PostMapping("/setup")
    public FarmSetupResponse setup(@RequestBody FarmSetupRequest request) {
        return service.setupFarm(request);
    }

    /** Plants a catalog crop, spending budget and land. */
    @PostMapping("/plant")
    public PlantResponse plant(@RequestBody PlantRequest request) {
        return service.plant(request);
    }
}
