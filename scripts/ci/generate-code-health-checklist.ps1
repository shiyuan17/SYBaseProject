param(
    [string]$OutputPath = "docs/reports/code-health-checklist.md",
    [switch]$SkipExecution,
    [string]$GateStatus = "",
    [double]$GateDurationSeconds = 0,
    [int]$GateExitCode = 0,
    [string]$GateSummary = "",
    [string]$FastStatus = "",
    [double]$FastDurationSeconds = 0,
    [int]$FastExitCode = 0,
    [string]$FastSummary = ""
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedOutputPath = Join-Path $repoRoot $OutputPath
$outputDir = Split-Path -Parent $resolvedOutputPath
$reportTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

$largestFilesReportPath = Join-Path $repoRoot "docs\reports\largest-files-report.md"
$exemptionConfigPath = Join-Path $repoRoot "docs\file-health-exemptions.properties"
$baselineConfigPath = Join-Path $repoRoot "docs\file-health-baseline.properties"
$jacocoBaselinePath = Join-Path $repoRoot "bl-center\jacoco-baseline.properties"

function Invoke-NativeCommand {
    param(
        [scriptblock]$ScriptBlock,
        [string]$Name
    )

    $buffer = [System.Collections.Generic.List[string]]::new()
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $ScriptBlock 2>&1 | ForEach-Object {
            $text = $_.ToString()
            Write-Host $text
            if ([string]::IsNullOrWhiteSpace($text)) {
                return
            }
            $buffer.Add($text)
            if ($buffer.Count -gt 12) {
                $buffer.RemoveAt(0)
            }
        }
        $exitCode = $LASTEXITCODE
        if ($null -eq $exitCode) {
            $exitCode = 0
        }
    } catch {
        $exitCode = 1
        $message = $_.Exception.Message
        Write-Host $message
        if (-not [string]::IsNullOrWhiteSpace($message)) {
            $buffer.Add($message)
        }
    }
    $stopwatch.Stop()

    if ($exitCode -eq 0) {
        $resultStatus = "PASS"
    } else {
        $resultStatus = "FAIL"
    }

    return [PSCustomObject]@{
        Name = $Name
        Status = $resultStatus
        ExitCode = $exitCode
        DurationSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        Summary = ($buffer -join "`n")
    }
}

function New-ExternalResult {
    param(
        [string]$Name,
        [string]$Status,
        [double]$DurationSeconds,
        [int]$ExitCode,
        [string]$Summary
    )

    $normalizedStatus = $Status.Trim().ToUpperInvariant()
    if ([string]::IsNullOrWhiteSpace($normalizedStatus)) {
        $normalizedStatus = "UNKNOWN"
    }

    return [PSCustomObject]@{
        Name = $Name
        Status = $normalizedStatus
        ExitCode = $ExitCode
        DurationSeconds = [Math]::Round($DurationSeconds, 3)
        Summary = $Summary
    }
}

function Read-TextFile {
    param([string]$Path)

    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Get-PropertyValue {
    param(
        [string]$Content,
        [string]$Key
    )

    $escapedKey = [regex]::Escape($Key)
    $pattern = "(?m)^$escapedKey=(?<value>.+)$"
    $match = [regex]::Match($Content, $pattern)
    if ($match.Success) {
        return $match.Groups["value"].Value.Trim()
    }
    return $null
}

function Convert-ToMarkdownCell {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return ""
    }

    return ($Text.Trim() -replace '\|', '/' -replace "\r?\n", '<br>')
}

function Get-TrackedFiles {
    $files = @()
    try {
        $gitRoot = & git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $gitRoot) {
            $files = & git -c core.quotepath=false ls-files --cached --modified --others --exclude-standard
        }
    } catch {
        $files = @()
    }

    if ($files.Count -gt 0) {
        return $files | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    }

    return Get-ChildItem -Path $repoRoot -Recurse -File |
        ForEach-Object { $_.FullName.Substring($repoRoot.Length + 1).Replace('\', '/') }
}

function Get-Exemptions {
    if (-not (Test-Path $exemptionConfigPath)) {
        return [PSCustomObject]@{
            Count = 0
            Baseline = $null
            MaxLines = 0
            MaxSize = 0
            Items = @()
        }
    }

    $content = Read-TextFile -Path $exemptionConfigPath
    $grouped = @{}
    foreach ($line in $content -split "`r?`n") {
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

    $items = @($grouped.GetEnumerator() | Sort-Object Name | ForEach-Object { [PSCustomObject]$_.Value })
    $baseline = $null
    if (Test-Path $baselineConfigPath) {
        $baselineContent = Read-TextFile -Path $baselineConfigPath
        $baselineText = Get-PropertyValue -Content $baselineContent -Key "file.health.max-exemptions"
        if ($baselineText) {
            $baseline = [int]$baselineText
        }
    }

    return [PSCustomObject]@{
        Count = $items.Count
        Baseline = $baseline
        MaxLines = @($items | Where-Object { $_.Waive -match '(^|,)\s*MAX_LINES\s*(,|$)' }).Count
        MaxSize = @($items | Where-Object { $_.Waive -match '(^|,)\s*MAX_SIZE\s*(,|$)' }).Count
        Items = $items
    }
}

function Get-LargestFilesSummary {
    if (-not (Test-Path $largestFilesReportPath)) {
        return [PSCustomObject]@{
            Available = $false
            ActiveExemptions = 0
            ExemptionBaseline = $null
            MaxLinesWaivers = 0
            MaxSizeWaivers = 0
            HardLimitRows = @()
            JavaOver300 = $null
            JavaOver500 = $null
            TopHotspots = @()
        }
    }

    $content = Read-TextFile -Path $largestFilesReportPath
    $getMatch = {
        param([string]$pattern)
        $match = [regex]::Match($content, $pattern)
        if ($match.Success) {
            return $match.Groups[1].Value.Trim()
        }
        return $null
    }

    $hardLimitSection = [regex]::Match($content, '(?s)## Files Exceeding Current Hard Limits\s+\| Lines \| Limit \| File \|\s+\| ---: \| ---: \| --- \|\s+(?<rows>(?:\|.*\r?\n)+)')
    $hardLimitRows = @()
    if ($hardLimitSection.Success) {
        $hardLimitRows = $hardLimitSection.Groups["rows"].Value -split "`r?`n" |
            Where-Object { $_ -match '^\| \d+ \| \d+ \| `.*` \|$' }
    }

    $top20Section = [regex]::Match($content, '(?s)## Top 20 Java Files\s+\| Lines \| File \|\s+\| ---: \| --- \|\s+(?<rows>(?:\|.*\r?\n)+)')
    $top20Rows = @()
    if ($top20Section.Success) {
        $top20Rows = $top20Section.Groups["rows"].Value -split "`r?`n" |
            Where-Object { $_ -match '^\| \d+ \| `.*` \|$' } |
            Select-Object -First 5
    }

    $javaOver300Match = [regex]::Match($content, '(?s)## Java Files Over 300 Lines.*?Count: `(\d+)`')
    $javaOver500Match = [regex]::Match($content, '(?s)## Java Files Over 500 Lines.*?Count: `(\d+)`')

    if ($javaOver300Match.Success) {
        $javaOver300 = [int]$javaOver300Match.Groups[1].Value
    } else {
        $javaOver300 = $null
    }
    if ($javaOver500Match.Success) {
        $javaOver500 = [int]$javaOver500Match.Groups[1].Value
    } else {
        $javaOver500 = $null
    }

    return [PSCustomObject]@{
        Available = $true
        ActiveExemptions = [int](& $getMatch 'Active exemptions: `(\d+)`')
        ExemptionBaseline = & $getMatch 'Exemption baseline: `(\d+)`'
        MaxLinesWaivers = [int](& $getMatch 'MAX_LINES waivers: `(\d+)`')
        MaxSizeWaivers = [int](& $getMatch 'MAX_SIZE waivers: `(\d+)`')
        HardLimitRows = $hardLimitRows
        JavaOver300 = $javaOver300
        JavaOver500 = $javaOver500
        TopHotspots = $top20Rows
    }
}

function Get-CharsetAndCatchScan {
    $encodingHits = @()
    $todoHits = @()
    $emptyCatchHits = @()

    if (Get-Command rg -ErrorAction SilentlyContinue) {
        $encodingHits = & rg -n --glob '*.java' 'new String\([^,)]*\)|FileReader\(|FileWriter\(' common bl-center user-center auth-center tools 2>$null |
            Where-Object { $_ -notmatch 'StandardCharsets|Charset' }
        $todoHits = & rg -n --glob '*.java' 'TODO|FIXME' common bl-center user-center auth-center tools 2>$null |
            Where-Object { $_ -notmatch 'TODO_TASK' }
        $emptyCatchHits = & rg -n -U --glob '*.java' 'catch\s*\([^)]*\)\s*\{\s*\}' common bl-center user-center auth-center tools 2>$null
    }

    return [PSCustomObject]@{
        EncodingHits = @($encodingHits)
        TodoHits = @($todoHits)
        EmptyCatchHits = @($emptyCatchHits)
    }
}

function Get-GenericNameHits {
    $genericNames = @('utils', 'common', 'helper', 'helpers', 'tools', 'tmp', 'temp')
    $hits = Get-TrackedFiles |
        ForEach-Object {
            $fileName = [System.IO.Path]::GetFileNameWithoutExtension($_).ToLowerInvariant()
            if ($genericNames -contains $fileName) {
                $_
            }
        } |
        Sort-Object -Unique
    return @($hits)
}

function Get-OverviewStatus {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Evidence
    )

    return [PSCustomObject]@{
        Name = $Name
        Status = $Status
        Evidence = $Evidence
    }
}

function Get-CheckStatus {
    param(
        [string]$Preferred,
        [string]$Fallback = "WATCH"
    )

    if ([string]::IsNullOrWhiteSpace($Preferred)) {
        return $Fallback
    }
    return $Preferred
}

function Get-CommandEvidence {
    param(
        $Result,
        [string]$SuccessText,
        [string]$FailurePrefix,
        [string]$UnknownText
    )

    switch ($Result.Status) {
        "PASS" { return $SuccessText }
        "FAIL" {
            $summary = Convert-ToMarkdownCell $Result.Summary
            if ($summary) {
                return "$FailurePrefix Exit code: $($Result.ExitCode). Summary: $summary"
            }
            return "$FailurePrefix Exit code: $($Result.ExitCode)."
        }
        default { return $UnknownText }
    }
}

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

if ($SkipExecution) {
    $gateResult = New-ExternalResult -Name "gate" -Status $GateStatus -DurationSeconds $GateDurationSeconds -ExitCode $GateExitCode -Summary $GateSummary
} else {
    $gateResult = Invoke-NativeCommand -Name "gate" -ScriptBlock {
        & .\mvnw.cmd -pl common\common-test -Dtest=RepositoryFileHealthGateTest test
    }
}

if ($SkipExecution) {
    $fastResult = New-ExternalResult -Name "fast" -Status $FastStatus -DurationSeconds $FastDurationSeconds -ExitCode $FastExitCode -Summary $FastSummary
} else {
    $fastResult = Invoke-NativeCommand -Name "fast" -ScriptBlock {
        & .\mvnw.cmd test "-Dsurefire.excludedGroups=slow"
    }
}

$largest = Get-LargestFilesSummary
$exemptions = Get-Exemptions
$scans = Get-CharsetAndCatchScan
$genericNameHits = Get-GenericNameHits
$coverageLine = $null
$coverageBranch = $null
if (Test-Path $jacocoBaselinePath) {
    $jacocoContent = Read-TextFile -Path $jacocoBaselinePath
    $coverageLine = Get-PropertyValue -Content $jacocoContent -Key "jacoco.minimum.line.coverage"
    $coverageBranch = Get-PropertyValue -Content $jacocoContent -Key "jacoco.minimum.branch.coverage"
}

if ($null -ne $largest.JavaOver300 -and $null -ne $largest.JavaOver500) {
    $maintainabilitySummary = 'Current repository state has {0} Java files over 300 lines and {1} over 500 lines.' -f $largest.JavaOver300, $largest.JavaOver500
} else {
    $maintainabilitySummary = 'largest-files-report.md is unavailable, so hotspot counts could not be refreshed.'
}

if ($gateResult.Status -eq "FAIL") {
    $fileHealthOverviewStatus = "FAIL"
} elseif (-not $largest.Available -or $largest.HardLimitRows.Count -gt 0) {
    $fileHealthOverviewStatus = "WATCH"
} else {
    $fileHealthOverviewStatus = "PASS"
}

if ($gateResult.Status -eq "FAIL" -or $fastResult.Status -eq "FAIL") {
    $testabilityOverviewStatus = "FAIL"
} elseif (-not $coverageLine -or -not $coverageBranch -or $gateResult.Status -eq "UNKNOWN" -or $fastResult.Status -eq "UNKNOWN") {
    $testabilityOverviewStatus = "WATCH"
} else {
    $testabilityOverviewStatus = "PASS"
}

if ($genericNameHits.Count -gt 0 -or $scans.TodoHits.Count -gt 0) {
    $namingStatus = "WATCH"
} else {
    $namingStatus = "PASS"
}
if ($scans.EncodingHits.Count -gt 0 -or $scans.EmptyCatchHits.Count -gt 0) {
    $errorStatus = "WATCH"
} else {
    $errorStatus = "PASS"
}
if (-not $largest.Available) {
    $maintainabilityStatus = "WATCH"
} elseif (($largest.JavaOver300 | ForEach-Object { $_ }) -gt 0) {
    $maintainabilityStatus = "WATCH"
} else {
    $maintainabilityStatus = "PASS"
}

if ($gateResult.Status -eq "FAIL") {
    $fileHealthOverviewText = "RepositoryFileHealthGateTest failed; see the Testability section for command output."
} elseif (-not $largest.Available) {
    $fileHealthOverviewText = "largest-files-report.md is unavailable, so hard-limit conclusions are provisional."
} elseif ($largest.HardLimitRows.Count -gt 0) {
    $fileHealthOverviewText = "Hard gate status is separated from structural debt; oversized files still rely on approved exemptions."
} else {
    $fileHealthOverviewText = "The file-health gate passed and no managed text files currently exceed the configured hard limits."
}

if ($gateResult.Status -eq "FAIL" -or $fastResult.Status -eq "FAIL") {
    $testabilityOverviewText = "At least one verification command failed; the report was still generated with recorded evidence."
} elseif (-not $coverageLine -or -not $coverageBranch) {
    $testabilityOverviewText = "Verification commands completed, but the JaCoCo baseline file is missing or incomplete."
} elseif ($gateResult.Status -eq "UNKNOWN" -or $fastResult.Status -eq "UNKNOWN") {
    $testabilityOverviewText = "Verification commands were skipped; the report relies on existing repository artifacts."
} else {
    $testabilityOverviewText = "RepositoryFileHealthGateTest and fast feedback both passed; JaCoCo baseline file exists."
}

if ($genericNameHits.Count -gt 0) {
    $namingOverviewText = "Tracked files still include generic names: $(Convert-ToMarkdownCell (($genericNameHits | Select-Object -First 3) -join ', '))."
} elseif ($scans.TodoHits.Count -gt 0) {
    $namingOverviewText = "TODO/FIXME markers were found outside the approved TODO_TASK domain constant."
} else {
    $namingOverviewText = "No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK."
}

if ($scans.EncodingHits.Count -gt 0 -or $scans.EmptyCatchHits.Count -gt 0) {
    $errorsOverviewText = "Potential implicit charset conversions or empty catch blocks were found in the current source tree."
} else {
    $errorsOverviewText = "No implicit charset conversions or empty catch blocks found."
}

$overviewRows = @(
    Get-OverviewStatus "File Health" $fileHealthOverviewStatus $fileHealthOverviewText
    Get-OverviewStatus "Testability" $testabilityOverviewStatus $testabilityOverviewText
    Get-OverviewStatus "Maintainability" $maintainabilityStatus $maintainabilitySummary
    Get-OverviewStatus "Naming and Boundaries" $namingStatus $namingOverviewText
    Get-OverviewStatus "Errors and Encoding" $errorStatus $errorsOverviewText
)

$hotspots = $largest.TopHotspots
if ($gateResult.Status -eq "FAIL") {
    $fileHealthStatus = "FAIL"
} elseif (-not $largest.Available -or $largest.HardLimitRows.Count -gt 0) {
    $fileHealthStatus = "WATCH"
} else {
    $fileHealthStatus = "PASS"
}
if ($gateResult.Status -eq "FAIL" -or $fastResult.Status -eq "FAIL") {
    $testCommandStatus = "FAIL"
} elseif ($gateResult.Status -eq "UNKNOWN" -or $fastResult.Status -eq "UNKNOWN") {
    $testCommandStatus = "WATCH"
} else {
    $testCommandStatus = "PASS"
}
if ($coverageLine -and $coverageBranch) {
    $coverageStatus = "PASS"
} else {
    $coverageStatus = "WATCH"
}
if (-not $largest.Available) {
    $structuralStatus = "WATCH"
} elseif (($largest.JavaOver300 | ForEach-Object { $_ }) -gt 0) {
    $structuralStatus = "WATCH"
} else {
    $structuralStatus = "PASS"
}
if ($genericNameHits.Count -gt 0) {
    $genericFilesStatus = "WATCH"
} else {
    $genericFilesStatus = "PASS"
}
if ($scans.TodoHits.Count -gt 0) {
    $todoStatus = "WATCH"
} else {
    $todoStatus = "PASS"
}
if ($scans.EncodingHits.Count -gt 0) {
    $encodingStatus = "WATCH"
} else {
    $encodingStatus = "PASS"
}
if ($scans.EmptyCatchHits.Count -gt 0) {
    $emptyCatchStatus = "WATCH"
} else {
    $emptyCatchStatus = "PASS"
}

$report = [System.Collections.Generic.List[string]]::new()
$report.Add("# Repository Code Health Checklist")
$report.Add("")
$report.Add(('Generated at `{0}`.' -f $reportTime))
$report.Add("")
$report.Add("## Overview")
$report.Add("")
$report.Add("| Area | Status | Conclusion |")
$report.Add("| --- | --- | --- |")
foreach ($row in $overviewRows) {
    $report.Add("| $($row.Name) | $($row.Status) | $($row.Evidence) |")
}
$report.Add("")
$report.Add("## File Health")
$report.Add("")
$report.Add("| Check Item | Basis | Status | Evidence | Suggested Action |")
$report.Add("| --- | --- | --- | --- | --- |")
$report.Add("| UTF-8 / BOM / line endings | docs/rules/CODING_RULES.md section 7; common/common-test/.../RepositoryFileHealthGateTest.java | $fileHealthStatus | $(Get-CommandEvidence -Result $gateResult -SuccessText 'The repository file-health gate passed, so the current tree does not expose encoding or line-ending violations.' -FailurePrefix 'The repository file-health gate failed.' -UnknownText 'The repository file-health gate was not executed in this run.') | Keep UTF-8 without BOM and LF as the default. |")
$report.Add("| File size / line limits / exemptions | docs/rules/CODING_RULES.md section 7; docs/file-health-exemptions.properties; docs/file-health-baseline.properties | $(if ($largest.Available) { if ($largest.HardLimitRows.Count -gt 0) { 'WATCH' } else { 'PASS' } } else { 'WATCH' }) | $(if ($largest.Available) { "Active exemptions: $($exemptions.Count); exemption baseline: $($exemptions.Baseline); oversized files still covered by approved exemptions: $($largest.HardLimitRows.Count)." } else { 'largest-files-report.md is unavailable, so exemption and hard-limit counts could not be refreshed.' }) | Keep the exemption cap flat and move generated large artifacts away from hard-limit paths. |")
$report.Add("")
$report.Add("## Testability")
$report.Add("")
$report.Add("| Check Item | Basis | Status | Evidence | Suggested Action |")
$report.Add("| --- | --- | --- | --- | --- |")
$report.Add("| Fast feedback and gate checks | docs/rules/PROJECT_HEALTH_RULES.md; common/common-test/.../RepositoryFileHealthGateTest.java | $testCommandStatus | Gate status: $($gateResult.Status) in $($gateResult.DurationSeconds)s (exit $($gateResult.ExitCode)); fast feedback status: $($fastResult.Status) in $($fastResult.DurationSeconds)s (exit $($fastResult.ExitCode)).$(if ($gateResult.Status -eq 'FAIL') { ' Gate summary: ' + (Convert-ToMarkdownCell $gateResult.Summary) + '.' } else { '' })$(if ($fastResult.Status -eq 'FAIL') { ' Fast summary: ' + (Convert-ToMarkdownCell $fastResult.Summary) + '.' } else { '' }) | Keep slow excluded from fast feedback and preserve the static gate. |")
$report.Add("| Coverage baseline | bl-center/jacoco-baseline.properties; docs/reports/code-health-baseline-20260530.md | $coverageStatus | $(if ($coverageLine -and $coverageBranch) { "line baseline: $coverageLine; branch baseline: $coverageBranch." } else { 'The JaCoCo baseline file is missing or incomplete.' }) | Keep coverage changes inside the existing baseline-governance flow. |")
$report.Add("")
$report.Add("## Maintainability")
$report.Add("")
$report.Add("| Check Item | Basis | Status | Evidence | Suggested Action |")
$report.Add("| --- | --- | --- | --- | --- |")
$report.Add("| Structural hotspots | docs/rules/AI-CODE-HEALTH.md; docs/rules/CODING_RULES.md; docs/reports/largest-files-report.md | $structuralStatus | $(if ($largest.Available) { "Java files over 300 lines: $($largest.JavaOver300); Java files over 500 lines: $($largest.JavaOver500)." } else { 'largest-files-report.md is unavailable, so hotspot counts could not be refreshed.' }) | Prioritize breaking up persistence, service, and controller files with heavy line counts. |")
$report.Add("")
$report.Add("### Priority Hotspots")
$report.Add("")
$report.Add("| Lines | File | Why It Matters |")
$report.Add("| ---: | --- | --- |")
foreach ($row in $hotspots) {
    if ($row -match '^\| (?<lines>\d+) \| `(?<file>[^`]+)` \|$') {
        $lines = $Matches["lines"]
        $file = $Matches["file"]
        $focus = 'Structural hotspot'
        if ($file -match 'Repository|Jdbc|Support') {
            $focus = 'Persistence responsibilities are too concentrated'
        } elseif ($file -match 'Service') {
            $focus = 'Application orchestration is too large'
        } elseif ($file -match 'Controller') {
            $focus = 'Interface-layer mapping is too heavy'
        }
        $report.Add("| $lines | $file | $focus |")
    }
}
if ($hotspots.Count -eq 0) {
    $report.Add("| - | - | largest-files-report.md is unavailable or did not return hotspot rows. |")
}
$report.Add("")
$report.Add("## Naming and Boundaries")
$report.Add("")
$report.Add("| Check Item | Basis | Status | Evidence | Suggested Action |")
$report.Add("| --- | --- | --- | --- | --- |")
$report.Add("| Generic filenames | docs/rules/AI-CODE-HEALTH.md; docs/rules/CODING_RULES.md | $genericFilesStatus | $(if ($genericNameHits.Count -gt 0) { 'Matched generic basenames: ' + (Convert-ToMarkdownCell (($genericNameHits | Select-Object -First 5) -join ', ')) + '.' } else { 'No tracked files matched generic basenames such as utils, common, helper, helpers, tools, tmp, or temp.' }) | Keep file and module names anchored in domain language. |")
$report.Add("| TODO / FIXME noise | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | $todoStatus | $(if ($scans.TodoHits.Count -gt 0) { 'TODO/FIXME hits: ' + (Convert-ToMarkdownCell (($scans.TodoHits | Select-Object -First 5) -join '; ')) + '.' } else { 'The scan only matched the domain constant TODO_TASK; no stray TODO/FIXME markers were found.' }) | Keep domain constants distinct from comment-based follow-up markers. |")
$report.Add("")
$report.Add("## Errors and Encoding")
$report.Add("")
$report.Add("| Check Item | Basis | Status | Evidence | Suggested Action |")
$report.Add("| --- | --- | --- | --- | --- |")
$report.Add("| Implicit charset conversions | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | $encodingStatus | $(if ($scans.EncodingHits.Count -gt 0) { 'Potential hotspots: ' + (Convert-ToMarkdownCell (($scans.EncodingHits | Select-Object -First 5) -join '; ')) + '.' } else { 'No raw new String(byte[]), FileReader, or FileWriter usage was found in the scanned source tree.' }) | Keep charsets explicit, especially on import, export, and log-writing paths. |")
$report.Add("| Empty catch / silent exception swallowing | docs/rules/CODING_RULES.md; docs/rules/AI-CODE-HEALTH.md | $emptyCatchStatus | $(if ($scans.EmptyCatchHits.Count -gt 0) { 'Empty catch hits: ' + (Convert-ToMarkdownCell (($scans.EmptyCatchHits | Select-Object -First 5) -join '; ')) + '.' } else { 'No empty catch blocks were found by the regex scan.' }) | Keep exceptions structured and propagate them at the right layer. |")
$report.Add("")
$report.Add("## Notes")
$report.Add("")
$report.Add("- This checklist separates the hard file-health gate from structural debt: passing the gate does not mean the repository is debt-free.")
if ($SkipExecution) {
    $report.Add("- Verification commands were supplied by the caller rather than re-executed inside this script.")
}

$gitStatus = @()
try {
    $gitStatus = & git status --short
} catch {
    $gitStatus = @()
}
$untracked = @($gitStatus | Where-Object { $_ -match '^\?\?' })
if ($untracked.Count -gt 0) {
    $report.Add("- Untracked files were present during report generation: $(Convert-ToMarkdownCell (($untracked | ForEach-Object { $_.Substring(3) } | Select-Object -First 5) -join ', ')).")
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($resolvedOutputPath, ($report -join "`n") + "`n", $utf8NoBom)
