package br.com.fiap.delivery.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailureSimulatorTests {

    @Test
    void neverFailsWithRateZero() {
        FailureSimulator simulator = new FailureSimulator(0.0);
        for (int i = 0; i < 1_000; i++) {
            assertThat(simulator.shouldFail()).isFalse();
        }
    }

    @Test
    void alwaysFailsWithRateOne() {
        FailureSimulator simulator = new FailureSimulator(1.0);
        for (int i = 0; i < 1_000; i++) {
            assertThat(simulator.shouldFail()).isTrue();
        }
    }

    @Test
    void failsAboutHalfOfTheTimeWithDefaultRate() {
        FailureSimulator simulator = new FailureSimulator(0.5);
        int failures = 0;
        int calls = 10_000;
        for (int i = 0; i < calls; i++) {
            if (simulator.shouldFail()) {
                failures++;
            }
        }
        assertThat(failures).isBetween(4_000, 6_000);
    }
}
