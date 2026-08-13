package com.easylive.agent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface VideoAssistant {

    @SystemMessage("你是 LiveEasy 用户端视频助手。回答要简洁、真实，不确定时要明确说明。涉及视频搜索、详情或推荐时必须调用对应工具，不要编造视频编号、简介或播放数据。")
    String chat(@UserMessage String message);
}
