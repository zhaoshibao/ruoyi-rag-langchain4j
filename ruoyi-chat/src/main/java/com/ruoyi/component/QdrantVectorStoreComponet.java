package com.ruoyi.component;

import com.ruoyi.enums.SystemConstant;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Qdrant向量存储组件
 */
@Component
public class QdrantVectorStoreComponet {
    @Value("${vectorstore.qdrant.host}")
    private String host;
    @Value("${vectorstore.qdrant.port}")
    private Integer port;

    /**
     * 获取Ollama Qdrant向量存储组件
     * @param baseUrl
     * @param embeddingmodel
     * @return
     * @throws Exception
     */
    public EmbeddingStore getOllamaQdrantVectorStore (String baseUrl, String embeddingmodel ) throws Exception {
        QdrantClient qdrantClient =
                new QdrantClient(
                        QdrantGrpcClient.newBuilder(host, port, false)
                                .build());
        if (!qdrantClient.collectionExistsAsync(SystemConstant.OLLAMA_QDRANT).get()) {
            qdrantClient.createCollectionAsync(SystemConstant.OLLAMA_QDRANT,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(512)
                            .build()).get();
        }
        return  QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                //.host(baseUrl)
                .collectionName(SystemConstant.OLLAMA_QDRANT)
                .build();
    }

    /**
     * 获取OpenAi Qdrant向量存储组件
     * @param baseUrl
     * @param apiKey
     * @param embeddingmodel
     * @return
     * @throws Exception
     */
    public EmbeddingStore getOpenAiQdrantVectorStore(String baseUrl, String apiKey, String embeddingmodel) throws Exception  {
        QdrantClient qdrantClient =
                new QdrantClient(
                        QdrantGrpcClient.newBuilder(host, port, false)
                                .build());

        if (!qdrantClient.collectionExistsAsync(SystemConstant.OPENAI_QDRANT).get()) {
            qdrantClient.createCollectionAsync(SystemConstant.OPENAI_QDRANT,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(512)
                            .build()).get();
        }

        return  QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                //.host(baseUrl)
                .collectionName(SystemConstant.OPENAI_QDRANT)
                .build();
    }



    /**
     * 获取智普AI 向量存储组件
     * @param baseUrl
     * @param embeddingmodel
     * @return
     * @throws Exception
     */
    public EmbeddingStore getZhiPuAiQdrantVectorStore (String baseUrl, String apiKey, String embeddingmodel) throws Exception {
        QdrantClient qdrantClient =
                new QdrantClient(
                        QdrantGrpcClient.newBuilder(host, port, false)
                                .build());

        if (!qdrantClient.collectionExistsAsync(SystemConstant.ZHIPUAI_QDRANT).get()) {
            qdrantClient.createCollectionAsync(SystemConstant.ZHIPUAI_QDRANT,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(512)
                            .build()).get();
        }

        return  QdrantEmbeddingStore.builder()
                //.host(baseUrl)
                .client(qdrantClient)
                .collectionName(SystemConstant.ZHIPUAI_QDRANT)
                .build();
    }

}
