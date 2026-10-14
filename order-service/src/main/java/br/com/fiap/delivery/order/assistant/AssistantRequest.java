package br.com.fiap.delivery.order.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 500, message = "Question must have at most 500 characters")
        String question) {
}
