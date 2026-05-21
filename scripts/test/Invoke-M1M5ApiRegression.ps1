param(
    [string]$ReportDate = (Get-Date -Format "yyyyMMdd"),
    [switch]$SkipExecution
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$mvnw = Join-Path $repoRoot "mvnw.cmd"
$reportDir = Join-Path $repoRoot "docs\reports"
$reportPath = Join-Path $reportDir ("m1-m5-api-test-report-{0}.md" -f $ReportDate)

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$groups = @(
    @{
        Name = "AUTH"
        Module = "auth-center"
        Milestone = "cross-cutting"
        Category = "single-api"
        Tests = @("AuthControllerIntegrationTest")
        Command = '.\mvnw.cmd -pl auth-center -am test "-Dtest=AuthControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "USER"
        Module = "user-center"
        Milestone = "cross-cutting"
        Category = "single-api"
        Tests = @("UserControllerIntegrationTest")
        Command = '.\mvnw.cmd -pl user-center -am test "-Dtest=UserControllerIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "M1"
        Module = "bl-center"
        Milestone = "M1"
        Category = "single-api,cross-cutting"
        Tests = @(
            "SystemManagementUserIntegrationTest",
            "SystemManagementRoleAndMenuIntegrationTest",
            "MasterDataControllerIntegrationTest",
            "M1RoleAuthorizationIntegrationTest",
            "M1SingleApiLifecycleIntegrationTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=SystemManagementUserIntegrationTest,SystemManagementRoleAndMenuIntegrationTest,MasterDataControllerIntegrationTest,M1RoleAuthorizationIntegrationTest,M1SingleApiLifecycleIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "M2"
        Module = "bl-center"
        Milestone = "M2"
        Category = "single-api,scenario,cross-cutting"
        Tests = @(
            "ApplicationControllerIntegrationTest",
            "SpecimenWorkflowHappyPathIntegrationTest",
            "SpecimenWorkflowClosureIntegrationTest",
            "M2RoleAuthorizationIntegrationTest",
            "M2RoleScenarioIntegrationTest",
            "M2CollectionAndLabelIntegrationTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=ApplicationControllerIntegrationTest,SpecimenWorkflowHappyPathIntegrationTest,SpecimenWorkflowClosureIntegrationTest,M2RoleAuthorizationIntegrationTest,M2RoleScenarioIntegrationTest,M2CollectionAndLabelIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "M3"
        Module = "bl-center"
        Milestone = "M3"
        Category = "single-api,scenario,cross-cutting"
        Tests = @(
            "TechnicalWorkflowIntegrationTest",
            "TechnicalWorkflowQueryEnhancementIntegrationTest",
            "M3RoleAuthorizationMatrixIntegrationTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=TechnicalWorkflowIntegrationTest,TechnicalWorkflowQueryEnhancementIntegrationTest,M3RoleAuthorizationMatrixIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "M4"
        Module = "bl-center"
        Milestone = "M4"
        Category = "single-api,scenario,cross-cutting"
        Tests = @(
            "DiagnosticWorkflowIntegrationTest",
            "DiagnosticRevisionIntegrationTest",
            "InternalConsultationIntegrationTest",
            "MedicalOrderIntegrationTest",
            "M4RoleAuthorizationIntegrationTest",
            "M4Batch2AuthorizationIntegrationTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=DiagnosticWorkflowIntegrationTest,DiagnosticRevisionIntegrationTest,InternalConsultationIntegrationTest,MedicalOrderIntegrationTest,M4RoleAuthorizationIntegrationTest,M4Batch2AuthorizationIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "M5"
        Module = "bl-center"
        Milestone = "M5"
        Category = "single-api,scenario,cross-cutting"
        Tests = @(
            "ArchiveWorkflowIntegrationTest",
            "ArchiveRoleAuthorizationIntegrationTest",
            "OperationSupportIntegrationTest",
            "M5SingleApiIntegrationTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=ArchiveWorkflowIntegrationTest,ArchiveRoleAuthorizationIntegrationTest,OperationSupportIntegrationTest,M5SingleApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    },
    @{
        Name = "GATE"
        Module = "bl-center"
        Milestone = "quality-gate"
        Category = "migration-gate"
        Tests = @(
            "FlywayTableCoverageTest",
            "LegacyDmFlywayOnboardingTest"
        )
        Command = '.\mvnw.cmd -pl bl-center -am test "-Dtest=FlywayTableCoverageTest,LegacyDmFlywayOnboardingTest" "-Dsurefire.failIfNoSpecifiedTests=false"'
    }
)

function Get-GroupSummary {
    param(
        [string]$Module,
        [string[]]$TestClasses
    )

    $reportRoot = Join-Path $repoRoot ($Module + "\target\surefire-reports")
    $summary = [ordered]@{
        Tests = 0
        Failures = 0
        Errors = 0
        Skipped = 0
        FailedClasses = @()
    }

    if (-not (Test-Path $reportRoot)) {
        return $summary
    }

    $files = Get-ChildItem $reportRoot -Filter "TEST-*.xml" | Where-Object {
        $baseName = $_.BaseName.Substring(5)
        $className = $baseName.Split(".")[-1]
        $TestClasses -contains $className
    }

    foreach ($file in $files) {
        [xml]$xml = Get-Content -Raw $file.FullName
        $suite = $xml.testsuite
        if (-not $suite) {
            continue
        }

        $summary.Tests += [int]$suite.tests
        $summary.Failures += [int]$suite.failures
        $summary.Errors += [int]$suite.errors
        $summary.Skipped += [int]$suite.skipped

        if (([int]$suite.failures) -gt 0 -or ([int]$suite.errors) -gt 0) {
            $summary.FailedClasses += $suite.name.Split(".")[-1]
        }
    }

    $summary.FailedClasses = $summary.FailedClasses | Sort-Object -Unique
    return $summary
}

$results = @()
$overallStatus = "PASS"

Push-Location $repoRoot
try {
    foreach ($group in $groups) {
        $commandArgs = @(
            "-pl", $group.Module,
            "-am",
            "test",
            ("-Dtest=" + ($group.Tests -join ",")),
            "-Dsurefire.failIfNoSpecifiedTests=false"
        )

        $exitCode = 0
        if (-not $SkipExecution) {
            & $mvnw @commandArgs
            $exitCode = $LASTEXITCODE
        }

        $summary = Get-GroupSummary -Module $group.Module -TestClasses $group.Tests
        $status = if ($exitCode -eq 0 -and $summary.Failures -eq 0 -and $summary.Errors -eq 0) { "PASS" } else { "FAIL" }
        if ($status -eq "FAIL") {
            $overallStatus = "FAIL"
        }

        $results += [pscustomobject]@{
            Name = $group.Name
            Module = $group.Module
            Milestone = $group.Milestone
            Category = $group.Category
            Tests = $group.Tests
            Command = $group.Command
            ExitCode = $exitCode
            Status = $status
            Summary = $summary
        }

        if ($exitCode -ne 0 -and -not $SkipExecution) {
            break
        }
    }
}
finally {
    Pop-Location
}

$generatedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$commandLines = ($results | ForEach-Object { $_.Command }) -join "`n"
$summaryLines = $results | ForEach-Object {
    "| {0} | {1} | {2} | {3} | {4} | {5} | {6} |" -f $_.Name, $_.Module, $_.Milestone, $_.Summary.Tests, $_.Summary.Failures, $_.Summary.Errors, $_.Summary.Skipped
}

$failedGroups = $results | Where-Object { $_.Status -eq "FAIL" }
$failureSection = if ($failedGroups.Count -eq 0) {
    "- None"
} else {
    ($failedGroups | ForEach-Object {
        $failedClasses = if ($_.Summary.FailedClasses.Count -eq 0) { "Unknown class, inspect Surefire artifacts" } else { ($_.Summary.FailedClasses -join ", ") }
        "- {0}: {1}" -f $_.Name, $failedClasses
    }) -join "`n"
}

$coverageLines = $results | ForEach-Object {
    "- {0} ({1} / {2}): {3}" -f $_.Name, $_.Milestone, $_.Category, ($_.Tests -join ", ")
}

$markdown = @"
# M1-M5 API Regression Report

- Generated At: $generatedAt
- Environment: local H2 `test` profile
- Runner: Maven Surefire + SpringBootTest + MockMvc
- Overall Status: $overallStatus
- Surefire artifacts:
  - `auth-center/target/surefire-reports`
  - `user-center/target/surefire-reports`
  - `bl-center/target/surefire-reports`

## Commands

```powershell
$commandLines
```

## Group Summary

| Group | Module | Milestone | Tests | Failures | Errors | Skipped |
| --- | --- | --- | ---: | ---: | ---: | ---: |
$($summaryLines -join "`n")

## Coverage

$($coverageLines -join "`n")

## Quality Gates

- `FlywayTableCoverageTest`
- `LegacyDmFlywayOnboardingTest`

## Deferred Or Uncovered

- None

## Failures And Root Cause

$failureSection

## Final Conclusion

- Status: $overallStatus
- For module-level details, inspect the corresponding `target/surefire-reports` XML and text files.
"@

Set-Content -Path $reportPath -Value $markdown -Encoding UTF8
Write-Output $reportPath
