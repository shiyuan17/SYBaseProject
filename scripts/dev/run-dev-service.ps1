[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $ModuleName,

    [Parameter(Mandatory = $true)]
    [string] $LogFile
)

$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$commonClasspath = @(
    "../common/common-core/target/classes",
    "../common/common-security/target/classes",
    "../common/common-web/target/classes"
) -join ","
$compileCommand = "mvnw.cmd -Dmaven.repo.local=.m2/repository -pl $ModuleName -am -DskipTests compile"
$runCommand = "mvnw.cmd -Dmaven.repo.local=.m2/repository -f $ModuleName/pom.xml -DskipTests ""-Dspring-boot.run.profiles=dev"" ""-Dspring-boot.run.additional-classpath-elements=$commonClasspath"" spring-boot:run"
$watcherScript = Join-Path $rootDir "scripts\dev\watch-dev-reload.ps1"

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Command
    )

    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = "cmd.exe"
    $processInfo.Arguments = "/d /c $Command 2>&1"
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $false
    $processInfo.WorkingDirectory = $rootDir

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $processInfo

    $logStream = [System.IO.FileStream]::new(
        $LogFile,
        [System.IO.FileMode]::Append,
        [System.IO.FileAccess]::Write,
        [System.IO.FileShare]::ReadWrite
    )
    $writer = [System.IO.StreamWriter]::new(
        $logStream,
        [System.Text.UTF8Encoding]::new($false)
    )
    $writer.AutoFlush = $true

    try {
        $startLine = "[{0}] START {1}" -f (Get-Date).ToString("u"), $Command
        [Console]::Out.WriteLine($startLine)
        $writer.WriteLine("")
        $writer.WriteLine($startLine)

        [void] $process.Start()
        while (($line = $process.StandardOutput.ReadLine()) -ne $null) {
            [Console]::Out.WriteLine($line)
            $writer.WriteLine($line)
        }
        $process.WaitForExit()

        $endLine = "[{0}] END EXIT {1}" -f (Get-Date).ToString("u"), $process.ExitCode
        [Console]::Out.WriteLine($endLine)
        $writer.WriteLine($endLine)

        return $process.ExitCode
    }
    finally {
        $writer.Dispose()
        $logStream.Dispose()
        $process.Dispose()
    }
}

$logDirectory = Split-Path -Parent $LogFile
if (-not (Test-Path -LiteralPath $logDirectory)) {
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
}

$compileExitCode = Invoke-LoggedCommand -Command $compileCommand
if ($compileExitCode -ne 0) {
    exit $compileExitCode
}

$watcher = Start-Process `
    -FilePath "powershell.exe" `
    -WorkingDirectory $rootDir `
    -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        $watcherScript,
        "-ModuleName",
        $ModuleName,
        "-LogFile",
        $LogFile
    ) `
    -WindowStyle Hidden `
    -PassThru

try {
    $runExitCode = Invoke-LoggedCommand -Command $runCommand
    exit $runExitCode
}
finally {
    if ($watcher -and -not $watcher.HasExited) {
        Stop-Process -Id $watcher.Id -Force
    }
}
