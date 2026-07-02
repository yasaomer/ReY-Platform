# ReY Platform Maintenance Guide

This document describes routine maintenance operations: rotating credentials, backup configurations, database optimization, and log cleanup.

## 1. Database Cleanup & Optimization

As logging events accumulate, D1 SQL size increases. To prevent hitting free-tier database limits, run log cleanups:

```bash
# Clear informational logs older than 30 days
npx wrangler d1 execute rey_db --command="DELETE FROM system_logs WHERE timestamp < date('now', '-30 days') AND severity = 'INFO'"

# Shrink sqlite space
npx wrangler d1 execute rey_db --command="VACUUM;"
```

## 2. Configuration Backups & Restores

### Configuration Export
The owner's database stores sensitive configurations (API keys, recovery numbers). The APK allows exporting configurations:
1. Tap **Backup** in APK options.
2. The app compiles configurations keys into a JSON object, encrypts it using the App Password with AES-256-CBC, and uploads it to `Google Drive -> Private OML -> Backups`.

### Configuration Import
To restore configurations on a new device:
1. Tap **Restore Settings** in the Setup Wizard.
2. Select the latest backup file from Google Drive.
3. Input the previous App Password to decrypt and re-seed the local DB.
