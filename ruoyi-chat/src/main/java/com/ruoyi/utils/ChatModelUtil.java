package com.ruoyi.utils;

import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

/**
 * 工具类，用于获取聊天模型
 */
@Slf4j
public class ChatModelUtil {
    /**
     * 获取OpenAI聊天模型
     * @param baseUrl
     * @param apiKey
     * @param model
     * @return
     */
    public static OpenAiStreamingChatModel getOpenAiChatModel(String baseUrl, String apiKey, String model) {

        return  OpenAiStreamingChatModel.builder()
                .logRequests(true)
                .logResponses(true)
               .baseUrl(baseUrl).apiKey(apiKey).modelName(model).temperature(0.4)
               .build();
    }


    /**
     * 获取Ollama聊天模型
     * @param baseUrl
     * @param model
     * @return
     */
    public static OllamaStreamingChatModel getOllamaChatModel(String baseUrl, String model) {
        return  OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .temperature(0.4)
                .logRequests(true)
                .logResponses(true)
                .modelName(model)
                .build();
    }


    /**
     * 获取智普AI聊天模型
     * @param baseUrl
     * @param apiKey
     * @param model
     * @return
     */
    public static ZhipuAiStreamingChatModel getZhiPuAiChatModel(String baseUrl, String apiKey, String model) {
        return  ZhipuAiStreamingChatModel.builder()
                .logRequests(true)
                .logResponses(true)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .temperature(0.4)
                .build();
    }


}
