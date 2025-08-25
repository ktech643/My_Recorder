#!/bin/bash

# Fix CrashLogger.java
sed -i 's/logExecutor.execute(() -> writeLog(log));/log("ANR", tag, message, null);/' /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java

# Remove the formatLog line
sed -i '/String log = formatLog("ANR", tag, message);/d' /workspace/app/src/main/java/com/checkmate/android/util/CrashLogger.java

echo "Fixed CrashLogger"