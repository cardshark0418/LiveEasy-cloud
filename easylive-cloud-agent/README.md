# EasyLive 用户端 AI 助手

这是一个独立的 Java 17 微服务，使用 LangChain4j 连接大模型，并通过工具调用现有视频服务。

## 当前能力

- `POST /agent/chat`：普通聊天接口
- 根据用户意图调用视频搜索工具
- 注册到 Nacos 服务名 `easylive-cloud-agent`
- 端口：7074

## 启动

先准备 Java 17 或更高版本，并配置模型密钥：

```powershell
$env:AGENT_MODEL_API_KEY="你的模型密钥"
$env:AGENT_MODEL_BASE_URL="https://api.deepseek.com"
$env:AGENT_MODEL_NAME="deepseek-v4-flash"
```

也可以把 `agent.model` 配置直接写入 Nacos 中的 `easylive-cloud-agent-dev.yml`。仓库里的同名文件只保留占位符，不包含真实密钥。

然后在本目录执行：

```powershell
mvn spring-boot:run
```

当前仓库没有 Maven Wrapper，也可以使用 IDEA 直接运行 `EasyLiveAgentApplication`。

## 测试

先启动 `EasyLiveAgentApplication`，然后在 IDEA 中运行：

```text
src/test/java/com/easylive/agent/AgentChatApiTest.java
```

也可以在 IDEA 的运行配置中给这个测试类设置程序参数，例如：

```text
帮我找一些动画短片
```

默认请求地址是 `http://127.0.0.1:7074/agent/chat`，需要修改时可以增加虚拟机参数：

```text
-Dagent.chat.url=http://127.0.0.1:7074/agent/chat
```

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://127.0.0.1:7074/agent/chat `
  -ContentType "application/json" `
  -Body '{"message":"帮我找一些动画短片"}'
```

## 下一步

- 把视频搜索工具改为 Feign 或网关内部调用
- 增加 SSE 流式输出
- 增加 MySQL 对话记录
- 增加 Redis 短期记忆
- 增加用户登录身份传递和权限校验
