[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string] $Action = "start",

    [Parameter(Position = 1)]
    [string] $Service = "all"
)

$ErrorActionPreference = "Stop"

$script:ExtraArgs = @($args)
$script:ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$script:ProdDir = (Resolve-Path (Join-Path $script:ScriptDir "..")).Path
$script:DefaultConfigFile = Join-Path (Join-Path $script:ProdDir "config") "run-centers.conf"
$script:ConfigFile = if ($env:RUN_CENTERS_CONFIG_FILE) { $env:RUN_CENTERS_CONFIG_FILE } else { $script:DefaultConfigFile }
$script:DefaultProd06ConfigFile = Join-Path (Join-Path $script:ProdDir "config") "run-centers-prod-06.conf"
$script:Prod06ConfigFile = if ($env:RUN_CENTERS_PROD06_CONFIG_FILE) { $env:RUN_CENTERS_PROD06_CONFIG_FILE } else { $script:DefaultProd06ConfigFile }
$script:HelperScript = Join-Path $script:ScriptDir "run-centers-prod-06.helpers.ps1"

$script:Services = @{
    bl = @{
        Name = "bl-center"
        JarEnv = "BL_JAR_PATH"
        DefaultJar = "bl-center.jar"
        JavaOptsEnv = "BL_JAVA_OPTS"
        ProfileEnv = "BL_CENTER_SPRING_PROFILES_ACTIVE"
        DatasourcePrefix = "BL_CENTER"
    }
    auth = @{
        Name = "auth-center"
        JarEnv = "AUTH_JAR_PATH"
        DefaultJar = "auth-center.jar"
        JavaOptsEnv = "AUTH_JAVA_OPTS"
        ProfileEnv = "AUTH_CENTER_SPRING_PROFILES_ACTIVE"
        DatasourcePrefix = "AUTH_CENTER"
    }
}

if (-not (Test-Path -LiteralPath $script:HelperScript)) {
    throw "Required file not found: $script:HelperScript"
}

. $script:HelperScript

function Get-ServiceDefinition {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    if (-not $script:Services.ContainsKey($ServiceKey)) {
        throw "Unsupported service: $ServiceKey"
    }

    return $script:Services[$ServiceKey]
}

function Get-TargetServices {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Selector
    )

    switch ($Selector) {
        "all" { return @("bl", "auth") }
        "bl" { return @("bl") }
        "auth" { return @("auth") }
        default { throw "Unsupported service selector: $Selector" }
    }
}

function Get-JarPath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $defaultPath = Join-Path $script:ProdDir $definition.DefaultJar
    return (Get-Setting -Name $definition.JarEnv -DefaultValue $defaultPath)
}

function Get-PidFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    return (Get-ServiceFile -ServiceKey $ServiceKey -Kind "pid")
}

function Get-StdoutLogFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    return (Get-ServiceFile -ServiceKey $ServiceKey -Kind "log")
}

function Get-StderrLogFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    return (Get-ServiceFile -ServiceKey $ServiceKey -Kind "stderr")
}

function Ensure-Directories {
    foreach ($path in @((Get-ManagedDirectory "RUNTIME_DIR"), (Get-ManagedDirectory "LOG_DIR"))) {
        if (-not (Test-Path -LiteralPath $path)) {
            New-Item -ItemType Directory -Path $path | Out-Null
        }
    }
}

function Get-EffectiveProfile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $serviceProfile = Get-Setting -Name $definition.ProfileEnv
    if ($serviceProfile) {
        return $serviceProfile
    }

    $sharedProfile = Get-Setting -Name "SPRING_PROFILES_ACTIVE" -DefaultValue "prod"
    if ($sharedProfile) {
        return $sharedProfile
    }

    return "prod"
}

function Test-ProfileRequiresExternalDatasource {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Profile
    )

    $normalized = ($Profile -replace "\s", "").ToLowerInvariant().Split(",")
    return (-not ($normalized -contains "local" -or $normalized -contains "test"))
}

function Test-CliDatasourceValue {
    param(
        [Parameter(Mandatory = $true)]
        [string] $PropertyName,

        [string[]] $Arguments = @()
    )

    foreach ($argument in $Arguments) {
        if ($argument -like "--$PropertyName=*") {
            return $true
        }
    }

    return $false
}

function Require-ServiceRuntimeConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [string[]] $Arguments = @()
    )

    $profile = Get-EffectiveProfile $ServiceKey
    if (-not (Test-ProfileRequiresExternalDatasource $profile)) {
        return
    }

    $definition = Get-ServiceDefinition $ServiceKey
    foreach ($setting in @("URL", "USERNAME", "PASSWORD")) {
        $envName = "{0}_DATASOURCE_{1}" -f $definition.DatasourcePrefix, $setting
        $envValue = Get-Setting -Name $envName
        if ($envValue) {
            continue
        }

        $propertyName = "spring.datasource.{0}" -f $setting.ToLowerInvariant()
        if (Test-CliDatasourceValue -PropertyName $propertyName -Arguments $Arguments) {
            continue
        }

        throw "Missing required datasource setting for $($definition.Name) under profile '$profile': $envName"
    }
}

function Require-Jar {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $jarPath = Get-JarPath $ServiceKey
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "$((Get-ServiceDefinition $ServiceKey).Name) jar not found: $jarPath"
    }
}

function Get-RunningProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $pidFile = Get-PidFile $ServiceKey
    if (-not (Test-Path -LiteralPath $pidFile)) {
        return $null
    }

    $pidValue = ([System.IO.File]::ReadAllText($pidFile)).Trim()
    if (-not $pidValue) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    $process = Get-Process -Id ([int] $pidValue) -ErrorAction SilentlyContinue
    if (-not $process) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    return $process
}

function Get-JavaArguments {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [string[]] $Arguments = @()
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $javaArgs = New-Object System.Collections.Generic.List[string]

    foreach ($option in (Split-CommandLine (Get-Setting -Name "JAVA_OPTS"))) {
        $javaArgs.Add($option)
    }

    foreach ($option in (Split-CommandLine (Get-Setting -Name $definition.JavaOptsEnv))) {
        $javaArgs.Add($option)
    }

    $javaArgs.Add("-jar")
    $javaArgs.Add((Get-JarPath $ServiceKey))
    $javaArgs.Add("--spring.profiles.active=$(Get-EffectiveProfile $ServiceKey)")

    foreach ($argument in $Arguments) {
        $javaArgs.Add($argument)
    }

    return ,$javaArgs.ToArray()
}

function Write-ServiceStatus {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $process = Get-RunningProcess $ServiceKey
    if ($process) {
        Write-Output "$($definition.Name) is running with PID $($process.Id)"
    }
    else {
        Write-Output "$($definition.Name) is not running"
    }

    Write-Output "Profile: $(Get-EffectiveProfile $ServiceKey)"
    Write-Output "Config: $script:ConfigFile"
    Write-Output "Jar: $(Get-JarPath $ServiceKey)"
    Write-Output "Log: $(Get-StdoutLogFile $ServiceKey)"
    Write-Output "ErrorLog: $(Get-StderrLogFile $ServiceKey)"
}

function Start-ServiceProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [string[]] $Arguments = @()
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $existingProcess = Get-RunningProcess $ServiceKey
    if ($existingProcess) {
        Write-Output "$($definition.Name) is already running with PID $($existingProcess.Id)"
        return
    }

    Require-Jar $ServiceKey
    Require-ServiceRuntimeConfig -ServiceKey $ServiceKey -Arguments $Arguments

    $javaCmd = Get-Setting -Name "JAVA_CMD" -DefaultValue "java"
    if (-not (Get-Command $javaCmd -ErrorAction SilentlyContinue)) {
        throw "Java command is not available: $javaCmd"
    }

    $stdoutLog = Get-StdoutLogFile $ServiceKey
    $stderrLog = Get-StderrLogFile $ServiceKey
    $pidFile = Get-PidFile $ServiceKey

    if (-not (Test-Path -LiteralPath $stdoutLog)) {
        New-Item -ItemType File -Path $stdoutLog | Out-Null
    }
    if (-not (Test-Path -LiteralPath $stderrLog)) {
        New-Item -ItemType File -Path $stderrLog | Out-Null
    }

    $startProcessArguments = @{
        FilePath = $javaCmd
        ArgumentList = (Join-CommandLineArguments -Arguments (Get-JavaArguments -ServiceKey $ServiceKey -Arguments $Arguments))
        WorkingDirectory = $script:ProdDir
        RedirectStandardOutput = $stdoutLog
        RedirectStandardError = $stderrLog
        PassThru = $true
    }
    if ($Host.Version.Major -ge 3) {
        $startProcessArguments["WindowStyle"] = "Hidden"
    }

    $process = Start-Process @startProcessArguments

    [System.IO.File]::WriteAllText($pidFile, [string] $process.Id)
    Start-Sleep -Seconds 1
    $process.Refresh()
    if ($process.HasExited) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        throw "$($definition.Name) exited immediately after start. Check logs: $stdoutLog and $stderrLog"
    }

    Write-Output "Started $($definition.Name) with PID $($process.Id)"
    Write-Output "Profile: $(Get-EffectiveProfile $ServiceKey)"
    Write-Output "Config: $script:ConfigFile"
    Write-Output "Jar: $(Get-JarPath $ServiceKey)"
    Write-Output "Log: $stdoutLog"
    Write-Output "ErrorLog: $stderrLog"
}

function Stop-ServiceProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey
    )

    $definition = Get-ServiceDefinition $ServiceKey
    $process = Get-RunningProcess $ServiceKey
    if (-not $process) {
        Write-Output "$($definition.Name) is not running"
        return
    }

    Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
    try {
        Wait-Process -Id $process.Id -Timeout 20 -ErrorAction Stop
    }
    catch {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $process.Id -Timeout 5 -ErrorAction SilentlyContinue
    }

    Remove-Item -LiteralPath (Get-PidFile $ServiceKey) -Force -ErrorAction SilentlyContinue
    Write-Output "Stopped $($definition.Name)"
}

function Restart-ServiceProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [string[]] $Arguments = @()
    )

    Stop-ServiceProcess $ServiceKey
    Start-ServiceProcess -ServiceKey $ServiceKey -Arguments $Arguments
}

function Show-ServiceLog {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [string[]] $Arguments = @()
    )

    $stdoutLog = Get-StdoutLogFile $ServiceKey
    $stderrLog = Get-StderrLogFile $ServiceKey
    $paths = @()

    foreach ($candidate in @($stdoutLog, $stderrLog)) {
        if (Test-Path -LiteralPath $candidate) {
            $paths += $candidate
        }
    }

    if ($paths.Count -eq 0) {
        throw "No log files found for $((Get-ServiceDefinition $ServiceKey).Name)."
    }

    $tailLines = [int] (Get-Setting -Name "TAIL_LINES" -DefaultValue "200")
    if ($Arguments -contains "-f" -or $Arguments -contains "--follow") {
        Get-TailContentCompat -Path $paths -TailLines $tailLines
        Get-Content -Path $paths -Wait
        return
    }

    Get-TailContentCompat -Path $paths -TailLines $tailLines
}

function Show-Usage {
    @"
Usage:
  .\run-centers-prod-06.ps1 start [all|bl|auth] [extra java args...]
  .\run-centers-prod-06.ps1 stop [all|bl|auth]
  .\run-centers-prod-06.ps1 restart [all|bl|auth] [extra java args...]
  .\run-centers-prod-06.ps1 status [all|bl|auth]
  .\run-centers-prod-06.ps1 log [bl|auth] [-f]
"@
}

Import-KeyValueConfig -Path $script:ConfigFile
Import-KeyValueConfig -Path $script:Prod06ConfigFile
[Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "prod-06", "Process")
Ensure-Directories
if ($Action -ieq "logs") {
    $Action = "log"
}

switch ($Action.ToLowerInvariant()) {
    "start" {
        foreach ($target in (Get-TargetServices $Service)) {
            Start-ServiceProcess -ServiceKey $target -Arguments $script:ExtraArgs
        }
    }
    "stop" {
        foreach ($target in (Get-TargetServices $Service)) {
            Stop-ServiceProcess $target
        }
    }
    "restart" {
        foreach ($target in (Get-TargetServices $Service)) {
            Restart-ServiceProcess -ServiceKey $target -Arguments $script:ExtraArgs
        }
    }
    "status" {
        foreach ($target in (Get-TargetServices $Service)) {
            Write-ServiceStatus $target
        }
    }
    "log" {
        if ($Service -eq "all") {
            throw "log command only supports one service at a time: bl or auth"
        }
        Show-ServiceLog -ServiceKey $Service -Arguments $script:ExtraArgs
    }
    default {
        Show-Usage
        exit 1
    }
}
