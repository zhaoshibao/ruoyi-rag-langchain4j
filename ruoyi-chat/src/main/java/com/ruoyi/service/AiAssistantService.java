package com.ruoyi.assistant;

import dev.langchain4j.data.message.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiAssistantService {
    public Flux<String> chat(List<ChatMessage> messages);
}
