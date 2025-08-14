package com.ruoyi.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

public interface AiAssistantService {
    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(@MemoryId long chatId, @UserMessage String userMessage,@V("systemPrompt") String systemPrompt);
}
