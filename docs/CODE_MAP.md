# 代码阅读地图

Java 根目录：`D:\LearnFlow\backend\src\main\java\com\learnflow`。不要从 Getter 开始逐文件看，选一条业务链。

## 阅读顺序

1. 任务：`frontend/src/views/TasksView.vue` → `api.ts` → `task/TaskController.java` → `TaskService.java` → `TaskMapper.java`。
2. 登录：`stores/auth.ts` → `user/AuthController.java` → `config/SecurityConfig.java` → `user/UserService.java`。
3. 笔记：`NotesView.vue` → `note/NoteController.java` → `NoteService.java` → `NoteMapper.java`。
4. 缓存：`stats/StatsController.java` → `StatsCache.java` → 任务 Service 发布的 `TaskChanged`。
5. 索引：NoteMapper 更新内容与 PENDING → `ai/NoteIndexWorker.java` → `AiGateway.java` → `ai-service/app/main.py` → `vector_store.py`。
6. 问答：`AskView.vue` → `AiController.java` → `AiGateway.java` → `agent.py` → `vector_store.py`。

## 关键理解

**用户是谁**：登录后显式保存 SecurityContext，并更换会话 ID。Controller 的 LoginUser 来自后端认证。
SQL 同时限制 id 与 user_id，不能相信前端提交的 userId。DTO 不返回密码哈希。

**CSRF**：会话 Cookie 自动携带，写请求另外带 Token。登录/退出后轮换 Token，前端重新获取，不在 localStorage 保存登录令牌。

**事务与缓存**：任务先提交 MySQL，AFTER_COMMIT 事件才清缓存。按用户分 key，TTL 60 秒，故障回数据库。
并发回填或缓存失效失败可能短时旧值，TTL 限定寿命，不宣称强一致性。

**笔记与索引**：内容、revision、PENDING 同事务提交，Python 离线不阻止保存。
后台重试延迟 10/20/40/80 秒，总计 5 次后停，用户可手动重试。
成功回写必须匹配 revision，旧结果不能覆盖新版本状态。

**删除与 AI**：Java 实时生成当前账号 noteId/revision 白名单；Python 再过滤 user_id 和版本。
刚删除/更新、还未清理的旧向量不会进入模型上下文。
Python 保留删除版本墓碑，晚到旧索引不能恢复已删内容。
当前面向单 Java 实例、单 Uvicorn worker；横向扩容需额外协调，未提前引入。

**Agent**：检索 → 证据 → 回答。本地用稳定哈希向量返回摘录；模型用 LangChain `prompt | model | parser`。
没有执行命令或写数据库的工具，笔记中的指令当成不可信数据。

**Markdown**：marked 转换后用 DOMPurify 清理，笔记与模型输出复用组件，避免原始 HTML 直接插入页面。

## 提问方式

例如：“NoteIndexWorker 的 revision 条件为什么不能省略？”
提供文件和方法即可，再围绕该段的业务、数据流、取舍解释。
