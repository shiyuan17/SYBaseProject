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

    throw "bash is required to test the prod sync scripts."
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
        [hashtable] $Environment = @()
    )

    $bash = Find-Bash
    $envAssignments = @()
    foreach ($key in $Environment.Keys) {
        $escaped = $Environment[$key].Replace("'", "'\''")
        $envAssignments += "$key='$escaped'"
    }

    $command = ($envAssignments + @("'$Script'")) -join " "
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

function Assert-OutputContains([string] $Output, [string] $Expected, [string] $Message) {
    if (-not $Output.Contains($Expected)) {
        throw ("{0} Expected output to contain '{1}'. Actual output: {2}" -f $Message, $Expected, $Output)
    }
}

function New-ZipWithXcWeb([string] $ZipPath, [string] $MarkerContent) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    if (Test-Path $ZipPath) {
        Remove-Item -Force $ZipPath
    }

    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $entry = $zip.CreateEntry("xc_web/index.html")
        $writer = New-Object System.IO.StreamWriter($entry.Open())
        try {
            $writer.Write($MarkerContent)
        }
        finally {
            $writer.Dispose()
        }
    }
    finally {
        $zip.Dispose()
    }
}

function New-ZipWithRootWeb([string] $ZipPath, [string] $MarkerContent) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    if (Test-Path $ZipPath) {
        Remove-Item -Force $ZipPath
    }

    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $entry = $zip.CreateEntry("index.html")
        $writer = New-Object System.IO.StreamWriter($entry.Open())
        try {
            $writer.Write($MarkerContent)
        }
        finally {
            $writer.Dispose()
        }
    }
    finally {
        $zip.Dispose()
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$legacyCentersScript = Join-Path $repoRoot "scripts\prod\sync-centers-from-smb.sh"
$legacyWebScript = Join-Path $repoRoot "scripts\prod\sync-web-from-smb.sh"
$centersScript = Join-Path $repoRoot "scripts\prod\unix\sync-centers-from-smb.sh"
$webScript = Join-Path $repoRoot "scripts\prod\unix\sync-web-from-smb.sh"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("prod-sync-tests-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    Assert-True (-not (Test-Path $legacyCentersScript)) "Legacy centers sync script should be removed from scripts/prod."
    Assert-True (-not (Test-Path $legacyWebScript)) "Legacy web sync script should be removed from scripts/prod."
    Assert-True (Test-Path $centersScript) "Centers sync script should exist at scripts/prod/unix/sync-centers-from-smb.sh."
    Assert-True (Test-Path $webScript) "Web sync script should exist at scripts/prod/unix/sync-web-from-smb.sh."

    $sourceDir = Join-Path $tempRoot "smb-source"
    $apiTarget = Join-Path $tempRoot "online-api"
    $webTarget = Join-Path $tempRoot "online-web"
    New-Item -ItemType Directory -Path $sourceDir, $apiTarget, $webTarget | Out-Null

    Set-Content -Path (Join-Path $sourceDir "bl-center.jar") -Value "bl-jar" -NoNewline
    Set-Content -Path (Join-Path $sourceDir "auth-center.jar") -Value "auth-jar" -NoNewline

    $runCenters = Join-Path $apiTarget "run-centers.sh"
    Set-Content -Path $runCenters -Value @("#!/usr/bin/env sh", 'printf ''%s\n'' "$*" > run-centers.args')
    & (Find-Bash) -lc "chmod +x '$(To-BashPath $runCenters)'"

    $centersResult = Invoke-BashScript -Script (To-BashPath $centersScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        API_TARGET_DIR = To-BashPath $apiTarget
    }

    Assert-True ($centersResult.ExitCode -eq 0) "centers sync should succeed. Output: $($centersResult.Output)"
    Assert-True ((Get-Content (Join-Path $apiTarget "bl-center.jar") -Raw) -eq "bl-jar") "bl-center.jar should be copied."
    Assert-True ((Get-Content (Join-Path $apiTarget "auth-center.jar") -Raw) -eq "auth-jar") "auth-center.jar should be copied."
    Assert-True ((Get-Content (Join-Path $apiTarget "run-centers.args") -Raw).Trim() -eq "restart all") "run-centers.sh should be called with restart all."
    Assert-OutputContains $centersResult.Output "Start Java center sync" "centers sync should log start."
    Assert-OutputContains $centersResult.Output "bl-center.jar copy success" "centers sync should log bl jar copy success."
    Assert-OutputContains $centersResult.Output "auth-center.jar copy success" "centers sync should log auth jar copy success."
    Assert-OutputContains $centersResult.Output "run-centers.sh restart all success" "centers sync should log run-centers success."
    Assert-OutputContains $centersResult.Output "Java center sync success" "centers sync should log completion."

    New-ZipWithXcWeb -ZipPath (Join-Path $sourceDir "xc_web.zip") -MarkerContent "new-web"
    New-Item -ItemType Directory -Path (Join-Path $webTarget "xc_web") | Out-Null
    Set-Content -Path (Join-Path $webTarget "xc_web/old.txt") -Value "old-file" -NoNewline

    $webResult = Invoke-BashScript -Script (To-BashPath $webScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        WEB_TARGET_DIR = To-BashPath $webTarget
    }

    Assert-True ($webResult.ExitCode -eq 0) "web sync should succeed. Output: $($webResult.Output)"
    Assert-True ((Get-Content (Join-Path $webTarget "xc_web/index.html") -Raw) -eq "new-web") "xc_web should be replaced with zip content."
    Assert-True (-not (Test-Path (Join-Path $webTarget "xc_web/old.txt"))) "old xc_web files should not remain after replacement."
    Assert-True (Test-Path (Join-Path $webTarget "xc_web.zip")) "xc_web.zip should be copied to target directory."
    Assert-OutputContains $webResult.Output "Start Web sync" "web sync should log start."
    Assert-OutputContains $webResult.Output "xc_web.zip copy success" "web sync should log zip copy success."
    Assert-OutputContains $webResult.Output "xc_web.zip unzip success" "web sync should log unzip success."
    Assert-OutputContains $webResult.Output "xc_web directory replace success" "web sync should log replacement success."
    Assert-OutputContains $webResult.Output "xc_web sync success" "web sync should log final success."

    New-ZipWithRootWeb -ZipPath (Join-Path $sourceDir "xc_web.zip") -MarkerContent "root-web"
    Set-Content -Path (Join-Path $webTarget "xc_web/index.html") -Value "old-web-before-root-zip" -NoNewline

    $rootZipWebResult = Invoke-BashScript -Script (To-BashPath $webScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        WEB_TARGET_DIR = To-BashPath $webTarget
    }

    Assert-True ($rootZipWebResult.ExitCode -eq 0) "web sync should support zips whose root contains web files. Output: $($rootZipWebResult.Output)"
    Assert-True ((Get-Content (Join-Path $webTarget "xc_web/index.html") -Raw) -eq "root-web") "zip root files should be deployed under xc_web."
    Assert-OutputContains $rootZipWebResult.Output "Zip root content will be deployed as xc_web" "web sync should log zip root fallback."

    Set-Content -Path (Join-Path $sourceDir "xc_web.zip") -Value "not-a-zip" -NoNewline
    Set-Content -Path (Join-Path $webTarget "xc_web/index.html") -Value "stable-old-web" -NoNewline

    $badWebResult = Invoke-BashScript -Script (To-BashPath $webScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        WEB_TARGET_DIR = To-BashPath $webTarget
    }

    Assert-True ($badWebResult.ExitCode -ne 0) "web sync should fail for invalid zip."
    Assert-True ((Get-Content (Join-Path $webTarget "xc_web/index.html") -Raw) -eq "stable-old-web") "invalid zip should not remove existing xc_web."

    Remove-Item -Force (Join-Path $sourceDir "bl-center.jar")
    $missingJarResult = Invoke-BashScript -Script (To-BashPath $centersScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        API_TARGET_DIR = To-BashPath $apiTarget
    }

    Assert-True ($missingJarResult.ExitCode -ne 0) "centers sync should fail when bl-center.jar is missing."
    Assert-True ($missingJarResult.Output.Contains("Required file not found")) "missing jar failure should explain the missing file."

    Set-Content -Path (Join-Path $sourceDir "bl-center.jar") -Value "bl-jar" -NoNewline
    Remove-Item -Force $runCenters
    $missingRunCentersResult = Invoke-BashScript -Script (To-BashPath $centersScript) -Environment @{
        SOURCE_DIR = To-BashPath $sourceDir
        API_TARGET_DIR = To-BashPath $apiTarget
    }

    Assert-True ($missingRunCentersResult.ExitCode -ne 0) "centers sync should fail when run-centers.sh is missing."
    Assert-True ($missingRunCentersResult.Output.Contains("Required file not found")) "missing run-centers failure should explain the missing file."

    Write-Host "Prod sync script tests passed."
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
