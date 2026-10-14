package br.com.fiap.delivery.review.messaging;

import br.com.fiap.delivery.review.buffer.ReviewBuffer;
import br.com.fiap.delivery.review.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Backpressure: each message only touches memory (the buffer). Nothing is written to the
 * database here, so a burst of reviews never turns into a burst of INSERTs.
 *
 * The body is parsed manually so the consumer does not depend on the producer's Java
 * class name (the __TypeId__ header points to a class that only exists in order-service).
 */
@Component
public class ReviewListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewListener.class);

    private final ReviewBuffer buffer;
    private final JsonMapper jsonMapper;

    public ReviewListener(ReviewBuffer buffer, JsonMapper jsonMapper) {
        this.buffer = buffer;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = RabbitConfig.REVIEWS_QUEUE)
    public void onReview(Message message) {
        ReviewMessage review;
        try {
            review = jsonMapper.readValue(message.getBody(), ReviewMessage.class);
        } catch (JacksonException ex) {
            // A malformed message would fail forever: drop it instead of requeueing
            throw new AmqpRejectAndDontRequeueException("Invalid review payload", ex);
        }

        if (!review.isValid()) {
            log.warn("Discarding invalid review: {}", review);
            return;
        }
        buffer.add(review.dishId(), review.dishName(), review.rating());
    }
}
