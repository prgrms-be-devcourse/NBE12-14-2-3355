param(
    [string]$BaseUrl = 'http://localhost:8080/api/v1/games',
    [string]$OutputPath = 'k6/results/search-before.json',
    [ValidateRange(1, 1000)][int]$Samples = 5,
    [ValidateRange(1, 60000)][int]$MaxMilliseconds = 500,
    [string]$ComparePath,
    [string[]]$Cases = @('suggestions?keyword=a', 'suggestions?keyword=al', 'page?page=0&size=15', 'page?page=100&size=15')
)
$ErrorActionPreference = 'Stop'
$results = @()
$failed = $false
foreach ($case in $cases) {
    $uri = "$BaseUrl/$case"
    $null = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec 30
    $times = @()
    $payload = $null
    for ($i = 0; $i -lt $Samples; $i++) {
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec 30
        $watch.Stop()
        $times += $watch.Elapsed.TotalMilliseconds
        $payload = $response.Content | ConvertFrom-Json
        if ($null -eq $payload.data) { throw "Missing data: $case" }
    }
    $sorted = @($times | Sort-Object)
    $median = [math]::Round($sorted[[int][math]::Floor($sorted.Count / 2)], 1)
    $failed = $failed -or ($median -gt $MaxMilliseconds)
    Write-Output "$case median=${median}ms (budget=${MaxMilliseconds}ms)"
    $results += [pscustomobject]@{ path = $case; medianMs = $median; samplesMs = $times; data = $payload.data }
}
$parent = Split-Path -Parent $OutputPath
if ($parent) { $null = New-Item -ItemType Directory -Force -Path $parent }
$results | ConvertTo-Json -Depth 30 | Set-Content -Encoding UTF8 -LiteralPath $OutputPath
if ($ComparePath) {
    $baseline = Get-Content -LiteralPath $ComparePath -Raw | ConvertFrom-Json
    foreach ($result in $results) {
        $previous = @($baseline | Where-Object { $_.path -eq $result.path })
        if ($previous.Count -ne 1) { throw "Missing or duplicate baseline: $($result.path)" }
        $before = $previous[0].data | ConvertTo-Json -Depth 30 -Compress
        $after = $result.data | ConvertTo-Json -Depth 30 -Compress
        if ($before -cne $after) { throw "Response changed: $($result.path)" }
        Write-Output "$($result.path) response unchanged"
    }
}
if ($failed) { exit 1 }
