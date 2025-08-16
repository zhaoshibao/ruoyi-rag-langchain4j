package com.ruoyi.factory;

import com.ruoyi.component.QdrantVectorStoreComponet;
import com.ruoyi.domain.ChatProject;
import com.ruoyi.enums.AiTypeEnum;
import com.ruoyi.service.AiAssistantService;
import com.ruoyi.utils.ChatModelUtil;
import com.ruoyi.utils.EmbeddingModelUtil;
import dev.langchain4j.community.web.search.searxng.SearXNGWebSearchEngine;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.web.search.WebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class AiServiceFactory {
    @Autowired
    private ChatMemoryStore chatMemoryStore;
    @Autowired
    private QdrantVectorStoreComponet qdrantVectorStoreComponet;
    @Value("${searxng.host}")
    private String searxngHost;
    @Value("${searxng.timeout}")
    private Integer searxngTimeout;
    @Value("${searxng.maxResults}")
    private Integer searxngMaxResults;


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

        EmbeddingStore embeddingStore = qdrantVectorStoreComponet.getEmbeddingStoreByAiType(aiTypeEnum);

        ContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever
                .builder()
                // 设置用于生成嵌入向量的嵌入模型
                .embeddingModel(EmbeddingModelUtil.getLocalEmbeddingModel())
                // 指定要使用的嵌入存储
                .embeddingStore(embeddingStore)
                // 设置最大检索结果数量，这里表示最多返回 1 条匹配结果
                .maxResults(1)
                // 设置最小得分阈值，只有得分大于等于 0.8 的结果才会被返回
                .minScore(0.8)
                // 构建最终的 EmbeddingStoreContentRetriever 实例
                .build();

        WebSearchEngine webSearchEngine = SearXNGWebSearchEngine.builder()
                .baseUrl(searxngHost)
                .duration(Duration.ofMillis(searxngTimeout))
                .build();
//        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
//                .apiKey("TAVILY_API_KEY") // get a free key: https://app.tavily.com/sign-in
//                .build();

        ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(searxngMaxResults)
                .build();

        List<ContentRetriever> contentRetrieverList = new ArrayList<>();
        contentRetrieverList.add(embeddingStoreContentRetriever);
        if (chatProject.getUseWebSearch()) {
            contentRetrieverList.add(webSearchContentRetriever);
        }

        QueryRouter queryRouter = new DefaultQueryRouter(contentRetrieverList);


        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();
        return switch (aiTypeEnum) {
            // 基于 OpenAI 模型
            case OPENAI -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getOpenAiChatModel(chatProject.getBaseUrl(), chatProject.getApiKey(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build();
            // 基于 Ollama 模型
            case OLLAMA -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getOllamaChatModel(chatProject.getBaseUrl(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build();
            // 基于智普AI模型
            case ZHIPUAI -> AiServices.builder(AiAssistantService.class)
                    .streamingChatModel(ChatModelUtil.getZhiPuAiChatModel(chatProject.getBaseUrl(), chatProject.getApiKey(), chatProject.getModel()))
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build();
            default ->
                    throw new RuntimeException("不支持的大模型类型: " + aiTypeEnum.getType());
        };
    }

}
