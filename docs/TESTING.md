# 开发检查

日常不用每改一行就全量测试。修改业务边界时运行相关用例；准备交付时再检查完整流程。
以下命令均在 PowerShell 执行，项目数据与缓存写入 D 盘。

## 常规检查

```powershell
cd D:\LearnFlow
. .\scripts\common.ps1
Set-LearnFlowJava

cd D:\LearnFlow\backend
mvn test

cd D:\LearnFlow\ai-service
.\.venv\Scripts\python.exe -m pytest --basetemp D:\LearnFlow\work\pytest-temp

cd D:\LearnFlow\frontend
npm run build
```

Java 常规测试使用隔离 H2，通过真实 HTTP 检查会话、CSRF、CRUD 与用户归属，不连接本机 MySQL。
Python 测试使用真实 Chroma 和临时向量目录，不调用收费模型；其中一个用例只验证模型模式能正确装配。
未提供专用环境变量时，Java 的 Redis / Python 实联用例会跳过。

## Redis 实联

```powershell
cd D:\LearnFlow\backend
$env:LEARNFLOW_REDIS_BINARY = 'E:\redis\redis-server.exe'
mvn -Dtest=CacheBridgeTest test
Remove-Item Env:LEARNFLOW_REDIS_BINARY
```

用例自行启动独立 Redis 随机端口，检查 TTL、提交后失效与断线回源，再停止自己创建的实例。
它不会停止已运行的 Redis 服务。

## Java 与 Python 实联

第一个终端启动专用测试服务，避免混入日常知识库：

```powershell
cd D:\LearnFlow
. .\scripts\common.ps1
$env:AI_SERVICE_TOKEN = 'test-internal-token-for-learnflow-123456'
$env:AI_PROVIDER = 'local'
$env:CHROMA_DIR = 'D:\LearnFlow\work\ai-test-' + [guid]::NewGuid().ToString('N')
cd D:\LearnFlow\ai-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 18002
```

第二个终端：

```powershell
cd D:\LearnFlow
. .\scripts\common.ps1
Set-LearnFlowJava
$env:LEARNFLOW_AI_TEST_URL = 'http://127.0.0.1:18002'
cd D:\LearnFlow\backend
mvn -Dtest=AiBridgeTest test
Remove-Item Env:LEARNFLOW_AI_TEST_URL
```

完成后在第一个终端按 Ctrl+C。该用例覆盖索引、引用、用户隔离、编辑后排除旧版本和删除清理。
固定测试令牌只用于此独立测试进程，不能替代日常 .env 中生成的随机令牌。

## 本次整体验收

已使用独立 MySQL 8.0.43、打包 JAR、Python 服务和 Chrome，实际操作注册登录、任务三态、笔记预览、后台索引、问答引用、统计、删除和移动端退出。
测试数据库位于 work/browser-verification-*，不连接用户现有库；旧表迁移保留测试旧任务且不自动分配归属。
Docker、模型服务与云端部署需要在目标环境单独验收。
