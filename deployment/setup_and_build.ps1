# ReY Platform Setup and Compilation Script
# Enforces Node path overrides and executes setups non-interactively

$NODE_PATH = "C:\Users\yasao\.gemini\antigravity\scratch\node\node-v20.11.1-win-x64"
$JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"

# Inject Paths
$env:Path = "$NODE_PATH;$env:Path"
$env:JAVA_HOME = $JAVA_HOME
$env:Path = "$JAVA_HOME\bin;$env:Path"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "       ReY Platform Build Automation     " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Setup Backend
Write-Host "`n[Step 1] Configuring Coordinator Backend..." -ForegroundColor Green
cd "$PSScriptRoot\..\backend"
npm install
Write-Host "Backend dependencies installed successfully." -ForegroundColor Yellow

# 2. Setup Website
Write-Host "`n[Step 2] Compiling Viewer Website..." -ForegroundColor Green
cd "$PSScriptRoot\..\website"
npm install
npm run build
Write-Host "Website compiled to static dist/ assets folder." -ForegroundColor Yellow

# 3. Setup Android App
Write-Host "`n[Step 3] Bootstrapping Private OML Admin APK..." -ForegroundColor Green
cd "$PSScriptRoot\..\android"
./gradlew assembleDebug
Write-Host "Android APK assembled to: android/app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Yellow

Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host "  Setup completed. System ready to deploy." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
