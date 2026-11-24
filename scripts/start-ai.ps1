. "$PSScriptRoot\common.ps1"
$python = Join-Path $LearnFlowRoot 'ai-service\.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python)) { throw 'Run scripts/setup.ps1 first.' }
$env:CHROMA_DIR = Join-Path $LearnFlowWork 'chroma'
Set-Location -LiteralPath (Join-Path $LearnFlowRoot 'ai-service')
& $python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
exit $LASTEXITCODE
