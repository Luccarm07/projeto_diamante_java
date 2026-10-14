package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class ReviewPublishException extends ApiException {

    public ReviewPublishException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Review could not be published, please try again later");
    }

    public ReviewPublishException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Review could not be published, please try again later", cause);
    }
}
