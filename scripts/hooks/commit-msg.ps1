param(
    [Parameter(Mandatory = $true)]
    [string] $MessageFile
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $MessageFile)) {
    [Console]::Error.WriteLine('[hooks] commit-msg requires the Git commit message file path.')
    exit 1
}

$firstLine = (Get-Content -LiteralPath $MessageFile -TotalCount 1) -replace "`r", ''

if ($firstLine -match '^(Merge |Revert |fixup! |squash! )') {
    exit 0
}

if ($firstLine -match '^(feat|fix|refactor|docs|test|build|chore|ci|perf)(\([a-z0-9._/-]+\))?: .+') {
    exit 0
}

$errorMessage = @'
[hooks] Invalid commit message.

Expected Conventional Commits format:
  type(scope): subject

Allowed types:
  feat, fix, refactor, docs, test, build, chore, ci, perf

Examples:
  feat(user): add role binding API
  fix(bl-center): handle duplicated specimen receipt
'@

$errorMessage -split "`n" | ForEach-Object { [Console]::Error.WriteLine($_) }

exit 1
