package SmartHarvest360.weather;

import SmartHarvest360.Weather;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Fetches recent daily farm weather from NASA POWER's free Agroclimatology API. */
public final class NasaPowerClient {
    public record Location(double latitude, double longitude) { }
    public record DailyWeather(Weather weather, boolean frost) { }

    private static final String ENDPOINT =
            "https://power.larc.nasa.gov/api/temporal/daily/point";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private static final Map<String, Location> MALAYSIA_STATES = Map.ofEntries(
            Map.entry("Johor", new Location(1.4854, 103.7618)),
            Map.entry("Kedah", new Location(6.1184, 100.3685)),
            Map.entry("Kelantan", new Location(6.1254, 102.2381)),
            Map.entry("Kuala Lumpur", new Location(3.1390, 101.6869)),
            Map.entry("Labuan", new Location(5.2831, 115.2308)),
            Map.entry("Melaka", new Location(2.1896, 102.2501)),
            Map.entry("Negeri Sembilan", new Location(2.7258, 101.9424)),
            Map.entry("Pahang", new Location(3.8126, 103.3256)),
            Map.entry("Penang", new Location(5.4141, 100.3288)),
            Map.entry("Pulau Pinang", new Location(5.4141, 100.3288)),
            Map.entry("Perak", new Location(4.5975, 101.0901)),
            Map.entry("Perlis", new Location(6.4414, 100.1986)),
            Map.entry("Putrajaya", new Location(2.9264, 101.6964)),
            Map.entry("Sabah", new Location(5.9804, 116.0735)),
            Map.entry("Sarawak", new Location(1.5533, 110.3592)),
            Map.entry("Selangor", new Location(3.0738, 101.5183)),
            Map.entry("Terengganu", new Location(5.3117, 103.1324)),
            Map.entry("Wilayah Persekutuan", new Location(3.1390, 101.6869))
    );

    private NasaPowerClient() { }

    /** Maps the existing Farm Setup state selector to representative coordinates. */
    public static Location forMalaysiaState(String state) {
        if (state == null) return null;
        String wanted = state.trim().toLowerCase(Locale.ROOT);
        return MALAYSIA_STATES.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).equals(wanted))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Returns the most recent complete daily records, or null when the API is unavailable. */
    public static List<DailyWeather> fetchSeason(Location location, int days) {
        if (location == null || days <= 0) return null;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((long) days + 7L);
        String url = ENDPOINT
                + "?parameters=T2M_MIN,PRECTOTCORR,RH2M&community=AG"
                + "&longitude=" + location.longitude()
                + "&latitude=" + location.latitude()
                + "&start=" + start.format(DATE)
                + "&end=" + end.format(DATE)
                + "&format=CSV";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? parse(response.body(), days) : null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static List<DailyWeather> parse(String csv, int wanted) {
        int minIndex = -1;
        int rainIndex = -1;
        int humidityIndex = -1;
        boolean headerSeen = false;
        List<DailyWeather> result = new ArrayList<>();

        for (String raw : csv.split("\\r?\\n")) {
            String[] columns = raw.split(",", -1);
            if (!headerSeen && columns.length > 0 && columns[0].trim().equals("YEAR")) {
                for (int i = 2; i < columns.length; i++) {
                    switch (columns[i].trim()) {
                        case "T2M_MIN" -> minIndex = i;
                        case "PRECTOTCORR" -> rainIndex = i;
                        case "RH2M" -> humidityIndex = i;
                        default -> { }
                    }
                }
                headerSeen = true;
                continue;
            }
            int greatest = Math.max(minIndex, Math.max(rainIndex, humidityIndex));
            if (!headerSeen || minIndex < 0 || rainIndex < 0 || humidityIndex < 0
                    || columns.length <= greatest || !columns[0].trim().matches("\\d{4}")) {
                continue;
            }
            double minimum = number(columns[minIndex]);
            double rainfall = number(columns[rainIndex]);
            double humidity = number(columns[humidityIndex]);
            if (minimum == -999.0 || rainfall == -999.0 || humidity == -999.0) continue;

            Weather weather = rainfall >= 2.0 ? Weather.RAIN
                    : humidity >= 80.0 || rainfall >= 0.5 ? Weather.CLOUDY
                    : Weather.SUNNY;
            result.add(new DailyWeather(weather, minimum <= 0.0));
        }
        if (result.isEmpty()) return null;
        return new ArrayList<>(result.subList(Math.max(0, result.size() - wanted), result.size()));
    }

    private static double number(String token) {
        try {
            return Double.parseDouble(token.trim());
        } catch (NumberFormatException exception) {
            return -999.0;
        }
    }
}
