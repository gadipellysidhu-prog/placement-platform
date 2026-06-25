<#
.SYNOPSIS
    Verifies the health of the placement platform deployment.

.PARAMETER BaseUrl
    Base URL of the application. Default: http://localhost:8081

.PARAMETER AdminToken
    Admin JWT token for checking protected endpoints. Optional.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\health-check.ps1
    powershell -ExecutionPolicy Bypass -File scripts\health-check.ps1 -BaseUrl https://your-domain.com
#>

param(
    [string]$BaseUrl    = "http://localhost:8081",
    [string]$AdminToken = ""
)

$ErrorActionPreference = "SilentlyContinue"
$PassCount = 0
$FailCount = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [hashtable]$Headers = @{},
        [int]$ExpectedStatus = 200
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Headers $Headers -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "  [PASS] $Name" -ForegroundColor Green
            $script:PassCount++
        } else {
            Write-Host "  [FAIL] $Name — Expected $ExpectedStatus, got $($response.StatusCode)" -ForegroundColor Red
            $script:FailCount++
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -eq $ExpectedStatus) {
            Write-Host "  [PASS] $Name (status $status)" -ForegroundColor Green
            $script:PassCount++
        } else {
            Write-Host "  [FAIL] $Name — $($_.Exception.Message)" -ForegroundColor Red
            $script:FailCount++
        }
    }
}

Write-Host ""
Write-Host "=== Placement Platform Health Check ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Time:     $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host ""

# ── Application Health ────────────────────────────────────────────────────────
Write-Host "[ Application Health ]" -ForegroundColor White
Test-Endpoint "Liveness probe"  "$BaseUrl/actuator/health/liveness"
Test-Endpoint "Readiness probe" "$BaseUrl/actuator/health/readiness"
Test-Endpoint "Health endpoint" "$BaseUrl/actuator/health"

# ── Public endpoints ──────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[ Public Endpoints ]" -ForegroundColor White
Test-Endpoint "Swagger UI"     "$BaseUrl/swagger-ui/index.html"
Test-Endpoint "OpenAPI docs"   "$BaseUrl/v3/api-docs"

# ── Protected endpoints (no auth → 401) ──────────────────────────────────────
Write-Host ""
Write-Host "[ Authentication Enforcement ]" -ForegroundColor White
Test-Endpoint "Protected /api/users/me (no auth → 401)" "$BaseUrl/api/users/me" @{} 401
Test-Endpoint "Actuator metrics (no auth → 401)"        "$BaseUrl/actuator/metrics" @{} 401

# ── Admin endpoints (with token if provided) ──────────────────────────────────
if (-not [string]::IsNullOrEmpty($AdminToken)) {
    Write-Host ""
    Write-Host "[ Admin Endpoints (with token) ]" -ForegroundColor White
    $authHeaders = @{ "Authorization" = "Bearer $AdminToken" }
    Test-Endpoint "Actuator metrics (admin)"    "$BaseUrl/actuator/metrics" $authHeaders
    Test-Endpoint "Actuator prometheus (admin)" "$BaseUrl/actuator/prometheus" $authHeaders
    Test-Endpoint "Actuator info (admin)"       "$BaseUrl/actuator/info" $authHeaders
}

# ── Docker containers ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[ Docker Containers ]" -ForegroundColor White

$containers = @("placement-app", "placement-postgres", "placement-nginx", "placement-clamav")
foreach ($c in $containers) {
    $running = docker ps --filter "name=$c" --filter "status=running" -q 2>$null
    if ($running) {
        Write-Host "  [PASS] Container $c is running" -ForegroundColor Green
        $PassCount++
    } else {
        Write-Host "  [WARN] Container $c is not running (may be optional)" -ForegroundColor Yellow
    }
}

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Results: $PassCount passed, $FailCount failed" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Red" })
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

if ($FailCount -gt 0) {
    Write-Host "Some checks failed. Review logs:" -ForegroundColor Red
    Write-Host "  docker-compose logs -f app"
    exit 1
} else {
    Write-Host "All checks passed. System is healthy." -ForegroundColor Green
    exit 0
}
