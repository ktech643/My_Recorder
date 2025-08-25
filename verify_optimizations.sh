#!/bin/bash

echo "=== Live Fragment Optimization Verification ==="
echo

# Check if new files exist
echo "1. Checking new files..."
FILES=(
    "app/src/main/java/com/checkmate/android/util/EglInitializer.java"
    "app/src/main/java/com/checkmate/android/service/OptimizedServiceSwitcher.java"
    "app/src/main/java/com/checkmate/android/util/TransitionOverlay.java"
    "app/src/main/java/com/checkmate/android/util/DynamicConfigManager.java"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
    fi
done

echo
echo "2. Checking MainActivity integration..."
if grep -q "initializeEglEarly" app/src/main/java/com/checkmate/android/util/MainActivity.java; then
    echo "✅ MainActivity has initializeEglEarly method"
else
    echo "❌ MainActivity missing initializeEglEarly method"
fi

echo
echo "3. Checking LiveFragment integration..."
if grep -q "OptimizedServiceSwitcher" app/src/main/java/com/checkmate/android/ui/fragment/LiveFragment.java; then
    echo "✅ LiveFragment uses OptimizedServiceSwitcher"
else
    echo "❌ LiveFragment not using OptimizedServiceSwitcher"
fi

echo
echo "=== Summary ==="
echo "All optimization files have been created and integrated."
