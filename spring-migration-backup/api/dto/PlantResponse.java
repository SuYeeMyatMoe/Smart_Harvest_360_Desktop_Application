package SmartHarvest360.api.dto;

import java.util.List;

/** Result of POST /api/farm/plant. */
public record PlantResponse(
        boolean planted,
        String message,
        String cropName,
        ResourceDto resources,
        List<String> plantedCrops
) {
}
