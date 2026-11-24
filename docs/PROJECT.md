# LearnFlow 项目说明

LearnFlow 使用 Vue 3、TypeScript、Pinia、Spring Boot、MyBatis、Spring Security、FastAPI、LangChain 与 Chroma。

架构：Vue → Spring Boot → MySQL / Redis；Java → 内部令牌 → FastAPI → Chroma / 可选模型 API。
浏览器只访问 Java。MySQL 存业务，Redis 缓存任务统计，Python 管索引与问答。

功能：注册登录退出资料；任务 CRUD/三态；Markdown 笔记分类搜索分页预览；统计；异步索引、重试、知识问答和引用。

设计：
- MyBatis 显式 SQL 用户归属；BCrypt、会话和 CSRF。
- Flyway 保留旧数据，NULL 任务归属不自动分配。
- 笔记内容与索引待办同事务，后台重试；软删除保留清理任务。
- Python 用户过滤、数据库版本白名单、删除墓碑。
- Redis 故障回源，TTL60秒允许短时旧值，不宣称强一致性。
- 本地模式真实关键词向量检索；模型模式需自己的 API 配置。
- 不做上传/PDF/管理员/邮箱找回/Kubernetes/自主执行工具。
- 本地配置与运行数据不纳入源码仓库；Docker 数据放在 work/docker。
- 普通单实例，重启后需重新登录。

README 负责入口，STARTUP 负责配置，CODE_MAP 负责阅读，API 负责契约，TESTING 说明测试方法。
