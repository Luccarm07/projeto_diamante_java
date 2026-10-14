package br.com.fiap.delivery.review.summary;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RankingItem(Long dishId, String dishName, double average, long count) {

    public static RankingItem from(ReviewSummary summary) {
        double rounded = BigDecimal.valueOf(summary.getAverage())
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        return new RankingItem(summary.getDishId(), summary.getDishName(), rounded, summary.getReviewCount());
    }
}
