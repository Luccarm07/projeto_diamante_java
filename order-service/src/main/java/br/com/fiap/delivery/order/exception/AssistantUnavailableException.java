package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class AssistantUnavailableException extends ApiException {

    public AssistantUnavailableException() {
        super(HttpStatus.BAD_GATEWAY, "Assistant is unavailable right now, please try again later");
    }

    public AssistantUnavailableException(Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "Assistant is unavailable right now, please try again later", cause);
    }
}
