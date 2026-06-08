param(
    [Parameter(Mandatory = $true)]
    [string] $Command,

    [Parameter(Mandatory = $true)]
    [string] $LogFile
)

$processInfo = [System.Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = 'cmd.exe'
$processInfo.Arguments = "/d /c $Command 2>&1"
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $false

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $processInfo

$logStream = [System.IO.FileStream]::new(
    $LogFile,
    [System.IO.FileMode]::Append,
    [System.IO.FileAccess]::Write,
    [System.IO.FileShare]::ReadWrite
)
$writer = [System.IO.StreamWriter]::new(
    $logStream,
    [System.Text.UTF8Encoding]::new($false)
)
$writer.AutoFlush = $true

try {
    [void] $process.Start()
    while (($line = $process.StandardOutput.ReadLine()) -ne $null) {
        [Console]::Out.WriteLine($line)
        $writer.WriteLine($line)
    }
    $process.WaitForExit()
    exit $process.ExitCode
}
finally {
    $writer.Dispose()
    $logStream.Dispose()
    $process.Dispose()
}
