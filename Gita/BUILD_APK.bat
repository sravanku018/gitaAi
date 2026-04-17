@echo off
REM Build Release APK for Gita Learning App
REM This script will:
REM 1. Clean previous builds
REM 2. Build release APK with ProGuard enabled and resource shrinking
REM 3. Display the output APK location

echo.
echo ====================================
echo Gita Learning App - APK Builder
echo ====================================
echo.
echo Starting release build with:
echo - ProGuard minification: ENABLED
echo - Resource shrinking: ENABLED
echo - Line number preservation: YES
echo.

REM Clean before build
echo Cleaning previous builds...
call gradlew.bat clean

REM Build release APK
echo.
echo Building release APK (this may take 2-5 minutes)...
call gradlew.bat assembleRelease

REM Check if build succeeded
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================
    echo BUILD SUCCESSFUL!
    echo ====================================
    echo.
    echo Your APK files are located at:
    echo   %CD%\app\build\outputs\apk\release\
    echo.
    echo Main APK: app-release.apk
    echo.
    echo What to do next:
    echo 1. Sign the APK with your keystore
    echo 2. Test on physical device or emulator
    echo 3. Upload to Google Play Store or distribute
    echo.
) else (
    echo.
    echo ====================================
    echo BUILD FAILED!
    echo ====================================
    echo.
    echo Please check the error messages above.
    echo Common issues:
    echo - Android SDK not installed
    echo - Java not in PATH
    echo - Insufficient disk space
    echo.
)

pause
