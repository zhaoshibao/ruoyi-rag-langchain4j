package com.ruoyi.component;

import com.ruoyi.enums.AiTypeEnum;
import com.ruoyi.enums.SystemConstant;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Qdrant向量存储组件
 */
@Slf4j
@Component
public class QdrantVectorStoreComponet {
    @Value("${vectorstore.qdrant.host}")
    private String host;
    @Value("${vectorstore.qdrant.port}")
    private Integer port;
    // 向量维度配置（从配置文件读取，默认512），不同模型可能需要不同维度
    @Value("${vectorstore.qdrant.vector-size:512}")
    private Integer vectorSize;

    // 复用QdrantClient实例，避免重复创建
    private QdrantClient qdrantClient;

    /**
     * 初始化Qdrant客户端（项目启动时执行）
     */
    @PostConstruct
    public void initQdrantClient() {
        try {
            this.qdrantClient = new QdrantClient(
                    QdrantGrpcClient.newBuilder(host, port, false)
                            .withTimeout(Duration.ofSeconds(30)) // 添加超时配置
                            .build()
            );
            log.info("Qdrant客户端初始化成功，连接地址: {}:{}", host, port);
        } catch (Exception e) {
            log.error("Qdrant客户端初始化失败", e);
            throw new RuntimeException("初始化Qdrant客户端失败", e);
        }
    }

    public EmbeddingStore getEmbeddingStoreByAiType(AiTypeEnum aiTypeEnum) {
        switch (aiTypeEnum) {
            case OLLAMA:
                return getOllamaQdrantVectorStore();
            case OPENAI:
                return getOpenAiQdrantVectorStore();
            case ZHIPUAI:
                return getZhiPuAiQdrantVectorStore();
            default:
                throw new IllegalArgumentException("不支持的向量存储类型: " + aiTypeEnum);
        }
    }

    /**
     * 获取Ollama Qdrant向量存储组件
     */
    private EmbeddingStore getOllamaQdrantVectorStore () {
        return createEmbeddingStore(SystemConstant.OLLAMA_QDRANT);
    }

    /**
     * 获取OpenAi Qdrant向量存储组件
     * @return
     * @throws Exception
     */
    private EmbeddingStore getOpenAiQdrantVectorStore() {
        return createEmbeddingStore(SystemConstant.OPENAI_QDRANT);
    }



    /**
     * 获取智普AI 向量存储组件
     */
    private EmbeddingStore getZhiPuAiQdrantVectorStore () {
        return createEmbeddingStore(SystemConstant.ZHIPUAI_QDRANT);
    }

    /**
     * 通用创建向量存储的方法（核心优化：消除重复代码）
     * @param collectionName 集合名称
     * @return 向量存储实例
     */
    private EmbeddingStore createEmbeddingStore(String collectionName) {
        try {
            // 确保集合存在（不存在则创建）
            ensureCollectionExists(collectionName);

            // 构建并返回向量存储
            return QdrantEmbeddingStore.builder()
                    .client(qdrantClient) // 复用客户端
                    .collectionName(collectionName)
                    .build();
        } catch (Exception e) {
            log.error("创建Qdrant向量存储失败，集合名称: {}", collectionName, e);
            throw new RuntimeException("创建向量存储失败: " + collectionName, e);
        }
    }

    /**
     * 确保集合存在（不存在则创建）
     * @param collectionName 集合名称
     */
    private void ensureCollectionExists(String collectionName) throws Exception {
        // 检查集合是否存在
        boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
        if (!exists) {
            log.info("Qdrant集合不存在，开始创建: {}", collectionName);
            // 创建集合（向量配置统一管理）
            qdrantClient.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine) // 距离计算方式
                            .setSize(vectorSize) // 向量维度（配置化）
                            .build()
            ).get();
            log.info("Qdrant集合创建成功: {}", collectionName);
        } else {
            log.debug("Qdrant集合已存在: {}", collectionName);
        }
    }

}
