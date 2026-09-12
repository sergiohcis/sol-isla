# IIS deployment

Same shape as SweetHome's deployment on the same shared Windows Server (`192.168.1.74`), same
credentials, folder renamed `sweethome` -> `sol-isla` and a distinct backend port (6003, not
SweetHome's 6002 — they run on the same box and must not collide).

Single IIS site, `sol-isla.hosanna-solutions.com`. Angular is served as static files; `/api/*`
is reverse-proxied (ARR + URL Rewrite, `frontend-web.config`) to the Spring Boot backend, which
runs as a Windows Service (WinSW, `backend-service.xml.template`) on local port 6003 — not
exposed externally, only reachable from IIS on the same box.

| | Path |
|---|---|
| Frontend site root | `C:\inetpub\wwwroot\sol-isla\frontend` (contains `web.config`, copied from `frontend-web.config`) |
| Backend service | `C:\inetpub\wwwroot\sol-isla\backend` (jar + WinSW exe/xml, copied from `backend-service.xml.template` with the real `DB_PASSWORD` and WhatsApp credentials filled in) |
| Database | `sol_isla_prod` on the shared Postgres instance — created once (`CREATE DATABASE sol_isla_prod;`), migrated automatically by Flyway on backend startup |

TLS certificate is issued and bound automatically by win-acme (`C:\Program Files\win-acme\wacs.exe --renew`), which already manages certs for the other sites on this box — a new site
with an http/80 binding gets picked up and given an https/443 binding on the next renew run.
No manual certificate handling needed.

Deploy steps:
1. `mvn clean package` (backend), `ng build` (frontend).
2. Copy the jar to the backend path, the `dist/frontend/browser` contents to the frontend
   path, `frontend-web.config` to `frontend\web.config`.
3. First deploy only: copy a WinSW executable + `backend-service.xml.template` (filled in) to
   the backend path as `SolIslaBackend.exe` / `SolIslaBackend.xml`, then
   `SolIslaBackend.exe install` and `start`. Subsequent deploys just replace the jar and
   restart the service.
4. First deploy only: run the backend once with `SPRING_PROFILES_ACTIVE=bootstrap` (see
   `PlatformBootstrapRunner`) to create the admin account, then go back to running without
   that profile.

**Before the first real deploy**, confirm port 6003 is actually free on `192.168.1.74` (check
what else is running there beyond SweetHome's 6002) and that DNS for
`sol-isla.hosanna-solutions.com` points at this server.

## Database backups

`infrastructure/db/backup-postgres.ps1` dumps `sol_isla_prod` (compressed, via `pg_dump -Fc`)
to a timestamped file and prunes anything older than 14 days (configurable). It's a standalone
script — not wired into the app — meant to run daily via Windows Task Scheduler on
`192.168.1.74`:

1. Copy `backup-postgres.ps1` to the server (e.g. `C:\inetpub\wwwroot\sol-isla\backup\backup-postgres.ps1`).
2. Register the scheduled task (adjust the path and `DB_PASSWORD`):
   ```powershell
   $action = New-ScheduledTaskAction -Execute "powershell.exe" `
       -Argument '-NoProfile -Command "$env:PGPASSWORD=''__FILL_IN_AT_DEPLOY_TIME__''; & \"C:\inetpub\wwwroot\sol-isla\backup\backup-postgres.ps1\""'
   $trigger = New-ScheduledTaskTrigger -Daily -At 3am
   Register-ScheduledTask -TaskName "SolIslaPostgresBackup" -Action $action -Trigger $trigger -RunLevel Highest
   ```
3. Backups land in `C:\inetpub\wwwroot\sol-isla\backup\backups\`. **This script does not copy
   them off-box** — that's a separate, manual (or separately automated) step; a backup that
   lives only on the same server it protects doesn't survive that server failing.

To restore: `pg_restore --host=localhost --port=5433 --username=postgres --dbname=sol_isla_prod --clean <file>.dump`.
