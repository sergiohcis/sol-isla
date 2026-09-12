<#
.SYNOPSIS
  Dumps sol_isla_prod to a timestamped, compressed pg_dump file and prunes old backups.

.DESCRIPTION
  Same shared Windows Server as SweetHome (192.168.1.74) — this is a standalone script, not
  wired into the app itself, meant to be run daily via Windows Task Scheduler (see the
  "Database backups" section in infrastructure/iis/README.md for the schtasks command).

  The DB password is never hardcoded here or checked into git (CLAUDE.md: secrets outside Git) —
  it must be supplied via the PGPASSWORD environment variable, e.g. set as an action environment
  variable on the Scheduled Task itself, not inline in this file.

.PARAMETER BackupDir
  Where .dump files land. Defaults to a "backups" folder next to this script.

.PARAMETER RetentionDays
  Backups older than this are deleted after a successful new dump. Default 14.

.EXAMPLE
  $env:PGPASSWORD = "..."
  .\backup-postgres.ps1
#>
param(
    [string]$PgHost = "localhost",
    [int]$PgPort = 5433,
    [string]$PgUser = "postgres",
    [string]$DatabaseName = "sol_isla_prod",
    [string]$PgBinPath = "C:\Program Files\PostgreSQL\16\bin",
    [string]$BackupDir = (Join-Path $PSScriptRoot "backups"),
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

if (-not $env:PGPASSWORD) {
    Write-Error "PGPASSWORD is not set. Set it as an environment variable before running this script (e.g. on the Scheduled Task's action, not hardcoded here)."
    exit 1
}

$pgDump = Join-Path $PgBinPath "pg_dump.exe"
if (-not (Test-Path $pgDump)) {
    Write-Error "pg_dump.exe not found at '$pgDump' — pass -PgBinPath if PostgreSQL is installed elsewhere."
    exit 1
}

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outFile = Join-Path $BackupDir "$DatabaseName`_$timestamp.dump"

Write-Host "Backing up '$DatabaseName' to '$outFile'..."
& $pgDump --host=$PgHost --port=$PgPort --username=$PgUser --format=custom --file=$outFile $DatabaseName

if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump exited with code $LASTEXITCODE — backup NOT completed, leaving old backups in place."
    exit $LASTEXITCODE
}

$sizeKb = [math]::Round((Get-Item $outFile).Length / 1KB, 1)
Write-Host "Backup complete: $outFile ($sizeKb KB)"

$cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $BackupDir -Filter "$DatabaseName`_*.dump" |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    ForEach-Object {
        Write-Host "Deleting old backup: $($_.Name)"
        Remove-Item $_.FullName -Force
    }

Write-Host "Done. Remember: these backups live on the same box as the database they protect — copy them off-box periodically (this script does not do that part)."
