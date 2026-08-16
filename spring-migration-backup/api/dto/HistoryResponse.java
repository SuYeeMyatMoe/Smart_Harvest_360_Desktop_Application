package SmartHarvest360.api.dto;

import java.util.List;

/** All stored seasons, oldest first. */
public record HistoryResponse(List<SeasonHistoryDto> seasons) {
}
