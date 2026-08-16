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

/**
 * Fetches recent daily weather for a farm location from the NASA POWER
 * Agroclimatology API. NASA POWER publishes historical data, so the most
 * recent complete days are used to drive the simulation. Rain and humidity
 * readings are mapped onto the simulation's Weather enum, and the daily
 * minimum temperature drives frost events. Any failure returns null so the
 * simulation can fall back to generated weather.
 */
public final class NasaPowerClient {

    public record Location(double latitude, double longitude) {}

    public record DailyWeather(Weather weather, boolean frost) {}

    /** Common city presets so the farmer can pick a location instead of typing coordinates. */
    public record CityPreset(String name, double latitude, double longitude) {}

    public static final List<CityPreset> CITIES = List.of(
            new CityPreset("Kuala Lumpur, Malaysia", 3.1390, 101.6869),
            new CityPreset("Singapore", 1.3521, 103.8198),
            new CityPreset("Jakarta, Indonesia", -6.2088, 106.8456),
            new CityPreset("Bangkok, Thailand", 13.7563, 100.5018),
            new CityPreset("Manila, Philippines", 14.5995, 120.9842),
            new CityPreset("Ho Chi Minh City, Vietnam", 10.8231, 106.6297),
            new CityPreset("New Delhi, India", 28.6139, 77.2090),
            new CityPreset("Dubai, UAE", 25.2048, 55.2708),
            new CityPreset("London, United Kingdom", 51.5074, -0.1278),
            new CityPreset("Paris, France", 48.8566, 2.3522),
            new CityPreset("Berlin, Germany", 52.5200, 13.4050),
            new CityPreset("New York, USA", 40.7128, -74.0060),
            new CityPreset("Los Angeles, USA", 34.0522, -118.2437),
            new CityPreset("Iowa, USA", 42.0308, -93.6319),
            new CityPreset("Fairbanks, USA (cold)", 64.8378, -147.7164)
    );

    private static final String ENDPOINT = "https://power.larc.nasa.gov/api/temporal/daily/point";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private NasaPowerClient() {
    }

    /** Fetches the {@code days} most recent complete daily weather records, or null if unavailable. */
    public static List<DailyWeather> fetchSeason(Location location, int days) {
        if (location == null || days <= 0) {
            return null;
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((long) days + 7L);
        String url = ENDPOINT
                + "?parameters=T2M_MIN,PRECTOTCORR,RH2M"
                + "&community=AG"
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
            if (response.statusCode() != 200) {
                return null;
            }
            return parse(response.body(), days);
        } catch (IOException | InterruptedException exception) {
            return null;
        }
    }

    private static List<DailyWeather> parse(String csv, int wanted) {
        int minIdx = -1;
        int precipIdx = -1;
        int humIdx = -1;
        boolean headerSeen = false;

        List<DailyWeather> collected = new ArrayList<>();
        for (String raw : csv.split("\\r?\\n")) {
            String[] cols = raw.split(",", -1);

            if (!headerSeen && cols.length > 0 && cols[0].trim().equals("YEAR")) {
                for (int i = 2; i < cols.length; i++) {
                    switch (cols[i].trim()) {
                        case "T2M_MIN" -> minIdx = i;
                        case "PRECTOTCORR" -> precipIdx = i;
                        case "RH2M" -> humIdx = i;
                        default -> { }
                    }
                }
                headerSeen = true;
                continue;
            }
            if (!headerSeen || minIdx < 0 || precipIdx < 0 || humIdx < 0) {
                continue;
            }
            if (cols.length < 2 || !cols[0].trim().matches("\\d{4}")) {
                continue;
            }
            if (cols.length <= Math.max(minIdx, Math.max(precipIdx, humIdx))) {
                continue;
            }

            double tempMin = value(cols[minIdx]);
            double precip = value(cols[precipIdx]);
            double humidity = value(cols[humIdx]);
            if (tempMin == -999.0 || precip == -999.0 || humidity == -999.0) {
                continue;
            }

            Weather weather;
            if (precip >= 2.0) {
                weather = Weather.RAIN;
            } else if (humidity >= 80.0 || precip >= 0.5) {
                weather = Weather.CLOUDY;
            } else {
                weather = Weather.SUNNY;
            }
            collected.add(new DailyWeather(weather, tempMin <= 0.0));
        }

        if (collected.isEmpty()) {
            return null;
        }
        int from = Math.max(0, collected.size() - wanted);
        return new ArrayList<>(collected.subList(from, collected.size()));
    }

    private static double value(String token) {
        try {
            return Double.parseDouble(token.trim());
        } catch (NumberFormatException exception) {
            return -999.0;
        }
    }
}
