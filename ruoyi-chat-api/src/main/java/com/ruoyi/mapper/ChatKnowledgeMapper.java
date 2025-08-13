package com.ruoyi.mapper;

import java.util.List;
import com.ruoyi.domain.ChatKnowledge;

/**
 * 知识库管理Mapper接口
 * 
 * @author lixianfeng
 * @date 2024-06-27
 */
public interface ChatKnowledgeMapper 
{
    /**
     * 查询知识库管理
     * 
     * @param knowledgeId 知识库管理主键
     * @return 知识库管理
     */
    public ChatKnowledge selectChatKnowledgeByKnowledgeId(String knowledgeId);

    /**
     * 查询知识库管理列表
     * 
     * @param chatKnowledge 知识库管理
     * @return 知识库管理集合
     */
    public List<ChatKnowledge> selectChatKnowledgeList(ChatKnowledge chatKnowledge);

    /**
     * 新增知识库管理
     * 
     * @param chatKnowledge 知识库管理
     * @return 结果
     */
    public int insertChatKnowledge(ChatKnowledge chatKnowledge);

    /**
     * 修改知识库管理
     * 
     * @param chatKnowledge 知识库管理
     * @return 结果
     */
    public int updateChatKnowledge(ChatKnowledge chatKnowledge);

    /**
     * 删除知识库管理
     * 
     * @param knowledgeId 知识库管理主键
     * @return 结果
     */
    public int deleteChatKnowledgeByKnowledgeId(String knowledgeId);

    /**
     * 批量删除知识库管理
     * 
     * @param knowledgeIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteChatKnowledgeByKnowledgeIds(Long[] knowledgeIds);
}
