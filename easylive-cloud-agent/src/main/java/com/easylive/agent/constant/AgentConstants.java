package com.easylive.agent.constant;

/** AI 助手专用常量。键名沿用项目 easylive: 前缀规范。 */
public final class AgentConstants {

    private AgentConstants() {
    }

    public static final String REDIS_KEY_AGENT_MEMORY = "easylive:agent:memory:";

    /** 短期记忆保存 30 天，期间每次更新都会重新计时。 */
    public static final long AGENT_MEMORY_EXPIRE_MILLIS = 30L * 24 * 60 * 60 * 1000;
}
