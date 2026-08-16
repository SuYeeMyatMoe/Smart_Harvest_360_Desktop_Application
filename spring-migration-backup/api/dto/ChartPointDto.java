package SmartHarvest360.api.dto;

/** One bar of the season report's revenue-by-crop chart. */
public record ChartPointDto(String cropName, double revenue) {
}
