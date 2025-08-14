package com.ruoyi.utils;

import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 带缓存的工具类，用于获取聊天模型
 */
@Slf4j
public class ChatModelUtil {
    // 私有构造方法，防止实例化
    private ChatModelUtil() {
        throw new AssertionError("禁止实例化工具类");
    }
    // 缓存OpenAI模型实例，key为配置组合字符串
    private static final Map<String, OpenAiStreamingChatModel> openAiModelCache = new HashMap<>();

    // 缓存Ollama模型实例
    private static final Map<String, OllamaStreamingChatModel> ollamaModelCache = new HashMap<>();

    // 缓存智普AI模型实例
    private static final Map<String, ZhipuAiStreamingChatModel> zhiPuAiModelCache = new HashMap<>();
    /**
     *  获取OpenAI聊天模型（带缓存）
     * @param baseUrl   基础URL
     * @param apiKey    API密钥
     * @param model     模型名称
     * @return          OpenAI流式聊天模型
     */
    public static synchronized OpenAiStreamingChatModel getOpenAiChatModel(String baseUrl, String apiKey, String model) {
        // 生成唯一缓存键：组合配置参数
        String cacheKey = generateCacheKey(baseUrl, apiKey, model);
        // 检查缓存，不存在则创建并缓存
        if (!openAiModelCache.containsKey(cacheKey)) {
            log.info("创建新的OpenAI模型实例，缓存键: {}", cacheKey);
            OpenAiStreamingChatModel chatModel =  OpenAiStreamingChatModel.builder()
                    .logRequests(true)
                    .logResponses(true)
                    .baseUrl(baseUrl).apiKey(apiKey).modelName(model).temperature(0.4)
                    .build();
            openAiModelCache.put(cacheKey, chatModel);
        }
        return openAiModelCache.get(cacheKey);
    }


    /**
     * 获取Ollama聊天模型（带缓存）
     * @param baseUrl  基础URL
     * @param model    模型名称
     * @return         Ollama流式聊天模型
     */
    public static OllamaStreamingChatModel getOllamaChatModel(String baseUrl, String model) {
        String cacheKey = generateCacheKey(baseUrl, model);
        if (!ollamaModelCache.containsKey(cacheKey)) {
            log.info("创建新的Ollama模型实例，缓存键: {}", cacheKey);
            OllamaStreamingChatModel chatModel =  OllamaStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .temperature(0.4)
                    .logRequests(true)
                    .logResponses(true)
                    .modelName(model)
                    .build();
            ollamaModelCache.put(cacheKey, chatModel);
        }
        return ollamaModelCache.get(cacheKey);
    }


    /**
     * 获取智普AI聊天模型（带缓存）
     * @param baseUrl   基础URL
     * @param apiKey    API密钥
     * @param model     模型名称
     * @return          智普AI流式聊天模型
     */
    public static ZhipuAiStreamingChatModel getZhiPuAiChatModel(String baseUrl, String apiKey, String model) {
        String cacheKey = generateCacheKey(baseUrl, apiKey, model);

        if (!zhiPuAiModelCache.containsKey(cacheKey)) {
            log.info("创建新的智普AI模型实例，缓存键: {}", cacheKey);
            ZhipuAiStreamingChatModel chatModel =  ZhipuAiStreamingChatModel.builder()
                    .logRequests(true)
                    .logResponses(true)
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .model(model)
                    .temperature(0.4)
                    .build();
            zhiPuAiModelCache.put(cacheKey, chatModel);
        }
        return zhiPuAiModelCache.get(cacheKey);
    }

    /**
     * 生成缓存键，根据传入的参数组合
     */
    private static String generateCacheKey(String... params) {
        StringBuilder keyBuilder = new StringBuilder();
        for (String param : params) {
            keyBuilder.append(Objects.requireNonNullElse(param, "null")).append("|");
        }
        return keyBuilder.toString();
    }

    /**
     * 清除指定类型的模型缓存
     */
    public static synchronized void clearCache(CacheType cacheType) {
        switch (cacheType) {
            case OPENAI:
                openAiModelCache.clear();
                log.info("已清除OpenAI模型缓存");
                break;
            case OLLAMA:
                ollamaModelCache.clear();
                log.info("已清除Ollama模型缓存");
                break;
            case ZHIPU_AI:
                zhiPuAiModelCache.clear();
                log.info("已清除智普AI模型缓存");
                break;
            case ALL:
                openAiModelCache.clear();
                ollamaModelCache.clear();
                zhiPuAiModelCache.clear();
                log.info("已清除所有模型缓存");
                break;
        }
    }

    /**
     * 缓存类型枚举
     */
    public enum CacheType {
        OPENAI, OLLAMA, ZHIPU_AI, ALL
    }


}
