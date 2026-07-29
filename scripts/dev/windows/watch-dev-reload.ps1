[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $ModuleName,

    [Parameter(Mandatory = $true)]
    [string] $LogFile,

    [int] $PollIntervalSeconds = 2
)

$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$serviceSourcePath = Join-Path $rootDir "$ModuleName\src\main"
$sharedSourcePaths = @(
    (Join-Path $rootDir "common\common-core\src\main"),
    (Join-Path $rootDir "common\common-security\src\main"),
    (Join-Path $rootDir "common\common-web\src\main")
)
$watchExtensions = @(".java", ".properties", ".yml", ".yaml", ".xml", ".sql")
$compileCommand = "mvnw.cmd -Dmaven.repo.local=.m2/repository -pl $ModuleName -am -DskipTests compile"
$triggerFile = Join-Path $rootDir "$ModuleName\target\classes\.reloadtrigger"

function Write-WatcherLog {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $timestampedMessage = "[{0}] [hot-reload] {1}" -f (Get-Date).ToString("u"), $Message
    Add-Content -LiteralPath $LogFile -Value $timestampedMessage -Encoding UTF8
}

function Get-Fingerprint {
    param(
        [Parameter(Mandatory = $true)]
        [string[]] $Paths
    )

    $files = foreach ($path in $Paths) {
        if (Test-Path -LiteralPath $path) {
            Get-ChildItem -LiteralPath $path -File -Recurse | Where-Object {
                $watchExtensions -contains $_.Extension
            }
        }
    }

    $sortedFiles = $files | Sort-Object FullName
    if (-not $sortedFiles) {
        return ""
    }

    $builder = [System.Text.StringBuilder]::new()
    foreach ($file in $sortedFiles) {
        [void] $builder.Append($file.FullName)
        [void] $builder.Append("|")
        [void] $builder.Append($file.LastWriteTimeUtc.Ticks)
        [void] $builder.Append("|")
        [void] $builder.Append($file.Length)
        [void] $builder.Append("`n")
    }

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($builder.ToString())
    $hash = [System.Security.Cryptography.SHA256]::HashData($bytes)
    return [Convert]::ToHexString($hash)
}

function Invoke-Compile {
    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = "cmd.exe"
    $processInfo.Arguments = "/d /c $compileCommand 2>&1"
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $false
    $processInfo.WorkingDirectory = $rootDir

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $processInfo

    try {
        [void] $process.Start()
        while (($line = $process.StandardOutput.ReadLine()) -ne $null) {
            Add-Content -LiteralPath $LogFile -Value $line -Encoding UTF8
        }
        $process.WaitForExit()
        return $process.ExitCode
    }
    finally {
        $process.Dispose()
    }
}

function Touch-TriggerFile {
    $triggerDirectory = Split-Path -Parent $triggerFile
    if (-not (Test-Path -LiteralPath $triggerDirectory)) {
        New-Item -ItemType Directory -Path $triggerDirectory -Force | Out-Null
    }
    if (-not (Test-Path -LiteralPath $triggerFile)) {
        New-Item -ItemType File -Path $triggerFile -Force | Out-Null
    }
    (Get-Item -LiteralPath $triggerFile).LastWriteTimeUtc = [DateTime]::UtcNow
}

$serviceFingerprint = Get-Fingerprint -Paths @($serviceSourcePath)
$sharedFingerprint = Get-Fingerprint -Paths $sharedSourcePaths
Write-WatcherLog "Watching $ModuleName sources for hot reload changes."

while ($true) {
    Start-Sleep -Seconds $PollIntervalSeconds

    $nextServiceFingerprint = Get-Fingerprint -Paths @($serviceSourcePath)
    $nextSharedFingerprint = Get-Fingerprint -Paths $sharedSourcePaths

    if ($nextServiceFingerprint -eq $serviceFingerprint -and $nextSharedFingerprint -eq $sharedFingerprint) {
        continue
    }

    Write-WatcherLog "Detected source change. Running incremental compile."
    $exitCode = Invoke-Compile

    if ($exitCode -eq 0) {
        Touch-TriggerFile
        Write-WatcherLog "Compile succeeded and restart trigger updated."
    }
    else {
        Write-WatcherLog "Compile failed with exit code $exitCode."
    }

    $serviceFingerprint = $nextServiceFingerprint
    $sharedFingerprint = $nextSharedFingerprint
}
