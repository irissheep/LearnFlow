. "$PSScriptRoot\common.ps1"
$recordFile = Join-Path $LearnFlowWork 'local-processes.json'
if (-not (Test-Path -LiteralPath $recordFile)) { Write-Host 'No recorded LearnFlow processes.'; return }
# Windows PowerShell 5.1 会将管道返回的 JSON 数组保留为单个对象；先赋值再逐条遍历。
$records = Get-Content -LiteralPath $recordFile -Raw | ConvertFrom-Json
foreach ($record in $records) {
    $process = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
    if (-not $process -or $process.StartTime.ToUniversalTime().Ticks -ne [long]$record.StartTicks) { continue }
    # 只停止本项目启动且启动时间吻合的进程树，不误停用户其他进程。
    $snapshot = @(Get-CimInstance Win32_Process)
    $owned = New-Object 'System.Collections.Generic.List[int]'
    $owned.Add([int]$record.Id)
    for ($i=0; $i -lt $owned.Count; $i++) {
        foreach ($child in $snapshot | Where-Object { $_.ParentProcessId -eq $owned[$i] }) { $owned.Add([int]$child.ProcessId) }
    }
    for ($i=$owned.Count-1; $i -ge 0; $i--) { Stop-Process -Id $owned[$i] -ErrorAction SilentlyContinue }
    Write-Host ("Stopped " + $record.Name)
}
Remove-Item -LiteralPath $recordFile
