# Docker Infra Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local Docker Compose stack (Redis, Nacos→本机 MySQL, Elasticsearch, Seata) plus Nacos `*-dev.yml` templates and an import script so IDEA can start the five Spring services after `docker compose up`.

**Architecture:** Infrastructure-only Compose on Windows Docker Desktop; containers reach host MySQL via `host.docker.internal`. Java apps stay on the host, discover Nacos at `127.0.0.1:8848`, and load `*-dev.yml` from Nacos. Misplaced gateway Dockerfile fragment is deleted.

**Tech Stack:** Docker Compose v2, Redis 7, Nacos 2.2.3, Elasticsearch 7.17.21, Seata Server 1.6.1, PowerShell import via Nacos Open API, MySQL 8 on host (`root`/`123456`).

## Global Constraints

- MySQL stays on the host — never containerize it in this plan
- Do not Dockerize gateway/web/admin/interact/resource
- Seata server image must be `1.6.1` (matches `pom.xml` client)
- Nacos image `nacos/nacos-server:v2.2.3`; ES `elasticsearch:7.17.21`; Redis `redis:7-alpine`
- Host ports: Redis `6379`, Nacos `8848`/`9848`, ES `9200`, Seata `8091`
- App ports in Nacos templates: gateway `8080`, web `7070`, admin `7071`, interact `7072`, resource `7073`
- Admin gateway entry: `Path=/admin/**` + `StripPrefix=1` + filter name `Admin`
- ES for apps: `es.host.port=127.0.0.1:9200` (never `elasticsearch:9200` in host IDE configs)
- `.env` must be gitignored; default password in `.env.example` is `123456`
- No git commits unless the user explicitly asks (repo may have no `.git`)

---

## File map

| Path | Responsibility |
|------|----------------|
| `docker-compose.yml` | Orchestrate redis/nacos/elasticsearch/seata-server |
| `.env.example` / `.env` | MySQL credentials for containers |
| `.gitignore` | Ignore `.env` |
| `docker/nacos/application.properties` | Nacos standalone + MySQL store |
| `docker/seata/application.yml` | Seata server DB store + file registry |
| `docker/nacos-config/*-dev.yml` | Per-service Spring config for Nacos |
| `docker/scripts/import-nacos-config.ps1` | Publish YAMLs to Nacos |
| `docker/README.md` | Short local-dev usage |
| delete `easylive-cloud-common/.../tempfile_*.dockerfile` | Cleanup |

---

### Task 1: Cleanup, gitignore, env files

**Files:**
- Delete: `easylive-cloud-common/src/main/java/com/easylive/aspect/tempfile_1775829903030.dockerfile`
- Modify: `.gitignore`
- Create: `.env.example`
- Create: `.env` (local copy; not for commit)

- [ ] **Step 1: Delete misplaced Dockerfile**

Delete file:
`easylive-cloud-common/src/main/java/com/easylive/aspect/tempfile_1775829903030.dockerfile`

- [ ] **Step 2: Append to `.gitignore`**

Add at end of `.gitignore`:

```
### Local Docker ###
.env
```

- [ ] **Step 3: Create `.env.example`**

```env
MYSQL_HOST=host.docker.internal
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=123456
```

- [ ] **Step 4: Create `.env`**

Copy same content as `.env.example` into `.env`.

- [ ] **Step 5: Verify MySQL still accepts credentials**

Run:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --password=123456 --host=127.0.0.1 --protocol=tcp -e "SHOW DATABASES LIKE 'easylive%';"
```

Expected: rows for `easylive` and/or `easylive_nacos`. If missing, run `mysql/init/01_create_db.sql`, `02_nacos_tables.sql`, `03_business_tables.sql` against root before continuing.

---

### Task 2: Nacos + Seata server config files

**Files:**
- Create: `docker/nacos/application.properties`
- Create: `docker/seata/application.yml`

**Interfaces:**
- Consumes: `.env` keys `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD` (substituted in compose when generating runtime env; properties file may use placeholders filled by compose environment or literal `host.docker.internal` + env for user/password)
- Produces: mountable configs for Task 3 containers

- [ ] **Step 1: Create `docker/nacos/application.properties`**

```properties
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://${MYSQL_HOST:host.docker.internal}:${MYSQL_PORT:3306}/easylive_nacos?characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
db.user.0=${MYSQL_USER:root}
db.password.0=${MYSQL_PASSWORD:123456}
db.pool.config.connectionTimeout=30000
db.pool.config.validationTimeout=10000
db.pool.config.maximumPoolSize=20
db.pool.config.minimumIdle=2
```

Note: Official Nacos image does **not** expand `${MYSQL_*}` inside a mounted `application.properties` the same way Spring does. Prefer writing **literal** values in the mounted file and generating them from `.env` in compose via an env-based approach:

**Preferred final content** (compose will pass DB via env vars; use this properties file only for non-secret defaults, OR bake user/password via compose `environment` which Nacos image already supports):

Actually use the Nacos image’s documented env vars in compose (`MYSQL_SERVICE_HOST`, `MYSQL_SERVICE_DB_NAME`, etc.) and keep `application.properties` minimal **or** mount a properties file with literals `host.docker.internal` / `root` / `123456` matching `.env.example`, documented that changing password requires editing both `.env` and this file **or** regenerate properties in the import/setup script.

**Final decision for implementer:** mount `docker/nacos/application.properties` with explicit values matching `.env.example`, and document that password changes must update this file. Also set compose `environment` for redundancy where supported:

```properties
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://host.docker.internal:3306/easylive_nacos?characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
db.user.0=root
db.password.0=123456
```

- [ ] **Step 2: Create `docker/seata/application.yml`**

```yaml
server:
  port: 7091

spring:
  application:
    name: seata-server

logging:
  config: classpath:logback-spring.xml
  file:
    path: ${log.home:${user.home}/logs/seata}

console:
  user:
    username: seata
    password: seata

seata:
  config:
    type: file
  registry:
    type: file
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://host.docker.internal:3306/easylive?useUnicode=true&rewriteBatchedStatements=true&characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
      user: root
      password: "123456"
      min-conn: 5
      max-conn: 100
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
      query-limit: 1000
      max-wait: 5000
  server:
    service-port: 8091
    enable-check-auth: false
```

Password must stay in sync with `.env` (`123456` default).

---

### Task 3: Root `docker-compose.yml`

**Files:**
- Create: `docker-compose.yml`

**Interfaces:**
- Consumes: `.env`, `docker/nacos/application.properties`, `docker/seata/application.yml`
- Produces: running containers on ports 6379, 8848, 9848, 9200, 8091

- [ ] **Step 1: Write `docker-compose.yml`**

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: easylive-redis
    ports:
      - "6379:6379"
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - easylive-redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  elasticsearch:
    image: elasticsearch:7.17.21
    container_name: easylive-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - bootstrap.memory_lock=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    ports:
      - "9200:9200"
    volumes:
      - easylive-es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:9200 >/dev/null || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 20

  nacos:
    image: nacos/nacos-server:v2.2.3
    container_name: easylive-nacos
    environment:
      MODE: standalone
      JVM_XMS: 256m
      JVM_XMX: 512m
      NACOS_AUTH_ENABLE: "false"
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: host.docker.internal
      MYSQL_SERVICE_PORT: ${MYSQL_PORT:-3306}
      MYSQL_SERVICE_DB_NAME: easylive_nacos
      MYSQL_SERVICE_USER: ${MYSQL_USER:-root}
      MYSQL_SERVICE_PASSWORD: ${MYSQL_PASSWORD:-123456}
      MYSQL_SERVICE_DB_PARAM: characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    ports:
      - "8848:8848"
      - "9848:9848"
    volumes:
      - ./docker/nacos/application.properties:/home/nacos/conf/application.properties:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:8848/nacos/ || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 30s

  seata-server:
    image: seataio/seata-server:1.6.1
    container_name: easylive-seata
    environment:
      SEATA_IP: 127.0.0.1
      SEATA_PORT: 8091
    ports:
      - "8091:8091"
      - "7091:7091"
    volumes:
      - ./docker/seata/application.yml:/seata-server/resources/application.yml:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "bash -c 'exec 3<>/dev/tcp/127.0.0.1/8091'"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 20s

volumes:
  easylive-redis-data:
  easylive-es-data:
```

Notes for implementer:
- If Nacos fails because both env and mounted `application.properties` conflict, keep **mounted properties as source of truth** and remove redundant `MYSQL_SERVICE_*` env, or vice versa — prefer one source; recommended: **env vars only** (official image) and **omit** mounting `application.properties` if env works. Spec asked for the properties file — keep mount; if boot fails, drop mount and rely on env.
- `SEATA_IP=127.0.0.1` helps host IDEA clients dial published `8091`.

- [ ] **Step 2: Start stack**

```powershell
docker compose up -d
docker compose ps
```

Expected: redis, elasticsearch, nacos, seata-server running (nacos/es/redis healthy).

- [ ] **Step 3: Smoke-check endpoints**

```powershell
docker compose exec redis redis-cli ping
curl.exe -fsS http://127.0.0.1:9200
curl.exe -fsS -o NUL -w "%{http_code}" http://127.0.0.1:8848/nacos/
```

Expected: `PONG`, ES JSON, Nacos HTTP 200.

If Nacos cannot connect to MySQL, fix password in `.env` + `docker/nacos/application.properties`, recreate nacos container.

---

### Task 4: Nacos config templates (non-gateway)

**Files:**
- Create: `docker/nacos-config/easylive-cloud-web-dev.yml`
- Create: `docker/nacos-config/easylive-cloud-admin-dev.yml`
- Create: `docker/nacos-config/easylive-cloud-interact-dev.yml`
- Create: `docker/nacos-config/easylive-cloud-resource-dev.yml`

**Interfaces:**
- Produces: Data IDs matching `spring.application.name` + `-dev.yml`

Shared YAML fragments (repeat in each file with correct `server.port` and `seata.application-id`):

```yaml
server:
  port: 7070

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/easylive?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

project:
  folder: D:/easylive-data

es:
  host:
    port: 127.0.0.1:9200
  index:
    video:
      name: easylive_video

admin:
  account: admin
  password: admin123

seata:
  enabled: true
  application-id: easylive-cloud-web
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
  registry:
    type: file
  config:
    type: file
  enable-auto-data-source-proxy: true
```

- [ ] **Step 1: Write `easylive-cloud-web-dev.yml`**

Use shared fragment with `server.port: 7070`, `seata.application-id: easylive-cloud-web`. Include `project.folder`, `es.*`, datasource, redis, seata.

- [ ] **Step 2: Write `easylive-cloud-admin-dev.yml`**

Same as web but `server.port: 7071`, `seata.application-id: easylive-cloud-admin`. Keep `admin.account` / `admin.password`. ES optional but harmless to include.

- [ ] **Step 3: Write `easylive-cloud-interact-dev.yml`**

`server.port: 7072`, `seata.application-id: easylive-cloud-interact`. Datasource + redis + seata required. ES not required but may omit `es.*`.

- [ ] **Step 4: Write `easylive-cloud-resource-dev.yml`**

`server.port: 7073`. Resource module has **no datasource** — omit `spring.datasource` and seata (or `seata.enabled: false`). Keep redis + `project.folder` (must match web for shared files).

```yaml
server:
  port: 7073

spring:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0

project:
  folder: D:/easylive-data

seata:
  enabled: false
```

- [ ] **Step 5: Create host folder**

```powershell
New-Item -ItemType Directory -Force -Path "D:\easylive-data"
```

---

### Task 5: Gateway Nacos config

**Files:**
- Create: `docker/nacos-config/easylive-cloud-gateway-dev.yml`

**Interfaces:**
- Consumes: service names `easylive-cloud-web|admin|interact|resource`
- Produces: routes + redis for AdminFilter token checks

- [ ] **Step 1: Write gateway YAML with ordered routes**

Conflict notes from codebase:
- Admin and web both expose `/account`, `/category` → admin only via `/admin/**` + StripPrefix
- web and interact both use `/ucenter/**` → list interact-specific paths first

```yaml
server:
  port: 8080

spring:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_FIRST
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
      routes:
        - id: admin-route
          uri: lb://easylive-cloud-admin
          predicates:
            - Path=/admin/**
          filters:
            - StripPrefix=1
            - name: Admin

        - id: interact-ucenter
          uri: lb://easylive-cloud-interact
          predicates:
            - Path=/ucenter/loadComment,/ucenter/delComment,/ucenter/loadDanmu,/ucenter/delDanmu

        - id: interact-route
          uri: lb://easylive-cloud-interact
          predicates:
            - Path=/comment/**,/danmu/**,/userAction/**,/online/**,/message/**

        - id: resource-route
          uri: lb://easylive-cloud-resource
          predicates:
            - Path=/getResource,/preUploadVideo,/uploadVideo,/delUploadVideo,/uploadImage,/videoResource/**

        - id: web-route
          uri: lb://easylive-cloud-web
          predicates:
            - Path=/account/**,/category/**,/video/**,/uhome/**,/ucenter/**,/sysSetting/**

seata:
  enabled: false
```

Path predicate multi-value syntax must be valid for Spring Cloud Gateway 3.1.x (`Path=/a,/b` is supported).

---

### Task 6: Import script + README

**Files:**
- Create: `docker/scripts/import-nacos-config.ps1`
- Create: `docker/README.md`

- [ ] **Step 1: Write `import-nacos-config.ps1`**

```powershell
param(
  [string]$NacosAddr = "http://127.0.0.1:8848",
  [string]$Group = "DEFAULT_GROUP",
  [string]$ConfigDir = (Join-Path $PSScriptRoot "..\nacos-config")
)

$ErrorActionPreference = "Stop"
$ConfigDir = (Resolve-Path $ConfigDir).Path

Write-Host "Waiting for Nacos at $NacosAddr ..."
for ($i = 0; $i -lt 60; $i++) {
  try {
    $r = Invoke-WebRequest -Uri "$NacosAddr/nacos/" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -ge 200) { break }
  } catch {
    Start-Sleep -Seconds 2
  }
  if ($i -eq 59) { throw "Nacos not ready" }
}

Get-ChildItem -Path $ConfigDir -Filter "*.yml" | ForEach-Object {
  $dataId = $_.Name
  $content = Get-Content -Raw -Path $_.FullName
  $body = @{
    dataId  = $dataId
    group   = $Group
    type    = "yaml"
    content = $content
  }
  Write-Host "Publishing $dataId ..."
  Invoke-RestMethod -Method Post -Uri "$NacosAddr/nacos/v1/cs/configs" -Body $body | Out-Null
}

Write-Host "Done. Open $NacosAddr/nacos  (no auth) to verify configs."
```

- [ ] **Step 2: Write `docker/README.md`**

Include:
1. Prerequisites: Docker Desktop, host MySQL with init scripts, FFmpeg on PATH
2. Copy `.env.example` → `.env`
3. `docker compose up -d`
4. `powershell -File docker/scripts/import-nacos-config.ps1`
5. Create `D:\easylive-data`
6. IDEA start order: resource → web → interact → admin → gateway
7. Gateway URL `http://127.0.0.1:8080`, Nacos `http://127.0.0.1:8848/nacos`

- [ ] **Step 3: Run import**

```powershell
powershell -ExecutionPolicy Bypass -File docker/scripts/import-nacos-config.ps1
```

Expected: five “Publishing …” lines, no throw.

- [ ] **Step 4: Verify in Nacos API**

```powershell
curl.exe -fsS "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=easylive-cloud-web-dev.yml&group=DEFAULT_GROUP"
```

Expected: YAML body containing `server.port`.

---

### Task 7: End-to-end acceptance

**Files:** none (verification only)

- [ ] **Step 1: `docker compose ps`** — all four services up
- [ ] **Step 2: Confirm misplaced dockerfile gone**
- [ ] **Step 3: Confirm five configs exist via API** (gateway/web/admin/interact/resource)
- [ ] **Step 4: Optional IDEA smoke** — start `EasyliveCloudGatewayRunApplication` after other services; watch logs for Nacos pull success and Redis connect. Full business login not required for this Docker plan.

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Compose Redis/Nacos/ES/Seata | Task 3 |
| MySQL host-only + host.docker.internal | Task 2–3 |
| Nacos MySQL `easylive_nacos` | Task 2–3 |
| Seata DB on `easylive` 1.6.1 | Task 2–3 |
| `.env` / `.env.example` | Task 1 |
| Delete tempfile dockerfile | Task 1 |
| Five `*-dev.yml` + ports + ES localhost | Task 4–5 |
| Gateway routes + Admin filter | Task 5 |
| Import script | Task 6 |
| Usage docs | Task 6 |
| Acceptance | Task 7 |

## Placeholder / consistency self-review

- Passwords unified to `123456` across `.env`, nacos properties, seata yml, app templates
- Ports match Global Constraints
- `seata.application-id` differs per service; resource disables seata
- Gateway admin path `/admin/**` avoids `/account` clash with web
