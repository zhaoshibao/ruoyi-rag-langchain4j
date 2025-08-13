package com.ruoyi.assistant;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface Assistant {
    public Flux<String> chat(@UserMessage List<ChatMessage> messages);
}
