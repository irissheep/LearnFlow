# API 契约

浏览器仅调用 Java /api。除探活、CSRF、注册登录外都需要会话。
写操作携带 GET /api/auth/csrf 返回的 headerName/token；登录后重新获取。

| 方法 | 路径 | 输入/用途 |
| --- | --- | --- |
| GET | /api/health | 探活 |
| GET | /api/auth/csrf | Token |
| POST | /api/auth/register | username、password、nickname，201 |
| POST | /api/auth/login | username、password，200 |
| POST | /api/auth/logout | 204 |
| GET | /api/auth/me | 当前资料 |
| GET / POST | /api/tasks | 列表 / title 新增 |
| GET / PUT / DELETE | /api/tasks/{id} | 详情 / title、status 修改 / 删除 |
| GET / POST | /api/notes | 分页搜索 / title、category、content 新增 |
| GET | /api/notes/categories | 当前账号分类 |
| GET / PUT / DELETE | /api/notes/{id} | 详情 / 修改 / 软删除 |
| POST | /api/notes/{id}/retry-index | 手动重试 |
| GET | /api/stats | 数量、完成率、最近笔记 |
| GET | /api/ai/status | available、mode |
| POST | /api/ai/ask | question → answer、sources、mode |
| POST | /api/ai/reindex | 当前账号全部重建 → queued |

分页参数 page 从 0 开始，size 默认 20/最大 100，keyword 搜索标题正文，category 精确分类。返回 {items,page,size,total}。
标题最多 200，笔记分类最多 50、正文最多 100000。空分类为“未分类”。三态 TODO/IN_PROGRESS/DONE 自由切换。
用户名 4–32 位字母数字下划线、统一小写；密码至少 8 字符、UTF-8 最多 72 字节。完成率 0–100，保留一位小数。
笔记包含 revision、indexStatus、indexError。

错误 ProblemDetail：400 参数，401 登录，403 CSRF，404 不存在/不属于自己，409 用户名冲突，503 AI 不可用。校验错误含 errors 字段，不返回 SQL/堆栈。

Python 内部接口需 X-Internal-Token：
- GET /health：status、mode。
- POST /internal/index：user_id、note_id、revision、title、content。
- POST /internal/delete：user_id、note_id、revision。
- POST /internal/query：user_id、question、notes（note_id/revision 白名单）。

user_id 和白名单由认证后的 Java 生成，浏览器不直接访问 Python。
