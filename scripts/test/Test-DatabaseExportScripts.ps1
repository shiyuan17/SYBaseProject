$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$windowsScript = Join-Path $repoRoot 'scripts\database\run-database-export.cmd'
$unixScript = Join-Path $repoRoot 'scripts\database\run-database-export.sh'

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

    $path = Join-Path $repoRoot ("tmp\test-db-export\{0}-{1}" -f $Name, (Get-Date -Format 'yyyyMMddHHmmssfff'))
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

function New-FakeDexpBin {
    $root = New-TestDirectory 'fake-dexp-bin'
    $cmdPath = Join-Path $root 'dexp.cmd'
    $ps1Path = Join-Path $root 'fake-dexp.ps1'
    $shPath = Join-Path $root 'dexp'

    @'
@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0fake-dexp.ps1" %*
exit /b %ERRORLEVEL%
'@ | Set-Content -NoNewline -Encoding ascii $cmdPath

    @'
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ArgsList
)

$line = ($ArgsList -join ' ')
if ($env:FAKE_DEXP_CAPTURE) {
    Add-Content -Path $env:FAKE_DEXP_CAPTURE -Value $line
}

if ($env:FAKE_DEXP_FAIL_MATCH -and $line.Contains($env:FAKE_DEXP_FAIL_MATCH)) {
    exit 9
}

$directory = ''
$fileName = ''
$logName = ''
foreach ($arg in $ArgsList) {
    if ($arg.StartsWith('DIRECTORY=')) {
        $directory = $arg.Substring(10).Trim('"')
    } elseif ($arg.StartsWith('FILE=')) {
        $fileName = $arg.Substring(5)
    } elseif ($arg.StartsWith('LOG=')) {
        $logName = $arg.Substring(4)
    }
}

if ($directory) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}
if ($directory -and $fileName) {
    New-Item -ItemType File -Force -Path (Join-Path $directory $fileName) | Out-Null
}
if ($directory -and $logName) {
    New-Item -ItemType File -Force -Path (Join-Path $directory $logName) | Out-Null
}
'@ | Set-Content -NoNewline -Encoding ascii $ps1Path

    @'
#!/usr/bin/env sh
set -eu

if [ -n "${FAKE_DEXP_CAPTURE:-}" ]; then
  printf '%s\n' "$*" >> "$FAKE_DEXP_CAPTURE"
fi

directory=
file_name=
log_name=
for arg in "$@"; do
  case "$arg" in
    DIRECTORY=*) directory=${arg#DIRECTORY=} ;;
    FILE=*) file_name=${arg#FILE=} ;;
    LOG=*) log_name=${arg#LOG=} ;;
  esac
done

if [ -n "${FAKE_DEXP_FAIL_MATCH:-}" ] && printf '%s' "$*" | grep -F -- "${FAKE_DEXP_FAIL_MATCH}" >/dev/null; then
  exit 9
fi

if [ -n "$directory" ]; then
  mkdir -p "$directory"
fi
if [ -n "$file_name" ]; then
  : > "$directory/$file_name"
fi
if [ -n "$log_name" ]; then
  : > "$directory/$log_name"
fi
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

    foreach ($argument in $Arguments) {
        [void]$startInfo.ArgumentList.Add($argument)
    }

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
        [string]$FakeBinPath,
        [string]$CapturePath
    )

    return @{
        AUTH_CENTER_DATASOURCE_URL = $null
        AUTH_CENTER_DATASOURCE_USERNAME = $null
        AUTH_CENTER_DATASOURCE_PASSWORD = $null
        BL_CENTER_DATASOURCE_URL = $null
        BL_CENTER_DATASOURCE_USERNAME = $null
        BL_CENTER_DATASOURCE_PASSWORD = $null
        DB_EXPORT_OUTPUT_DIR = $null
        DM_EXPORT_TOOL = $null
        DM_HOME = $null
        FAKE_DEXP_CAPTURE = $CapturePath
        PATH = if ($FakeBinPath) { "$FakeBinPath;$env:PATH" } else { $env:PATH }
    }
}

Assert-True (Test-Path $windowsScript) "Missing Windows export script: $windowsScript"
Assert-True (Test-Path $unixScript) "Missing Unix export script: $unixScript"

$fakeBin = New-FakeDexpBin
$capturePath = Join-Path (New-TestDirectory 'capture') 'dexp-calls.log'
New-Item -ItemType File -Force -Path $capturePath | Out-Null

$missingToolOutput = New-TestDirectory 'missing-tool-output'
$missingToolEnv = New-Environment -FakeBinPath '' -CapturePath $capturePath
$missingToolEnv['BL_CENTER_DATASOURCE_URL'] = 'jdbc:dm://127.0.0.1:5236'
$missingToolEnv['BL_CENTER_DATASOURCE_USERNAME'] = 'SYSDBA'
$missingToolEnv['BL_CENTER_DATASOURCE_PASSWORD'] = 'Dm.2027.Pwd.'
$missingTool = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript, $missingToolOutput) -Environment $missingToolEnv
Assert-True ($missingTool.ExitCode -ne 0) 'Expected missing-tool scenario to fail.'
Assert-Match 'Unable to locate dexp' ($missingTool.StdOut + "`n" + $missingTool.StdErr) 'Missing-tool failure should explain how to locate dexp.'

$partialConfigOutput = New-TestDirectory 'partial-config-output'
$partialConfigEnv = New-Environment -FakeBinPath $fakeBin -CapturePath $capturePath
$partialConfigEnv['BL_CENTER_DATASOURCE_URL'] = 'jdbc:dm://127.0.0.1:5236'
$partialConfigEnv['BL_CENTER_DATASOURCE_USERNAME'] = 'SYSDBA'
$partialConfig = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript, $partialConfigOutput) -Environment $partialConfigEnv
Assert-True ($partialConfig.ExitCode -ne 0) 'Expected partial-config scenario to fail.'
Assert-Match 'BL_CENTER_DATASOURCE_PASSWORD' ($partialConfig.StdOut + "`n" + $partialConfig.StdErr) 'Partial-config failure should name the missing variable.'

Clear-Content $capturePath
$defaultFallbackOutput = New-TestDirectory 'default-fallback-output'
$defaultFallbackEnv = New-Environment -FakeBinPath $fakeBin -CapturePath $capturePath
$defaultFallback = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript, $defaultFallbackOutput) -Environment $defaultFallbackEnv
Assert-Equal 0 $defaultFallback.ExitCode 'Expected no-config scenario to fall back to the default local datasource.'
$defaultFallbackCalls = @(Get-Content $capturePath)
Assert-Equal 1 $defaultFallbackCalls.Count 'Expected default local datasource fallback to deduplicate into one dexp call.'
$defaultFallbackDumps = @(Get-ChildItem -Path $defaultFallbackOutput -Filter *.dmp)
Assert-Equal 1 $defaultFallbackDumps.Count 'Expected default local datasource fallback to create one dump file.'
Assert-Match 'Using default local datasource for auth-center' ($defaultFallback.StdOut + "`n" + $defaultFallback.StdErr) 'Default fallback output should mention auth-center local defaults.'
Assert-Match 'Using default local datasource for bl-center' ($defaultFallback.StdOut + "`n" + $defaultFallback.StdErr) 'Default fallback output should mention bl-center local defaults.'
Assert-Match 'Deduplicating bl-center with auth-center' ($defaultFallback.StdOut + "`n" + $defaultFallback.StdErr) 'Default fallback output should still deduplicate matching local configs.'

Clear-Content $capturePath
$dedupeOutput = New-TestDirectory 'dedupe-output'
$dedupeEnv = New-Environment -FakeBinPath $fakeBin -CapturePath $capturePath
$dedupeEnv['AUTH_CENTER_DATASOURCE_URL'] = 'jdbc:dm://127.0.0.1:5236'
$dedupeEnv['AUTH_CENTER_DATASOURCE_USERNAME'] = 'SYSDBA'
$dedupeEnv['AUTH_CENTER_DATASOURCE_PASSWORD'] = 'Dm.2027.Pwd.'
$dedupeEnv['BL_CENTER_DATASOURCE_URL'] = 'jdbc:dm://127.0.0.1:5236'
$dedupeEnv['BL_CENTER_DATASOURCE_USERNAME'] = 'SYSDBA'
$dedupeEnv['BL_CENTER_DATASOURCE_PASSWORD'] = 'Dm.2027.Pwd.'
$dedupe = Invoke-Process -FilePath 'cmd.exe' -Arguments @('/c', $windowsScript, $dedupeOutput) -Environment $dedupeEnv
Assert-Equal 0 $dedupe.ExitCode 'Expected dedupe scenario to succeed.'
$dedupeCalls = @(Get-Content $capturePath)
Assert-Equal 1 $dedupeCalls.Count 'Expected duplicate datasource config to call dexp once.'
$dedupeDumps = @(Get-ChildItem -Path $dedupeOutput -Filter *.dmp)
Assert-Equal 1 $dedupeDumps.Count 'Expected duplicate datasource config to create one dump file.'
Assert-Match 'auth-center, bl-center' ($dedupe.StdOut + "`n" + $dedupe.StdErr) 'Dedupe output should mention the shared auth-center and bl-center backup.'

Clear-Content $capturePath
$unixOutput = New-TestDirectory 'unix-output'
$repoRootWsl = Convert-ToWslPath $repoRoot
$unixScriptWsl = Convert-ToWslPath $unixScript
$unixOutputWsl = Convert-ToWslPath $unixOutput
$fakeBinWsl = Convert-ToWslPath $fakeBin
$capturePathWsl = Convert-ToWslPath $capturePath
$unixCommand = @"
set -eu
chmod +x '$fakeBinWsl/dexp' '$unixScriptWsl'
export PATH='$fakeBinWsl':'`$PATH'
export FAKE_DEXP_CAPTURE='$capturePathWsl'
export AUTH_CENTER_DATASOURCE_URL='jdbc:dm://127.0.0.1:5236'
export AUTH_CENTER_DATASOURCE_USERNAME='SYSDBA'
export AUTH_CENTER_DATASOURCE_PASSWORD='Dm.2027.Pwd.'
export BL_CENTER_DATASOURCE_URL='jdbc:dm://127.0.0.2:5236'
export BL_CENTER_DATASOURCE_USERNAME='SYSDBA'
export BL_CENTER_DATASOURCE_PASSWORD='Dm.2027.Pwd.'
cd '$repoRootWsl'
'$unixScriptWsl' '$unixOutputWsl'
"@
$unix = Invoke-WslShell -Command $unixCommand
Assert-Equal 0 $unix.ExitCode 'Expected Unix export script to succeed.'
$unixCalls = @(Get-Content $capturePath)
Assert-Equal 2 $unixCalls.Count 'Expected distinct auth-center and bl-center configs to call dexp twice.'
$unixDumps = @(Get-ChildItem -Path $unixOutput -Filter *.dmp)
Assert-Equal 2 $unixDumps.Count 'Expected distinct auth-center and bl-center configs to create two dump files.'
Assert-Match 'export_count=2' ($unix.StdOut + "`n" + $unix.StdErr) 'Unix summary should report two exports.'

Write-Host 'Database export script tests passed.'
