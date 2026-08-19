package com.easylive.agent.memory;

import com.easylive.agent.constant.AgentConstants;
import com.easylive.redis.RedisUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 将 LangChain4j 的短期对话记忆保存到 Redis。 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final RedisUtils redisUtils;

    public RedisChatMemoryStore(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Object value = redisUtils.get(buildKey(memoryId));
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return ChatMessageDeserializer.messagesFromJson((String) value);
        } catch (Exception e) {
            log.warn("读取 AI 助手 Redis 记忆失败，已忽略旧记忆，键名：{}", buildKey(memoryId), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            redisUtils.setex(
                    buildKey(memoryId),
                    json,
                    AgentConstants.AGENT_MEMORY_EXPIRE_MILLIS
            );
        } catch (Exception e) {
            log.error("保存 AI 助手 Redis 记忆失败，键名：{}", buildKey(memoryId), e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisUtils.delete(buildKey(memoryId));
    }

    private String buildKey(Object memoryId) {
        return AgentConstants.REDIS_KEY_AGENT_MEMORY + memoryId;
    }
}
