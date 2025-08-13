package com.ruoyi.service.async;

import com.ruoyi.domain.ChatKnowledge;
import com.ruoyi.service.IChatKnowledgeService;
import com.ruoyi.utils.EmbeddingModelUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 异步处理向量存储
 */
@Service
@Slf4j
public class VectorStoreAsyncService {

    @Autowired
    IChatKnowledgeService chatKnowledgeService;

    /**
     * 异步执行根据knowledgeId删除向量存储
     * @param embeddingStore
     * @param knowledgeId
     * @throws Exception
     */
    @Async
    public void removeByknowledgeId(EmbeddingStore embeddingStore, String knowledgeId) throws Exception {
//        embeddingStore.delete(
//                new FilterExpressionBuilder().eq("knowledgeId", knowledgeId).build()
//        );
        MetadataFilterBuilder metadataFilterBuilder = new MetadataFilterBuilder("knowledgeId");

         embeddingStore.removeAll(metadataFilterBuilder.isEqualTo(knowledgeId));
        log.info("异步执行根据knowledgeId删除向量存储成功");

    }

    /**
     * 异步执行新增向量存储
     * @param knowledgeId
     * @param embeddingStore
     * @param textSegmentList
     * @throws Exception
     */
    @Async
    public void addVectorStore(String knowledgeId,EmbeddingStore embeddingStore, List<TextSegment> textSegmentList) throws Exception {
        EmbeddingModel localEmbeddingModel = EmbeddingModelUtil.getLocalEmbeddingModel();
        if (!CollectionUtils.isEmpty(textSegmentList)) {
            for (TextSegment textSegment : textSegmentList) {
                String text = textSegment.text();
                String segmentId = textSegment.metadata().getString("segmentId");
                Embedding embedding  = localEmbeddingModel.embed(text).content();
                //embeddingStore.add(segmentId, embedding);
                embeddingStore.addAll(Collections.singletonList(segmentId), Collections.singletonList(embedding), textSegment == null ? null : Collections.singletonList(textSegment));
            }
            log.info("异步执行新增向量存储成功");
        }

        ChatKnowledge chatKnowledge = new ChatKnowledge();
        chatKnowledge.setKnowledgeId(knowledgeId);
        chatKnowledge.setIsVector(1);
        chatKnowledgeService.updateChatKnowledge(chatKnowledge);
        log.info("异步修改知识库是否向量化完成为完成状态");
    }
}
