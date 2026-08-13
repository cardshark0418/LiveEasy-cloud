# Nacos 配置导出包

这几个文件对应项目启动时从 Nacos 读取的配置，默认使用 `DEFAULT_GROUP`：

| Nacos 配置名 | 对应服务 |
|---|---|
| `easylive-cloud-admin-dev.yml` | 管理服务，7071 |
| `easylive-cloud-gateway-dev.yml` | 网关服务，8080 |
| `easylive-cloud-interact-dev.yml` | 互动服务，7072 |
| `easylive-cloud-resource-dev.yml` | 资源服务，7073 |
| `easylive-cloud-web-dev.yml` | 主站服务，7070 |
| `easylive-cloud-agent-dev.yml` | 用户端 AI 助手，7074 |

导入时请保持以下配置：

- 配置格式：YAML
- 配置分组：`DEFAULT_GROUP`
- 配置名：必须与上表完全一致
- 命名空间：默认命名空间
- 环境：`dev`

注意事项：

1. 这是一份项目内的配置导出文件，不是从当前 Nacos 实例实时拉取的快照。
2. 当前项目配置使用本机地址：MySQL `127.0.0.1:3306`、Redis `127.0.0.1:6379`、ES `127.0.0.1:9200`、Seata `127.0.0.1:8091`。
3. `project.folder` 当前为 `D:/easylive-data`，换电脑时需要修改成新电脑上的实际目录。
4. `docker-compose.yml` 只会启动 Nacos 服务，不会自动把本目录的 YAML 导入 Nacos，需要手动导入这些文件。
5. 文件中包含本地开发账号和数据库密码，部署到公网前应更换密码。

## AI 助手密钥

`easylive-cloud-agent-dev.yml` 中的 `agent.model.api-key` 只保留占位配置，不放真实密钥。

部署或本地启动时，请先把这个文件导入 Nacos，再在 Nacos 控制台修改：

```yaml
agent:
  model:
    api-key: 你的模型密钥
    base-url: https://api.deepseek.com
    model-name: deepseek-v4-flash
    temperature: 0.3
```

保存后重启 `easylive-cloud-agent`。仓库文件只作为给其他部署者使用的模板，实际密钥应保存在各自的 Nacos 配置中，不要提交到 Git。
