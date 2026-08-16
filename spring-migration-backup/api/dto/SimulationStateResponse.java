package SmartHarvest360.api.dto;

import java.util.List;

/** Current season state, without advancing the simulation. */
public record SimulationStateResponse(
        boolean seasonStarted,
        Integer day,
        String weatherLabel,
        String weatherIcon,
        boolean usesRealWeather,
        boolean allReady,
        List<CropProgressDto> crops,
        ResourceDto resources,
        List<String> log
) {
}
