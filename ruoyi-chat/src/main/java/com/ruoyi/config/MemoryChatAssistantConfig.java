package com.ruoyi.config;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryChatAssistantConfig {
    @Bean
    ChatMemoryStore chatMemoryStore() {
        //设置聊天记忆记录的message数量
        return new InMemoryChatMemoryStore();
    }
}
