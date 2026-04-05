#!/bin/bash
# IPTV Pro Android - Build Script

set -e

echo "=========================================="
echo "   IPTV Pro Android - Build"
echo "=========================================="

# Check if running in Docker
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Please run from project root: /home/reyan/Projects/iptv-pro-android"
    exit 1
fi

cd app

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "📥 Downloading Gradle wrapper..."
    gradle wrapper --gradle-version 8.2
fi

# Make gradlew executable
chmod +x gradlew

echo "🔨 Building release APK..."
./gradlew assembleRelease --no-daemon --offline 2>/dev/null || ./gradlew assembleRelease --no-daemon

# Check if build succeeded
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    
    # Get APK size
    APK_SIZE=$(stat -c%s "$APK_PATH")
    APK_SIZE_MB=$(echo "scale=2; $APK_SIZE / 1024 / 1024" | bc)
    
    echo "📦 APK Info:"
    echo "   Path: $APK_PATH"
    echo "   Size: ${APK_SIZE} bytes (${APK_SIZE_MB} MB)"
    echo ""
    
    # Copy to output
    cp "$APK_PATH" /app/iptv-pro-release.apk
    echo "📱 Output: /app/iptv-pro-release.apk"
    echo ""
    
    if [ $APK_SIZE -lt 5242880 ]; then
        echo "✅ Size check passed (under 5MB)"
    else
        echo "⚠️  Size is over 5MB target: ${APK_SIZE_MB} MB"
    fi
else
    echo "❌ Build failed - APK not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "   Build Complete!"
echo "=========================================="
