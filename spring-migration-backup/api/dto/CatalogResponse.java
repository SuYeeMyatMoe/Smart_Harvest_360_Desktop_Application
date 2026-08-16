package SmartHarvest360.api.dto;

import java.util.List;

/** The full crop catalog plus the recommended best-ROI crop name (may be null). */
public record CatalogResponse(
        List<CropDto> crops,
        String bestCropName,
        int count
) {
    public static CatalogResponse of(List<CropDto> crops, String bestCropName) {
        return new CatalogResponse(crops, bestCropName, crops.size());
    }
}
