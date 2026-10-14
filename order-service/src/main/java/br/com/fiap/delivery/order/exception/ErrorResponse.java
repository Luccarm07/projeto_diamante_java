package br.com.fiap.delivery.order.exception;

/** Error body used by every 4xx/5xx answer: {"error": "..."} */
public record ErrorResponse(String error) {
}
