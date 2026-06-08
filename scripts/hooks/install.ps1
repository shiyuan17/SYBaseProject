$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel).Trim()
$hookSourceDir = Join-Path $repoRoot 'scripts/hooks'
$gitHooksDir = Join-Path $repoRoot '.git/hooks'

New-Item -ItemType Directory -Path $gitHooksDir -Force | Out-Null

function Install-Hook {
    param(
        [string] $TargetName,
        [string] $ScriptName,
        [switch] $PassCommitMessageFile
    )

    $scriptPath = (Join-Path $hookSourceDir $ScriptName).Replace('\', '/')
    $argumentSuffix = if ($PassCommitMessageFile) { ' "$1"' } else { '' }
    $hookBody = @"
#!/bin/sh
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$scriptPath"$argumentSuffix
"@

    $targetPath = Join-Path $gitHooksDir $TargetName
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($targetPath, $hookBody, $utf8NoBom)
    Write-Host "[hooks] Installed $TargetName"
}

Install-Hook 'pre-commit' 'pre-commit.ps1'
Install-Hook 'commit-msg' 'commit-msg.ps1' -PassCommitMessageFile
Install-Hook 'pre-push' 'pre-push.ps1'

Write-Host '[hooks] Backend Git hooks installed.'
