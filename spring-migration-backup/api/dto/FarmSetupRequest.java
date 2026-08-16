package SmartHarvest360.api.dto;

import SmartHarvest360.SeasonGoal;

/**
 * Farm setup payload, mirroring the FarmSetup screen inputs.
 * latitude/longitude are optional; provide both to enable live NASA POWER weather.
 */
public record FarmSetupRequest(
        String farmName,
        Double budget,
        Double water,
        Double fertilizer,
        Double land,
        Double latitude,
        Double longitude,
        SeasonGoal seasonGoal
) {
}
