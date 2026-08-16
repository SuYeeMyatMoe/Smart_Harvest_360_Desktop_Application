package SmartHarvest360.api.dto;

/** Growth progress for one planted crop. */
public record CropProgressDto(
        String cropName,
        double progress,
        int growthPercent,
        boolean ready,
        boolean sold
) {
}
