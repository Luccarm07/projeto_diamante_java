package br.com.fiap.delivery.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Decides, at random, whether a payment call should fail.
 * With the default rate of 0.5 roughly half of the calls answer 500.
 */
@Component
public class FailureSimulator {

    private final Random random = new Random();
    private final double failureRate;

    public FailureSimulator(@Value("${payment.failure-rate:0.5}") double failureRate) {
        this.failureRate = failureRate;
    }

    public boolean shouldFail() {
        return random.nextDouble() < failureRate;
    }
}
