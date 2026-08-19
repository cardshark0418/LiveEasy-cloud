package com.easylive.agent.service;

import com.easylive.agent.tool.VideoTools;
import com.easylive.agent.memory.RedisChatMemoryStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class AgentService {

    private final VideoAssistant assistant;
    private final StreamingVideoAssistant streamingAssistant;

    public AgentService(ChatModel chatModel, StreamingChatModel streamingChatModel,
                        VideoTools videoTools, RedisChatMemoryStore memoryStore) {
        this.assistant = AiServices.builder(VideoAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(memoryStore)
                        .build())
                .tools(videoTools)
                .build();
        this.streamingAssistant = AiServices.builder(StreamingVideoAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(memoryStore)
                        .build())
                .tools(videoTools)
                .build();
    }

    public String chat(String userId, String conversationId, String message) {
        String memoryId = userId + ":" + conversationId;
        return assistant.chat(memoryId, message);
    }

    public void streamChat(String userId, String conversationId, String message,
                           Consumer<String> onNext, Runnable onComplete,
                           Consumer<Throwable> onError) {
        String memoryId = userId + ":" + conversationId;
        TokenStream tokenStream = streamingAssistant.chat(memoryId, message)
                .onPartialResponse(onNext)
                .onCompleteResponse(response -> onComplete.run())
                .onError(onError);
        tokenStream.start();
    }
}
