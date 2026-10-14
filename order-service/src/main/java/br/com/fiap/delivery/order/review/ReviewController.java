package br.com.fiap.delivery.order.review;

import br.com.fiap.delivery.order.dish.Dish;
import br.com.fiap.delivery.order.dish.DishRepository;
import br.com.fiap.delivery.order.exception.DishNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Producer side of the backpressure flow: the review is NOT written to a database here.
 * It is validated, published to RabbitMQ and answered with 202 Accepted right away.
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final DishRepository dishRepository;
    private final ReviewPublisher reviewPublisher;

    public ReviewController(DishRepository dishRepository, ReviewPublisher reviewPublisher) {
        this.dishRepository = dishRepository;
        this.reviewPublisher = reviewPublisher;
    }

    @PostMapping
    public ResponseEntity<Void> submit(@Valid @RequestBody ReviewRequest request) {
        Dish dish = dishRepository.findById(request.dishId())
                .orElseThrow(DishNotFoundException::new);

        reviewPublisher.publish(new ReviewMessage(
                dish.getId(), dish.getName(), request.rating(), request.comment()));

        return ResponseEntity.accepted().build();
    }
}
