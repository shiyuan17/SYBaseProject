$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

Write-Host '[hooks] Running backend fast verification...'
& .\mvnw.cmd -B -ntp clean test '-Dsurefire.excludedGroups=slow'
exit $LASTEXITCODE
