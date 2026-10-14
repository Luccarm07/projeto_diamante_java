package br.com.fiap.delivery.order.review;

import br.com.fiap.delivery.order.config.RabbitConfig;
import br.com.fiap.delivery.order.exception.ReviewPublishException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReviewPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ReviewPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ReviewMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REVIEWS_ROUTING_KEY, message);
            log.debug("Review published for dish {} (rating {})", message.dishId(), message.rating());
        } catch (AmqpException ex) {
            throw new ReviewPublishException(ex);
        }
    }
}
