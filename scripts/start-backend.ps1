. "$PSScriptRoot\common.ps1"
Set-LearnFlowJava
$jar = Join-Path $LearnFlowRoot 'backend\target\learnflow-backend-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Run scripts/setup.ps1 first.' }
Set-Location -LiteralPath (Join-Path $LearnFlowRoot 'backend')
& (Join-Path $env:JAVA_HOME 'bin\java.exe') ('-Djava.io.tmpdir=' + $env:TEMP) -jar $jar
exit $LASTEXITCODE
