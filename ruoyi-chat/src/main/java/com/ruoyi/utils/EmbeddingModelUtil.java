package com.ruoyi.utils;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 单例模式的工具类，用于获取嵌入式模型
 */
@Slf4j
public class EmbeddingModelUtil {
    private static EmbeddingModel embeddingModel;

    // 私有构造方法，防止实例化
    private EmbeddingModelUtil() {
        throw new AssertionError("禁止实例化工具类");
    }


    /**
     * 获取本地嵌入式模型
     * @return
     * @throws Exception
     */
    public static synchronized EmbeddingModel getLocalEmbeddingModel() throws Exception {
        if (embeddingModel == null) {
            Path pathToModel = convertClasspathToPath("onnx/bge-small-zh-v1.5/model.onnx");
            Path pathToTokenizer = convertClasspathToPath("onnx/bge-small-zh-v1.5/tokenizer.json");
            PoolingMode poolingMode = PoolingMode.MEAN;
            embeddingModel = new OnnxEmbeddingModel(pathToModel, pathToTokenizer, poolingMode);
        }
        return embeddingModel;
   }

    private static Path convertClasspathToPath(String classpath) throws Exception {

        // 获取类加载器并读取资源 URL
        ClassLoader classLoader = EmbeddingModelUtil.class.getClassLoader();
        java.net.URL resourceUrl = classLoader.getResource(classpath);

        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found: " + classpath);
        }

        // 验证是否支持文件系统路径（排除 JAR 包内资源）
        if ("file".equals(resourceUrl.getProtocol())) {
            return Paths.get(resourceUrl.toURI());
        } else {
            throw new UnsupportedOperationException(
                    "Resource is not in filesystem (likely embedded in JAR): " + resourceUrl
            );
        }
    }




}
