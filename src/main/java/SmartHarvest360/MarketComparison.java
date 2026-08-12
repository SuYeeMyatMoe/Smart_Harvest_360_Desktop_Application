package SmartHarvest360;

import java.util.List;

/**
 * Compares the different markets and also recommends the best selling option.
 */
public class MarketComparison {

    /**
     * Finds the best market using price and demand.
     */
    public MarketOption findBestMarket(List<MarketOption> markets) {

        if (markets == null || markets.isEmpty()) {
            return null;
        }

        MarketOption bestMarket = markets.get(0);
        double bestScore = calculateScore(bestMarket);

        for (MarketOption market : markets) {

            double score = calculateScore(market);

            if (score > bestScore) {
                bestScore = score;
                bestMarket = market;
            }
        }

        return bestMarket;
    }

    /**
     * Calculates a market score.
     * Price contributes 70% and demand contributes 30%.
     */
    public double calculateScore(MarketOption market) {

        double demandScore = getDemandScore(market.getDemand());

        return (market.getPricePerKg() * 0.70)
                + (demandScore * 0.30);
    }

    /**
     * Converts demand into a simple numerical score.
     */
    private double getDemandScore(String demand) {

        if (demand == null) {
            return 0;
        }

        switch (demand.toLowerCase()) {

            case "high":
                return 10;

            case "medium":
                return 7;

            case "low":
                return 4;

            default:
                return 0;
        }
    }

    /**
     * Returns a simple explanation for the recommendation.
     */
    public String getRecommendation(MarketOption market) {

        if (market == null) {
            return "No market available.";
        }

        return "Recommended Market: " + market.getMarketName() + " | Price: RM" + String.format("%.2f", market.getPricePerKg()) + "/kg" + " | Demand: " + market.getDemand();
    }
}
