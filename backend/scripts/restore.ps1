<#
.SYNOPSIS
    Restores a PostgreSQL database from a backup SQL file.

.PARAMETER BackupFile
    Path to the SQL backup file (required).

.PARAMETER ContainerName
    Name of the PostgreSQL Docker container. Default: placement-postgres

.PARAMETER DbName
    Database name to restore into. Default: placement_prod

.PARAMETER DbUser
    Database user. Default: placement_user

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\restore.ps1 -BackupFile "backups\placement_backup_20250601_120000.sql"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [string]$ContainerName = "placement-postgres",
    [string]$DbName        = "placement_prod",
    [string]$DbUser        = "placement_user"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== PostgreSQL Restore ===" -ForegroundColor Cyan
Write-Host "Container  : $ContainerName"
Write-Host "Database   : $DbName"
Write-Host "Backup file: $BackupFile"
Write-Host ""

# Validate backup file exists
if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file not found: $BackupFile"
}

$BackupFile = Resolve-Path $BackupFile

# Verify container is running
$containerRunning = docker ps --filter "name=$ContainerName" --filter "status=running" -q
if ([string]::IsNullOrEmpty($containerRunning)) {
    Write-Error "Container '$ContainerName' is not running. Start the Docker stack first."
}

# Confirm with user
Write-Host "WARNING: This will DROP and recreate the '$DbName' database." -ForegroundColor Red
Write-Host "All existing data will be PERMANENTLY DELETED." -ForegroundColor Red
Write-Host ""
$confirm = Read-Host "Type 'yes' to confirm restore"
if ($confirm -ne "yes") {
    Write-Host "Restore cancelled." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Step 1: Stopping the application container..." -ForegroundColor Yellow
docker stop placement-app -ErrorAction SilentlyContinue | Out-Null

Write-Host "Step 2: Dropping and recreating database..." -ForegroundColor Yellow
docker exec $ContainerName psql -U $DbUser -c "DROP DATABASE IF EXISTS $DbName;" postgres 2>&1 | Out-Null
docker exec $ContainerName psql -U $DbUser -c "CREATE DATABASE $DbName;" postgres 2>&1 | Out-Null

Write-Host "Step 3: Restoring from backup..." -ForegroundColor Yellow
Get-Content $BackupFile | docker exec -i $ContainerName psql -U $DbUser -d $DbName

if ($LASTEXITCODE -ne 0) {
    Write-Error "Restore failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "=== Restore Complete ===" -ForegroundColor Green
Write-Host "Database '$DbName' has been restored from:" -ForegroundColor White
Write-Host "  $BackupFile"
Write-Host ""
Write-Host "Step 4: Restart the application:" -ForegroundColor Cyan
Write-Host "  docker start placement-app"
Write-Host "  # or:"
Write-Host "  docker-compose -f docker-compose.prod.yml up -d app"
