<#
.SYNOPSIS
    Monitors the placement platform application metrics.

.PARAMETER BaseUrl
    Base URL of the application. Default: http://localhost:8081

.PARAMETER AdminToken
    Admin JWT token for accessing protected metrics endpoints. Required.

.PARAMETER IntervalSeconds
    Polling interval in seconds. Default: 30

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\monitor.ps1 -AdminToken "eyJ..."
#>

param(
    [string]$BaseUrl         = "http://localhost:8081",
    [Parameter(Mandatory = $true)]
    [string]$AdminToken,
    [int]$IntervalSeconds    = 30
)

$ErrorActionPreference = "SilentlyContinue"

function Get-Metric {
    param([string]$MetricName)
    try {
        $r = Invoke-RestMethod "$BaseUrl/actuator/metrics/$MetricName" `
            -Headers @{ "Authorization" = "Bearer $AdminToken" } -TimeoutSec 5
        return $r.measurements[0].value
    } catch {
        return "N/A"
    }
}

function Format-Bytes([double]$bytes) {
    if ($bytes -ge 1GB) { return "$([Math]::Round($bytes / 1GB, 1)) GB" }
    if ($bytes -ge 1MB) { return "$([Math]::Round($bytes / 1MB, 1)) MB" }
    if ($bytes -ge 1KB) { return "$([Math]::Round($bytes / 1KB, 1)) KB" }
    return "$bytes B"
}

Write-Host ""
Write-Host "=== Placement Platform Monitor ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl | Interval: ${IntervalSeconds}s | Press Ctrl+C to stop"
Write-Host ""

while ($true) {
    $timestamp = Get-Date -Format "HH:mm:ss"

    # Collect metrics
    $heapUsed   = Get-Metric "jvm.memory.used?tag=area:heap"
    $heapMax    = Get-Metric "jvm.memory.max?tag=area:heap"
    $threads    = Get-Metric "jvm.threads.live"
    $httpActive = Get-Metric "tomcat.threads.busy?tag=name:http-nio-8080"
    $dbActive   = Get-Metric "hikaricp.connections.active"
    $dbPool     = Get-Metric "hikaricp.connections"
    $uptime     = Get-Metric "process.uptime"

    $heapPct = if ($heapMax -ne "N/A" -and $heapMax -gt 0) {
        "$([Math]::Round(($heapUsed / $heapMax) * 100, 1))%"
    } else { "N/A" }

    $uptimeStr = if ($uptime -ne "N/A") {
        $ts = [TimeSpan]::FromSeconds($uptime)
        "$($ts.Hours)h $($ts.Minutes)m $($ts.Seconds)s"
    } else { "N/A" }

    Clear-Host
    Write-Host "=== Placement Platform Monitor — $timestamp ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  JVM Heap:     $(Format-Bytes $heapUsed) / $(Format-Bytes $heapMax) ($heapPct)" -ForegroundColor $(if ($heapPct -ne 'N/A' -and [double]$heapPct.TrimEnd('%') -gt 80) {"Red"} else {"White"})
    Write-Host "  Threads:      $threads live"
    Write-Host "  HTTP active:  $httpActive threads"
    Write-Host "  DB pool:      $dbActive / $dbPool connections"
    Write-Host "  Uptime:       $uptimeStr"
    Write-Host ""

    # Health status
    try {
        $health = Invoke-RestMethod "$BaseUrl/actuator/health" `
            -Headers @{ "Authorization" = "Bearer $AdminToken" } -TimeoutSec 5
        $status = $health.status
        $color  = if ($status -eq "UP") { "Green" } else { "Red" }
        Write-Host "  Health:       $status" -ForegroundColor $color

        if ($health.components) {
            foreach ($component in $health.components.PSObject.Properties) {
                $cStatus = $component.Value.status
                $cColor  = if ($cStatus -eq "UP") { "Green" } else { "Red" }
                Write-Host "    - $($component.Name): $cStatus" -ForegroundColor $cColor
            }
        }
    } catch {
        Write-Host "  Health:       UNREACHABLE" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "  [Refreshing every ${IntervalSeconds}s — Press Ctrl+C to stop]" -ForegroundColor DarkGray

    Start-Sleep -Seconds $IntervalSeconds
}
