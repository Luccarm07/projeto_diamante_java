package br.com.fiap.delivery.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableResilientMethods // activates @Retryable (Spring Framework 7 native resilience)
public class HttpClientConfig {

    /** @LoadBalanced resolves service names (http://PAYMENT-SERVICE) through Eureka. */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
