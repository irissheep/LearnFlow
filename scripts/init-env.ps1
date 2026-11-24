$ErrorActionPreference = 'Stop'
$LearnFlowRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $LearnFlowRoot '.env'
if (Test-Path -LiteralPath $envFile) { Write-Host 'Existing .env preserved.'; return }
function New-LearnFlowSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}
$content = @(
    'MYSQL_DATABASE=learnflow'
    'MYSQL_USER=learnflow_app'
    ('MYSQL_PASSWORD=' + (New-LearnFlowSecret))
    ('MYSQL_ROOT_PASSWORD=' + (New-LearnFlowSecret))
    ('AI_SERVICE_TOKEN=' + (New-LearnFlowSecret))
    'AI_PROVIDER=local'
    'OPENAI_API_KEY='
    'OPENAI_BASE_URL=https://api.openai.com/v1'
    'LLM_MODEL=gpt-4.1-mini'
    'EMBEDDING_MODEL=text-embedding-3-small'
    'COOKIE_SECURE=false'
) -join [Environment]::NewLine
[System.IO.File]::WriteAllText($envFile, $content + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
Write-Host 'Created local .env with random secrets. It is ignored by Git.'
