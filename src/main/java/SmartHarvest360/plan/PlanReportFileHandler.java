package SmartHarvest360.plan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * File handling for detailed plan reports (CSV).
 * Uses ASCII-safe text so Excel / Notepad do not show garbled symbols.
 */
public final class PlanReportFileHandler {
    private PlanReportFileHandler() {
    }

    public static Path saveToDataFolder(DetailedPlanReport report) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path target = dir.resolve("detailed_plan_report.csv");
        writeCsv(target, report);
        return target.toAbsolutePath().normalize();
    }

    public static Path exportTo(Path target, DetailedPlanReport report) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        writeCsv(target, report);
        return target.toAbsolutePath().normalize();
    }

    private static void writeCsv(Path target, DetailedPlanReport report) throws IOException {
        StringBuilder out = new StringBuilder();
        // BOM helps Excel open UTF-8 correctly on Windows.
        out.append('\uFEFF');
        out.append("section,key,value\n");
        out.append(row("summary", "farm", report.getFarmName()));
        out.append(row("summary", "location", report.getLocation()));
        out.append(row("summary", "soil", report.getSoil()));
        out.append(row("summary", "crop", report.getCropName()));
        out.append(row("summary", "careScore", String.valueOf(report.getCareScore())));
        out.append(row("summary", "plantGrade", report.getPlantGrade()));
        out.append(row("summary", "liveGrade", report.getLiveGrade()));
        out.append(row("summary", "fertilizerPlan", report.getFertilizerPlan()));
        out.append(row("summary", "overview", report.getSummary()));

        out.append("\n");
        out.append("stepNumber,action,weather,outcome,note\n");
        for (PlanStep step : report.getSteps()) {
            out.append(String.format(Locale.US, "%d,%s,%s,%s,%s%n",
                    step.getStepNumber(),
                    csv(step.getAction()),
                    csv(step.getWeather()),
                    csv(step.getOutcome()),
                    csv(step.getCoachingNote())));
        }

        out.append("\n");
        out.append("priority,topic,advice\n");
        for (PlanRecommendation recommendation : report.getRecommendations()) {
            out.append(String.format(Locale.US, "%s,%s,%s%n",
                    csv(recommendation.getPriority()),
                    csv(recommendation.getTopic()),
                    csv(recommendation.getAdvice())));
        }

        out.append("\n");
        out.append("itemCategory,title,detail\n");
        for (PlanItem item : report.allItems()) {
            out.append(String.format(Locale.US, "%s,%s,%s%n",
                    csv(item.getCategory()),
                    csv(item.getTitle()),
                    csv(item.getDetail())));
        }

        Files.writeString(target, out.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String row(String section, String key, String value) {
        return section + "," + key + "," + csv(value) + "\n";
    }

    private static String csv(String value) {
        String safe = asciiSafe(value);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    /** Replace fancy punctuation that breaks in Excel/ANSI viewers. */
    static String asciiSafe(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00B7', '-')   // ·
                .replace('\u2022', '-')   // •
                .replace('\u2013', '-')   // –
                .replace('\u2014', '-')   // —
                .replace('\u2018', '\'')  // ‘
                .replace('\u2019', '\'')  // ’
                .replace('\u201C', '"')   // “
                .replace('\u201D', '"')   // ”
                .replace("Â·", "-")
                .replace("â€”", "-")
                .replace("â€“", "-")
                .replace("â€™", "'");
    }
}
