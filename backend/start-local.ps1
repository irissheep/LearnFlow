# 旧入口转到完整项目统一启动脚本；原本机数据库配置保留。
& (Join-Path (Split-Path -Parent $PSScriptRoot) 'scripts\start-backend.ps1')
