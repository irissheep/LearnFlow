. "$PSScriptRoot\common.ps1"
Set-Location -LiteralPath (Join-Path $LearnFlowRoot 'frontend')
& npm.cmd run dev
exit $LASTEXITCODE
