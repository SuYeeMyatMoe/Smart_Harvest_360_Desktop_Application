package SmartHarvest360.plan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * File handling for detailed plan reports.
 * Writes a tidy multi-section CSV and an Excel workbook with separate sheets.
 * Uses ASCII-safe text so Excel / Notepad do not show garbled symbols.
 */
public final class PlanReportFileHandler {
    private PlanReportFileHandler() {
    }

    public static Path saveToDataFolder(DetailedPlanReport report) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        Path csv = dir.resolve("detailed_plan_report.csv");
        Path workbook = dir.resolve("detailed_plan_report.xls");
        writeCsv(csv, report);
        writeExcelWorkbook(workbook, report);
        return workbook.toAbsolutePath().normalize();
    }

    public static Path exportTo(Path target, DetailedPlanReport report) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        String name = target.getFileName() == null ? "" : target.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".xml")) {
            Path workbook = name.endsWith(".xlsx")
                    ? target.resolveSibling(replaceExtension(target.getFileName().toString(), ".xls"))
                    : target;
            writeExcelWorkbook(workbook, report);
            return workbook.toAbsolutePath().normalize();
        }
        writeCsv(target, report);
        return target.toAbsolutePath().normalize();
    }

    private static String replaceExtension(String fileName, String extension) {
        int dot = fileName.lastIndexOf('.');
        String base = dot < 0 ? fileName : fileName.substring(0, dot);
        return base + extension;
    }

    private static void writeCsv(Path target, DetailedPlanReport report) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append('\uFEFF');
        out.append("# SmartHarvest 360 Detailed Plan Report\n");
        out.append("# Open detailed_plan_report.xls in Excel for separate worksheets.\n");
        out.append("# Each block below is one logical sheet.\n\n");

        out.append("[SHEET] Summary\n");
        out.append("key,value\n");
        out.append(row("farm", report.getFarmName()));
        out.append(row("location", report.getLocation()));
        out.append(row("soil", report.getSoil()));
        out.append(row("crop", report.getCropName()));
        out.append(row("careScore", String.valueOf(report.getCareScore())));
        out.append(row("plantGrade", report.getPlantGrade()));
        out.append(row("liveGrade", report.getLiveGrade()));
        out.append(row("fertilizerPlan", report.getFertilizerPlan()));
        out.append(row("overview", report.getSummary()));
        out.append('\n');

        out.append("[SHEET] ActionByDayGroup\n");
        out.append("fromDay,toDay,phase,dominantAction,mainWeather,irrigate,conserve,fertilize,protect,other,waterL,fertilizerKg,growthFrom,growthTo,actionMix,summary\n");
        for (ActionDayGroup group : report.getDayGroups()) {
            out.append(String.format(Locale.US,
                    "%d,%d,%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%.2f,%d,%d,%s,%s%n",
                    group.getFromDay(),
                    group.getToDay(),
                    csv(group.getPhase()),
                    csv(group.getDominantAction()),
                    csv(group.getMainWeather()),
                    group.getIrrigateCount(),
                    group.getConserveCount(),
                    group.getFertilizeCount(),
                    group.getProtectCount(),
                    group.getOtherCount(),
                    group.getWaterUsed(),
                    group.getFertilizerUsed(),
                    group.getStartGrowth(),
                    group.getEndGrowth(),
                    csv(group.getActionMix()),
                    csv(group.getSummary())));
        }
        out.append('\n');

        out.append("[SHEET] DailySteps\n");
        out.append("stepNumber,action,weather,outcome,note\n");
        for (PlanStep step : report.getSteps()) {
            out.append(String.format(Locale.US, "%d,%s,%s,%s,%s%n",
                    step.getStepNumber(),
                    csv(step.getAction()),
                    csv(step.getWeather()),
                    csv(step.getOutcome()),
                    csv(step.getCoachingNote())));
        }
        out.append('\n');

        out.append("[SHEET] Recommendations\n");
        out.append("priority,topic,advice\n");
        for (PlanRecommendation recommendation : report.getRecommendations()) {
            out.append(String.format(Locale.US, "%s,%s,%s%n",
                    csv(recommendation.getPriority()),
                    csv(recommendation.getTopic()),
                    csv(recommendation.getAdvice())));
        }

        Files.writeString(target, out.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeExcelWorkbook(Path target, DetailedPlanReport report) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        out.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ");
        out.append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
        out.append(" <Styles>\n");
        out.append("  <Style ss:ID=\"Header\"><Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>");
        out.append("<Interior ss:Color=\"#239467\" ss:Pattern=\"Solid\"/></Style>\n");
        out.append("  <Style ss:ID=\"Title\"><Font ss:Bold=\"1\" ss:Size=\"14\" ss:Color=\"#193F33\"/></Style>\n");
        out.append(" </Styles>\n");

        startSheet(out, "Summary");
        titleRow(out, "Season summary");
        headerRow(out, "Key", "Value");
        textRow(out, "Farm", report.getFarmName());
        textRow(out, "Location", report.getLocation());
        textRow(out, "Soil", report.getSoil());
        textRow(out, "Crop", report.getCropName());
        numberRow(out, "Care score", report.getCareScore());
        textRow(out, "Plant grade", report.getPlantGrade());
        textRow(out, "Live grade", report.getLiveGrade());
        textRow(out, "Fertilizer plan", report.getFertilizerPlan());
        textRow(out, "Overview", report.getSummary());
        endSheet(out);

        startSheet(out, "ActionByDayGroup");
        titleRow(out, "Action summary by day group");
        headerRow(out,
                "From day", "To day", "Phase", "Dominant action", "Main weather",
                "Irrigate", "Conserve", "Fertilize", "Protect", "Other",
                "Water L", "Fertilizer kg", "Growth from %", "Growth to %", "Action mix", "Summary");
        for (ActionDayGroup group : report.getDayGroups()) {
            out.append("   <Row>\n");
            numberCell(out, group.getFromDay());
            numberCell(out, group.getToDay());
            stringCell(out, group.getPhase());
            stringCell(out, group.getDominantAction());
            stringCell(out, group.getMainWeather());
            numberCell(out, group.getIrrigateCount());
            numberCell(out, group.getConserveCount());
            numberCell(out, group.getFertilizeCount());
            numberCell(out, group.getProtectCount());
            numberCell(out, group.getOtherCount());
            numberCell(out, group.getWaterUsed());
            numberCell(out, group.getFertilizerUsed());
            numberCell(out, group.getStartGrowth());
            numberCell(out, group.getEndGrowth());
            stringCell(out, group.getActionMix());
            stringCell(out, group.getSummary());
            out.append("   </Row>\n");
        }
        endSheet(out);

        startSheet(out, "DailySteps");
        titleRow(out, "Daily field steps");
        headerRow(out, "Step", "Action", "Weather", "Outcome", "Coaching note");
        for (PlanStep step : report.getSteps()) {
            out.append("   <Row>\n");
            numberCell(out, step.getStepNumber());
            stringCell(out, step.getAction());
            stringCell(out, step.getWeather());
            stringCell(out, step.getOutcome());
            stringCell(out, step.getCoachingNote());
            out.append("   </Row>\n");
        }
        endSheet(out);

        startSheet(out, "Recommendations");
        titleRow(out, "Plan recommendations");
        headerRow(out, "Priority", "Topic", "Advice");
        for (PlanRecommendation recommendation : report.getRecommendations()) {
            out.append("   <Row>\n");
            stringCell(out, recommendation.getPriority());
            stringCell(out, recommendation.getTopic());
            stringCell(out, recommendation.getAdvice());
            out.append("   </Row>\n");
        }
        endSheet(out);

        out.append("</Workbook>\n");
        Files.writeString(target, out.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void startSheet(StringBuilder out, String name) {
        out.append(" <Worksheet ss:Name=\"").append(xml(name)).append("\">\n");
        out.append("  <Table>\n");
    }

    private static void endSheet(StringBuilder out) {
        out.append("  </Table>\n");
        out.append("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">");
        out.append("<FreezePanes/><FrozenNoSplit/><SplitHorizontal>2</SplitHorizontal>");
        out.append("<TopRowBottomPane>2</TopRowBottomPane></WorksheetOptions>\n");
        out.append(" </Worksheet>\n");
    }

    private static void titleRow(StringBuilder out, String title) {
        out.append("   <Row><Cell ss:StyleID=\"Title\" ss:MergeAcross=\"4\"><Data ss:Type=\"String\">");
        out.append(xml(title));
        out.append("</Data></Cell></Row>\n");
    }

    private static void headerRow(StringBuilder out, String... headers) {
        out.append("   <Row>\n");
        for (String header : headers) {
            out.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">");
            out.append(xml(header));
            out.append("</Data></Cell>\n");
        }
        out.append("   </Row>\n");
    }

    private static void textRow(StringBuilder out, String key, String value) {
        out.append("   <Row>");
        stringCell(out, key);
        stringCell(out, value);
        out.append("</Row>\n");
    }

    private static void numberRow(StringBuilder out, String key, double value) {
        out.append("   <Row>");
        stringCell(out, key);
        numberCell(out, value);
        out.append("</Row>\n");
    }

    private static void stringCell(StringBuilder out, String value) {
        out.append("<Cell><Data ss:Type=\"String\">").append(xml(asciiSafe(value))).append("</Data></Cell>");
    }

    private static void numberCell(StringBuilder out, int value) {
        out.append("<Cell><Data ss:Type=\"Number\">").append(value).append("</Data></Cell>");
    }

    private static void numberCell(StringBuilder out, double value) {
        out.append("<Cell><Data ss:Type=\"Number\">")
                .append(String.format(Locale.US, "%.4f", value))
                .append("</Data></Cell>");
    }

    private static String xml(String value) {
        String safe = asciiSafe(value);
        return safe.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String row(String key, String value) {
        return csv(key) + "," + csv(value) + "\n";
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
                .replace('\u00B7', '-')
                .replace('\u2022', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace("Â·", "-")
                .replace("â€”", "-")
                .replace("â€“", "-")
                .replace("â€™", "'");
    }
}
