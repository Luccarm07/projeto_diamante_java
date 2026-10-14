package br.com.fiap.delivery.review.summary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class RankingController {

    private final ReviewSummaryService summaryService;

    public RankingController(ReviewSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/ranking")
    public List<RankingItem> ranking() {
        return summaryService.ranking();
    }
}
