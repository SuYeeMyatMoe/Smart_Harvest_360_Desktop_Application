package SmartHarvest360.plan;

/**
 * Forward-looking recommendation to improve the next season grade.
 * OOP: Encapsulation + Polymorphism via {@link PlanItem}.
 */
public final class PlanRecommendation implements PlanItem {
    private final String priority;
    private final String topic;
    private final String advice;

    public PlanRecommendation(String priority, String topic, String advice) {
        this.priority = priority == null ? "Medium" : priority;
        this.topic = topic == null ? "General" : topic;
        this.advice = advice == null ? "" : advice;
    }

    public String getPriority() {
        return priority;
    }

    public String getTopic() {
        return topic;
    }

    public String getAdvice() {
        return advice;
    }

    @Override
    public String getCategory() {
        return "RECOMMEND";
    }

    @Override
    public String getTitle() {
        return "[" + priority + "] " + topic;
    }

    @Override
    public String getDetail() {
        return advice;
    }
}
