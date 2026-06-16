#!/bin/bash

# Build Release APK for Gita Learning App
# This script will:
# 1. Clean previous builds
# 2. Build release APK with ProGuard enabled and resource shrinking
# 3. Display the output APK location

echo ""
echo "===================================="
echo "Gita Learning App - APK Builder"
echo "===================================="
echo ""
echo "Starting release build with:"
echo "- ProGuard minification: ENABLED"
echo "- Resource shrinking: ENABLED"
echo "- Line number preservation: YES"
echo ""

# Clean before build
echo "Cleaning previous builds..."
./gradlew clean

# Build release APK
echo ""
echo "Building release APK (this may take 2-5 minutes)..."
./gradlew assembleRelease

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo ""
    echo "===================================="
    echo "BUILD SUCCESSFUL!"
    echo "===================================="
    echo ""
    echo "Your APK files are located at:"
    echo "  app/build/outputs/apk/release/"
    echo ""
    echo "Main APK: app-release.apk"
    echo ""
    echo "What to do next:"
    echo "1. Sign the APK with your keystore"
    echo "2. Test on physical device or emulator"
    echo "3. Upload to Google Play Store or distribute"
    echo ""
else
    echo ""
    echo "===================================="
    echo "BUILD FAILED!"
    echo "===================================="
    echo ""
    echo "Please check the error messages above."
    echo "Common issues:"
    echo "- Android SDK not installed"
    echo "- Java not in PATH"
    echo "- Insufficient disk space"
    echo ""
fi
