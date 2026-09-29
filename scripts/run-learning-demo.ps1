[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [string]$InputFile = (Join-Path $PSScriptRoot '..\data\sample\fwuds-example.txt'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\results\learning-demo')
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jar = Join-Path $projectRoot 'target\fwuds-java-1.0.0-SNAPSHOT-all.jar'
$inputPath = [IO.Path]::GetFullPath($InputFile)
$outputPath = [IO.Path]::GetFullPath($OutputDirectory)

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java was not found on PATH. Install JDK 21 and open a new PowerShell.'
}
$javaVersion = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($javaVersion -notmatch '21') {
    throw "This project requires JDK 21; detected: $javaVersion"
}
if (-not (Test-Path -LiteralPath $inputPath)) { throw "Missing learning input: $inputPath" }

Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        & .\mvnw.cmd verify
        if ($LASTEXITCODE -ne 0) { throw "mvnw verify failed with exit code $LASTEXITCODE" }
    }
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing shaded jar: $jar. Run .\mvnw.cmd package first." }

    New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
    $config = @(
        '--input', $inputPath,
        '--window-panes', '2',
        '--pane-size', '2',
        '--weight-batch-size', '3',
        '--min-wus', '0.5',
        '--seed', '42',
        '--max-slides', '1'
    )
    foreach ($algorithm in @('oracle', 'ct', 'dwt')) {
        $output = Join-Path $outputPath "sample-$algorithm.csv"
        Write-Host "Running learning demo: $algorithm"
        & java -jar $jar mine --algorithm $algorithm @config --output $output
        if ($LASTEXITCODE -ne 0) { throw "Learning demo failed for $algorithm with exit code $LASTEXITCODE" }
    }

    function Read-PatternMap([string]$path) {
        $map = @{}
        foreach ($row in (Import-Csv -LiteralPath $path)) {
            $key = "$($row.window)|$($row.pattern)"
            if ($map.ContainsKey($key)) { throw "Duplicate pattern key in $path`: $key" }
            $map[$key] = [double]::Parse($row.wus, [Globalization.CultureInfo]::InvariantCulture)
        }
        return $map
    }

    $oracle = Read-PatternMap (Join-Path $outputPath 'sample-oracle.csv')
    foreach ($algorithm in @('ct', 'dwt')) {
        $actual = Read-PatternMap (Join-Path $outputPath "sample-$algorithm.csv")
        if ($actual.Count -ne $oracle.Count) {
            throw "$algorithm differs from oracle: $($actual.Count) rows vs $($oracle.Count)"
        }
        foreach ($key in $oracle.Keys) {
            if (-not $actual.ContainsKey($key)) { throw "$algorithm is missing oracle pattern: $key" }
            if ([Math]::Abs($actual[$key] - $oracle[$key]) -gt 1e-12) {
                throw "$algorithm has a different wus for ${key}: $($actual[$key]) vs $($oracle[$key])"
            }
        }
    }

    Write-Host "LEARNING DEMO: PASS ($($oracle.Count) pattern rows across 2 windows)" -ForegroundColor Green
    Write-Host "CSV output: $outputPath"
}
finally {
    Pop-Location
}
