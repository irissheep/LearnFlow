$ErrorActionPreference = 'Stop'
$LearnFlowRoot = Split-Path -Parent $PSScriptRoot
$LearnFlowWork = Join-Path $LearnFlowRoot 'work'
New-Item -ItemType Directory -Path (Join-Path $LearnFlowWork 'tmp') -Force | Out-Null
$env:TEMP = Join-Path $LearnFlowWork 'tmp'
$env:TMP = $env:TEMP
$env:PIP_CACHE_DIR = Join-Path $LearnFlowWork 'pip-cache'
$env:npm_config_cache = Join-Path $LearnFlowWork 'npm-cache'
$envFile = Join-Path $LearnFlowRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in [System.IO.File]::ReadAllLines($envFile)) {
        if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}
function Set-LearnFlowJava {
    $jdk = if ($env:LEARNFLOW_JAVA_HOME) { $env:LEARNFLOW_JAVA_HOME } else { Join-Path $env:USERPROFILE '.jdks\ms-17.0.17' }
    if (-not (Test-Path -LiteralPath (Join-Path $jdk 'bin\java.exe'))) { throw 'Set LEARNFLOW_JAVA_HOME to your JDK 17+ installation.' }
    $env:JAVA_HOME = $jdk
    $env:Path = (Join-Path $jdk 'bin') + ';' + $env:Path
}
function Get-LearnFlowMaven {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $fallback = 'E:\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd'
    if (Test-Path -LiteralPath $fallback) { return $fallback }
    throw 'Maven was not found. Add Maven bin to PATH.'
}
