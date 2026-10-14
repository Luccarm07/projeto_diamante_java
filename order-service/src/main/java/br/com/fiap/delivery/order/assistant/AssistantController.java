package br.com.fiap.delivery.order.assistant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final ChatService chatService;

    public AssistantController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return new AssistantResponse(chatService.ask(request.question()));
    }
}
