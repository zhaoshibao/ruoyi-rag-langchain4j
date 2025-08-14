package com.ruoyi.factory;

import com.ruoyi.component.QdrantVectorStoreComponet;
import com.ruoyi.domain.ChatProject;
import com.ruoyi.enums.AiTypeEnum;
import com.ruoyi.service.AiAssistantService;
import com.ruoyi.utils.ChatModelUtil;
import com.ruoyi.utils.EmbeddingModelUtil;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AiServiceFactory {
    @Autowired
    private ChatMemoryStore chatMemoryStore;
    @Autowired
    private QdrantVectorStoreComponet qdrantVectorStoreComponet;

    /**
     * 创建新的 AI 服务实例
     * @param chatId      聊天 id
     * @param chatProject 聊天项目
     * @param aiTypeEnum  大模型类型
     * @return
     */
    public AiAssistantService createAiService(long chatId, ChatProject chatProject, AiTypeEnum aiTypeEnum) throws Exception {
        log.info("为 chatId: {} 创建新的 AI 服务实例", chatId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(chatId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(20)
                .build();
        return switch (aiTypeEnum) {
            // 基于 OpenAI 模型
            case OPENAI -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getOpenAiChatModel(chatProject.getBaseUrl(), chatProject.getApiKey(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .contentRetriever(EmbeddingStoreContentRetriever
                            .builder()
                            // 设置用于生成嵌入向量的嵌入模型
                            .embeddingModel(EmbeddingModelUtil.getLocalEmbeddingModel())
                            // 指定要使用的嵌入存储
                            .embeddingStore(qdrantVectorStoreComponet.getOpenAiQdrantVectorStore())
                            // 设置最大检索结果数量，这里表示最多返回 1 条匹配结果
                            .maxResults(1)
                            // 设置最小得分阈值，只有得分大于等于 0.8 的结果才会被返回
                            .minScore(0.8)
                            // 构建最终的 EmbeddingStoreContentRetriever 实例
                            .build())
                    .build();
            // 基于 Ollama 模型
            case OLLAMA -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getOllamaChatModel(chatProject.getBaseUrl(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .contentRetriever(EmbeddingStoreContentRetriever
                            .builder()
                            // 设置用于生成嵌入向量的嵌入模型
                            .embeddingModel(EmbeddingModelUtil.getLocalEmbeddingModel())
                            // 指定要使用的嵌入存储
                            .embeddingStore(qdrantVectorStoreComponet.getOllamaQdrantVectorStore())
                            // 设置最大检索结果数量，这里表示最多返回 1 条匹配结果
                            .maxResults(1)
                            // 设置最小得分阈值，只有得分大于等于 0.8 的结果才会被返回
                            .minScore(0.8)
                            // 构建最终的 EmbeddingStoreContentRetriever 实例
                            .build())
                    .build();
            // 基于智普AI模型
            case ZHIPUAI -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getZhiPuAiChatModel(chatProject.getBaseUrl(), chatProject.getApiKey(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .contentRetriever(EmbeddingStoreContentRetriever
                            .builder()
                            // 设置用于生成嵌入向量的嵌入模型
                            .embeddingModel(EmbeddingModelUtil.getLocalEmbeddingModel())
                            // 指定要使用的嵌入存储
                            .embeddingStore(qdrantVectorStoreComponet.getZhiPuAiQdrantVectorStore())
                            // 设置最大检索结果数量，这里表示最多返回 1 条匹配结果
                            .maxResults(1)
                            // 设置最小得分阈值，只有得分大于等于 0.8 的结果才会被返回
                            .minScore(0.8)
                            // 构建最终的 EmbeddingStoreContentRetriever 实例
                            .build())
                    .build();
            default ->
                    throw new RuntimeException("不支持的大模型类型: " + aiTypeEnum.getType());
        };
    }

}
