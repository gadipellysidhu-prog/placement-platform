<#
.SYNOPSIS
    Backs up the PostgreSQL database to a timestamped SQL file.

.PARAMETER ContainerName
    Name of the PostgreSQL Docker container. Default: placement-postgres

.PARAMETER DbName
    Database name. Default: placement_prod

.PARAMETER DbUser
    Database user. Default: placement_user

.PARAMETER OutputDir
    Directory for backup files. Default: backups\ relative to script directory.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\backup.ps1
    powershell -ExecutionPolicy Bypass -File scripts\backup.ps1 -DbName placement_dev
#>

param(
    [string]$ContainerName = "placement-postgres",
    [string]$DbName        = "placement_prod",
    [string]$DbUser        = "placement_user",
    [string]$OutputDir     = ""
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
if ([string]::IsNullOrEmpty($OutputDir)) {
    $OutputDir = Join-Path (Split-Path -Parent $ScriptDir) "backups"
}

New-Item -ItemType Directory -Force $OutputDir | Out-Null

$Timestamp  = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = Join-Path $OutputDir "placement_backup_$Timestamp.sql"
$GzipFile   = "$BackupFile.gz"

Write-Host ""
Write-Host "=== PostgreSQL Backup ===" -ForegroundColor Cyan
Write-Host "Container : $ContainerName"
Write-Host "Database  : $DbName"
Write-Host "Output    : $BackupFile"
Write-Host ""

# Verify container is running
$containerRunning = docker ps --filter "name=$ContainerName" --filter "status=running" -q
if ([string]::IsNullOrEmpty($containerRunning)) {
    Write-Error "Container '$ContainerName' is not running. Start the Docker stack first."
}

Write-Host "Running pg_dump..." -ForegroundColor Yellow

docker exec $ContainerName pg_dump `
    --username=$DbUser `
    --dbname=$DbName `
    --no-password `
    --verbose `
    --format=plain `
    --create | Out-File -Encoding utf8 $BackupFile

if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump failed with exit code $LASTEXITCODE"
}

$FileSize = (Get-Item $BackupFile).Length
Write-Host ""
Write-Host "Backup complete." -ForegroundColor Green
Write-Host "File: $BackupFile ($([Math]::Round($FileSize / 1KB, 1)) KB)"

# Verify the backup is non-empty and contains expected content
$FirstLine = Get-Content $BackupFile -TotalCount 1
if (-not $FirstLine.StartsWith("--")) {
    Write-Warning "Backup file may be invalid — first line: $FirstLine"
} else {
    Write-Host "Backup verified: valid PostgreSQL dump format." -ForegroundColor Green
}

Write-Host ""
Write-Host "To restore this backup:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\restore.ps1 -BackupFile ""$BackupFile"""
