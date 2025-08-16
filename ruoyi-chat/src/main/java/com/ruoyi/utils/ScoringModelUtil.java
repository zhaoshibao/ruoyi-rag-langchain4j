package com.ruoyi.utils;

import ai.onnxruntime.OrtLoggingLevel;
import ai.onnxruntime.OrtSession;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 单例模式的工具类，用于获取排序模型
 */
@Slf4j
public class ScoringModelUtil {
    private static OnnxScoringModel scoringModel;

    // 模型和Tokenizer的相对路径（相对于classpath根目录）
    private static final String MODEL_RELATIVE_PATH = "onnx/ms-marco-MiniLM-L-6-v2/model_quantized.onnx";
    private static final String TOKENIZER_RELATIVE_PATH = "onnx/ms-marco-MiniLM-L-6-v2/tokenizer.json";
    // 私有构造方法，防止实例化
    private ScoringModelUtil() {
        throw new AssertionError("禁止实例化工具类");
    }


    /**
     * 获取本地排序模型
     * @throws Exception
     */
    public static synchronized OnnxScoringModel getLocalScoringModel() throws Exception {
        if (scoringModel == null) {
            // 获取模型和Tokenizer的实际路径
            String modelPath = getResourcePath(MODEL_RELATIVE_PATH);
            String tokenizerPath = getResourcePath(TOKENIZER_RELATIVE_PATH);
            // 检查文件是否存在
            checkFileExists(modelPath, "模型文件");
            checkFileExists(tokenizerPath, "Tokenizer文件");

            // 初始化ONNX会话配置
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            // 可选：设置日志级别（调试时使用）
            sessionOptions.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_INFO);

            // 加载模型
            scoringModel = new OnnxScoringModel(
                    modelPath,
                    sessionOptions,
                    tokenizerPath,
                    512,  // 最大序列长度
                    false  // 是否归一化
            );
            log.info("ONNX模型加载成功，路径：{}", modelPath);
        }
        return scoringModel;
   }

    /**
     * 获取资源文件的绝对路径
     * @param relativePath 相对于classpath的路径
     * @return 资源的绝对路径
     * @throws Exception 资源不存在时抛出异常
     */
    private static String getResourcePath(String relativePath) throws Exception {
        ClassLoader classLoader = ScoringModelUtil.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(relativePath);
        if (resourceUrl == null) {
            throw new Exception("资源不存在：" + relativePath + "（请检查classpath下是否有该文件）");
        }

        // 处理JAR包内和外部文件的路径
        try {
            return new File(resourceUrl.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new Exception("资源路径解析失败：" + relativePath, e);
        }
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @param fileDesc 文件描述（用于日志）
     * @throws Exception 文件不存在时抛出异常
     */
    private static void checkFileExists(String filePath, String fileDesc) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new Exception(fileDesc + "不存在或不是文件：" + filePath);
        }
    }


    public static void main(String[] args) {
        ClassLoader classLoader = ScoringModelUtil.class.getClassLoader();
        String path = classLoader.getResource("").getPath();
        System.out.println(path);
    }








}

