package br.com.fiap.delivery.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit messaging topology:
 * delivery.exchange (topic) --[reviews.new]--> reviews.queue (durable)
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "delivery.exchange";
    public static final String REVIEWS_QUEUE = "reviews.queue";
    public static final String REVIEWS_ROUTING_KEY = "reviews.new";

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewsQueue() {
        return new Queue(REVIEWS_QUEUE, true); // durable: survives a broker restart
    }

    @Bean
    public Binding reviewsBinding(Queue reviewsQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(reviewsQueue).to(deliveryExchange).with(REVIEWS_ROUTING_KEY);
    }

    /** Messages are sent as JSON (content-type application/json). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
