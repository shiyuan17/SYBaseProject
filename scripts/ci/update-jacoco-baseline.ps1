param(
    [string]$ReportPath = "bl-center/target/site/jacoco/jacoco.xml",
    [string]$OutputPath = "bl-center/jacoco-baseline.properties"
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
$resolvedReportPath = Join-Path $repoRoot $ReportPath
$resolvedOutputPath = Join-Path $repoRoot $OutputPath
$outputDir = Split-Path -Parent $resolvedOutputPath

if (-not (Test-Path $resolvedReportPath -PathType Leaf)) {
    throw "Jacoco report not found: $resolvedReportPath"
}

[xml]$xml = Get-Content $resolvedReportPath
$lineCounter = $xml.report.counter | Where-Object { $_.type -eq 'LINE' } | Select-Object -Last 1
$branchCounter = $xml.report.counter | Where-Object { $_.type -eq 'BRANCH' } | Select-Object -Last 1

if (-not $lineCounter -or -not $branchCounter) {
    throw "Jacoco counters not found in report: $resolvedReportPath"
}

function Get-RatioText {
    param($Counter)

    $covered = [double]$Counter.covered
    $missed = [double]$Counter.missed
    if (($covered + $missed) -le 0) {
        return "0.000000"
    }
    return [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, "{0:F6}", ($covered / ($covered + $missed)))
}

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$content = @(
    "# Generated from $ReportPath",
    "jacoco.minimum.line.coverage=$(Get-RatioText $lineCounter)",
    "jacoco.minimum.branch.coverage=$(Get-RatioText $branchCounter)"
) -join "`n"
[System.IO.File]::WriteAllText($resolvedOutputPath, $content + "`n", $utf8NoBom)
