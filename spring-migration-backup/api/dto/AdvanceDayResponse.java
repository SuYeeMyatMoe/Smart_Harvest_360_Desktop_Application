package SmartHarvest360.api.dto;

import java.util.List;

/** Result of POST /api/simulation/advance-day. */
public record AdvanceDayResponse(
        int day,
        String weatherLabel,
        String weatherIcon,
        boolean usesRealWeather,
        boolean allReady,
        List<CropProgressDto> crops,
        ResourceDto resources,
        List<String> logLines
) {
}
