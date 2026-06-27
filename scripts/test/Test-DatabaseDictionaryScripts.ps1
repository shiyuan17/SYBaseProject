$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$windowsScript = Join-Path $repoRoot 'scripts\database\run-database-dictionary.cmd'
$unixScript = Join-Path $repoRoot 'scripts\database\run-database-dictionary.sh'
$legacyWindowsScript = Join-Path $repoRoot 'scripts\database\run-database-dictionary-legacy.cmd'
$legacyUnixScript = Join-Path $repoRoot 'scripts\database\run-database-dictionary-legacy.sh'

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

function Assert-Match {
    param(
        [string]$Pattern,
        [string]$Text,
        [string]$Message
    )

    if ($Text -notmatch $Pattern) {
        throw "$Message Pattern=[$Pattern] Text=[$Text]"
    }
}

function New-TestDirectory {
    param([string]$Name)

    $path = Join-Path $repoRoot ("tmp\test-db-dictionary\{0}-{1}" -f $Name, (Get-Date -Format 'yyyyMMddHHmmssfff'))
    New-Item -ItemType Directory -Force -Path $path | Out-Null
    return $path
}

function Convert-ToWslPath {
    param([string]$WindowsPath)

    $normalized = $WindowsPath -replace '\\', '/'
    if ($normalized -match '^([A-Za-z]):/(.*)$') {
        return "/mnt/$($matches[1].ToLower())/$($matches[2])"
    }

    throw "Cannot convert path to WSL format: $WindowsPath"
}

function New-FakeMvnwBin {
    $root = New-TestDirectory 'fake-mvnw-bin'
    $cmdPath = Join-Path $root 'fake-mvnw.cmd'
    $shPath = Join-Path $root 'fake-mvnw'

    @'
@echo off
setlocal
>>"%FAKE_MVNW_CAPTURE%" echo args=%*
>>"%FAKE_MVNW_CAPTURE%" echo auth_url=%AUTH_CENTER_DATASOURCE_URL%
>>"%FAKE_MVNW_CAPTURE%" echo bl_url=%BL_CENTER_DATASOURCE_URL%
exit /b 0
'@ | Set-Content -NoNewline -Encoding ascii $cmdPath

    @'
#!/usr/bin/env sh
set -eu
{
  printf 'args=%s\n' "$*"
  printf 'auth_url=%s\n' "${AUTH_CENTER_DATASOURCE_URL:-}"
  printf 'bl_url=%s\n' "${BL_CENTER_DATASOURCE_URL:-}"
} >> "${FAKE_MVNW_CAPTURE}"
'@ | Set-Content -NoNewline -Encoding ascii $shPath

    return $root
}

function Invoke-Process {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [hashtable]$Environment
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.WorkingDirectory = $repoRoot
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.UseShellExecute = $false
    $startInfo.Arguments = (($Arguments | ForEach-Object {
        if ($_ -match '\s') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }) -join ' ')

    foreach ($entry in $Environment.GetEnumerator()) {
        if ($null -eq $entry.Value) {
            [void]$startInfo.Environment.Remove($entry.Key)
        } else {
            $startInfo.Environment[$entry.Key] = [string]$entry.Value
        }
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        StdOut   = $stdout.Trim()
        StdErr   = $stderr.Trim()
    }
}

function Invoke-WslShell {
    param(
        [string]$Command
    )

    return Invoke-Process -FilePath 'wsl.exe' -Arguments @('sh', '-lc', $Command) -Environment @{}
}

function New-Environment {
    param(
        [string]$FakeMvnwPath,
        [string]$CapturePath
    )

    return @{
        JAVA_HOME = 'C:\fake-jdk-17'
        FAKE_MVNW_CAPTURE = $CapturePath
        MVNW_CMD = $FakeMvnwPath
        MVNW_BIN = $FakeMvnwPath
        AUTH_CENTER_DATASOURCE_URL = $null
        AUTH_CENTER_DATASOURCE_USERNAME = $null
        AUTH_CENTER_DATASOURCE_PASSWORD = $null
        BL_CENTER_DATASOURCE_URL = $null
        BL_CENTER_DATASOURCE_USERNAME = $null
        BL_CENTER_DATASOURCE_PASSWORD = $null
    }
}

Assert-True (Test-Path $windowsScript) "Missing Windows dictionary script: $windowsScript"
Assert-True (Test-Path $unixScript) "Missing Unix dictionary script: $unixScript"
Assert-True (Test-Path $legacyWindowsScript) "Missing Windows legacy dictionary script: $legacyWindowsScript"
Assert-True (Test-Path $legacyUnixScript) "Missing Unix legacy dictionary script: $legacyUnixScript"

$fakeBin = New-FakeMvnwBin
$capturePath = Join-Path (New-TestDirectory 'capture') 'mvnw-calls.log'
New-Item -ItemType File -Force -Path $capturePath | Out-Null

$missingJavaEnv = New-Environment -FakeMvnwPath (Join-Path $fakeBin 'fake-mvnw.cmd') -CapturePath $capturePath
$missingJavaEnv['JAVA_HOME'] = $null
$missingJava = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript) -Environment $missingJavaEnv
Assert-True ($missingJava.ExitCode -ne 0) 'Expected missing JAVA_HOME scenario to fail.'
Assert-Match 'JAVA_HOME is not set' ($missingJava.StdOut + "`n" + $missingJava.StdErr) 'Missing JAVA_HOME message should be explicit.'

Clear-Content $capturePath
$customOutput = Join-Path (New-TestDirectory 'output') 'dictionary.html'
$windowsEnv = New-Environment -FakeMvnwPath (Join-Path $fakeBin 'fake-mvnw.cmd') -CapturePath $capturePath
$windows = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript, $customOutput, '--scope', 'visible-all') -Environment $windowsEnv
Assert-Equal 0 $windows.ExitCode 'Expected Windows dictionary script to succeed.'
$windowsCalls = @(Get-Content $capturePath)
Assert-Equal 6 $windowsCalls.Count 'Expected Windows wrapper to call fake mvnw twice with logged env snapshots.'
Assert-Match 'args=-Dmaven.repo.local=.m2/repository -pl tools/app-cli -am -Dmaven.test.skip=true install' $windowsCalls[0] 'Windows script should prepare dependencies first.'
$windowsOutputPattern = [regex]::Escape($customOutput)
Assert-Match $windowsOutputPattern $windowsCalls[3] 'Windows run call should include the requested output path.'
Assert-Match '--scope visible-all' $windowsCalls[3] 'Windows run call should forward extra CLI arguments.'
Assert-Match 'auth_url=jdbc:dm://127.0.0.1:5236' ($windowsCalls -join "`n") 'Windows wrapper should apply auth-center local default URL.'
Assert-Match 'bl_url=jdbc:dm://127.0.0.1:5236' ($windowsCalls -join "`n") 'Windows wrapper should apply bl-center local default URL.'

Clear-Content $capturePath
$repoRootWsl = Convert-ToWslPath $repoRoot
$unixScriptWsl = Convert-ToWslPath $unixScript
$fakeMvnwWsl = Convert-ToWslPath (Join-Path $fakeBin 'fake-mvnw')
$capturePathWsl = Convert-ToWslPath $capturePath
$customOutputWsl = Convert-ToWslPath $customOutput
$unixCommand = @"
set -eu
chmod +x '$fakeMvnwWsl' '$unixScriptWsl'
export JAVA_HOME='/fake/jdk-17'
export FAKE_MVNW_CAPTURE='$capturePathWsl'
export MVNW_BIN='$fakeMvnwWsl'
cd '$repoRootWsl'
'$unixScriptWsl' '$customOutputWsl' --scope visible-all
"@
$unix = Invoke-WslShell -Command $unixCommand
Assert-Equal 0 $unix.ExitCode 'Expected Unix dictionary script to succeed.'
$unixCalls = @(Get-Content $capturePath)
Assert-Equal 6 $unixCalls.Count 'Expected Unix wrapper to call fake mvnw twice with logged env snapshots.'
Assert-Match 'args=-Dmaven.repo.local=.m2/repository -pl tools/app-cli -am -Dmaven.test.skip=true install' $unixCalls[0] 'Unix script should prepare dependencies first.'
Assert-Match '--output="/mnt/' $unixCalls[3] 'Unix run call should include quoted output path.'
Assert-Match 'auth_url=jdbc:dm://127.0.0.1:5236' ($unixCalls -join "`n") 'Unix wrapper should apply auth-center local default URL.'
Assert-Match 'bl_url=jdbc:dm://127.0.0.1:5236' ($unixCalls -join "`n") 'Unix wrapper should apply bl-center local default URL.'

Clear-Content $capturePath
$legacyOutput = Join-Path (New-TestDirectory 'legacy-output') 'legacy-dictionary.html'
$legacyWindowsEnv = New-Environment -FakeMvnwPath (Join-Path $fakeBin 'fake-mvnw.cmd') -CapturePath $capturePath
$legacyWindows = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $legacyWindowsScript, $legacyOutput, '--targets', 'bl-center') -Environment $legacyWindowsEnv
Assert-Equal 0 $legacyWindows.ExitCode 'Expected Windows legacy dictionary script to succeed.'
$legacyWindowsCalls = @(Get-Content $capturePath)
Assert-Equal 6 $legacyWindowsCalls.Count 'Expected Windows legacy wrapper to call fake mvnw twice with logged env snapshots.'
Assert-Match 'dictionary-html-legacy' $legacyWindowsCalls[3] 'Windows legacy wrapper should invoke the legacy CLI command.'
Assert-Match ([regex]::Escape($legacyOutput)) $legacyWindowsCalls[3] 'Windows legacy wrapper should include the requested output path.'

Clear-Content $capturePath
$legacyUnixScriptWsl = Convert-ToWslPath $legacyUnixScript
$legacyOutputWsl = Convert-ToWslPath $legacyOutput
$legacyUnixCommand = @"
set -eu
chmod +x '$fakeMvnwWsl' '$legacyUnixScriptWsl'
export JAVA_HOME='/fake/jdk-17'
export FAKE_MVNW_CAPTURE='$capturePathWsl'
export MVNW_BIN='$fakeMvnwWsl'
cd '$repoRootWsl'
'$legacyUnixScriptWsl' '$legacyOutputWsl' --targets bl-center
"@
$legacyUnix = Invoke-WslShell -Command $legacyUnixCommand
Assert-Equal 0 $legacyUnix.ExitCode 'Expected Unix legacy dictionary script to succeed.'
$legacyUnixCalls = @(Get-Content $capturePath)
Assert-Equal 6 $legacyUnixCalls.Count 'Expected Unix legacy wrapper to call fake mvnw twice with logged env snapshots.'
Assert-Match 'dictionary-html-legacy' $legacyUnixCalls[3] 'Unix legacy wrapper should invoke the legacy CLI command.'
Assert-Match '--output="/mnt/' $legacyUnixCalls[3] 'Unix legacy wrapper should include quoted output path.'

Write-Host 'Database dictionary script tests passed.'
