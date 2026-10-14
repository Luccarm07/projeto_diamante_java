package br.com.fiap.delivery.payment;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unstable payment endpoint. Every response and every log line carries the port of the
 * instance that handled it, so the load balancing between 8081 and 8082 is visible.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final FailureSimulator failureSimulator;
    private final Environment environment;

    public PaymentController(FailureSimulator failureSimulator, Environment environment) {
        this.failureSimulator = failureSimulator;
        this.environment = environment;
    }

    @PostMapping
    public ResponseEntity<?> pay(@Valid @RequestBody PaymentRequest request) {
        int port = currentPort();

        if (failureSimulator.shouldFail()) {
            log.warn("[instance {}] Simulated failure for amount {}", port, request.amount());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Simulated payment failure on instance " + port));
        }

        log.info("[instance {}] Payment APPROVED for amount {}", port, request.amount());
        return ResponseEntity.ok(PaymentResponse.approved(port));
    }

    private int currentPort() {
        // local.server.port is the real port after startup (honours --server.port=8082)
        return environment.getProperty("local.server.port", Integer.class,
                environment.getProperty("server.port", Integer.class, 8081));
    }
}
