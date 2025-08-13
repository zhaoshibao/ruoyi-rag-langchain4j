package com.ruoyi.controller;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.pojo.Chat;
import com.ruoyi.pojo.Message;
import com.ruoyi.service.AiService;
import com.ruoyi.vo.ChatVo;
import com.ruoyi.vo.MessageVo;
import com.ruoyi.vo.QueryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@Tag(name = "AI大模型交互")
@RequestMapping("ai")
@Slf4j
public class ChatController extends BaseController {


    @Autowired
    private AiService aiService;


    @Operation(summary = "文本问答")
    @PostMapping(value = "chat-stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody @Valid QueryVo queryVo) {

        try {
            return aiService.chatStream(queryVo);
        } catch (Exception e) {
            log.error("AI大模型交互-文本文答接口异常", e);
            throw new RuntimeException(e);
        }
    }



    @Operation(summary = "创建新的会话")
    @PostMapping("create-chat")
    public String createChat(@Valid @RequestBody ChatVo chatVo){
        Long userId = getUserId();
        chatVo.setUserId(userId);
        return this.aiService.createChat(chatVo);
    }

    @Operation(summary = "修改会话标题")
    @PostMapping("update-chat")
    public AjaxResult updateChat(@RequestBody ChatVo chatVo){
        this.aiService.updateChat(chatVo);
        return success();
    }

    @Operation(summary = "查询会话列表")
    @GetMapping("list-chat")
    public AjaxResult listChat(String projectId){
        Long userId = getUserId();
        AjaxResult ajaxResult = this.aiService.listChat(projectId, userId);
        log.info("查询会话列表:{}", JSON.toJSONString(ajaxResult.get("data")));
        return ajaxResult;
    }

    @Operation(summary = "删除一个会话")
    @GetMapping("delete-chat")
    public AjaxResult deleteChat(String projectId, Long chatId){
        this.aiService.deleteChat(projectId, chatId);
        return success();
    }

    @Operation(summary = "查询一个会话中的问答消息")
    @GetMapping("list-msg")
    public AjaxResult listMsg(Long chatId){
        return this.aiService.listMsg(chatId);
    }

    @Operation(summary = "如果需要保存AI回答的结果，调用此接口")
    @PostMapping("save-msg")
    public AjaxResult saveMsg(@Valid @RequestBody MessageVo messageVo){
        this.aiService.saveMsg(messageVo);
        return success();
    }




    public record CompletionRequest(String text, int cursorPosition) {}
    public record CompletionResponse(String completion, String fullText) {}




}
