package br.com.fiap.delivery.review.buffer;

import br.com.fiap.delivery.review.summary.ReviewSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Every 5 seconds writes the accumulated reviews to H2 in one batch and clears the buffer. */
@Component
public class ReviewFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewFlushScheduler.class);

    private final ReviewBuffer buffer;
    private final ReviewSummaryService summaryService;

    public ReviewFlushScheduler(ReviewBuffer buffer, ReviewSummaryService summaryService) {
        this.buffer = buffer;
        this.summaryService = summaryService;
    }

    @Scheduled(fixedRateString = "${reviews.flush-interval-ms:5000}")
    public void flush() {
        Map<Long, PendingReviews> batch = buffer.drain();
        if (batch.isEmpty()) {
            return;
        }
        try {
            summaryService.apply(batch);
            long reviews = batch.values().stream().mapToLong(PendingReviews::count).sum();
            log.info("Flush: {} reviews of {} dishes saved to the database", reviews, batch.size());
        } catch (RuntimeException ex) {
            buffer.restore(batch);
            log.error("Flush failed, {} dishes returned to the buffer", batch.size(), ex);
        }
    }
}
