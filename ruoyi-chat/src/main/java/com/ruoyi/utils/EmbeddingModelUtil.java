package com.ruoyi.utils;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工具类，用于获取嵌入式模型
 */
@Slf4j
public class EmbeddingModelUtil {
    /**
     * 获取本地嵌入式模型
     * @return
     * @throws Exception
     */
//   public static TransformersEmbeddingModel getLocalEmbeddingModel() throws Exception {
//       TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
//       // 设置tokenizer文件路径
//       embeddingModel.setTokenizerResource("classpath:/onnx/bge-small-zh-v1.5/tokenizer.json");
//       // 设置Onnx模型文件路径
//       embeddingModel.setModelResource("classpath:/onnx/bge-small-zh-v1.5/model.onnx");
//       // 缓存位置
//       embeddingModel.setResourceCacheDirectory("/tmp/onnx-cache");
//       // 自动填充
//       embeddingModel.setTokenizerOptions(Map.of("padding", "true"));
//       // 模型输出层的名称，默认是 last_hidden_state, 需要根据所选模型设置
//       embeddingModel.setModelOutputName("token_embeddings");
//       embeddingModel.afterPropertiesSet();
//       return embeddingModel;
//   }

       public static EmbeddingModel getLocalEmbeddingModel() throws Exception {

           Path pathToModel = convertClasspathToPath("onnx/bge-small-zh-v1.5/model.onnx");
           Path pathToTokenizer = convertClasspathToPath("onnx/bge-small-zh-v1.5/tokenizer.json");
           PoolingMode poolingMode = PoolingMode.MEAN;
           EmbeddingModel embeddingModel = new OnnxEmbeddingModel(pathToModel,pathToTokenizer,poolingMode);
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
