package SmartHarvest360.ml;

/**
 * Malaysia farm context collected on Farm Setup (state + soil).
 */
public final class FarmProfile {
    public static final String[] MALAYSIA_STATES = {
            "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang",
            "Perak", "Perlis", "Pulau Pinang", "Sabah", "Sarawak", "Selangor",
            "Terengganu", "Wilayah Persekutuan"
    };

    public static final String[] SOIL_TYPES = {
            "Clay", "Loam", "Sandy", "Silty"
    };

    private final String location;
    private final String soilType;

    public FarmProfile(String location, String soilType) {
        this.location = location == null || location.isBlank()
                ? "Selangor" : location.trim();
        this.soilType = soilType == null || soilType.isBlank()
                ? "Loam" : soilType.trim();
    }

    public String getLocation() {
        return location;
    }

    public String getSoilType() {
        return soilType;
    }

    /** Soft rainfall / climate proxy (0–2) used as Kaggle-style npkBand feature. */
    public int npkBand() {
        return switch (location) {
            case "Perlis" -> 0;
            case "Kedah", "Melaka", "Negeri Sembilan", "Pulau Pinang",
                    "Selangor", "Wilayah Persekutuan" -> 1;
            default -> 2;
        };
    }
}
