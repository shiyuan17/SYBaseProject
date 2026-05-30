param(
    [string]$OutputPath = "docs/reports/largest-files-report.md"
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
$resolvedOutputPath = Join-Path $repoRoot $OutputPath
$outputDir = Split-Path -Parent $resolvedOutputPath
$reportTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

$excludedSegments = @("\.git\", "\.idea\", "\.m2\", "\.mvn\", "\target\")
$managedExtensions = @{
    ".java" = 1000
    ".md" = 300
    ".yml" = 200
    ".yaml" = 200
    ".xml" = 300
    ".json" = 200
    ".properties" = 200
    ".sh" = 500
    ".ps1" = 500
    ".cmd" = 200
    ".bat" = 200
}

function Test-IncludedPath {
    param([string]$Path)

    foreach ($segment in $excludedSegments) {
        if ($Path -like "*$segment*") {
            return $false
        }
    }
    return $true
}

function Get-LineCount {
    param([string]$Path)

    return @(Get-Content $Path).Count
}

function Get-ManagedFiles {
    $gitFiles = @()
    $seenRelativePaths = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)

    try {
        $gitRoot = git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $gitRoot) {
            $gitFiles = git -c core.quotepath=false ls-files --cached --modified --others --exclude-standard
        }
    } catch {
        $gitFiles = @()
    }

    if ($gitFiles.Count -gt 0) {
        foreach ($relativePath in $gitFiles) {
            if ([string]::IsNullOrWhiteSpace($relativePath)) {
                continue
            }

            if (-not $seenRelativePaths.Add($relativePath)) {
                continue
            }

            $fullPath = Join-Path $repoRoot $relativePath
            if (-not (Test-Path $fullPath -PathType Leaf)) {
                continue
            }

            if (-not (Test-IncludedPath $fullPath)) {
                continue
            }

            Get-Item $fullPath
        }
        return
    }

    Get-ChildItem -Path $repoRoot -Recurse -File | Where-Object { Test-IncludedPath $_.FullName }
}

function Convert-ToRepoRelativePath {
    param([string]$Path)

    $rootUri = New-Object System.Uri(($repoRoot.TrimEnd("\") + "\"))
    $fileUri = New-Object System.Uri($Path)
    return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($fileUri).ToString()).Replace("\", "/")
}

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$allFiles = Get-ManagedFiles
$exemptionConfigPath = Join-Path $repoRoot "docs\\file-health-exemptions.properties"
$baselineConfigPath = Join-Path $repoRoot "docs\\file-health-baseline.properties"

$javaCounts = foreach ($file in $allFiles | Where-Object { $_.Extension -eq ".java" }) {
    [PSCustomObject]@{
        Lines = Get-LineCount $file.FullName
        File  = Convert-ToRepoRelativePath $file.FullName
    }
}

$textViolations = foreach ($file in $allFiles) {
    $limit = $managedExtensions[$file.Extension.ToLowerInvariant()]
    if (-not $limit) {
        continue
    }

    $lines = Get-LineCount $file.FullName
    if ($lines -gt $limit) {
        [PSCustomObject]@{
            Lines = $lines
            Limit = $limit
            File  = Convert-ToRepoRelativePath $file.FullName
        }
    }
}

$exemptions = [System.Collections.Generic.List[object]]::new()
if (Test-Path $exemptionConfigPath) {
    $grouped = @{}
    foreach ($line in Get-Content $exemptionConfigPath) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }

        $match = [regex]::Match($line, '^exemption\.(\d+)\.(glob|waive|reason)=(.*)$')
        if (-not $match.Success) {
            continue
        }

        $index = [int]$match.Groups[1].Value
        $field = $match.Groups[2].Value
        $value = $match.Groups[3].Value
        if (-not $grouped.ContainsKey($index)) {
            $grouped[$index] = [ordered]@{
                Index = $index
                Glob = ""
                Waive = ""
                Reason = ""
            }
        }
        switch ($field) {
            "glob" { $grouped[$index].Glob = $value }
            "waive" { $grouped[$index].Waive = $value }
            "reason" { $grouped[$index].Reason = $value }
        }
    }

    foreach ($entry in ($grouped.GetEnumerator() | Sort-Object Name)) {
        $exemptions.Add([PSCustomObject]$entry.Value)
    }
}

$maxLinesWaivers = @($exemptions | Where-Object { $_.Waive -match '(^|,)\s*MAX_LINES\s*(,|$)' }).Count
$maxSizeWaivers = @($exemptions | Where-Object { $_.Waive -match '(^|,)\s*MAX_SIZE\s*(,|$)' }).Count
$maxExemptions = $null
if (Test-Path $baselineConfigPath) {
    foreach ($line in Get-Content $baselineConfigPath) {
        if ($line -match '^file\.health\.max-exemptions=(\d+)$') {
            $maxExemptions = [int]$Matches[1]
            break
        }
    }
}

$over300 = @($javaCounts | Where-Object { $_.Lines -gt 300 } | Sort-Object Lines -Descending)
$over500 = @($javaCounts | Where-Object { $_.Lines -gt 500 } | Sort-Object Lines -Descending)
$top20 = @($javaCounts | Sort-Object Lines -Descending | Select-Object -First 20)
$violationsSorted = @($textViolations | Sort-Object Lines -Descending)

$reportLines = [System.Collections.Generic.List[string]]::new()
$reportLines.Add("# Largest Files Report")
$reportLines.Add("")
$reportLines.Add("Generated at ``$reportTime``.")
$reportLines.Add("")
$reportLines.Add("## Temporary Exemptions")
$reportLines.Add("")
$reportLines.Add("- Active exemptions: ``$($exemptions.Count)``")
if ($null -ne $maxExemptions) {
    $reportLines.Add("- Exemption baseline: ``$maxExemptions``")
}
$reportLines.Add("- MAX_LINES waivers: ``$maxLinesWaivers``")
$reportLines.Add("- MAX_SIZE waivers: ``$maxSizeWaivers``")
$reportLines.Add("")
if ($exemptions.Count -gt 0) {
    $reportLines.Add("| # | Waive | Target |")
    $reportLines.Add("| ---: | --- | --- |")
    foreach ($entry in $exemptions) {
        $reportLines.Add("| $($entry.Index) | ``$($entry.Waive)`` | ``$($entry.Glob)`` |")
    }
    $reportLines.Add("")
    $reportLines.Add("### Exemption Rationale")
    $reportLines.Add("")
    foreach ($entry in $exemptions) {
        $reportLines.Add("$($entry.Index). ``$($entry.Glob)``: $($entry.Reason)")
    }
} else {
    $reportLines.Add("No temporary exemptions are currently configured.")
}
$reportLines.Add("")
$reportLines.Add("## Files Exceeding Current Hard Limits")
$reportLines.Add("")
if ($violationsSorted.Count -gt 0) {
    $reportLines.Add("| Lines | Limit | File |")
    $reportLines.Add("| ---: | ---: | --- |")
    foreach ($entry in $violationsSorted) {
        $reportLines.Add("| $($entry.Lines) | $($entry.Limit) | ``$($entry.File)`` |")
    }
} else {
    $reportLines.Add("No managed text files currently exceed the configured hard limits.")
}
$reportLines.Add("")
$reportLines.Add("## Java Files Over 300 Lines")
$reportLines.Add("")
$reportLines.Add("Count: ``$($over300.Count)``")
$reportLines.Add("")
if ($over300.Count -gt 0) {
    $reportLines.Add("| Lines | File |")
    $reportLines.Add("| ---: | --- |")
    foreach ($entry in ($over300 | Select-Object -First 30)) {
        $reportLines.Add("| $($entry.Lines) | ``$($entry.File)`` |")
    }
}
$reportLines.Add("")
$reportLines.Add("## Java Files Over 500 Lines")
$reportLines.Add("")
$reportLines.Add("Count: ``$($over500.Count)``")
$reportLines.Add("")
if ($over500.Count -gt 0) {
    $reportLines.Add("| Lines | File |")
    $reportLines.Add("| ---: | --- |")
    foreach ($entry in $over500) {
        $reportLines.Add("| $($entry.Lines) | ``$($entry.File)`` |")
    }
}
$reportLines.Add("")
$reportLines.Add("## Top 20 Java Files")
$reportLines.Add("")
$reportLines.Add("| Lines | File |")
$reportLines.Add("| ---: | --- |")
foreach ($entry in $top20) {
    $reportLines.Add("| $($entry.Lines) | ``$($entry.File)`` |")
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$reportContent = ($reportLines -join "`n") + "`n"
[System.IO.File]::WriteAllText($resolvedOutputPath, $reportContent, $utf8NoBom)
