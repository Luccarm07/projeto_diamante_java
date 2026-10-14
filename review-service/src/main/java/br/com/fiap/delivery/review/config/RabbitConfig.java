package br.com.fiap.delivery.review.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The consumer also declares the durable queue (idempotent), so the listener can start
 * even if order-service has not published anything yet.
 */
@Configuration
public class RabbitConfig {

    public static final String REVIEWS_QUEUE = "reviews.queue";

    @Bean
    public Queue reviewsQueue() {
        return new Queue(REVIEWS_QUEUE, true);
    }
}
