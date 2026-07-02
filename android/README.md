# Private OML Android Application (Administrator)

This directory contains the administrator controller application built using Jetpack Compose and Material 3.

## Folder Organization

- **[`/app/src/main/AndroidManifest.xml`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/android/app/src/main/AndroidManifest.xml)**: Application permissions (SMS, GPS, Internet, Storage) and background worker service registration.
- **[`/app/src/main/java/com/example/privateoml/data`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/android/app/src/main/java/com/example/privateoml/data)**: Local configuration repository and SQLite caching logic.
- **[`/app/src/main/java/com/example/privateoml/services`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/android/app/src/main/java/com/example/privateoml/services)**: Telemetry coordinates tracking and background sync loops worker.
- **[`/app/src/main/java/com/example/privateoml/ui`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/android/app/src/main/java/com/example/privateoml/ui)**: Jetpack Compose layouts (lock dialog, multi-step wizard setup, edit last message dashboard, config chip selectors).

## Compilation

Build the application debug APK:
```bash
./gradlew assembleDebug
```

Refer to the global **[`/documentation/USER_GUIDE.md`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/documentation/USER_GUIDE.md)** for instructions on installing the app.
