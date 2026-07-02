$ErrorActionPreference = "Stop"

function Find-Bash {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($git) {
        $gitRoot = Split-Path (Split-Path $git.Source -Parent) -Parent
        $gitBashCandidates = @(
            (Join-Path $gitRoot "bin\bash.exe"),
            (Join-Path $gitRoot "usr\bin\bash.exe")
        )

        foreach ($candidate in $gitBashCandidates) {
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }

    $candidates = @(
        "${env:ProgramFiles}\Git\bin\bash.exe",
        "${env:ProgramFiles}\Git\usr\bin\bash.exe",
        "${env:ProgramFiles(x86)}\Git\bin\bash.exe",
        "${env:ProgramFiles(x86)}\Git\usr\bin\bash.exe"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    $bash = Get-Command bash -ErrorAction SilentlyContinue
    if ($bash) {
        return $bash.Source
    }

    throw "bash is required to test the prod-06 run scripts."
}

function To-BashPath([string] $Path) {
    $resolved = [System.IO.Path]::GetFullPath($Path)
    $drive = $resolved.Substring(0, 1).ToLowerInvariant()
    $rest = $resolved.Substring(2).Replace("\", "/")
    return "/$drive$rest"
}

function Invoke-BashScript {
    param(
        [string] $Script,
        [string[]] $Arguments = @()
    )

    $bash = Find-Bash
    $escapedArgs = @("'$Script'")
    foreach ($argument in $Arguments) {
        $escapedArgs += "'" + $argument.Replace("'", "'\''") + "'"
    }

    $command = $escapedArgs -join " "
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $bash -lc $command 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = ($output -join "`n")
    }
}

function Assert-True([bool] $Condition, [string] $Message) {
    if (-not $Condition) {
        throw $Message
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$wrapperScript = Join-Path $repoRoot "scripts\prod\run-centers-prod-06.sh"
$prod06Config = Join-Path $repoRoot "scripts\prod\run-centers-prod-06.conf"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("run-centers-prod-06-tests-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    Assert-True (Test-Path $wrapperScript) "Wrapper script should exist at scripts/prod/run-centers-prod-06.sh."
    Assert-True (Test-Path $prod06Config) "Prod-06 config should exist at scripts/prod/run-centers-prod-06.conf."

    $tempScriptDir = Join-Path $tempRoot "scripts\prod"
    New-Item -ItemType Directory -Path $tempScriptDir -Force | Out-Null

    Copy-Item $wrapperScript (Join-Path $tempScriptDir "run-centers-prod-06.sh")
    Copy-Item $prod06Config (Join-Path $tempScriptDir "run-centers-prod-06.conf")

    Set-Content -Path (Join-Path $tempScriptDir "run-centers.conf") -Value @(
        "SPRING_PROFILES_ACTIVE=prod"
        "BASE_CONFIG_MARKER=shared-config"
    )

    $captureFile = Join-Path $tempRoot "captured.txt"
    Set-Content -Path (Join-Path $tempScriptDir "run-centers.sh") -Value @(
        "#!/usr/bin/env sh"
        "set -eu"
        '. "$RUN_CENTERS_CONFIG_FILE"'
        "cat <<EOF > '$([System.IO.Path]::GetFullPath($captureFile).Replace('\', '/'))'"
        'profile=$SPRING_PROFILES_ACTIVE'
        'base_marker=${BASE_CONFIG_MARKER:-}'
        'args=$*'
        'config=$RUN_CENTERS_CONFIG_FILE'
        'EOF'
    )

    $bash = Find-Bash
    & $bash -lc "chmod +x '$(To-BashPath (Join-Path $tempScriptDir "run-centers-prod-06.sh"))' '$(To-BashPath (Join-Path $tempScriptDir "run-centers.sh"))'"

    $result = Invoke-BashScript -Script (To-BashPath (Join-Path $tempScriptDir "run-centers-prod-06.sh")) -Arguments @("restart", "all")

    Assert-True ($result.ExitCode -eq 0) "Wrapper script should succeed. Output: $($result.Output)"
    Assert-True (Test-Path $captureFile) "Wrapper script should invoke run-centers.sh."

    $captured = Get-Content -Raw $captureFile
    Assert-True ($captured.Contains("profile=prod-06")) "Wrapper should force the prod-06 profile."
    Assert-True ($captured.Contains("base_marker=shared-config")) "Wrapper should keep loading shared base config."
    Assert-True ($captured.Contains("args=restart all")) "Wrapper should forward command arguments."
    Assert-True ($captured.Contains("run-centers-prod-06.conf")) "Wrapper should point run-centers.sh at the prod-06 config file."

    Write-Host "Prod-06 run-centers wrapper tests passed."
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
