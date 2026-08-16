package SmartHarvest360.api.dto;

import SmartHarvest360.Resource;

/** Snapshot of the farm's water/fertilizer/budget/land levels. */
public record ResourceDto(
        double water,
        double fertilizer,
        double budget,
        double land
) {
    public static ResourceDto from(Resource resource) {
        return new ResourceDto(
                resource.getWater(),
                resource.getFertilizer(),
                resource.getBudget(),
                resource.getLand()
        );
    }
}
