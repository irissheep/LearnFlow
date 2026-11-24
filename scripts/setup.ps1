. "$PSScriptRoot\common.ps1"
& "$PSScriptRoot\init-env.ps1"
Set-LearnFlowJava
$pythonCommand = Get-Command python.exe -ErrorAction SilentlyContinue
if (-not $pythonCommand) { throw 'Python 3.12 is required.' }
$venvPython = Join-Path $LearnFlowRoot 'ai-service\.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $venvPython)) {
    & $pythonCommand.Source -m venv (Join-Path $LearnFlowRoot 'ai-service\.venv')
    if ($LASTEXITCODE -ne 0) { throw 'Could not create Python environment.' }
}
& $venvPython -m pip install -r (Join-Path $LearnFlowRoot 'ai-service\requirements.txt')
if ($LASTEXITCODE -ne 0) { throw 'Python dependency installation failed.' }
Push-Location (Join-Path $LearnFlowRoot 'frontend')
try {
    & npm.cmd ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'Frontend installation failed.' }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }
Push-Location (Join-Path $LearnFlowRoot 'backend')
try {
    & (Get-LearnFlowMaven) '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
} finally { Pop-Location }
Write-Host 'Ready. Run scripts/start-local.ps1, or open the backend in IDEA.'
