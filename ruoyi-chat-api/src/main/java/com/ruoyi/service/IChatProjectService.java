package com.ruoyi.service;

import java.util.List;
import com.ruoyi.domain.ChatProject;

/**
 * 项目配置Service接口
 * 
 * @author lixianfeng
 * @date 2024-06-27
 */
public interface IChatProjectService 
{
    /**
     * 查询项目配置
     * 
     * @param projectId 项目配置主键
     * @return 项目配置
     */
    public ChatProject selectChatProjectByProjectId(String projectId);

    /**
     * 查询项目配置列表
     * 
     * @param chatProject 项目配置
     * @return 项目配置集合
     */
    public List<ChatProject> selectChatProjectList(ChatProject chatProject);

    /**
     * 新增项目配置
     * 
     * @param chatProject 项目配置
     * @return 结果
     */
    public int insertChatProject(ChatProject chatProject);

    /**
     * 修改项目配置
     * 
     * @param chatProject 项目配置
     * @return 结果
     */
    public int updateChatProject(ChatProject chatProject);

    /**
     * 批量删除项目配置
     * 
     * @param projectIds 需要删除的项目配置主键集合
     * @return 结果
     */
    public int deleteChatProjectByProjectIds(String[] projectIds);

    /**
     * 删除项目配置信息
     * 
     * @param projectId 项目配置主键
     * @return 结果
     */
    public int deleteChatProjectByProjectId(String projectId);
}
