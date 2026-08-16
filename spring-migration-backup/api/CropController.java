package SmartHarvest360.api;

import SmartHarvest360.api.dto.CatalogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Crop catalog endpoint (wraps CropRepository / CSVFileHandler crop loading). */
@RestController
@RequestMapping("/api/crops")
public class CropController {

    private final SessionService service;

    public CropController(SessionService service) {
        this.service = service;
    }

    /** Returns the full crop catalog with economics and the best-ROI recommendation. */
    @GetMapping("/catalog")
    public CatalogResponse catalog() {
        return service.catalog();
    }
}
