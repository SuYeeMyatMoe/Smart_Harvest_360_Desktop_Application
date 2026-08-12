package SmartHarvest360.plan;

/**
 * Common contract for plan rows shown after simulation.
 * OOP: Abstraction / Polymorphism — steps and recommendations share one UI contract.
 */
public interface PlanItem {
    String getCategory();

    String getTitle();

    String getDetail();
}
