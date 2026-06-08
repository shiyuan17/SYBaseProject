$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

$textExtensions = @(
    '.bat', '.cmd', '.css', '.csv', '.html', '.java', '.js', '.json', '.md',
    '.properties', '.ps1', '.sh', '.sql', '.ts', '.txt', '.xml', '.yaml', '.yml'
)

$stagedFiles = & git diff --cached --name-only --diff-filter=ACMR

if (-not $stagedFiles) {
    Write-Host '[hooks] No staged files to check.'
    exit 0
}

$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$violations = New-Object System.Collections.Generic.List[string]

foreach ($relativePath in $stagedFiles) {
    $extension = [System.IO.Path]::GetExtension($relativePath).ToLowerInvariant()

    if (-not $textExtensions.Contains($extension)) {
        continue
    }

    $path = Join-Path $repoRoot $relativePath

    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        continue
    }

    $bytes = [System.IO.File]::ReadAllBytes($path)

    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $violations.Add("$relativePath [UTF_8_BOM] UTF-8 BOM is not allowed")
    }

    try {
        $content = $utf8Strict.GetString($bytes)
    } catch {
        $violations.Add("$relativePath [UTF_8_DECODE] File cannot be decoded as strict UTF-8")
        continue
    }

    if ($content.Contains("`r`n") -and $extension -notin @('.cmd', '.bat')) {
        $violations.Add("$relativePath [LINE_ENDING] CRLF line endings are only allowed for .cmd and .bat files")
    }

    if ($content -match "`r(?!`n)") {
        $violations.Add("$relativePath [LINE_ENDING] Standalone CR line endings are not allowed")
    }
}

if ($violations.Count -gt 0) {
    [Console]::Error.WriteLine('[hooks] Staged file-health check failed:')
    foreach ($violation in $violations) {
        [Console]::Error.WriteLine("  - $violation")
    }
    exit 1
}

Write-Host '[hooks] Staged file-health check passed.'
