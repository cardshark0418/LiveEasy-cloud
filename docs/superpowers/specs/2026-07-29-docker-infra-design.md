# LiveEasy Cloud — Docker 基础设施设计

**日期:** 2026-07-29  
**状态:** 待用户确认后实施  
**范围:** 本地开发用 Docker Compose（不含 MySQL、不含 Java 微服务镜像）

## 1. 目标

`docker compose up -d` 后，本机只需：

1. 本机 MySQL 已初始化（`mysql/init`）
2. 导入 Nacos 配置
3. 在 IDEA 启动 gateway / web / admin / interact / resource

即可联调。

**明确不做**

- MySQL 容器化（继续本机部署）
- 五个 Spring Boot 服务打进 Docker
- 生产级高可用 / 安全加固

## 2. 目录结构

```
LiveEasy-cloud-main/
├── docker-compose.yml
├── .env.example                 # 模板，可提交
├── .env                         # 本地密钥，gitignore，不提交
├── docker/
│   ├── nacos/
│   │   └── application.properties
│   ├── seata/
│   │   └── application.yml      # Seata Server 1.6.1
│   ├── nacos-config/
│   │   ├── easylive-cloud-gateway-dev.yml
│   │   ├── easylive-cloud-web-dev.yml
│   │   ├── easylive-cloud-admin-dev.yml
│   │   ├── easylive-cloud-interact-dev.yml
│   │   └── easylive-cloud-resource-dev.yml
│   └── scripts/
│       └── import-nacos-config.ps1
└── mysql/init/                  # 已有，本机执行
```

**清理:** 删除误放的  
`easylive-cloud-common/src/main/java/com/easylive/aspect/tempfile_1775829903030.dockerfile`

**`.gitignore`:** 增加 `.env`（若尚未忽略）

## 3. 什么是 `.env`

`.env` 是 Docker Compose 自动加载的**环境变量文件**，放在 `docker-compose.yml` 同级目录。

| 文件 | 作用 |
|------|------|
| `.env.example` | 给别人看的模板（无真实密码或占位说明），可进仓库 |
| `.env` | 你本机真实值；Compose 用 `${MYSQL_PASSWORD}` 等替换；**不要提交** |

本方案用它存放：本机 MySQL 账号密码、可选的 `PROJECT_FOLDER` 等。  
Java 服务仍从 Nacos 读配置；`.env` 主要给 **Nacos / Seata 容器连本机 MySQL** 用。

实测本机 MySQL：`127.0.0.1:3306`，`root` / **`123456`**（空密码与 `208989` 均不可用）。  
`.env.example` 默认写入该密码，用户可按需改。

## 4. Compose 服务

| 服务 | 镜像 | 宿主机端口 | 用途 |
|------|------|------------|------|
| redis | `redis:7-alpine` | 6379 | 缓存 / token / 队列 |
| nacos | `nacos/nacos-server:v2.2.3` | 8848, 9848 | 注册与配置中心 |
| elasticsearch | `elasticsearch:7.17.21` | 9200 | 视频搜索 |
| seata-server | `seataio/seata-server:1.6.1` | 8091 | 分布式事务（对齐客户端 1.6.1） |

### 4.1 连本机 MySQL

容器内 JDBC 主机：`host.docker.internal`（Docker Desktop for Windows）。

- Nacos → `easylive_nacos`
- Seata store → `easylive`（已有 `global_table` / `branch_table` / `lock_table` / `distributed_lock`）

变量来自 `.env`：`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_PORT`（默认 3306）。

### 4.2 Nacos

- 模式：`MODE=standalone`
- 挂载 `docker/nacos/application.properties`，配置 MySQL 数据源
- 健康检查：HTTP `8848/nacos/`

### 4.3 Elasticsearch

- `discovery.type=single-node`
- 关闭 xpack security（本地开发）
- `ES_JAVA_OPTS=-Xms512m -Xmx512m`

### 4.4 Seata Server

- 版本 1.6.1，与 `pom.xml` 中 `seata-spring-boot-starter` 一致
- store mode：`db`，库 `easylive`
- registry：优先注册到 Nacos（`127.0.0.1` 对容器则用服务名 `nacos:8848`），便于客户端发现；客户端侧配置写 `127.0.0.1:8091` 或 Nacos 发现（实施时二选一并写死一种，推荐客户端直连 `127.0.0.1:8091` 以降低 IDEA 本地复杂度）
- `depends_on` Nacos healthy 后再启动

### 4.5 Redis

- 无密码（仅本地）
- 可选 AOF，非必须

## 5. Nacos 业务配置模板

Data ID = 文件名，Group = `DEFAULT_GROUP`，对应 `spring.profiles.active=dev`。

### 5.1 公共约定（IDEA 在宿主机）

| 项 | 值 |
|----|-----|
| MySQL | `jdbc:mysql://127.0.0.1:3306/easylive?...`，用户/密码与 `.env` 一致 |
| Redis | `127.0.0.1:6379` |
| ES | `es.host.port=127.0.0.1:9200`（覆盖默认 `elasticsearch:9200`） |
| Seata | `seata.enabled=true`，server `127.0.0.1:8091`；`undo_log` 在业务库 |
| `project.folder` | 默认 `D:/easylive-data`（可在模板注释中说明需自建目录） |
| `admin.account` / `admin.password` | 保持代码默认 `admin` / `admin123` 或显式写出 |

### 5.2 建议服务端口

| 服务 | port |
|------|------|
| gateway | 8080 |
| web | 7070 |
| admin | 7071 |
| interact | 7072 |
| resource | 7073 |

### 5.3 Gateway 路由

`spring.cloud.gateway.routes` 指向：

- `lb://easylive-cloud-web`
- `lb://easylive-cloud-admin`
- `lb://easylive-cloud-interact`
- `lb://easylive-cloud-resource`

路径前缀与现有 Controller 习惯对齐（如用户侧 `/account`、`/video` 等走 web；管理端走 admin；文件/HLS 走 resource）。实施计划中按现有 Controller 的 `@RequestMapping` 核对后写死路由，避免猜测错误路径。

### 5.4 导入脚本

`docker/scripts/import-nacos-config.ps1`：

1. 等待 Nacos `8848` 可用
2. 对 `docker/nacos-config/*.yml` 调用 Nacos Open API 发布/更新
3. 可重复执行（幂等更新）

## 6. 使用流程

1. 本机执行 `mysql/init/01_create_db.sql`、`02_nacos_tables.sql`、`03_business_tables.sql`（若未执行）
2. 复制 `.env.example` → `.env`，确认密码
3. `docker compose up -d`
4. 执行 `docker/scripts/import-nacos-config.ps1`
5. 本机安装 FFmpeg（web 转码需要，不在 Compose 内）
6. 创建 `project.folder` 目录
7. IDEA 启动顺序建议：resource → web → interact → admin → gateway

## 7. 风险与约束

- **无 Nacos 配置则 IDEA 起不来**：必须跑导入脚本或手工粘贴模板
- **Seata 配置缺失**时 `@GlobalTransactional` 可能启动失败或静默异常，模板必须带最小可用 Seata 客户端段
- **ES 默认主机**在代码里是 `elasticsearch:9200`，模板必须覆盖为 `127.0.0.1:9200`
- 仓库当前**不是 git 仓库**，设计文档无法自动 commit；需要用户自行初始化 git 或接受仅落盘

## 8. 验收标准

- [ ] `docker compose ps` 四个服务均为 running/healthy
- [ ] 浏览器可开 `http://127.0.0.1:8848/nacos`
- [ ] 导入后 Nacos 中可见 5 个 `*-dev.yml`
- [ ] IDEA 启动 gateway 后，访问 `8080` 有网关响应（具体业务接口视路由而定）
- [ ] 误放的 tempfile Dockerfile 已删除
- [ ] `.env` 被 gitignore（若后续初始化 git）
