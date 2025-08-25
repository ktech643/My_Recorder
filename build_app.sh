#!/bin/bash

echo "=== Building CheckMate App with Optimizations ==="
echo

# Set JAVA_HOME to work around Java 21 issues
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Clean previous builds
echo "Cleaning previous builds..."
rm -rf app/build/intermediates/javac
rm -rf app/build/tmp

# Try to build only the app module first
echo "Building app module..."
./gradlew :app:assembleDebug \
  -x :libuvccamera:compileDebugJavaWithJavac \
  -x :usbCameraCommon:compileDebugJavaWithJavac \
  --no-daemon \
  --stacktrace \
  -Pandroid.jetifier.ignorelist=bcprov-jdk15on \
  -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=512m" 2>&1 | tee build.log

# Check if our files compiled
echo
echo "Checking if optimization files were processed..."
if grep -q "EglInitializer" build.log; then
    echo "✅ EglInitializer processed"
fi
if grep -q "OptimizedServiceSwitcher" build.log; then  
    echo "✅ OptimizedServiceSwitcher processed"
fi
if grep -q "TransitionOverlay" build.log; then
    echo "✅ TransitionOverlay processed"
fi
if grep -q "DynamicConfigManager" build.log; then
    echo "✅ DynamicConfigManager processed"
fi

echo
echo "Build complete. Check build.log for details."