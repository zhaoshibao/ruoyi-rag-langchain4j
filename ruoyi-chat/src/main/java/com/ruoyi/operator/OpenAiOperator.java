package com.ruoyi.operator;

import cn.hutool.core.util.IdUtil;
import com.ruoyi.annotation.BeanType;
import com.ruoyi.assistant.Assistant;
import com.ruoyi.component.QdrantVectorStoreComponet;
import com.ruoyi.domain.ChatFileSegment;
import com.ruoyi.domain.ChatKnowledge;
import com.ruoyi.domain.ChatProject;
import com.ruoyi.enums.AiTypeEnum;
import com.ruoyi.enums.LanguageEnum;
import com.ruoyi.enums.SystemConstant;
import com.ruoyi.searxng.SearXNGSearchResult;
import com.ruoyi.searxng.SearXNGService;
import com.ruoyi.service.IChatFileSegmentService;
import com.ruoyi.service.Neo4jService;
import com.ruoyi.service.async.VectorStoreAsyncService;
import com.ruoyi.utils.ChatModelUtil;
import com.ruoyi.utils.EmbeddingModelUtil;
import com.ruoyi.utils.MongoUtil;
import com.ruoyi.vo.QueryVo;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@BeanType(AiTypeEnum.OPENAI)
@Slf4j
public class OpenAiOperator implements AiOperator {


    @Autowired
    private QdrantVectorStoreComponet qdrantVectorStoreComponet;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SearXNGService searXNGService;

    @Autowired
    private Neo4jService neo4jService;


    @Autowired
    private IChatFileSegmentService iChatFileSegmentService;

    @Autowired
    private VectorStoreAsyncService vectorStoreAsyncService;







    @Override
    public Flux<String> chatStream(ChatProject chatProject, QueryVo queryVo) throws Exception {
        // 把问题记录到mongodb
        Long chatId = queryVo.getChatId();
        com.ruoyi.pojo.Message msg = new com.ruoyi.pojo.Message();
        msg.setChatId(chatId);
        msg.setType(0);
        msg.setContent(queryVo.getMsg());
        msg.setCreateTime(new Date());
        msg.setId(IdUtil.getSnowflake().nextId());
        this.mongoTemplate.insert(msg, MongoUtil.getMessageCollection(chatId));


        String baseUrl = chatProject.getBaseUrl();
        String apiKey = chatProject.getApiKey();
        String model = chatProject.getModel();
        //String embeddingModel = chatProject.getEmbeddingModel();
        EmbeddingModel localEmbeddingModel = EmbeddingModelUtil.getLocalEmbeddingModel();
        Embedding queryEmbedding = localEmbeddingModel.embed(queryVo.getMsg()).content();
        EmbeddingStore openAiQdrantVectorStore = qdrantVectorStoreComponet.getOpenAiQdrantVectorStore(baseUrl, apiKey,"");
        EmbeddingSearchResult<TextSegment> searchResult = openAiQdrantVectorStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(SystemConstant.TOPK) // 取前10个
                        .build()
        );
        List<EmbeddingMatch<TextSegment>> embeddingMatchList = searchResult.matches();
        List<ChatMessage> msgList;
        // 把本地知识库的内容作为系统提示放入
        List<String> knoledgeIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(embeddingMatchList)) {
            msgList = embeddingMatchList.stream().map(result -> {
                        String text = result.embedded().text();
                        return new SystemMessage(text);
            }
            ).collect(Collectors.toList());
        } else {
            msgList = new ArrayList<>();
        }

        // 添加 Neo4j 图数据库查询结果
        String graphContext = neo4jService.getAllRelationshipsContext(queryVo.getProjectId(), knoledgeIds);
        if (graphContext != null && !graphContext.isEmpty() && !graphContext.startsWith("未指定") && !graphContext.startsWith("指定的")) {
            msgList.add(new SystemMessage("以下是从图数据库中查询到的相关信息：\n" + graphContext));
        }
        //是否开启联网搜索
        Boolean useWebSearch = queryVo.getUseWebSearch();

        if (useWebSearch) {
            SearXNGSearchResult search = searXNGService.search(queryVo.getMsg());
            List<SearXNGSearchResult.Result> searchResultList = search.getResults();
            if (!CollectionUtils.isEmpty(searchResultList)) {
                searchResultList.stream().forEach(result -> {
                    msgList.add(new SystemMessage(result.getTitle()));
                    msgList.add(new SystemMessage(result.getContent()));
                });
            }

        }
        // 中英文切换
        msgList.add(new SystemMessage(LanguageEnum.getMsg(queryVo.getLanguage())));
        if (StringUtils.hasText(chatProject.getSystemPrompt())) {
            msgList.add(new SystemMessage(chatProject.getSystemPrompt()));
        }


        // 加入当前用户的提问
        msgList.add(new UserMessage(queryVo.getMsg()));


//        ChatRequest chatRequest = ChatRequest.builder()
//                .messages(msgList)
//                .parameters(OpenAiChatRequestParameters.builder()
//                        .build())
//                .build();
      OpenAiStreamingChatModel openAiStreamingChatModel = ChatModelUtil.getOpenAiChatModel(baseUrl, apiKey, model);
      Assistant assistant = AiServices.create(Assistant.class, openAiStreamingChatModel);
      Flux<String> flux = assistant.chat(msgList);




//        // 提交到大模型获取最终结果
////        ChatClient chatClient = ChatClient.builder(openAiChatModel)
////                .defaultToolCallbacks(tools)
////                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
////                .build();
//        openAiChatModel.
//        Flux<ChatResponse> responseFlux =chatClient.prompt(new Prompt(msgList)).stream().chatResponse();
//
//        Flux<String> flux = responseFlux.map(response -> {
//                   String result =  response.getResult() != null
//                            && response.getResult().getOutput() != null
//                            && response.getResult().getOutput().getText() != null
//                            ? response.getResult().getOutput().getText() : "";
//                    return result;
//                 }
//        );


        // flux.collectList().subscribe(list -> {
        //     此处获取的信息和最终返回的信息 是两个结果
        //     System.out.println(StringUtils.join(list, ""));
        // });
        return flux;
    }



    @Override
    public String imageUrl(QueryVo queryVo) {
        return null;
    }

    @Override
    public String imageBase64Json(QueryVo queryVo) {
        return null;
    }

    @Override
    public String textToAudio(QueryVo queryVo) {
        return null;
    }


    @Override
    public Boolean upload(ChatProject chatProject, ChatKnowledge chatKnowledge, MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        String fileSuffix  = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        List<TextSegment> textSegmentList = new ArrayList<>();
        // 创建文档分割器（按段落分割）
        DocumentSplitter paragraphSplitter = new DocumentByParagraphSplitter(300,30);
         switch (fileSuffix) {
            case "json":
            case "txt":
            case "md":
            case "csv":
                Document textfullDocument  = new TextDocumentParser().parse(file.getInputStream());
                textSegmentList = paragraphSplitter.split(textfullDocument);
                break;
             case "pdf":
                 textSegmentList = this.readPdfPages(file,chatProject);


                 //pdf增强解析（使用 Tesseract 实现 PDF 转图片并进行 OCR 识别文本 ）
//                 List<Document> pdfDocList = this.readPagesWithOcr(file);
//                 documentList.addAll(pdfDocList);


                 break;
             //`ApacheTikaDocumentParser`使用Apache Tika从各种文档格式中提取文本，如PDF、DOC/DOCX、PPT/PPTX和HTML。
             // 有关支持的格式的完整列表，请参阅https://tika.apache.org/3.1.0/formats.html[Tika文档]。
             default:
                 Document fullDocument  = new ApacheTikaDocumentParser().parse(file.getInputStream());
                 textSegmentList = paragraphSplitter.split(fullDocument);
                    break;

         }
        String projectId = chatProject.getProjectId();
        String knowledgeId = chatKnowledge.getKnowledgeId();
        List<TextSegment> newTextSegmentList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(textSegmentList)) {
            for (TextSegment textSegment : textSegmentList) {
                String segmentId = UUID.randomUUID().toString();
                String text = textSegment.text();
                //去除text 中的空白符
                text = text.replaceAll("\\s+", "");
                if (StringUtils.hasLength(text) ) {
                    Map<String,String> map = new HashMap<>();
                    map.put("segmentId",segmentId);
                    map.put("projectId",projectId);
                    map.put("knowledgeId",knowledgeId);
                    Metadata metadata = Metadata.from(map);
                    TextSegment newTextSegment = new TextSegment(text,metadata);
                    // Document doc = new Document(docId,text, Map.of("projectId", projectId, "knowledgeId", knowledgeId));
                    newTextSegmentList.add(newTextSegment);
                }
            }
            textSegmentList = newTextSegmentList;

            //Document document = new Document(knowledgeId, content, Map.of("projectId", projectId));
            if (!CollectionUtils.isEmpty(textSegmentList)) {

                List<ChatFileSegment> chatFileSegmentList = textSegmentList.stream().map(textSegment -> {
                    String segmentId = textSegment.metadata().getString("segmentId");
                    ChatFileSegment chatFileSegment = new ChatFileSegment();
                    chatFileSegment.setSegmentId(segmentId);
                    chatFileSegment.setKnowledgeId(knowledgeId);
                    chatFileSegment.setFileName(filename);
                    chatFileSegment.setContent(textSegment.text());
                    return chatFileSegment;
                }).collect(Collectors.toList());
                for (ChatFileSegment chatFileSegment : chatFileSegmentList) {
                    chatFileSegment.setCreateTime(new Date());
                    iChatFileSegmentService.insertChatFileSegment(chatFileSegment);
                }
                //判断是否开启知识图谱
                Integer isKnowledgeGraph = chatKnowledge.getIsKnowledgeGraph();
                if (isKnowledgeGraph == 1) {
                    neo4jService.processCsvFile(file,projectId,knowledgeId);
                }

                String baseUrl = chatProject.getBaseUrl();
                String apiKey = chatProject.getApiKey();
                String embeddingModel = chatProject.getEmbeddingModel();
                EmbeddingStore openAiQdrantVectorStore = qdrantVectorStoreComponet.getOpenAiQdrantVectorStore(baseUrl, apiKey, embeddingModel);
                //异步执行
                this.vectorStoreAsyncService.addVectorStore(knowledgeId,openAiQdrantVectorStore, textSegmentList);


            }

        }
        return true;
    }

    /**
     * 读取PDF每页内容（含乱码处理）
     * @param file
     * @return 每页内容的列表
     */
    public List<TextSegment> readPdfPages(MultipartFile file,ChatProject chatProject) throws Exception {
        // 尝试标准文本提取
        byte[] pdfData = file.getBytes();
        List<TextSegment> documentList = tryStandardExtraction(pdfData);

        // 如果提取失败（通常是乱码文档）
        //if (documentList == null || needsOcrFallback(documentList)    ) {
        if (documentList == null) {
            log.info("==============检测到文本提取异常，启用OCR回退方案==================");
            //判断pdf增强解析是否启用，如果启用则执行ocr解析
            if (chatProject.getPdfAnalysis().equals(0)) {
                return null;
            }
            return readPagesWithOcr(file);
        } else {
            //判断pdf增强解析是否启用，如果启用则执行ocr解析
            if (chatProject.getPdfAnalysis().equals(0)) {
                return documentList;
            }
            List<TextSegment> docListWithOcr = readPagesWithOcr(file);
            documentList.addAll(docListWithOcr);
        }

        return documentList;
    }

    // 尝试标准文本提取
    private List<TextSegment> tryStandardExtraction(byte[] pdfData) {
        List<TextSegment> results = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                if (StringUtils.hasLength(text)) {
                    results.add(TextSegment.from(text));
                }

            }
            return results;
        } catch (Exception e) {
            log.error("==============标准提取失败: " + e.getMessage() + "==================");
            return null;
        }
    }




    /**
     * OCR回退方案
     * @param pdfFile
     * @return
     * @throws IOException
     * @throws TesseractException
     * todo 后续优化：多线程pdf页面处理
     */
    public List<TextSegment> readPagesWithOcr(MultipartFile pdfFile) throws IOException, TesseractException {
        // 临时目录准备
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "pdf_images");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        List<TextSegment> ocrResults = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);

            // 设置DPI提高OCR精度
            int dpi = 300;

            Tesseract tesseract = new Tesseract();
            // 设置语言包（需事先安装）
           /// tesseract.setLanguage("eng");
            tesseract.setLanguage("chi_sim");
            // 如需要指定tessdata路径
            // tesseract.setDatapath("/path/to/tessdata");

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                // 渲染PDF页为BufferedImage
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);

                // 生成临时图像文件
                File imgFile = new File(tempDir, "page_" + page + ".png");
                // 最佳实践：使用 ImageIO 保存图像
                ImageIO.write(image, "png", imgFile);

                // 执行OCR
                String result = tesseract.doOCR(imgFile);
                if (StringUtils.hasLength(result)) {
                    ocrResults.add(TextSegment.from(result));
                }

                // 删除临时图像文件
                imgFile.delete();
            }
        }

        // 删除临时目录
        tempDir.delete();
        return ocrResults;
    }

    //    @Override
//    public Boolean remove(String docId) {
//        return this.openaiQdrantVectorStore.delete(List.of(docId)).get();
//    }
    @Override
    public void remove(ChatProject chatProject,String docId) throws Exception {
        String baseUrl = chatProject.getBaseUrl();
        String apiKey = chatProject.getApiKey();
        String embeddingModel = chatProject.getEmbeddingModel();
        EmbeddingStore openAiQdrantVectorStore = qdrantVectorStoreComponet.getOpenAiQdrantVectorStore(baseUrl, apiKey, embeddingModel);
        openAiQdrantVectorStore.remove(docId);

    }

    @Override
    public void removeByknowledgeId(ChatProject chatProject, String knowledgeId) throws Exception {
        String baseUrl = chatProject.getBaseUrl();
        String apiKey = chatProject.getApiKey();
        String embeddingModel = chatProject.getEmbeddingModel();
        EmbeddingStore openAiQdrantVectorStore = qdrantVectorStoreComponet.getOpenAiQdrantVectorStore(baseUrl,apiKey,embeddingModel);
        //异步执行
        vectorStoreAsyncService.removeByknowledgeId(openAiQdrantVectorStore,knowledgeId);
//        ollamaQdrantVectorStore.delete(
//                new FilterExpressionBuilder().eq("knowledgeId", knowledgeId).build()
//        );
    }

}


