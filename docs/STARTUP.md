# 启动与配置

## Windows 本地启动

需要 JDK 17、Maven、Node.js、Python、MySQL；Redis 和模型 API 为可选依赖。

首次安装依赖或重新构建：

```powershell
.\scripts\setup.ps1
```

首次启动前，根据 `.env.example` 配置环境变量。Windows 可运行 `scripts/init-env.ps1` 创建包含随机内部令牌与数据库密码的 `.env`。然后启动：

```powershell
.\scripts\start-local.ps1
```

打开 http://127.0.0.1:5173 并注册账号。停止服务：

```powershell
.\scripts\stop-local.ps1
```

日志写入 `work/logs`。启动脚本不会自动停止其他进程；所需端口被占用时会报告错误。

## 环境变量

| 配置 | 用途 |
| --- | --- |
| DB_URL / DB_USERNAME / DB_PASSWORD | MySQL 连接 |
| AI_SERVICE_TOKEN | Java 与 Python 共享的内部令牌，至少 32 字符 |
| AI_PROVIDER=local | 本地检索，不调用模型 API |
| AI_PROVIDER=openai | 启用 OpenAI 兼容的模型 API |
| OPENAI_API_KEY | 模型服务密钥 |
| OPENAI_BASE_URL | 模型 API 基地址 |
| LLM_MODEL / EMBEDDING_MODEL | 聊天模型与向量模型名称 |
| CACHE_ENABLED / REDIS_HOST / REDIS_PORT / REDIS_PASSWORD | 可选 Redis 缓存配置 |

模型模式需要为所选服务配置有效的聊天与向量模型。切换向量模型或服务地址后，重启 Python 服务并在问答页重建索引。只设置 API Key 不会自动切换到模型模式。

## Docker Compose

先按 `.env.example` 创建 `.env`，再运行：

```bash
docker compose --env-file .env -f deploy/compose.yml up -d --build
```

访问 http://localhost:8088。MySQL、Redis 与 Chroma 数据保存在 `work/docker`。公网部署应先配置 HTTPS，并设置 `COOKIE_SECURE=true`。

## 数据迁移

Flyway 会在首次启动时创建或升级表结构。执行迁移前请备份数据库。旧任务的 `user_id` 为 NULL 时不会自动分配给新账号；如需归属，请先核对 `backend/database/003_assign_legacy_tasks.sql` 中的查询结果。
