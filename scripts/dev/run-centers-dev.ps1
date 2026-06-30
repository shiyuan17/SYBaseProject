[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$services = @(
    @{
        Name = "bl-center"
        ScriptPath = Join-Path $rootDir "scripts\dev\run-bl-center-dev.cmd"
    },
    @{
        Name = "auth-center"
        ScriptPath = Join-Path $rootDir "scripts\dev\run-auth-center-dev.cmd"
    }
)

foreach ($service in $services) {
    if (-not (Test-Path -LiteralPath $service.ScriptPath)) {
        throw "Startup script not found: $($service.ScriptPath)"
    }

    $windowTitle = "$($service.Name) [dev]"
    $command = "title $windowTitle && set ""SPRING_PROFILES_ACTIVE=dev"" && call ""$($service.ScriptPath)"""

    Start-Process -FilePath "cmd.exe" -WorkingDirectory $rootDir -ArgumentList @("/k", $command)
}

Write-Host "Started bl-center and auth-center in separate windows with SPRING_PROFILES_ACTIVE=dev."
