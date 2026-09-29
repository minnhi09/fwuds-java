[CmdletBinding()]
param(
    [ValidateSet('ct', 'dwt')]
    [string[]]$Algorithm = @('ct', 'dwt'),
    [string]$Dataset,
    [double[]]$MinWus,
    [int]$Warmups = 1,
    [int]$Repetitions = 5,
    [int]$TimeoutMinutes = 0,
    [switch]$SkipExisting,
    [long]$Seed = 42,
    [string]$DataDirectory = (Join-Path $PSScriptRoot '..\data\raw'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\results'),
    [string]$Config = (Join-Path $PSScriptRoot '..\config\paper-benchmarks.csv')
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jar = Join-Path $projectRoot 'target\fwuds-java-1.0.0-SNAPSHOT-all.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw "Build the project first: .\mvnw.cmd package" }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$rows = Import-Csv -LiteralPath $Config
if ($Dataset) { $rows = $rows | Where-Object { $_.dataset -eq $Dataset } }
if ($MinWus.Count -gt 0) {
    $rows = $rows | Where-Object {
        $threshold = [double]::Parse($_.min_wus, [System.Globalization.CultureInfo]::InvariantCulture)
        $MinWus -contains $threshold
    }
}
if (-not $rows) { throw 'No benchmark configuration matched the requested dataset.' }

foreach ($row in $rows) {
    $inputFile = Join-Path $DataDirectory $row.file
    if (-not (Test-Path -LiteralPath $inputFile)) { throw "Missing dataset: $inputFile" }
    foreach ($algorithmName in $Algorithm) {
        $thresholdTag = $row.min_wus.Replace('.', 'p')
        $outputFile = Join-Path $OutputDirectory "$($row.dataset)-$algorithmName-minwus-$thresholdTag.csv"
        if ($SkipExisting -and (Test-Path -LiteralPath $outputFile)) {
            $completedRuns = [Math]::Max(0, (Get-Content -LiteralPath $outputFile).Count - 1)
            if ($completedRuns -ge $Repetitions) {
                Write-Host "Skipping completed result: $outputFile"
                continue
            }
        }
        Write-Host "Running $($row.dataset) / $algorithmName / minwus=$($row.min_wus)"
        $javaArguments = @(
            '-jar', $jar, 'benchmark',
            '--algorithm', $algorithmName, '--input', $inputFile,
            '--window-panes', $row.window_panes, '--pane-size', $row.pane_size,
            '--weight-batch-size', $row.weight_batch_size, '--min-wus', $row.min_wus,
            '--seed', $Seed, '--max-slides', $row.max_slides,
            '--warmups', $Warmups, '--repetitions', $Repetitions, '--output', $outputFile
        )
        if ($TimeoutMinutes -le 0) {
            & java @javaArguments
            if ($LASTEXITCODE -ne 0) { throw "Benchmark failed with exit code $LASTEXITCODE" }
            continue
        }

        $startInfo = [System.Diagnostics.ProcessStartInfo]::new('java')
        $startInfo.UseShellExecute = $false
        foreach ($argument in $javaArguments) { $null = $startInfo.ArgumentList.Add([string]$argument) }
        $process = [System.Diagnostics.Process]::Start($startInfo)
        $timeoutMilliseconds = [Math]::Min([int]::MaxValue, [long]$TimeoutMinutes * 60 * 1000)
        if (-not $process.WaitForExit([int]$timeoutMilliseconds)) {
            $process.Kill($true)
            $process.WaitForExit()
            throw "Benchmark timed out after $TimeoutMinutes minute(s): $($row.dataset) / $algorithmName / minwus=$($row.min_wus)"
        }
        if ($process.ExitCode -ne 0) { throw "Benchmark failed with exit code $($process.ExitCode)" }
    }
}
