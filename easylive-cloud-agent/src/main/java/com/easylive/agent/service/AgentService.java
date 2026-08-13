package com.easylive.agent.service;

import com.easylive.agent.tool.VideoTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final VideoAssistant assistant;

    public AgentService(ChatModel chatModel, VideoTools videoTools) {
        this.assistant = AiServices.builder(VideoAssistant.class)
                .chatModel(chatModel)
                .tools(videoTools)
                .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }
}
