package br.com.fiap.delivery.review.summary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Aggregated reviews of one dish. One row per dish, updated on every flush. */
@Entity
@Table(name = "review_summary")
public class ReviewSummary {

    @Id
    private Long dishId;

    @Column(nullable = false)
    private String dishName;

    @Column(nullable = false)
    private long ratingSum;

    @Column(nullable = false)
    private long reviewCount;

    @Column(nullable = false)
    private double average;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ReviewSummary() {
        // required by JPA
    }

    public ReviewSummary(Long dishId, String dishName) {
        this.dishId = dishId;
        this.dishName = dishName;
    }

    public void accumulate(String latestName, long ratingSumDelta, long countDelta) {
        if (latestName != null && !latestName.isBlank()) {
            this.dishName = latestName;
        }
        this.ratingSum += ratingSumDelta;
        this.reviewCount += countDelta;
        this.average = reviewCount == 0 ? 0 : (double) ratingSum / reviewCount;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getDishId() { return dishId; }
    public String getDishName() { return dishName; }
    public long getRatingSum() { return ratingSum; }
    public long getReviewCount() { return reviewCount; }
    public double getAverage() { return average; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
