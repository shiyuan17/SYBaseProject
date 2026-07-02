$ErrorActionPreference = "Stop"

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

function Read-TrimmedFile([string] $Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }

    $content = Get-Content -Raw -LiteralPath $Path
    if ($null -eq $content) {
        return ""
    }

    return $content.Trim()
}

function Format-CmdArgument([string] $Value) {
    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    return '"' + ($Value -replace '"', '\"') + '"'
}

function Invoke-CmdScript {
    param(
        [string] $ScriptPath,
        [string[]] $Arguments = @(),
        [int] $TimeoutSeconds = 60
    )

    Write-Host ("Running: {0} {1}" -f $ScriptPath, ($Arguments -join " "))
    $timer = [Diagnostics.Stopwatch]::StartNew()

    $commandText = ((@($ScriptPath) + $Arguments) | ForEach-Object { Format-CmdArgument $_ }) -join " "
    $stdoutFile = Join-Path ([System.IO.Path]::GetTempPath()) ("run-centers-prod-06-stdout-" + [guid]::NewGuid().ToString("N") + ".log")
    $stderrFile = Join-Path ([System.IO.Path]::GetTempPath()) ("run-centers-prod-06-stderr-" + [guid]::NewGuid().ToString("N") + ".log")

    try {
        $process = Start-Process -FilePath "cmd.exe" `
            -ArgumentList "/d", "/c", $commandText `
            -RedirectStandardOutput $stdoutFile `
            -RedirectStandardError $stderrFile `
            -WindowStyle Hidden `
            -PassThru

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try {
                $process.Kill()
                $process.WaitForExit()
            }
            catch {
            }

            $timer.Stop()
            $stdout = Read-TrimmedFile $stdoutFile
            $stderr = Read-TrimmedFile $stderrFile
            throw ("Command timed out after {0} seconds: {1} {2}`nSTDOUT:`n{3}`nSTDERR:`n{4}" -f $TimeoutSeconds, $ScriptPath, ($Arguments -join " "), $stdout, $stderr)
        }

        $stdoutText = Read-TrimmedFile $stdoutFile
        $stderrText = Read-TrimmedFile $stderrFile
        $timer.Stop()

        $exitCode = if ($null -eq $process.ExitCode) { 0 } else { [int] $process.ExitCode }

        Write-Host ("Completed in {0} ms with exit code {1}" -f $timer.ElapsedMilliseconds, $exitCode)

        return [pscustomobject]@{
            ExitCode = $exitCode
            Output = (@($stdoutText, $stderrText) | Where-Object { $_ }) -join "`n"
        }
    }
    finally {
        foreach ($logFile in @($stdoutFile, $stderrFile)) {
            if (Test-Path -LiteralPath $logFile) {
                Remove-Item -LiteralPath $logFile -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Stop-TrackedProcessIfNeeded([string] $PidFile) {
    if (-not (Test-Path -LiteralPath $PidFile)) {
        return
    }

    $pidValue = (Get-Content -Raw $PidFile).Trim()
    if (-not $pidValue) {
        return
    }

    $process = Get-Process -Id ([int]$pidValue) -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $process.Id -Timeout 5 -ErrorAction SilentlyContinue
    }
}

function Stop-TestProcesses([string] $TempRoot) {
    Get-CimInstance Win32_Process |
        Where-Object { $_.CommandLine -like "*$TempRoot*" } |
        Sort-Object ProcessId -Descending |
        ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$cmdScript = Join-Path $repoRoot "scripts\prod\run-centers-prod-06.cmd"
$ps1Script = Join-Path $repoRoot "scripts\prod\run-centers-prod-06.ps1"
$helperScript = Join-Path $repoRoot "scripts\prod\run-centers-prod-06.helpers.ps1"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("run-centers-prod-06-windows-tests-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

$runtimeDir = Join-Path $tempRoot "runtime"
$logDir = Join-Path $tempRoot "logs"
$scriptDir = Join-Path $tempRoot "scripts\prod"
$fakeJava = Join-Path $tempRoot "fake-java.ps1"
$blPidFile = Join-Path $runtimeDir "bl-center.pid"
$authPidFile = Join-Path $runtimeDir "auth-center.pid"

try {
    Assert-True (Test-Path -LiteralPath $cmdScript) "CMD prod-06 launcher should exist at scripts/prod/run-centers-prod-06.cmd."
    Assert-True (Test-Path -LiteralPath $ps1Script) "PowerShell prod-06 launcher should exist at scripts/prod/run-centers-prod-06.ps1."
    Assert-True (Test-Path -LiteralPath $helperScript) "PowerShell prod-06 helper script should exist at scripts/prod/run-centers-prod-06.helpers.ps1."

    New-Item -ItemType Directory -Path $runtimeDir, $logDir, $scriptDir | Out-Null

    Copy-Item $cmdScript (Join-Path $scriptDir "run-centers-prod-06.cmd")
    Copy-Item $ps1Script (Join-Path $scriptDir "run-centers-prod-06.ps1")
    Copy-Item $helperScript (Join-Path $scriptDir "run-centers-prod-06.helpers.ps1")

    Set-Content -Path (Join-Path $scriptDir "run-centers.conf") -Value @(
        "RUNTIME_DIR=$runtimeDir"
        "LOG_DIR=$logDir"
        "BL_JAR_PATH=$(Join-Path $tempRoot 'bl-center.jar')"
        "AUTH_JAR_PATH=$(Join-Path $tempRoot 'auth-center.jar')"
        "JAVA_CMD=powershell.exe"
        "JAVA_OPTS=-NoProfile -ExecutionPolicy Bypass -File `"$fakeJava`""
        "BL_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236/BL"
        "BL_CENTER_DATASOURCE_USERNAME=bl-user"
        "BL_CENTER_DATASOURCE_PASSWORD=bl-pass"
        "AUTH_CENTER_DATASOURCE_URL=jdbc:dm://127.0.0.1:5236/AUTH"
        "AUTH_CENTER_DATASOURCE_USERNAME=auth-user"
        "AUTH_CENTER_DATASOURCE_PASSWORD=auth-pass"
        "TAIL_LINES=20"
    )

    Set-Content -Path (Join-Path $tempRoot "bl-center.jar") -Value "fake-bl" -NoNewline
    Set-Content -Path (Join-Path $tempRoot "auth-center.jar") -Value "fake-auth" -NoNewline

    Set-Content -Path $fakeJava -Value @'
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $RemainingArgs
)

$jarIndex = [Array]::IndexOf($RemainingArgs, "-jar")
$jarPath = if ($jarIndex -ge 0 -and ($jarIndex + 1) -lt $RemainingArgs.Count) { $RemainingArgs[$jarIndex + 1] } else { "unknown.jar" }
$profileArg = $RemainingArgs | Where-Object { $_ -like "--spring.profiles.active=*" } | Select-Object -First 1
$profile = if ($profileArg) { $profileArg.Substring("--spring.profiles.active=".Length) } else { "missing" }
$service = if ((Split-Path $jarPath -Leaf) -like "bl-center*") { "bl-center" } else { "auth-center" }
$datasource = if ($service -eq "bl-center") { $env:BL_CENTER_DATASOURCE_URL } else { $env:AUTH_CENTER_DATASOURCE_URL }

Write-Output "service=$service"
Write-Output "profile=$profile"
Write-Output "datasource=$datasource"

while ($true) {
    Start-Sleep -Seconds 1
}
'@

    $initialStatusResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("status", "all")
    Assert-True ($initialStatusResult.ExitCode -eq 0) "cmd launcher should run status by delegating to the ps1 script. Output: $($initialStatusResult.Output)"
    Assert-OutputContains $initialStatusResult.Output "bl-center is not running" "initial cmd status should report bl-center stopped."
    Assert-OutputContains $initialStatusResult.Output "auth-center is not running" "initial cmd status should report auth-center stopped."
    Assert-OutputContains $initialStatusResult.Output "Profile: prod-06" "initial cmd status should report prod-06."

    $startResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("start", "all")
    Assert-True ($startResult.ExitCode -eq 0) "cmd launcher should start both services. Output: $($startResult.Output)"
    Assert-OutputContains $startResult.Output "Profile: prod-06" "cmd start output should show the forced prod-06 profile."

    Start-Sleep -Seconds 2

    Assert-True (Test-Path -LiteralPath $blPidFile) "bl-center pid file should exist after cmd start."
    Assert-True (Test-Path -LiteralPath $authPidFile) "auth-center pid file should exist after cmd start."

    $statusResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("status", "all")
    Assert-True ($statusResult.ExitCode -eq 0) "cmd status should succeed. Output: $($statusResult.Output)"
    Assert-OutputContains $statusResult.Output "bl-center is running with PID" "cmd status should report bl-center running."
    Assert-OutputContains $statusResult.Output "auth-center is running with PID" "cmd status should report auth-center running."
    Assert-OutputContains $statusResult.Output "Profile: prod-06" "cmd status should report prod-06."

    $blLogResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("log", "bl")
    Assert-True ($blLogResult.ExitCode -eq 0) "cmd bl log command should succeed. Output: $($blLogResult.Output)"
    Assert-OutputContains $blLogResult.Output "service=bl-center" "cmd bl log should include the service marker."
    Assert-OutputContains $blLogResult.Output "profile=prod-06" "cmd bl log should include the prod-06 profile."
    Assert-OutputContains $blLogResult.Output "datasource=jdbc:dm://127.0.0.1:5236/BL" "cmd bl log should include the BL datasource."

    $authLogResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("log", "auth")
    Assert-True ($authLogResult.ExitCode -eq 0) "cmd auth log command should succeed. Output: $($authLogResult.Output)"
    Assert-OutputContains $authLogResult.Output "service=auth-center" "cmd auth log should include the service marker."
    Assert-OutputContains $authLogResult.Output "profile=prod-06" "cmd auth log should include the prod-06 profile."
    Assert-OutputContains $authLogResult.Output "datasource=jdbc:dm://127.0.0.1:5236/AUTH" "cmd auth log should include the AUTH datasource."

    $oldBlPid = (Get-Content -Raw $blPidFile).Trim()
    $oldAuthPid = (Get-Content -Raw $authPidFile).Trim()

    $restartResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("restart", "all")
    Assert-True ($restartResult.ExitCode -eq 0) "cmd restart should succeed. Output: $($restartResult.Output)"

    Start-Sleep -Seconds 2

    $newBlPid = (Get-Content -Raw $blPidFile).Trim()
    $newAuthPid = (Get-Content -Raw $authPidFile).Trim()
    Assert-True ($newBlPid -ne $oldBlPid) "restart should replace the bl-center process."
    Assert-True ($newAuthPid -ne $oldAuthPid) "restart should replace the auth-center process."

    $stopResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("stop", "all")
    Assert-True ($stopResult.ExitCode -eq 0) "cmd stop should succeed. Output: $($stopResult.Output)"

    $cmdStatusResult = Invoke-CmdScript -ScriptPath (Join-Path $scriptDir "run-centers-prod-06.cmd") -Arguments @("status", "all")
    Assert-True ($cmdStatusResult.ExitCode -eq 0) "cmd final status should succeed. Output: $($cmdStatusResult.Output)"
    Assert-OutputContains $cmdStatusResult.Output "bl-center is not running" "cmd final status should report bl-center stopped."
    Assert-OutputContains $cmdStatusResult.Output "auth-center is not running" "cmd final status should report auth-center stopped."
    Assert-OutputContains $cmdStatusResult.Output "Profile: prod-06" "cmd final status should keep the prod-06 profile."

    Write-Host "Prod-06 Windows run-centers script tests passed."
}
finally {
    Stop-TrackedProcessIfNeeded $blPidFile
    Stop-TrackedProcessIfNeeded $authPidFile
    Stop-TestProcesses $tempRoot

    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
