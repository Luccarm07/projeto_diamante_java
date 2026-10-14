package br.com.fiap.delivery.order.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Calls payment-service through Eureka. The host "PAYMENT-SERVICE" is the registered
 * service name, so Spring Cloud LoadBalancer picks 8081 or 8082 (round robin) on every
 * attempt; a retry therefore usually lands on the other instance.
 *
 * This lives in its own bean on purpose: @Retryable works through a proxy, so it is only
 * applied when the method is called from another bean (OrderService).
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate loadBalancedRestTemplate) {
        this.restTemplate = loadBalancedRestTemplate;
    }

    @Retryable(
            includes = {RestClientException.class, PaymentDeclinedException.class},
            excludes = HttpClientErrorException.class,           // 4xx will not fix itself
            maxRetriesString = "${payment.retry.max-retries:4}",
            delayString = "${payment.retry.delay-ms:200}",
            multiplierString = "${payment.retry.multiplier:2.0}", // exponential backoff
            jitterString = "${payment.retry.jitter-ms:100}",
            maxDelayString = "${payment.retry.max-delay-ms:2000}")
    public PaymentResponse charge(BigDecimal amount) {
        log.info("Calling PAYMENT-SERVICE for amount {}", amount);
        try {
            PaymentResponse response = restTemplate.postForObject(
                    PAYMENT_URL, new PaymentRequest(amount), PaymentResponse.class);

            if (response == null || !response.isApproved()) {
                throw new PaymentDeclinedException("Payment not approved");
            }
            log.info("Payment APPROVED by instance {}", response.instance());
            return response;
        } catch (RestClientException ex) {
            log.warn("Payment attempt failed: {} - will retry if attempts remain", ex.getMessage());
            throw ex;
        }
    }
}
