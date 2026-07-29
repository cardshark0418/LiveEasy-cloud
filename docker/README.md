# EasyLive Docker stack

Local Redis, Elasticsearch, Nacos, and Seata for EasyLive cloud development.

## Prerequisites

- Docker Desktop
- Host MySQL with EasyLive init scripts applied (Nacos DB `easylive_nacos`, app schemas as needed)
- FFmpeg on `PATH` (for local media/transcode work outside containers)

## Setup

1. Copy env file at the repo root:

   ```powershell
   copy .env.example .env
   ```

   Edit `.env` if MySQL credentials differ from the defaults.

2. Create the local data directory used by app services:

   ```powershell
   mkdir D:\easylive-data -Force
   ```

3. Start the stack from the repo root:

   ```powershell
   docker compose up -d
   ```

4. Import Nacos application configs (five YAML files under `docker/nacos-config/`):

   ```powershell
   powershell -ExecutionPolicy Bypass -File docker/scripts/import-nacos-config.ps1
   ```

## URLs

| Service | URL |
|---------|-----|
| Gateway | http://127.0.0.1:8080 |
| Nacos console | http://127.0.0.1:8848/nacos (no auth) |

## Start apps in IDEA

Recommended order: **resource → web → interact → admin → gateway**.

## Notes (Windows Docker Desktop)

- Nacos compose sets `JAVA_OPT=-XX:-UseContainerSupport`. If the Nacos container OOMs or fails to start under Docker Desktop on Windows, keep this flag (or tune `JVM_XMS` / `JVM_XMX` in `docker-compose.yml`).
- Gateway Admin filter: YAML uses `name: AdminFilter` (matches the `AdminFilter` filter class).

## Verify Nacos config

```powershell
curl.exe -fsS "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=easylive-cloud-web-dev.yml&group=DEFAULT_GROUP"
```

You should see YAML containing `server.port`.
