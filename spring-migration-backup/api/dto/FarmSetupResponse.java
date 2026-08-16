package SmartHarvest360.api.dto;

/** Result of POST /api/farm/setup. farmId is null when MySQL is offline. */
public record FarmSetupResponse(
        Long farmId,
        String farmName,
        ResourceDto resources,
        String status
) {
}
