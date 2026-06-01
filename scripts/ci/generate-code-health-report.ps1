param(
    [string]$OutputDirectory = "docs/reports"
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedOutputDirectory = Join-Path $repoRoot $OutputDirectory
$todayStamp = Get-Date -Format "yyyyMMdd"
$latestReportPath = Join-Path $resolvedOutputDirectory "code-health-report-latest.html"
$datedReportPath = Join-Path $resolvedOutputDirectory "code-health-report-$todayStamp.html"
$largestFilesReportPath = Join-Path $resolvedOutputDirectory "largest-files-report.md"
$checklistPath = Join-Path $resolvedOutputDirectory "code-health-checklist.md"
$jacocoBaselinePath = Join-Path $repoRoot "bl-center\jacoco-baseline.properties"
$projectHealthRulesPath = Join-Path $repoRoot "docs\rules\PROJECT_HEALTH_RULES.md"
$reportTimeUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$reportTimeLocal = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")

function Write-Utf8NoBom {
    param(
        [string]$Path,
        [string]$Content
    )

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Read-TextFile {
    param([string]$Path)

    if (-not (Test-Path $Path -PathType Leaf)) {
        return $null
    }

    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Get-PropertyValue {
    param(
        [string]$Content,
        [string]$Key
    )

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return $null
    }

    $escapedKey = [regex]::Escape($Key)
    $match = [regex]::Match($Content, "(?m)^$escapedKey=(?<value>.+)$")
    if ($match.Success) {
        return $match.Groups["value"].Value.Trim()
    }
    return $null
}

function Convert-ToHtml {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    return [System.Security.SecurityElement]::Escape($Text)
}

function Convert-ToHtmlWithBreaks {
    param([string]$Text)

    return (Convert-ToHtml $Text) -replace "(\r?\n)", "<br>"
}

function Convert-ToShellLiteral {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    return $Text.Replace("'", "''")
}

function Get-StatusClass {
    param([string]$Status)

    switch ($Status) {
        "PASS" { return "pass" }
        "FAIL" { return "fail" }
        "WATCH" { return "watch" }
        default { return "unknown" }
    }
}

function Get-OverallStatus {
    param(
        [object[]]$OverviewRows,
        $GateResult,
        $FastResult
    )

    if ($GateResult.Status -eq "FAIL" -or $FastResult.Status -eq "FAIL" -or @($OverviewRows | Where-Object { $_.Status -eq "FAIL" }).Count -gt 0) {
        return "FAIL"
    }
    if (@($OverviewRows | Where-Object { $_.Status -ne "PASS" }).Count -gt 0) {
        return "WATCH"
    }
    return "PASS"
}

function Invoke-LoggedCommand {
    param(
        [string]$Name,
        [string]$CommandText,
        [scriptblock]$ScriptBlock
    )

    $tail = [System.Collections.Generic.List[string]]::new()
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $ScriptBlock 2>&1 | ForEach-Object {
            $text = $_.ToString()
            Write-Host $text
            if ([string]::IsNullOrWhiteSpace($text)) {
                return
            }
            $tail.Add($text)
            if ($tail.Count -gt 20) {
                $tail.RemoveAt(0)
            }
        }
        $exitCode = $LASTEXITCODE
        if ($null -eq $exitCode) {
            $exitCode = 0
        }
    } catch {
        $exitCode = 1
        $message = $_.Exception.Message
        if (-not [string]::IsNullOrWhiteSpace($message)) {
            Write-Host $message
            $tail.Add($message)
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
        Command = $CommandText
        Status = $resultStatus
        ExitCode = $exitCode
        DurationSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        Summary = ($tail -join "`n")
    }
}

function Invoke-ReportStep {
    param(
        [string]$Name,
        [string]$CommandText,
        [scriptblock]$ScriptBlock
    )

    return Invoke-LoggedCommand -Name $Name -CommandText $CommandText -ScriptBlock $ScriptBlock
}

function Get-ChecklistOverviewRows {
    param([string]$Content)

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return @()
    }

    $lines = $Content -split "`r?`n"
    $startIndex = -1
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i] -eq "## Overview") {
            $startIndex = $i
            break
        }
    }
    if ($startIndex -lt 0) {
        return @()
    }

    $rows = [System.Collections.Generic.List[object]]::new()
    $tableStarted = $false
    for ($i = $startIndex + 1; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        if ([string]::IsNullOrWhiteSpace($line)) {
            if ($tableStarted) {
                break
            }
            continue
        }
        if ($line -notmatch '^\|') {
            if ($tableStarted) {
                break
            }
            continue
        }
        if ($line -match '^\| Area \| Status \| Conclusion \|$' -or $line -match '^\| --- \| --- \| --- \|$') {
            $tableStarted = $true
            continue
        }
        if ($line -match '^\| (?<area>[^|]+?) \| (?<status>[^|]+?) \| (?<conclusion>.+) \|$') {
            $tableStarted = $true
            $rows.Add([PSCustomObject]@{
                Area = $Matches["area"].Trim()
                Status = $Matches["status"].Trim()
                Conclusion = $Matches["conclusion"].Trim()
            })
        }
    }
    return @($rows)
}

function Get-LargestFilesData {
    param([string]$Content)

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return [PSCustomObject]@{
            Available = $false
            HardLimitCount = $null
            JavaOver300 = $null
            JavaOver500 = $null
            TopHotspots = @()
        }
    }

    $hardLimitSection = [regex]::Match($Content, '(?ms)^## Files Exceeding Current Hard Limits\s+(?<body>.*?)(?=^## )')
    $hardLimitCount = 0
    if ($hardLimitSection.Success) {
        $hardLimitCount = @(($hardLimitSection.Groups["body"].Value -split "`r?`n") | Where-Object { $_ -match '^\| \d+ \| \d+ \| `.*` \|$' }).Count
    }

    $topHotspots = @()
    $hotspotSection = [regex]::Match($Content, '(?ms)^## Top 20 Java Files\s+\| Lines \| File \|\s+\| ---: \| --- \|\s+(?<rows>(?:\|.*\r?\n)+)')
    if ($hotspotSection.Success) {
        $topHotspots = @(($hotspotSection.Groups["rows"].Value -split "`r?`n") |
            Where-Object { $_ -match '^\| \d+ \| `.*` \|$' } |
            Select-Object -First 10 |
            ForEach-Object {
                if ($_ -match '^\| (?<lines>\d+) \| `(?<file>[^`]+)` \|$') {
                    [PSCustomObject]@{
                        Lines = [int]$Matches["lines"]
                        File = $Matches["file"]
                    }
                }
            })
    }

    $javaOver300Match = [regex]::Match($Content, '(?ms)^## Java Files Over 300 Lines.*?Count: `(?<count>\d+)`')
    $javaOver500Match = [regex]::Match($Content, '(?ms)^## Java Files Over 500 Lines.*?Count: `(?<count>\d+)`')

    if ($javaOver300Match.Success) {
        $javaOver300 = [int]$javaOver300Match.Groups["count"].Value
    } else {
        $javaOver300 = $null
    }
    if ($javaOver500Match.Success) {
        $javaOver500 = [int]$javaOver500Match.Groups["count"].Value
    } else {
        $javaOver500 = $null
    }

    return [PSCustomObject]@{
        Available = $true
        HardLimitCount = $hardLimitCount
        JavaOver300 = $javaOver300
        JavaOver500 = $javaOver500
        TopHotspots = $topHotspots
    }
}

function Get-ModuleStats {
    $pomPath = Join-Path $repoRoot "pom.xml"
    if (-not (Test-Path $pomPath -PathType Leaf)) {
        return @()
    }

    [xml]$pom = Get-Content $pomPath
    $modules = @($pom.project.modules.module | ForEach-Object { $_.'#text' })
    $rows = foreach ($module in $modules) {
        $modulePath = Join-Path $repoRoot $module
        $mainPath = Join-Path $modulePath "src\main\java"
        $testPath = Join-Path $modulePath "src\test\java"
        if (Test-Path $mainPath) {
            $mainCount = @(Get-ChildItem $mainPath -Recurse -File -Filter *.java).Count
        } else {
            $mainCount = 0
        }
        if (Test-Path $testPath) {
            $testCount = @(Get-ChildItem $testPath -Recurse -File -Filter *.java).Count
        } else {
            $testCount = 0
        }
        if ($mainCount -eq 0) {
            $ratio = "-"
        } else {
            $ratio = "{0:N2}" -f ($testCount / [double]$mainCount)
        }
        [PSCustomObject]@{
            Module = $module
            MainJava = $mainCount
            TestJava = $testCount
            Ratio = $ratio
        }
    }

    return @($rows)
}

function Get-WorktreeContext {
    $statusLines = @()
    try {
        $statusLines = & git status --short 2>$null
    } catch {
        $statusLines = @()
    }

    $modified = @($statusLines | Where-Object { $_ -notmatch '^\?\?' })
    $untracked = @($statusLines | Where-Object { $_ -match '^\?\?' })
    $samples = @($statusLines | Select-Object -First 8 | ForEach-Object { $_.Trim() })

    return [PSCustomObject]@{
        Dirty = ($statusLines.Count -gt 0)
        ModifiedCount = $modified.Count
        UntrackedCount = $untracked.Count
        SamplePaths = $samples
    }
}

function Get-CodeGraphContext {
    $rawLines = @()
    try {
        $rawLines = & codegraph.cmd status 2>&1 | ForEach-Object { $_.ToString() }
    } catch {
        return [PSCustomObject]@{
            Available = $false
            Summary = "codegraph.cmd status was unavailable."
        }
    }

    if ($LASTEXITCODE -ne 0 -and $rawLines.Count -eq 0) {
        return [PSCustomObject]@{
            Available = $false
            Summary = "codegraph.cmd status returned a non-zero exit code."
        }
    }

    $ansiPattern = [regex]::new("\x1b\[[0-9;]*m")
    $clean = ($rawLines | ForEach-Object { $ansiPattern.Replace($_, "") }) -join "`n"
    if ($clean -match 'Files:\s+([0-9,]+)') {
        $files = $Matches[1]
    } else {
        $files = "unknown"
    }
    if ($clean -match 'Nodes:\s+([0-9,]+)') {
        $nodes = $Matches[1]
    } else {
        $nodes = "unknown"
    }
    if ($clean -match 'Edges:\s+([0-9,]+)') {
        $edges = $Matches[1]
    } else {
        $edges = "unknown"
    }
    if ($clean -match 'Backend:\s+([A-Za-z0-9_-]+)') {
        $backend = $Matches[1]
    } else {
        $backend = "unknown"
    }
    if ($clean -match 'Modified:\s+([0-9]+)') {
        $pendingModified = $Matches[1]
    } else {
        $pendingModified = "0"
    }

    return [PSCustomObject]@{
        Available = $true
        Files = $files
        Nodes = $nodes
        Edges = $edges
        Backend = $backend
        PendingModified = $pendingModified
        Summary = "Files: $files; Nodes: $nodes; Pending modified: $pendingModified."
    }
}

function Get-CoverageBaseline {
    $content = Read-TextFile $jacocoBaselinePath
    return [PSCustomObject]@{
        Line = Get-PropertyValue -Content $content -Key "jacoco.minimum.line.coverage"
        Branch = Get-PropertyValue -Content $content -Key "jacoco.minimum.branch.coverage"
    }
}

function Get-RecommendedActions {
    param(
        $GateResult,
        $FastResult,
        $LargestData,
        [object[]]$ModuleStats
    )

    $actions = [System.Collections.Generic.List[string]]::new()

    if ($GateResult.Status -eq "FAIL") {
        $actions.Add((Get-HtmlText 'actionFixGate'))
    }
    if ($FastResult.Status -eq "FAIL") {
        $actions.Add((Get-HtmlText 'actionFixFastFeedback'))
    }
    if ($LargestData.HardLimitCount -gt 0) {
        $actions.Add((Get-HtmlText 'actionTightenExemptions'))
    }

    $hotspotKinds = @($LargestData.TopHotspots | Select-Object -First 3)
    if (@($hotspotKinds | Where-Object { $_.File -match 'Repository|Jdbc|Support' }).Count -gt 0) {
        $actions.Add((Get-HtmlText 'actionSplitPersistence'))
    }
    if (@($hotspotKinds | Where-Object { $_.File -match 'Service' }).Count -gt 0) {
        $actions.Add((Get-HtmlText 'actionSplitServices'))
    }
    if (@($hotspotKinds | Where-Object { $_.File -match 'Controller' }).Count -gt 0) {
        $actions.Add((Get-HtmlText 'actionMoveAssemblerLogic'))
    }

    $thinModules = @($ModuleStats | Where-Object { $_.MainJava -gt 0 -and [double]$_.Ratio -lt 0.15 } | Select-Object -First 2)
    foreach ($module in $thinModules) {
        $actions.Add(([string]::Format(
            (Get-HtmlText 'actionThinModule'),
            (Convert-ToHtml $module.Module),
            (Convert-ToHtml $module.Ratio)
        )))
    }

    if ($actions.Count -lt 3) {
        $actions.Add((Get-HtmlText 'actionKeepBaseline'))
    }

    return @($actions | Select-Object -Unique | Select-Object -First 6)
}

function Render-VerificationDetails {
    param($Result)

    if ([string]::IsNullOrWhiteSpace($Result.Summary)) {
        $summary = "No summary captured."
    } else {
        $summary = $Result.Summary
    }
    return "<details><summary>$(Get-HtmlText 'details')</summary><pre>$(Convert-ToHtml $summary)</pre></details>"
}

function Get-HtmlText {
    param([string]$Key)

    switch ($Key) {
        "title" { return "&#x4EE3;&#x7801;&#x5065;&#x5EB7;&#x5EA6;&#x62A5;&#x544A;" }
        "lede" { return "&#x8FD9;&#x4EFD;&#x62A5;&#x544A;&#x57FA;&#x4E8E;&#x4ED3;&#x5E93;&#x73B0;&#x6709;&#x7684; <code>largest-files-report</code>&#x3001;<code>code-health-checklist</code>&#x3001;JaCoCo baseline &#x4E0E;&#x9879;&#x76EE;&#x5065;&#x5EB7;&#x89C4;&#x5219;&#x751F;&#x6210;&#xFF0C;&#x91C7;&#x7528;&#x201C;&#x9759;&#x6001;&#x5206;&#x6790; + &#x5B9E;&#x9645;&#x9A8C;&#x8BC1;&#x201D;&#x6A21;&#x5F0F;&#xFF0C;&#x5E76;&#x4FDD;&#x7559;&#x810F;&#x5DE5;&#x4F5C;&#x533A;&#x4E0A;&#x4E0B;&#x6587;&#x3002;" }
        "generatedAt" { return "&#x751F;&#x6210;&#x65F6;&#x95F4;" }
        "summary" { return "&#x6267;&#x884C;&#x6458;&#x8981;" }
        "summaryIntro" { return "&#x6458;&#x8981;&#x5361;&#x7247;&#x76F4;&#x63A5;&#x6765;&#x81EA;&#x5237;&#x65B0;&#x540E;&#x7684; <code>docs/reports/code-health-checklist.md</code>&#x3002;&#x7EDF;&#x4E00;&#x5165;&#x53E3;&#x8D1F;&#x8D23;&#x6267;&#x884C;&#x9A8C;&#x8BC1;&#x5E76;&#x628A;&#x7ED3;&#x679C;&#x6CE8;&#x5165;&#x5230; Markdown &#x4EA7;&#x7269;&#xFF0C;&#x518D;&#x6E32;&#x67D3;&#x6210; HTML&#x3002;" }
        "verification" { return "&#x9A8C;&#x8BC1;&#x7ED3;&#x679C;" }
        "verificationIntro" { return "&#x8FD9;&#x91CC;&#x5C55;&#x793A;&#x672C;&#x6B21;&#x8FD0;&#x884C;&#x771F;&#x6B63;&#x6267;&#x884C;&#x8FC7;&#x7684;&#x6821;&#x9A8C;&#x547D;&#x4EE4;&#x3001;&#x8017;&#x65F6;&#x3001;&#x9000;&#x51FA;&#x7801;&#x548C;&#x5931;&#x8D25;&#x6458;&#x8981;&#x3002;&#x5373;&#x4F7F;&#x547D;&#x4EE4;&#x5931;&#x8D25;&#xFF0C;HTML &#x4ECD;&#x4F1A;&#x751F;&#x6210;&#x3002;" }
        "coverageNote" { return "&#x8986;&#x76D6;&#x7387;&#x57FA;&#x7EBF;&#x53D6;&#x81EA; <code>bl-center/jacoco-baseline.properties</code>&#xFF1A;line = <strong>{0}</strong>&#xFF0C;branch = <strong>{1}</strong>&#x3002;&#x8FD9;&#x662F;&#x6CBB;&#x7406;&#x57FA;&#x7EBF;&#xFF0C;&#x4E0D;&#x662F;&#x672C;&#x6B21;&#x5B9E;&#x65F6;&#x8986;&#x76D6;&#x7387;&#x3002;" }
        "hotspots" { return "&#x7ED3;&#x6784;&#x70ED;&#x70B9;" }
        "hotspotsIntro" { return "&#x70ED;&#x70B9;&#x6765;&#x81EA;&#x5237;&#x65B0;&#x540E;&#x7684; <code>docs/reports/largest-files-report.md</code>&#x3002;&#x5F53;&#x524D; hard-limit &#x8D85;&#x9650;&#x6587;&#x4EF6;&#x6570;&#x4E3A; <strong>{0}</strong>&#xFF0C;Java 500+ &#x6587;&#x4EF6;&#x6570;&#x4E3A; <strong>{1}</strong>&#x3002;" }
        "moduleDist" { return "&#x6A21;&#x5757;&#x6D4B;&#x8BD5;&#x5206;&#x5E03;" }
        "moduleIntro" { return "&#x7EDF;&#x8BA1;&#x6309;&#x6839; POM &#x4E2D;&#x58F0;&#x660E;&#x7684; Maven module &#x8BA1;&#x7B97;&#xFF0C;&#x5E2E;&#x52A9;&#x5FEB;&#x901F;&#x8BC6;&#x522B;&#x6D4B;&#x8BD5;&#x9762;&#x660E;&#x663E;&#x504F;&#x8584;&#x7684;&#x6A21;&#x5757;&#x3002;" }
        "context" { return "&#x5DE5;&#x4F5C;&#x533A;&#x4E0A;&#x4E0B;&#x6587;" }
        "contextIntro" { return "&#x62A5;&#x544A;&#x4E0D;&#x4F1A;&#x56E0;&#x4E3A;&#x5DE5;&#x4F5C;&#x533A;&#x672A;&#x6E05;&#x7406;&#x800C;&#x62D2;&#x7EDD;&#x751F;&#x6210;&#xFF0C;&#x4F46;&#x4F1A;&#x628A;&#x5F53;&#x524D;&#x4E0A;&#x4E0B;&#x6587;&#x663E;&#x5F0F;&#x5E26;&#x51FA;&#x6765;&#xFF0C;&#x907F;&#x514D;&#x628A;&#x6CBB;&#x7406;&#x7ED3;&#x8BBA;&#x8BEF;&#x5F53;&#x6210;&#x5E72;&#x51C0;&#x57FA;&#x7EBF;&#x3002;" }
        "sources" { return "&#x6CBB;&#x7406;&#x6765;&#x6E90;" }
        "actions" { return "&#x5EFA;&#x8BAE;&#x52A8;&#x4F5C;" }
        "actionsIntro" { return "&#x8FD9;&#x4E9B;&#x52A8;&#x4F5C;&#x6309;&#x5F53;&#x524D;&#x9A8C;&#x8BC1;&#x7ED3;&#x679C;&#x3001;&#x70ED;&#x70B9;&#x7C7B;&#x578B;&#x548C;&#x6A21;&#x5757;&#x6D4B;&#x8BD5;&#x5206;&#x5E03;&#x81EA;&#x52A8;&#x6574;&#x7406;&#xFF0C;&#x4F18;&#x5148;&#x5BF9;&#x9F50;&#x4ED3;&#x5E93;&#x73B0;&#x6709;&#x7684;&#x9879;&#x76EE;&#x5065;&#x5EB7;&#x89C4;&#x5219;&#x3002;" }
        "codegraph" { return "CodeGraph" }
        "gitWorktree" { return "Git &#x5DE5;&#x4F5C;&#x533A;" }
        "gateAndFast" { return "&#x95E8;&#x7981; / &#x5FEB;&#x53CD;&#x9988;" }
        "worktreeDirty" { return "&#x5DE5;&#x4F5C;&#x533A;&#x810F;&#x72B6;&#x6001;" }
        "command" { return "&#x547D;&#x4EE4;" }
        "status" { return "&#x72B6;&#x6001;" }
        "exit" { return "&#x9000;&#x51FA;&#x7801;" }
        "duration" { return "&#x8017;&#x65F6;" }
        "details" { return "&#x8BE6;&#x60C5;" }
        "summaryLabel" { return "&#x6458;&#x8981;" }
        "lines" { return "&#x884C;&#x6570;" }
        "file" { return "&#x6587;&#x4EF6;" }
        "why" { return "&#x5173;&#x6CE8;&#x539F;&#x56E0;" }
        "module" { return "&#x6A21;&#x5757;" }
        "mainJava" { return "&#x4E3B;&#x4EE3;&#x7801; Java" }
        "testJava" { return "&#x6D4B;&#x8BD5; Java" }
        "ratio" { return "&#x6D4B;&#x8BD5;/&#x4E3B;&#x4EE3;&#x7801;&#x6BD4;" }
        "dirty" { return "&#x810F;&#x72B6;&#x6001;" }
        "trackedChanges" { return "&#x5DF2;&#x8DDF;&#x8E2A;&#x53D8;&#x66F4;" }
        "untrackedFiles" { return "&#x672A;&#x8DDF;&#x8E2A;&#x6587;&#x4EF6;" }
        "indexedFiles" { return "&#x5DF2;&#x7D22;&#x5F15;&#x6587;&#x4EF6;" }
        "nodes" { return "&#x8282;&#x70B9;&#x6570;" }
        "edges" { return "&#x8FB9;&#x6570;" }
        "backend" { return "&#x540E;&#x7AEF;" }
        "pendingModified" { return "&#x5F85;&#x540C;&#x6B65;&#x4FEE;&#x6539;&#x6587;&#x4EF6;" }
        "largestReport" { return "&#x5927;&#x6587;&#x4EF6;&#x62A5;&#x544A;" }
        "checklistReport" { return "&#x5065;&#x5EB7;&#x68C0;&#x67E5;&#x6E05;&#x5355;" }
        "generatedUtc" { return "&#x751F;&#x6210; UTC" }
        "yes" { return "&#x662F;" }
        "no" { return "&#x5426;" }
        "noHotspots" { return "&#x672A;&#x4ECE; <code>largest-files-report.md</code> &#x4E2D;&#x8BFB;&#x5230;&#x53EF;&#x5C55;&#x793A;&#x7684;&#x70ED;&#x70B9;&#x884C;&#x3002;" }
        "noModules" { return "&#x672A;&#x8BFB;&#x5230; Maven module &#x7EDF;&#x8BA1;&#x6570;&#x636E;&#x3002;" }
        "worktreeClean" { return "&#x5DE5;&#x4F5C;&#x533A;&#x5DF2;&#x6E05;&#x7406;&#x5E72;&#x51C0;&#x3002;" }
        "projectRulesFound" { return "&#x9879;&#x76EE;&#x5065;&#x5EB7;&#x89C4;&#x5219;&#x6765;&#x81EA; <code>docs/rules/PROJECT_HEALTH_RULES.md</code>&#x3002;" }
        "projectRulesMissing" { return "&#x672A;&#x627E;&#x5230;&#x9879;&#x76EE;&#x5065;&#x5EB7;&#x89C4;&#x5219;&#x6587;&#x4EF6;&#xFF1B;&#x5F53;&#x524D;&#x5EFA;&#x8BAE;&#x4EC5;&#x57FA;&#x4E8E;&#x5DF2;&#x751F;&#x6210;&#x4EA7;&#x7269;&#x3002;" }
        "hotspotFocus" { return "&#x7ED3;&#x6784;&#x70ED;&#x70B9;" }
        "persistenceFocus" { return "&#x6301;&#x4E45;&#x5316;&#x804C;&#x8D23;&#x8FC7;&#x4E8E;&#x96C6;&#x4E2D;" }
        "serviceFocus" { return "&#x5E94;&#x7528;&#x7F16;&#x6392;&#x804C;&#x8D23;&#x8FC7;&#x5927;" }
        "controllerFocus" { return "&#x63A5;&#x53E3;&#x5C42;&#x7EC4;&#x88C5;&#x804C;&#x8D23;&#x8FC7;&#x91CD;" }
        "actionFixGate" { return "&#x5148;&#x4FEE;&#x590D; RepositoryFileHealthGateTest &#x7684;&#x5931;&#x8D25;&#x9879;&#xFF0C;&#x518D;&#x628A;&#x6587;&#x4EF6;&#x5065;&#x5EB7;&#x7ED3;&#x8BBA;&#x89C6;&#x4E3A;&#x53EF;&#x4FE1;&#x3002;" }
        "actionFixFastFeedback" { return "&#x5148;&#x6062;&#x590D; mvnw test -Dsurefire.excludedGroups=slow &#x901A;&#x8FC7;&#xFF0C;&#x518D;&#x628A;&#x8FD9;&#x4EFD;&#x62A5;&#x544A;&#x7EB3;&#x5165;&#x65E5;&#x5E38;&#x5FEB;&#x53CD;&#x9988;&#x3002;" }
        "actionTightenExemptions" { return "&#x7EE7;&#x7EED;&#x6536;&#x655B;&#x4E34;&#x65F6;&#x8C41;&#x514D;&#xFF0C;&#x628A;&#x8D85;&#x9650;&#x7684;&#x751F;&#x6210;&#x578B;&#x5927;&#x6587;&#x4EF6;&#x8FC1;&#x51FA;&#x6216;&#x6539;&#x9020;&#x6210;&#x53EF;&#x590D;&#x751F;&#x6210;&#x7269;&#x3002;" }
        "actionSplitPersistence" { return "&#x4F18;&#x5148;&#x62C6;&#x5206; Repository/Jdbc/Support &#x70ED;&#x70B9;&#xFF0C;&#x628A;&#x8BFB;&#x5199;&#x3001;schema &#x63A2;&#x6D4B;&#x548C;&#x6620;&#x5C04;&#x804C;&#x8D23;&#x7EE7;&#x7EED;&#x4E0B;&#x6C89;&#x3002;" }
        "actionSplitServices" { return "&#x5BF9;&#x8D85;&#x5927;&#x7684;&#x5E94;&#x7528;&#x670D;&#x52A1;&#x6309; query/write/import-export &#x6216;&#x5B50;&#x57DF;&#x62C6;&#x5206;&#xFF0C;&#x540C;&#x65F6;&#x4FDD;&#x6301; facade &#x7A33;&#x5B9A;&#x3002;" }
        "actionMoveAssemblerLogic" { return "&#x628A;&#x539A; controller/assembler &#x7684; DTO &#x7EC4;&#x88C5;&#x903B;&#x8F91;&#x7EE7;&#x7EED;&#x8FC1;&#x79FB;&#x5230;&#x663E;&#x5F0F; assembler &#x6216; mapper&#x3002;" }
        "actionThinModule" { return "&#x8865;&#x5F3A; <code>{0}</code> &#x7684;&#x6D4B;&#x8BD5;&#x9762;&#xFF1B;&#x5F53;&#x524D; test/main &#x6BD4;&#x503C;&#x7EA6;&#x4E3A; {1}&#x3002;" }
        "actionKeepBaseline" { return "&#x4FDD;&#x6301; <code>bl-center</code> &#x7684; JaCoCo baseline &#x6CBB;&#x7406;&#x6D41;&#x7A0B;&#xFF0C;&#x53EA;&#x5728;&#x660E;&#x786E;&#x9A8C;&#x8BC1;&#x540E;&#x66F4;&#x65B0;&#x9608;&#x503C;&#x3002;" }
        default { return $Key }
    }
}

function Get-StatusLabel {
    param([string]$Status)

    switch ($Status) {
        "PASS" { return "&#x901A;&#x8FC7;" }
        "WATCH" { return "&#x5173;&#x6CE8;" }
        "FAIL" { return "&#x5931;&#x8D25;" }
        default { return "&#x672A;&#x77E5;" }
    }
}

function Convert-AreaToZh {
    param([string]$Area)

    switch ($Area) {
        "File Health" { return "&#x6587;&#x4EF6;&#x5065;&#x5EB7;" }
        "Testability" { return "&#x53EF;&#x6D4B;&#x6027;" }
        "Maintainability" { return "&#x53EF;&#x7EF4;&#x62A4;&#x6027;" }
        "Naming and Boundaries" { return "&#x547D;&#x540D;&#x4E0E;&#x8FB9;&#x754C;" }
        "Errors and Encoding" { return "&#x5F02;&#x5E38;&#x4E0E;&#x7F16;&#x7801;" }
        default { return Convert-ToHtml $Area }
    }
}

function Convert-ConclusionToZh {
    param([string]$Conclusion)

    switch ($Conclusion) {
        "RepositoryFileHealthGateTest failed; see the Testability section for command output." { return "RepositoryFileHealthGateTest &#x5931;&#x8D25;&#xFF1B;&#x8BE6;&#x60C5;&#x89C1;&#x4E0B;&#x65B9;&#x9A8C;&#x8BC1;&#x7ED3;&#x679C;&#x3002;" }
        "At least one verification command failed; the report was still generated with recorded evidence." { return "&#x81F3;&#x5C11;&#x6709;&#x4E00;&#x6761;&#x9A8C;&#x8BC1;&#x547D;&#x4EE4;&#x5931;&#x8D25;&#xFF1B;&#x4F46;&#x62A5;&#x544A;&#x4ECD;&#x5DF2;&#x751F;&#x6210;&#xFF0C;&#x5E76;&#x4FDD;&#x7559;&#x4E86;&#x5931;&#x8D25;&#x8BC1;&#x636E;&#x3002;" }
        "Current repository state has 58 Java files over 300 lines and 0 over 500 lines." { return "&#x5F53;&#x524D;&#x4ED3;&#x5E93;&#x5171;&#x6709; 58 &#x4E2A; Java &#x6587;&#x4EF6;&#x8D85;&#x8FC7; 300 &#x884C;&#xFF0C;0 &#x4E2A;&#x8D85;&#x8FC7; 500 &#x884C;&#x3002;" }
        "No generic utility filenames found; TODO/FIXME hits only matched the domain constant TODO_TASK." { return "&#x672A;&#x53D1;&#x73B0;&#x6CDB;&#x5316;&#x5DE5;&#x5177;&#x7C7B;&#x6587;&#x4EF6;&#x540D;&#xFF1B;TODO/FIXME &#x4EC5;&#x547D;&#x4E2D;&#x4E86;&#x9886;&#x57DF;&#x5E38;&#x91CF; TODO_TASK&#x3002;" }
        "No implicit charset conversions or empty catch blocks found." { return "&#x672A;&#x53D1;&#x73B0;&#x9690;&#x5F0F;&#x5B57;&#x7B26;&#x96C6;&#x8F6C;&#x6362;&#x6216;&#x7A7A; catch &#x4EE3;&#x7801;&#x5757;&#x3002;" }
        default { return Convert-ToHtmlWithBreaks $Conclusion }
    }
}

Push-Location $repoRoot
try {
    $largestResult = Invoke-ReportStep -Name "largest-files" -CommandText ".\scripts\ci\generate-largest-files-report.ps1" -ScriptBlock {
        & (Join-Path $repoRoot "scripts\ci\generate-largest-files-report.ps1") -OutputPath "docs/reports/largest-files-report.md"
    }

    $gateResult = Invoke-LoggedCommand -Name "gate" -CommandText ".\mvnw.cmd -pl common\common-test -Dtest=RepositoryFileHealthGateTest test" -ScriptBlock {
        & .\mvnw.cmd -pl common\common-test -Dtest=RepositoryFileHealthGateTest test
    }

    $fastResult = Invoke-LoggedCommand -Name "fast-feedback" -CommandText ".\mvnw.cmd test ""-Dsurefire.excludedGroups=slow""" -ScriptBlock {
        & .\mvnw.cmd test "-Dsurefire.excludedGroups=slow"
    }

    $checklistResult = Invoke-ReportStep -Name "checklist" -CommandText ".\scripts\ci\generate-code-health-checklist.ps1 -SkipExecution" -ScriptBlock {
        & (Join-Path $repoRoot "scripts\ci\generate-code-health-checklist.ps1") `
            -OutputPath "docs/reports/code-health-checklist.md" `
            -SkipExecution `
            -GateStatus $gateResult.Status `
            -GateDurationSeconds $gateResult.DurationSeconds `
            -GateExitCode $gateResult.ExitCode `
            -GateSummary $gateResult.Summary `
            -FastStatus $fastResult.Status `
            -FastDurationSeconds $fastResult.DurationSeconds `
            -FastExitCode $fastResult.ExitCode `
            -FastSummary $fastResult.Summary
    }
} finally {
    Pop-Location
}

$checklistContent = Read-TextFile $checklistPath
$largestContent = Read-TextFile $largestFilesReportPath
$overviewRows = Get-ChecklistOverviewRows $checklistContent
$largestData = Get-LargestFilesData $largestContent
$moduleStats = Get-ModuleStats
$worktree = Get-WorktreeContext
$codegraph = Get-CodeGraphContext
$coverage = Get-CoverageBaseline
$recommendedActions = Get-RecommendedActions -GateResult $gateResult -FastResult $fastResult -LargestData $largestData -ModuleStats $moduleStats
$overallStatus = Get-OverallStatus -OverviewRows $overviewRows -GateResult $gateResult -FastResult $fastResult
if ($coverage.Line) {
    $coverageLineDisplay = $coverage.Line
} else {
    $coverageLineDisplay = "unknown"
}
if ($coverage.Branch) {
    $coverageBranchDisplay = $coverage.Branch
} else {
    $coverageBranchDisplay = "unknown"
}
if ($null -ne $largestData.JavaOver300) {
    $hotspotsOver300Display = [string]$largestData.JavaOver300
} else {
    $hotspotsOver300Display = "unknown"
}
if ($null -ne $largestData.JavaOver500) {
    $hotspotsOver500Display = [string]$largestData.JavaOver500
} else {
    $hotspotsOver500Display = "unknown"
}
if ($null -ne $largestData.HardLimitCount) {
    $hardLimitDisplay = [string]$largestData.HardLimitCount
} else {
    $hardLimitDisplay = "unknown"
}
if ($worktree.Dirty) {
    $dirtyDisplay = "Yes"
    $dirtyLabel = Get-HtmlText 'yes'
} else {
    $dirtyDisplay = "No"
    $dirtyLabel = Get-HtmlText 'no'
}

if (Test-Path $projectHealthRulesPath) {
    $rulesSummary = Get-HtmlText 'projectRulesFound'
} else {
    $rulesSummary = Get-HtmlText 'projectRulesMissing'
}

$summaryCards = foreach ($row in $overviewRows) {
    @"
<article class="summary-card">
  <div class="status-badge $(Get-StatusClass $row.Status)">$(Get-StatusLabel $row.Status)</div>
  <h3>$(Convert-AreaToZh $row.Area)</h3>
  <p>$(Convert-ConclusionToZh $row.Conclusion)</p>
</article>
"@
}

$hotspotRowsHtml = ""
if ($largestData.TopHotspots.Count -gt 0) {
    $hotspotRowsHtml = ($largestData.TopHotspots | ForEach-Object {
        $focus = Get-HtmlText 'hotspotFocus'
        if ($_.File -match 'Repository|Jdbc|Support') {
            $focus = Get-HtmlText 'persistenceFocus'
        } elseif ($_.File -match 'Service') {
            $focus = Get-HtmlText 'serviceFocus'
        } elseif ($_.File -match 'Controller') {
            $focus = Get-HtmlText 'controllerFocus'
        }
        "<tr><td>$($_.Lines)</td><td><code>$(Convert-ToHtml $_.File)</code></td><td>$focus</td></tr>"
    }) -join "`n"
} else {
    $hotspotRowsHtml = "<tr><td colspan=""3"">$(Get-HtmlText 'noHotspots')</td></tr>"
}

$moduleRowsHtml = ""
if ($moduleStats.Count -gt 0) {
    $moduleRowsHtml = ($moduleStats | ForEach-Object {
        "<tr><td><code>$(Convert-ToHtml $_.Module)</code></td><td>$($_.MainJava)</td><td>$($_.TestJava)</td><td>$($_.Ratio)</td></tr>"
    }) -join "`n"
} else {
    $moduleRowsHtml = "<tr><td colspan=""4"">$(Get-HtmlText 'noModules')</td></tr>"
}

$verificationRowsHtml = @(
    $gateResult,
    $fastResult
) | ForEach-Object {
    "<tr><td><code>$(Convert-ToHtml $_.Command)</code></td><td><span class=""status-badge $(Get-StatusClass $_.Status)"">$(Get-StatusLabel $_.Status)</span></td><td>$($_.ExitCode)</td><td>$($_.DurationSeconds)s</td><td>$(Render-VerificationDetails $_)</td></tr>"
} | Out-String

if ($worktree.SamplePaths.Count -gt 0) {
    $worktreeSamplesHtml = ($worktree.SamplePaths | ForEach-Object { "<li><code>$(Convert-ToHtml $_)</code></li>" }) -join "`n"
} else {
    $worktreeSamplesHtml = "<li>$(Get-HtmlText 'worktreeClean')</li>"
}

$recommendationItemsHtml = ($recommendedActions | ForEach-Object { "<li>$_</li>" }) -join "`n"

if ($codegraph.Available) {
    $codegraphHtml = @"
<ul class="facts">
  <li>$(Get-HtmlText 'indexedFiles')&#xFF1A;<strong>$(Convert-ToHtml $codegraph.Files)</strong></li>
  <li>$(Get-HtmlText 'nodes')&#xFF1A;<strong>$(Convert-ToHtml $codegraph.Nodes)</strong></li>
  <li>$(Get-HtmlText 'edges')&#xFF1A;<strong>$(Convert-ToHtml $codegraph.Edges)</strong></li>
  <li>$(Get-HtmlText 'backend')&#xFF1A;<strong>$(Convert-ToHtml $codegraph.Backend)</strong></li>
  <li>$(Get-HtmlText 'pendingModified')&#xFF1A;<strong>$(Convert-ToHtml $codegraph.PendingModified)</strong></li>
</ul>
"@
} else {
    $codegraphHtml = "<p>$(Convert-ToHtml $codegraph.Summary)</p>"
}

$html = @"
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>$(Get-HtmlText 'title')</title>
  <style>
    :root {
      --bg: #f4f1ea;
      --panel: #fffdf8;
      --ink: #1f2a30;
      --muted: #67757f;
      --line: #d8d2c4;
      --pass: #2f7d4a;
      --watch: #b87418;
      --fail: #b53a2d;
      --unknown: #5d6d79;
      --shadow: 0 18px 40px rgba(32, 39, 43, 0.08);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
      background:
        radial-gradient(circle at top right, rgba(205, 160, 67, 0.18), transparent 28%),
        linear-gradient(180deg, #efe8da 0%, var(--bg) 22%, #efe9dc 100%);
      color: var(--ink);
    }
    .page {
      max-width: 1240px;
      margin: 0 auto;
      padding: 32px 20px 56px;
    }
    .hero, .section {
      background: var(--panel);
      border: 1px solid rgba(143, 132, 114, 0.25);
      border-radius: 22px;
      box-shadow: var(--shadow);
    }
    .hero {
      padding: 28px;
      margin-bottom: 22px;
    }
    .hero-top {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    h1, h2, h3 { margin: 0; }
    h1 {
      font-size: clamp(30px, 5vw, 48px);
      letter-spacing: -0.04em;
    }
    .lede {
      margin-top: 12px;
      color: var(--muted);
      max-width: 760px;
      line-height: 1.6;
    }
    .status-pill {
      padding: 10px 16px;
      border-radius: 999px;
      font-weight: 700;
      letter-spacing: 0.06em;
      border: 1px solid currentColor;
      min-width: 120px;
      text-align: center;
    }
    .status-pill.pass { color: var(--pass); background: rgba(47, 125, 74, 0.08); }
    .status-pill.watch { color: var(--watch); background: rgba(184, 116, 24, 0.1); }
    .status-pill.fail { color: var(--fail); background: rgba(181, 58, 45, 0.1); }
    .status-pill.unknown { color: var(--unknown); background: rgba(93, 109, 121, 0.1); }
    .hero-grid, .summary-grid, .meta-grid {
      display: grid;
      gap: 16px;
    }
    .hero-grid {
      margin-top: 24px;
      grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    }
    .metric {
      background: rgba(247, 243, 235, 0.92);
      border: 1px solid rgba(143, 132, 114, 0.25);
      border-radius: 18px;
      padding: 16px;
    }
    .metric span {
      display: block;
      color: var(--muted);
      font-size: 13px;
      margin-bottom: 8px;
    }
    .metric strong {
      font-size: 28px;
      letter-spacing: -0.03em;
    }
    .section {
      padding: 24px;
      margin-top: 18px;
    }
    .section h2 {
      font-size: 24px;
      margin-bottom: 8px;
      letter-spacing: -0.03em;
    }
    .section-intro {
      color: var(--muted);
      margin-bottom: 18px;
      line-height: 1.6;
    }
    .summary-grid {
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }
    .summary-card {
      border: 1px solid rgba(143, 132, 114, 0.22);
      border-radius: 18px;
      padding: 18px;
      background: rgba(248, 245, 239, 0.85);
    }
    .summary-card h3 {
      margin-top: 12px;
      font-size: 18px;
    }
    .summary-card p {
      margin: 10px 0 0;
      color: var(--muted);
      line-height: 1.6;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 78px;
      padding: 6px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.06em;
      border: 1px solid currentColor;
    }
    .status-badge.pass { color: var(--pass); background: rgba(47, 125, 74, 0.08); }
    .status-badge.watch { color: var(--watch); background: rgba(184, 116, 24, 0.1); }
    .status-badge.fail { color: var(--fail); background: rgba(181, 58, 45, 0.1); }
    .status-badge.unknown { color: var(--unknown); background: rgba(93, 109, 121, 0.1); }
    .table-wrap {
      overflow-x: auto;
      border: 1px solid rgba(143, 132, 114, 0.2);
      border-radius: 16px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      min-width: 720px;
      background: #fffefb;
    }
    th, td {
      padding: 14px 16px;
      border-bottom: 1px solid rgba(143, 132, 114, 0.16);
      text-align: left;
      vertical-align: top;
      font-size: 14px;
      line-height: 1.55;
    }
    th {
      background: rgba(244, 238, 228, 0.94);
      color: #334047;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    code {
      font-family: "Cascadia Code", "Consolas", monospace;
      font-size: 0.92em;
      color: #183447;
    }
    pre {
      margin: 10px 0 0;
      padding: 12px;
      border-radius: 12px;
      background: #f4f1ea;
      border: 1px solid rgba(143, 132, 114, 0.2);
      white-space: pre-wrap;
      word-break: break-word;
      font-size: 12px;
      line-height: 1.5;
    }
    details summary {
      cursor: pointer;
      color: #2d4958;
      font-weight: 600;
    }
    .meta-grid {
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    }
    .panel {
      border: 1px solid rgba(143, 132, 114, 0.2);
      border-radius: 18px;
      padding: 18px;
      background: rgba(248, 245, 239, 0.85);
    }
    .panel h3 {
      font-size: 18px;
      margin-bottom: 12px;
    }
    .facts, .list {
      margin: 0;
      padding-left: 18px;
      color: var(--muted);
      line-height: 1.7;
    }
    .facts strong {
      color: var(--ink);
    }
    .footer-note {
      margin-top: 18px;
      color: var(--muted);
      font-size: 13px;
      line-height: 1.7;
    }
    @media (max-width: 720px) {
      .page { padding: 18px 14px 40px; }
      .hero, .section { padding: 18px; border-radius: 18px; }
      table { min-width: 620px; }
    }
  </style>
</head>
<body>
  <div class="page">
    <section class="hero">
      <div class="hero-top">
        <div>
          <h1>$(Get-HtmlText 'title')</h1>
          <p class="lede">$(Get-HtmlText 'lede')</p>
        </div>
        <div class="status-pill $(Get-StatusClass $overallStatus)">$(Get-StatusLabel $overallStatus)</div>
      </div>
      <div class="hero-grid">
        <div class="metric">
          <span>$(Get-HtmlText 'generatedAt')</span>
          <strong>$(Convert-ToHtml $reportTimeLocal)</strong>
        </div>
        <div class="metric">
          <span>300+ &#x884C;&#x70ED;&#x70B9;&#x6570;</span>
          <strong>$hotspotsOver300Display</strong>
        </div>
        <div class="metric">
          <span>$(Get-HtmlText 'gateAndFast')</span>
          <strong>$(Get-StatusLabel $gateResult.Status) / $(Get-StatusLabel $fastResult.Status)</strong>
        </div>
        <div class="metric">
          <span>$(Get-HtmlText 'worktreeDirty')</span>
          <strong>$dirtyLabel</strong>
        </div>
      </div>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'summary')</h2>
      <p class="section-intro">$(Get-HtmlText 'summaryIntro')</p>
      <div class="summary-grid">
        $($summaryCards -join "`n")
      </div>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'verification')</h2>
      <p class="section-intro">$(Get-HtmlText 'verificationIntro')</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>$(Get-HtmlText 'command')</th>
              <th>$(Get-HtmlText 'status')</th>
              <th>$(Get-HtmlText 'exit')</th>
              <th>$(Get-HtmlText 'duration')</th>
              <th>$(Get-HtmlText 'details')</th>
            </tr>
          </thead>
          <tbody>
            $verificationRowsHtml
          </tbody>
        </table>
      </div>
      <p class="footer-note">$([string]::Format((Get-HtmlText 'coverageNote'), (Convert-ToHtml $coverageLineDisplay), (Convert-ToHtml $coverageBranchDisplay)))</p>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'hotspots')</h2>
      <p class="section-intro">$([string]::Format((Get-HtmlText 'hotspotsIntro'), $hardLimitDisplay, $hotspotsOver500Display))</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>$(Get-HtmlText 'lines')</th>
              <th>$(Get-HtmlText 'file')</th>
              <th>$(Get-HtmlText 'why')</th>
            </tr>
          </thead>
          <tbody>
            $hotspotRowsHtml
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'moduleDist')</h2>
      <p class="section-intro">$(Get-HtmlText 'moduleIntro')</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>$(Get-HtmlText 'module')</th>
              <th>$(Get-HtmlText 'mainJava')</th>
              <th>$(Get-HtmlText 'testJava')</th>
              <th>$(Get-HtmlText 'ratio')</th>
            </tr>
          </thead>
          <tbody>
            $moduleRowsHtml
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'context')</h2>
      <p class="section-intro">$(Get-HtmlText 'contextIntro')</p>
      <div class="meta-grid">
        <article class="panel">
          <h3>$(Get-HtmlText 'gitWorktree')</h3>
          <ul class="facts">
            <li>$(Get-HtmlText 'dirty')&#xFF1A;<strong>$dirtyLabel</strong></li>
            <li>$(Get-HtmlText 'trackedChanges')&#xFF1A;<strong>$($worktree.ModifiedCount)</strong></li>
            <li>$(Get-HtmlText 'untrackedFiles')&#xFF1A;<strong>$($worktree.UntrackedCount)</strong></li>
          </ul>
          <ul class="list">
            $worktreeSamplesHtml
          </ul>
        </article>
        <article class="panel">
          <h3>$(Get-HtmlText 'codegraph')</h3>
          $codegraphHtml
        </article>
        <article class="panel">
          <h3>$(Get-HtmlText 'sources')</h3>
          <ul class="facts">
            <li>$(Get-HtmlText 'largestReport')&#xFF1A;<strong>$(Get-StatusLabel $largestResult.Status)</strong></li>
            <li>$(Get-HtmlText 'checklistReport')&#xFF1A;<strong>$(Get-StatusLabel $checklistResult.Status)</strong></li>
            <li>$(Get-HtmlText 'generatedUtc')&#xFF1A;<strong>$(Convert-ToHtml $reportTimeUtc)</strong></li>
          </ul>
          <p class="footer-note">$rulesSummary</p>
        </article>
      </div>
    </section>

    <section class="section">
      <h2>$(Get-HtmlText 'actions')</h2>
      <p class="section-intro">$(Get-HtmlText 'actionsIntro')</p>
      <ul class="list">
        $recommendationItemsHtml
      </ul>
    </section>
  </div>
</body>
</html>
"@

Write-Utf8NoBom -Path $latestReportPath -Content $html
Write-Utf8NoBom -Path $datedReportPath -Content $html

if ($largestResult.Status -eq "FAIL" -or $checklistResult.Status -eq "FAIL" -or $gateResult.Status -eq "FAIL" -or $fastResult.Status -eq "FAIL") {
    exit 1
}
