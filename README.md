# LearnFlow

学习任务与 AI 知识库管理系统。Vue 3 / TypeScript 前端，Spring Boot / MyBatis 后端，FastAPI / LangChain / Chroma 知识问答。

先按下方步骤启动，再参考[代码阅读地图](docs/CODE_MAP.md)了解代码结构。

## 功能

- 注册、登录、退出、个人资料；BCrypt、会话认证、CSRF。
- 任务 CRUD 和 TODO / IN_PROGRESS / DONE 三态。
- Markdown 笔记、分类、搜索、分页、预览。
- 任务统计、完成率、最近笔记；Redis 缓存与提交后失效。
- 笔记异步索引、失败重试、知识问答、可点击笔记引用。
- 任务、笔记、统计、AI 检索全部按登录用户隔离。
- Windows 启停脚本与 Docker Compose。

AI 默认是**本地关键词向量检索**，无需模型密钥，返回真实笔记摘录。配置模型 API 后可使用大模型生成回答，不把摘录冒充成模型生成。

## 本机运行

本机需 MySQL、JDK 17、Maven、Node 24、Python 3.12。Redis 默认关闭，普通业务无需 Redis 或模型 API。

1. 克隆仓库并进入项目目录。
2. 首次安装或重新构建：

   ```powershell
   .\scripts\setup.ps1
   ```

3. 启动服务：

   ```powershell
   .\scripts\start-local.ps1
   ```

4. 打开 **http://127.0.0.1:5173**，注册自己的账号。
5. 停止：`.\scripts\stop-local.ps1`。

日志在 `work/logs`。启动脚本不会自动停止你的其他程序，端口占用会提示。详情见 [启动指南](docs/STARTUP.md)。

## 原有数据

Flyway 首次启动补齐表结构，不删除旧任务。旧任务归属为 NULL，不自动交给第一个注册者。
需要导入自己账号时，按 `backend/database/003_assign_legacy_tasks.sql` 先查询，再确认账号和任务 id。

## 目录

| 目录 | 内容 |
| --- | --- |
| frontend/src/views | 登录、概览、任务、笔记、问答、个人资料 |
| frontend/src/api.ts | Axios、会话、CSRF |
| backend/src/main/java/com/learnflow | user / task / note / stats / ai |
| backend/src/main/resources/db/migration | 自动迁移 |
| ai-service/app/agent.py | 检索、组织证据、回答 |
| ai-service/app/vector_store.py | Chroma、用户与版本过滤 |
| scripts | 本机安装与启停 |
| deploy | Compose 与 Nginx |
| docs | 配置、接口、阅读地图与验收 |

## Docker / Linux

```bash
docker compose --env-file .env -f deploy/compose.yml up -d --build
```

入口 `http://localhost:8088`。Windows 运行 `scripts/init-env.ps1` 生成随机配置；Linux 按 `.env.example` 创建随机密码和至少 32 字符内部令牌。
Compose 使用独立 MySQL，持久化在项目 `work/docker`；不操作现有本机库。
公网部署先配置 HTTPS，再开启 `COOKIE_SECURE=true`；数据库、Redis、Python 不发布宿主机端口。

测试与部署说明见 [测试指南](docs/TESTING.md) 和 [启动指南](docs/STARTUP.md)。

开发检查入口见 [检查说明](docs/TESTING.md)，平时改动后只运行相关检查即可。
