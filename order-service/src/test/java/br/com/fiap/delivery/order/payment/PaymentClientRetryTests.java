package br.com.fiap.delivery.order.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Checks that @Retryable is really applied through the Spring proxy. */
@SpringJUnitConfig(PaymentClientRetryTests.Config.class)
@TestPropertySource(properties = {
        "payment.retry.max-retries=4",
        "payment.retry.delay-ms=10",
        "payment.retry.jitter-ms=0",
        "payment.retry.multiplier=2.0",
        "payment.retry.max-delay-ms=50"
})
class PaymentClientRetryTests {

    @Configuration
    @EnableResilientMethods
    static class Config {

        @Bean
        RestTemplate loadBalancedRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        PaymentClient paymentClient(RestTemplate loadBalancedRestTemplate) {
            return new PaymentClient(loadBalancedRestTemplate);
        }
    }

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void resetMock() {
        reset(restTemplate);
    }

    @Test
    void retriesUntilAnInstanceApproves() {
        when(restTemplate.postForObject(anyString(), any(), eq(PaymentResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(new PaymentResponse("APPROVED", 8082));

        PaymentResponse response = paymentClient.charge(new BigDecimal("79.80"));

        assertThat(response.instance()).isEqualTo(8082);
        verify(restTemplate, times(3)).postForObject(anyString(), any(), eq(PaymentResponse.class));
    }

    @Test
    void givesUpAfterMaxRetriesAndPropagatesTheFailure() {
        when(restTemplate.postForObject(anyString(), any(), eq(PaymentResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> paymentClient.charge(new BigDecimal("39.90")))
                .isInstanceOf(HttpServerErrorException.class);

        // 1 original call + 4 retries
        verify(restTemplate, times(5)).postForObject(anyString(), any(), eq(PaymentResponse.class));
    }
}
