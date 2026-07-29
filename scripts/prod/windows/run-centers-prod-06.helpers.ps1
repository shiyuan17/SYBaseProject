function Import-KeyValueConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        if ($trimmed -notmatch "^(?<key>[A-Za-z_][A-Za-z0-9_]*)=(?<value>.*)$") {
            continue
        }

        $key = $Matches.key
        $value = $Matches.value.Trim()
        if (
            ($value.Length -ge 2) -and
            (
                ($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))
            )
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

function Get-Setting {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,

        [string] $DefaultValue = $null
    )

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrEmpty($value)) {
        return $DefaultValue
    }

    return $value
}

function Get-ManagedDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string] $SettingName
    )

    return (Get-Setting -Name $SettingName -DefaultValue $script:ProdDir)
}

function Get-ServiceFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ServiceKey,

        [Parameter(Mandatory = $true)]
        [string] $Kind
    )

    $definition = Get-ServiceDefinition $ServiceKey
    switch ($Kind) {
        "pid" { return (Join-Path (Get-ManagedDirectory "RUNTIME_DIR") "$($definition.Name).pid") }
        "log" { return (Join-Path (Get-ManagedDirectory "LOG_DIR") "$($definition.Name).log") }
        "stderr" { return (Join-Path (Get-ManagedDirectory "LOG_DIR") "$($definition.Name).stderr.log") }
        default { throw "Unsupported service file kind: $Kind" }
    }
}

function Get-TailContentCompat {
    param(
        [Parameter(Mandatory = $true)]
        [string[]] $Path,

        [int] $TailLines = 200
    )

    foreach ($candidate in $Path) {
        $lines = @(Get-Content -Path $candidate)
        if ($lines.Count -eq 0) {
            continue
        }

        $startIndex = 0
        if ($lines.Count -gt $TailLines) {
            $startIndex = $lines.Count - $TailLines
        }

        for ($index = $startIndex; $index -lt $lines.Count; $index++) {
            $lines[$index]
        }
    }
}

function Join-CommandLineArguments {
    param(
        [string[]] $Arguments = @()
    )

    $parts = New-Object System.Collections.Generic.List[string]
    foreach ($argument in $Arguments) {
        if ($argument -eq $null) {
            continue
        }

        if ($argument.Length -eq 0) {
            $parts.Add('""')
            continue
        }

        $formatted = $argument.Replace('"', '\"')
        if ($formatted -match '\s|"') {
            $parts.Add('"' + $formatted + '"')
        }
        else {
            $parts.Add($formatted)
        }
    }

    return ($parts.ToArray() -join " ")
}

function Split-CommandLine {
    param(
        [string] $Text
    )

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @()
    }

    $parts = New-Object System.Collections.Generic.List[string]
    $current = New-Object System.Text.StringBuilder
    $quoteChar = $null
    $escapeCharacter = [char] 96

    for ($index = 0; $index -lt $Text.Length; $index++) {
        $character = $Text[$index]

        if ($quoteChar) {
            if ($character -eq $quoteChar) {
                $quoteChar = $null
                continue
            }

            if ($character -eq $escapeCharacter -and ($index + 1) -lt $Text.Length) {
                $nextCharacter = $Text[$index + 1]
                if ($nextCharacter -eq $quoteChar -or $nextCharacter -eq $escapeCharacter) {
                    [void] $current.Append($nextCharacter)
                    $index++
                    continue
                }
            }

            [void] $current.Append($character)
            continue
        }

        if ([char]::IsWhiteSpace($character)) {
            if ($current.Length -gt 0) {
                $parts.Add($current.ToString())
                [void] $current.Remove(0, $current.Length)
            }
            continue
        }

        if ($character -eq '"' -or $character -eq "'") {
            $quoteChar = $character
            continue
        }

        [void] $current.Append($character)
    }

    if ($quoteChar) {
        throw "Unable to parse command-line text: $Text"
    }

    if ($current.Length -gt 0) {
        $parts.Add($current.ToString())
    }

    if ($parts.Count -eq 0) {
        return @()
    }

    return ,$parts.ToArray()
}
