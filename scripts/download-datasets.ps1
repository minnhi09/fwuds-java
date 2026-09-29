[CmdletBinding()]
param(
    [string[]]$Name = @('Retail', 'Accidents', 'BMS-POS', 'Kosarak', 'Susy'),
    [string]$Destination = (Join-Path $PSScriptRoot '..\data\raw')
)

$ErrorActionPreference = 'Stop'
$destinationPath = [System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Force -Path $destinationPath | Out-Null

$datasets = @{
    'Retail' = @{ Url = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/retail.txt'; File = 'retail.dat'; Kind = 'raw' }
    'Accidents' = @{ Url = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/accidents.txt'; File = 'accidents.dat'; Kind = 'raw' }
    'BMS-POS' = @{ Url = 'https://storage.googleapis.com/set-similarity-search/BMS-POS_dup_dr.inp.gz'; File = 'bms-pos.dat'; Kind = 'set-gz' }
    'Kosarak' = @{ Url = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/kosarak.dat'; File = 'kosarak.dat'; Kind = 'raw' }
    'Susy' = @{ Url = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/zhangzhongjie/Susy.zip'; File = 'susy.txt'; Kind = 'zip' }
}

foreach ($datasetName in $Name) {
    if (-not $datasets.ContainsKey($datasetName)) { throw "Unknown dataset: $datasetName" }
    $dataset = $datasets[$datasetName]
    $target = Join-Path $destinationPath $dataset.File
    $checksumFile = "$target.sha256"
    if (Test-Path -LiteralPath $target) {
        $actual = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
        if (Test-Path -LiteralPath $checksumFile) {
            $expected = (Get-Content -LiteralPath $checksumFile -Raw).Trim().ToLowerInvariant()
            if ($actual -ne $expected) { throw "Checksum mismatch for existing file $target" }
        } else {
            Set-Content -LiteralPath $checksumFile -Value $actual -Encoding ascii
        }
        Write-Host "$datasetName already present: $target"
        continue
    }

    $archive = Join-Path ([System.IO.Path]::GetTempPath()) ("fwuds-" + [guid]::NewGuid().ToString('N'))
    try {
        Write-Host "Downloading $datasetName..."
        Invoke-WebRequest -Uri $dataset.Url -OutFile $archive
        if ($dataset.Kind -eq 'raw') {
            Move-Item -LiteralPath $archive -Destination $target
        } elseif ($dataset.Kind -eq 'set-gz') {
            $input = [System.IO.File]::OpenRead($archive)
            try {
                $gzip = [System.IO.Compression.GZipStream]::new($input, [System.IO.Compression.CompressionMode]::Decompress)
                try {
                    $reader = [System.IO.StreamReader]::new($gzip)
                    $writer = [System.IO.StreamWriter]::new($target, $false, [System.Text.UTF8Encoding]::new($false))
                    try {
                        $null = $reader.ReadLine() # Mirror metadata: number of sets and tokens.
                        while (($line = $reader.ReadLine()) -ne $null) {
                            $tab = $line.IndexOf("`t")
                            if ($tab -lt 0) { throw "Invalid BMS-POS mirror row: $line" }
                            $normalized = ($line.Substring($tab + 1).Split(',') | ForEach-Object { ([int]$_ + 1).ToString() }) -join ' '
                            $writer.WriteLine($normalized)
                        }
                    } finally {
                        $writer.Dispose()
                        $reader.Dispose()
                    }
                } finally { $gzip.Dispose() }
            } finally { $input.Dispose() }
        } elseif ($dataset.Kind -eq 'zip') {
            $extractDir = Join-Path ([System.IO.Path]::GetTempPath()) ("fwuds-unzip-" + [guid]::NewGuid().ToString('N'))
            try {
                Expand-Archive -LiteralPath $archive -DestinationPath $extractDir
                $source = Get-ChildItem -LiteralPath $extractDir -File -Recurse | Where-Object { $_.Name -ieq 'Susy.txt' } | Select-Object -First 1
                if ($null -eq $source) { throw 'Susy.txt was not found in the downloaded archive' }
                Copy-Item -LiteralPath $source.FullName -Destination $target
            } finally {
                if (Test-Path -LiteralPath $extractDir) { Remove-Item -LiteralPath $extractDir -Recurse -Force }
            }
        } else { throw "Unsupported archive kind: $($dataset.Kind)" }
        $actual = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
        Set-Content -LiteralPath $checksumFile -Value $actual -Encoding ascii
        Write-Host "$datasetName ready: $target (sha256=$actual)"
    } finally {
        if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive -Force }
    }
}

Write-Host 'Run java -jar target/fwuds-java-1.0.0-SNAPSHOT-all.jar datasets verify to validate counts.'
