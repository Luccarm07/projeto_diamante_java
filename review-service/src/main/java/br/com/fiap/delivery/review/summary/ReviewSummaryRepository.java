package br.com.fiap.delivery.review.summary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {

    List<ReviewSummary> findAllByOrderByAverageDescReviewCountDesc();
}
