package com.ruoyi.operator;

import com.ruoyi.domain.ChatKnowledge;
import com.ruoyi.domain.ChatProject;
import com.ruoyi.vo.QueryVo;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface AiOperator {


    /**
     * 流式输出文本问答
     *
     * @param chatProject
     * @param queryVo QueryVo
     * @return
     */
    default Flux<String> chatStream(ChatProject chatProject, QueryVo queryVo) throws Exception {
        throw new UnsupportedOperationException();
    }



    /**
     * 图像生成，返回url
     * @param queryVo QueryVo
     * @return
     */
    default String imageUrl(QueryVo queryVo){
        throw new UnsupportedOperationException();
    }

    /**
     * 图像生成，返回base64Json
     * @param queryVo QueryVo
     * @return
     */
    default String imageBase64Json(QueryVo queryVo){
        throw new UnsupportedOperationException();
    }

    /**
     * 文本转音频，返回url
     * @param queryVo QueryVo
     * @return
     */
    default String textToAudio(QueryVo queryVo){
        throw new UnsupportedOperationException();
    }



    /**
     * 上传本地知识库
     *
     * @param chatProject
     * @param knowledgeId
     * @param file
     * @return
     */
    Boolean upload(ChatProject chatProject, ChatKnowledge chatKnowledge, MultipartFile file) throws Exception;

    /**
     * 移除本地知识库
     * @param docId
     * @return
     */
//    Boolean remove(String docId);
    void remove(ChatProject project,String docId) throws Exception;


    /**
     * 移除本地知识库
     * @param knowledgeId
     * @return
     */
//    Boolean remove(String docId);
    void removeByknowledgeId(ChatProject project,String knowledgeId) throws Exception;


}
