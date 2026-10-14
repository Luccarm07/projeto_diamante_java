package br.com.fiap.delivery.review.summary;

import br.com.fiap.delivery.review.buffer.PendingReviews;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewSummaryService {

    private final ReviewSummaryRepository repository;

    public ReviewSummaryService(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    /** Adds a drained batch to the stored totals, all in one transaction. */
    @Transactional
    public void apply(Map<Long, PendingReviews> batch) {
        batch.forEach((dishId, pending) -> {
            ReviewSummary summary = repository.findById(dishId)
                    .orElseGet(() -> new ReviewSummary(dishId, pending.dishName()));
            summary.accumulate(pending.dishName(), pending.ratingSum(), pending.count());
            repository.save(summary);
        });
    }

    /** Ranking read from the database (not from the buffer), best average first. */
    @Transactional(readOnly = true)
    public List<RankingItem> ranking() {
        return repository.findAllByOrderByAverageDescReviewCountDesc().stream()
                .map(RankingItem::from)
                .toList();
    }
}
