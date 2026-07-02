# Deployment Automation Scripts

This folder contains setup scripts to build the ReY Platform projects:

- **[`setup_and_build.ps1`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/deployment/setup_and_build.ps1)**: Automatically links paths, installs npm packages, runs production builds of website assets, and compiles the Android Private OML APK.

## Run Build

To trigger compilation locally:
```powershell
powershell -ExecutionPolicy Bypass -File .\setup_and_build.ps1
```
