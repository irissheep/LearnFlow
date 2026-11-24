. "$PSScriptRoot\common.ps1"
if (-not (Test-Path -LiteralPath (Join-Path $LearnFlowRoot '.env'))) { & "$PSScriptRoot\init-env.ps1" }
foreach ($port in @(8080, 8000, 5173)) {
    $tcp = New-Object System.Net.Sockets.TcpClient
    try {
        $attempt = $tcp.ConnectAsync('127.0.0.1', $port)
        if ($attempt.Wait(300) -and $tcp.Connected) { throw "Port $port is in use. Stop the existing application first." }
    } catch [System.AggregateException] {
        # Connection refused means the port is available.
    } finally { $tcp.Dispose() }
}
$records = @()
$logs = Join-Path $LearnFlowWork 'logs'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
try {
    foreach ($name in @('ai', 'backend', 'frontend')) {
        $script = Join-Path $PSScriptRoot ("start-$name.ps1")
        $startArguments = @{
            FilePath = 'powershell.exe'
            WindowStyle = 'Hidden'
            PassThru = $true
            ArgumentList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $script + '"'))
            WorkingDirectory = $LearnFlowRoot
            RedirectStandardOutput = Join-Path $logs "$name.stdout.log"
            RedirectStandardError = Join-Path $logs "$name.stderr.log"
        }
        $process = Start-Process @startArguments
        $records += [pscustomobject]@{ Name=$name; Id=$process.Id; StartTicks=$process.StartTime.ToUniversalTime().Ticks }
    }
    $records | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $LearnFlowWork 'local-processes.json') -Encoding UTF8
    Write-Host 'Waiting for the three application services...'
    $checks = @{
        ai = 'http://127.0.0.1:8000/health'
        backend = 'http://127.0.0.1:8080/api/health'
        frontend = 'http://127.0.0.1:5173'
    }
    $deadline = (Get-Date).AddSeconds(90)
    while ($checks.Count -gt 0) {
        foreach ($record in $records) {
            if (-not (Get-Process -Id $record.Id -ErrorAction SilentlyContinue)) {
                throw ($record.Name + " exited during startup. Check $logs")
            }
        }
        foreach ($name in @($checks.Keys)) {
            try {
                $response = Invoke-WebRequest -Uri $checks[$name] -UseBasicParsing -TimeoutSec 2
                if ($response.StatusCode -eq 200) { $checks.Remove($name) }
            } catch { # Service may still be starting; retry until the deadline.
            }
        }
        if ($checks.Count -eq 0) { break }
        if ((Get-Date) -gt $deadline) { throw "Startup timed out. Check $logs" }
        Start-Sleep -Milliseconds 500
    }
    Write-Host 'LearnFlow is ready: http://127.0.0.1:5173'
    Write-Host "Logs: $logs"
    Write-Host 'Stop with scripts/stop-local.ps1. Existing processes are never stopped automatically.'
} catch {
    $startupError = $_
    try {
        $records | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $LearnFlowWork 'local-processes.json') -Encoding UTF8
        & "$PSScriptRoot\stop-local.ps1"
    } catch {
        Write-Warning ("Cleanup failed: " + $_.Exception.Message + ". Run scripts/stop-local.ps1 after resolving it.")
    }
    # 清理失败也必须报告原始启动错误，避免错误原因被第二个异常遮住。
    throw $startupError
}
