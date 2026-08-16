package SmartHarvest360.api.dto;

/**
 * Result of POST /api/session/start.
 * The API currently hosts a single shared farm, so sessionId is the constant "1".
 */
public record SessionStartResponse(
        String sessionId,
        String farmerName
) {
}
