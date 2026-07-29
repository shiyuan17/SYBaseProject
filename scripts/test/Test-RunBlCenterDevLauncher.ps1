$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$rootLauncher = Join-Path $repoRoot 'run-bl-center-dev.cmd'
$legacyLauncher = Join-Path $repoRoot 'scripts\dev\run-bl-center-dev.cmd'
$windowsLauncher = Join-Path $repoRoot 'scripts\dev\windows\run-bl-center-dev.cmd'
$readmePath = Join-Path $repoRoot 'README.md'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Equal {
    param(
        $Expected,
        $Actual,
        [string]$Message
    )

    if ($Expected -ne $Actual) {
        throw "$Message Expected=[$Expected] Actual=[$Actual]"
    }
}

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Expected,
        [string]$Message
    )

    if (-not $Text.Contains($Expected)) {
        throw "$Message Expected to find [$Expected] in output: $Text"
    }
}

function New-TestDirectory {
    param([string]$Name)

    $path = Join-Path $repoRoot ("tmp\test-run-bl-center-dev-launcher\{0}-{1}" -f $Name, (Get-Date -Format 'yyyyMMddHHmmssfff'))
    New-Item -ItemType Directory -Force -Path $path | Out-Null
    return $path
}

function Invoke-CmdScript {
    param(
        [string]$ScriptPath,
        [string]$WorkingDirectory
    )

    $originalLocation = Get-Location
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        Set-Location -LiteralPath $WorkingDirectory
        $output = & cmd.exe /d /c $ScriptPath 2>&1
        $exitCode = $LASTEXITCODE
        $text = (($output | ForEach-Object { "$_" }) -join "`n").Trim()

        return [pscustomobject]@{
            ExitCode = [int]$exitCode
            Output = $text
        }
    }
    finally {
        Set-Location -LiteralPath $originalLocation
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

Assert-True (Test-Path -LiteralPath $rootLauncher) "Root launcher should exist at $rootLauncher."
Assert-True (-not (Test-Path -LiteralPath $legacyLauncher)) "Legacy launcher should be removed from scripts/dev."
Assert-True (Test-Path -LiteralPath $windowsLauncher) "Windows launcher should exist at $windowsLauncher."
Assert-True (Test-Path -LiteralPath $readmePath) "README should exist at $readmePath."

$testRoot = New-TestDirectory 'cmd-wrapper'

try {
    $tempRootLauncher = Join-Path $testRoot 'run-bl-center-dev.cmd'
    $tempScriptsDir = Join-Path $testRoot 'scripts\dev'
    $tempWindowsDir = Join-Path $tempScriptsDir 'windows'
    $tempWindowsLauncher = Join-Path $tempWindowsDir 'run-bl-center-dev.cmd'
    New-Item -ItemType Directory -Force -Path $tempWindowsDir | Out-Null

    Copy-Item -LiteralPath $rootLauncher -Destination $tempRootLauncher

    Set-Content -LiteralPath $tempWindowsLauncher -Encoding ascii -Value @(
        '@echo off',
        'echo fake bl-center launcher invoked',
        'exit /b 7'
    )

    $delegation = Invoke-CmdScript -ScriptPath $tempRootLauncher -WorkingDirectory $testRoot
    Assert-Equal 7 $delegation.ExitCode 'Root launcher should preserve the delegated exit code.'
    Assert-Contains $delegation.Output 'fake bl-center launcher invoked' 'Root launcher should forward delegated output.'

    Remove-Item -LiteralPath $tempWindowsLauncher -Force

    $missingTarget = Invoke-CmdScript -ScriptPath $tempRootLauncher -WorkingDirectory $testRoot
    Assert-True ($missingTarget.ExitCode -ne 0) 'Root launcher should fail when the delegated script is missing.'
    Assert-Contains $missingTarget.Output 'scripts\dev\windows\run-bl-center-dev.cmd' 'Missing-target message should mention the delegated script path.'
    Assert-Contains $missingTarget.Output $testRoot 'Missing-target message should mention the backend repo root.'

    $readme = Get-Content -Raw -LiteralPath $readmePath
    Assert-Contains $readme '.\run-bl-center-dev.cmd' 'README should document the root Windows launcher.'
    Assert-Contains $readme '.\scripts\dev\windows\run-bl-center-dev.cmd' 'README should document the recommended Windows launcher path.'
    Assert-Contains $readme './scripts/dev/unix/run-bl-center-dev.sh' 'README should document the recommended Unix launcher path.'
    Assert-Contains $readme 'SYBaseProjectWeb' 'README should clarify that the launcher is not for the frontend repo.'

    Write-Host 'run-bl-center-dev launcher tests passed.'
}
finally {
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}
