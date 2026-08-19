package com.easylive.agent.controller;

import com.easylive.agent.auth.AgentAuthService;
import com.easylive.agent.service.AgentService;
import com.easylive.entity.vo.UserLoginDto;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final AgentAuthService authService;

    public AgentController(AgentService agentService, AgentAuthService authService) {
        this.agentService = agentService;
        this.authService = authService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        UserLoginDto user = authService.getCurrentUser(httpRequest);
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("请先登录后再使用 AI 助手"));
        }
        return ResponseEntity.ok(new ChatResponse(agentService.chat(
                user.getUserId(), request.getConversationId(), request.getMessage())));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object streamChat(@Valid @RequestBody ChatRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        UserLoginDto user = authService.getCurrentUser(httpRequest);
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("请先登录后再使用 AI 助手"));
        }

        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("X-Accel-Buffering", "no");
        httpResponse.setHeader("Connection", "keep-alive");
        SseEmitter emitter = new SseEmitter(120000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitter.complete());
        try {
            agentService.streamChat(
                    user.getUserId(),
                    request.getConversationId(),
                    request.getMessage(),
                    content -> send(emitter, new StreamChunk("content", content)),
                    () -> {
                        send(emitter, new StreamChunk("done", ""));
                        emitter.complete();
                    },
                    error -> {
                        send(emitter, new StreamChunk("error", "AI助手处理失败，请稍后再试"));
                        emitter.completeWithError(error);
                    });
        } catch (Exception exception) {
            send(emitter, new StreamChunk("error", "AI助手处理失败，请稍后再试"));
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private void send(SseEmitter emitter, StreamChunk chunk) {
        try {
            emitter.send(SseEmitter.event().name(chunk.getType()).data(chunk));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    @Data
    public static class ChatRequest {
        @NotBlank
        private String message;

        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{16,80}", message = "会话编号格式不正确")
        private String conversationId;
    }

    @Data
    public static class ChatResponse {
        private final String content;
    }

    @Data
    public static class StreamChunk {
        private final String type;
        private final String content;
    }

    @Data
    private static class ErrorResponse {
        private final boolean success = false;
        private final String message;

        private ErrorResponse(String message) {
            this.message = message;
        }
    }
}
