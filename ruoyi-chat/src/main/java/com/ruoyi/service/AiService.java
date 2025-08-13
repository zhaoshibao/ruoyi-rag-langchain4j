package com.ruoyi.service;

import cn.hutool.core.util.IdUtil;
import com.mongodb.client.result.UpdateResult;
import com.ruoyi.annotation.BeanType;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.component.QdrantVectorStoreComponet;
import com.ruoyi.domain.ChatKnowledge;
import com.ruoyi.domain.ChatProject;
import com.ruoyi.operator.AiOperator;
import com.ruoyi.pojo.Chat;
import com.ruoyi.pojo.ChatList;
import com.ruoyi.pojo.Message;
import com.ruoyi.utils.MongoUtil;
import com.ruoyi.vo.ChatVo;
import com.ruoyi.vo.MessageVo;
import com.ruoyi.vo.QueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AiService implements ApplicationContextAware {

    // 策略模式 的 bean容器
    private final Map<String, AiOperator> MAP = new ConcurrentHashMap<>();

    @Autowired
    private IChatProjectService projectService;

    @Autowired
    private IChatKnowledgeService knowledgeService;

    @Autowired
    private IChatFileSegmentService fileSegmentService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QdrantVectorStoreComponet qdrantVectorStoreComponet;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(BeanType.class);
        if (CollectionUtils.isEmpty(beanMap)) {
            return;
        }
        // 遍历放入Map集合
        beanMap.values().forEach(bean -> {
            BeanType beanType = bean.getClass().getAnnotation(BeanType.class);
            String model = beanType.value().getType();
            MAP.put(model, (AiOperator) bean);
        });
    }

    private AiOperator getAiOperator(String type) {
        return MAP.get(type);
    }



    public Flux<String> chatStream(QueryVo queryVo) throws Exception {
        // 根据项目id查询项目，获取类型 及 具体模型
        ChatProject chatProject = this.projectService.selectChatProjectByProjectId(queryVo.getProjectId());
        return this.getAiOperator(chatProject.getType()).chatStream(chatProject, queryVo);
    }


    @Transactional("transactionManager")
    public String upload(ChatKnowledge chatKnowledge, MultipartFile file) throws Exception {

        // 根据项目id查询项目，获取类型 及 具体模型
        ChatProject chatProject = this.projectService.selectChatProjectByProjectId(chatKnowledge.getProjectId());
        // 获取文件名 及 内容
        String filename = file.getOriginalFilename();
        //String content = FileUtil.getContentFromText(file);

        // 把知识库记录保存到mysql
        chatKnowledge.setProjectId(chatProject.getProjectId());
        chatKnowledge.setFileName(filename);
        //chatKnowledge.setContent(content);
        String  knowledgeId = UUID.randomUUID().toString();
        chatKnowledge.setKnowledgeId(knowledgeId);
        this.knowledgeService.insertChatKnowledge(chatKnowledge);


        // 上传到redis向量数据库
       //this.getAiOperator(chatProject.getType()).upload(chatProject, knowledgeId, content);
        this.getAiOperator(chatProject.getType()).upload(chatProject, chatKnowledge, file);
        return knowledgeId;
    }

    @Transactional("transactionManager")
    public Boolean remove(String projectId,String  knowledgeId) throws Exception {
        // 根据项目id查询项目，获取类型 及 具体模型
        ChatProject project = this.projectService.selectChatProjectByProjectId(projectId);
        // 删除知识库记录
        this.knowledgeService.deleteChatKnowledgeByKnowledgeId(knowledgeId);
        //删除文件分片
        this.fileSegmentService.deleteChatFileSegmentByKnowledgeId(knowledgeId);
        // 删除redis向量数据库中对应的文档
        this.getAiOperator(project.getType()).removeByknowledgeId(project,knowledgeId.toString());
        return true;
    }



    public String createChat(ChatVo chatVo) {
        Chat chat = new Chat();
        BeanUtils.copyProperties(chatVo, chat);
        chat.setCreateTime(new Date());
        Long chatId = IdUtil.getSnowflake().nextId();
        chat.setChatId(chatId);
        this.mongoTemplate.insert(chat, MongoUtil.getChatCollection(chatVo.getProjectId()));
        return chatId.toString();
    }

    public AjaxResult listChat(String projectId, Long userId) {
        List<ChatList> result = new ArrayList<>();
        List<Chat> chatList = this.mongoTemplate.find(
                Query.query(Criteria.where("userId").is(userId)).with(Sort.by(Sort.Order.desc("createTime"))),
                Chat.class,
                MongoUtil.getChatCollection(projectId));
        if (CollectionUtils.isEmpty(chatList)) {
            return AjaxResult.success(result);
        }
        result = chatList.stream().map(chat -> {
            ChatList chatList1 = new ChatList();
            BeanUtils.copyProperties(chat,chatList1);
            chatList1.setChatId(chat.getChatId().toString());
            return chatList1;
        }).collect(Collectors.toList());

        return AjaxResult.success(result);

    }

    public void updateChat(ChatVo chatVo) {
        if (chatVo == null || chatVo.getProjectId() == null) {
            throw new RuntimeException("projectId不能为空");
        }
        UpdateResult result = this.mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(chatVo.getChatId())),
                Update.update("title", chatVo.getTitle()),
                MongoUtil.getChatCollection(chatVo.getProjectId())
        );
    }

    public void deleteChat(String projectId, Long chatId) {
        if (projectId == null || chatId == null) {
            throw new RuntimeException("projectId或chatId不能为空");
        }
        this.mongoTemplate.remove(Query.query(Criteria.where("_id").is(chatId)), MongoUtil.getChatCollection(projectId));
    }

    public AjaxResult listMsg(Long chatId) {
        if (chatId == null) {
            throw new RuntimeException("chatId不能为空");
        }
        List<Message> messageList = this.mongoTemplate.find(
                Query.query(Criteria.where("chatId").is(chatId)).with(Sort.by(Sort.Order.asc("createTime"))),
                Message.class,
                MongoUtil.getMessageCollection(chatId)
        );
        return AjaxResult.success(messageList);
    }

    public void saveMsg(MessageVo messageVo) {
        Message message = new Message();
        BeanUtils.copyProperties(messageVo, message);
        message.setCreateTime(new Date());
        message.setType(1);
        message.setId(IdUtil.getSnowflake().nextId());
        this.mongoTemplate.insert(message, MongoUtil.getMessageCollection(messageVo.getChatId()));
    }



}
