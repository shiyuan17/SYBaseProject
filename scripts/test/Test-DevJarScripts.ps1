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

    throw "bash is required to test the dev jar scripts."
}

function To-BashPath([string] $Path) {
    $resolved = [System.IO.Path]::GetFullPath($Path)
    $drive = $resolved.Substring(0, 1).ToLowerInvariant()
    $rest = $resolved.Substring(2).Replace("\", "/")
    return "/$drive$rest"
}

function Invoke-BashCommand {
    param(
        [string] $Command,
        [hashtable] $Environment = @{}
    )

    $bash = Find-Bash
    $envAssignments = @()
    foreach ($key in $Environment.Keys) {
        $escaped = $Environment[$key].Replace("'", "'\''")
        $envAssignments += "$key='$escaped'"
    }

    $fullCommand = ($envAssignments + @($Command)) -join " "
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $bash -lc $fullCommand 2>&1
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

function Assert-OutputContains([string] $Output, [string] $Expected, [string] $Message) {
    if (-not $Output.Contains($Expected)) {
        throw ("{0} Expected output to contain '{1}'. Actual output: {2}" -f $Message, $Expected, $Output)
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$authScript = Join-Path $repoRoot "scripts\dev\unix\run-auth-center-jar.sh"
$blScript = Join-Path $repoRoot "scripts\dev\unix\run-bl-center-jar.sh"
$authKyConfig = Join-Path $repoRoot "auth-center\src\main\resources\application-ky.yml"
$blKyConfig = Join-Path $repoRoot "bl-center\src\main\resources\application-ky.yml"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("dev-jar-script-tests-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    Assert-True (Test-Path $authScript) "Auth jar script should exist."
    Assert-True (Test-Path $blScript) "BL jar script should exist."
    Assert-True (Test-Path $authKyConfig) "auth-center application-ky.yml should exist."
    Assert-True (Test-Path $blKyConfig) "bl-center application-ky.yml should exist."

    $scriptDir = Join-Path $tempRoot "scripts\dev\unix"
    New-Item -ItemType Directory -Path $scriptDir | Out-Null

    Copy-Item $authScript (Join-Path $scriptDir "run-auth-center-jar.sh")
    Copy-Item $blScript (Join-Path $scriptDir "run-bl-center-jar.sh")

    $bash = Find-Bash
    & $bash -lc "chmod +x '$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' '$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))'"

    $authStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' status"
    Assert-True ($authStatus.ExitCode -eq 0) "Auth status should succeed. Output: $($authStatus.Output)"
    Assert-OutputContains $authStatus.Output "Profile: default" "Auth default status should show the default profile."
    Assert-OutputContains $authStatus.Output "Jar: $(To-BashPath (Join-Path $scriptDir "auth-center.jar"))" "Auth status should use auth-center.jar in the script directory."
    Assert-OutputContains $authStatus.Output "Log: $(To-BashPath (Join-Path $scriptDir "log\\auth-center.log"))" "Auth status should use log/auth-center.log."

    $authKyStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' -ky status"
    Assert-True ($authKyStatus.ExitCode -eq 0) "Auth -ky status should succeed. Output: $($authKyStatus.Output)"
    Assert-OutputContains $authKyStatus.Output "Profile: ky" "Auth -ky status should switch to ky profile."

    $authProdStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' -prod status"
    Assert-True ($authProdStatus.ExitCode -eq 0) "Auth -prod status should succeed. Output: $($authProdStatus.Output)"
    Assert-OutputContains $authProdStatus.Output "Profile: prod" "Auth -prod status should switch to prod profile."

    $authProd06Status = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' -prod-06 status"
    Assert-True ($authProd06Status.ExitCode -eq 0) "Auth -prod-06 status should succeed. Output: $($authProd06Status.Output)"
    Assert-OutputContains $authProd06Status.Output "Profile: prod-06" "Auth -prod-06 status should switch to prod-06 profile."

    $authLogs = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-auth-center-jar.sh"))' logs"
    Assert-True ($authLogs.ExitCode -ne 0) "Auth logs should fail before the log file exists."
    Assert-OutputContains $authLogs.Output "Log file not found: $(To-BashPath (Join-Path $scriptDir "log\\auth-center.log"))" "Auth logs should point to log/auth-center.log."
    Assert-True (Test-Path (Join-Path $scriptDir "log")) "Auth logs command should create the log directory."

    $blStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))' status"
    Assert-True ($blStatus.ExitCode -eq 0) "BL status should succeed. Output: $($blStatus.Output)"
    Assert-OutputContains $blStatus.Output "Profile: dev" "BL default status should show the dev profile."
    Assert-OutputContains $blStatus.Output "Jar: $(To-BashPath (Join-Path $scriptDir "bl-center.jar"))" "BL status should use bl-center.jar in the script directory."
    Assert-OutputContains $blStatus.Output "Log: $(To-BashPath (Join-Path $scriptDir "log\\bl-center.log"))" "BL status should use log/bl-center.log."

    $blKyStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))' -ky status"
    Assert-True ($blKyStatus.ExitCode -eq 0) "BL -ky status should succeed. Output: $($blKyStatus.Output)"
    Assert-OutputContains $blKyStatus.Output "Profile: ky" "BL -ky status should switch to ky profile."

    $blProdStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))' -prod status"
    Assert-True ($blProdStatus.ExitCode -eq 0) "BL -prod status should succeed. Output: $($blProdStatus.Output)"
    Assert-OutputContains $blProdStatus.Output "Profile: prod" "BL -prod status should switch to prod profile."

    $blLocalStatus = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))' -local status"
    Assert-True ($blLocalStatus.ExitCode -eq 0) "BL -local status should succeed. Output: $($blLocalStatus.Output)"
    Assert-OutputContains $blLocalStatus.Output "Profile: local" "BL -local status should switch to local profile."

    $blLogs = Invoke-BashCommand -Command "'$(To-BashPath (Join-Path $scriptDir "run-bl-center-jar.sh"))' logs"
    Assert-True ($blLogs.ExitCode -ne 0) "BL logs should fail before the log file exists."
    Assert-OutputContains $blLogs.Output "Log file not found: $(To-BashPath (Join-Path $scriptDir "log\\bl-center.log"))" "BL logs should point to log/bl-center.log."
    Assert-True (Test-Path (Join-Path $scriptDir "log")) "BL logs command should create the log directory."

    Write-Host "Dev jar script tests passed."
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
