#!/bin/bash

# CheckMate Android Build and Test Script
# This script performs a complete build and test cycle

echo "================================================"
echo "CheckMate Android Build and Test Script"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check Android SDK
check_android_sdk() {
    echo -e "${YELLOW}Checking Android SDK...${NC}"
    
    if [ -z "$ANDROID_HOME" ] && [ ! -f "local.properties" ]; then
        echo -e "${RED}ERROR: Android SDK not found!${NC}"
        echo "Please set ANDROID_HOME environment variable or create local.properties file"
        exit 1
    fi
    
    echo -e "${GREEN}Android SDK found${NC}"
}

# Function to clean build
clean_build() {
    echo -e "${YELLOW}Cleaning previous build...${NC}"
    ./gradlew clean
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Clean successful${NC}"
    else
        echo -e "${RED}Clean failed${NC}"
        exit 1
    fi
}

# Function to run lint checks
run_lint() {
    echo -e "${YELLOW}Running lint checks...${NC}"
    ./gradlew lint
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Lint checks passed${NC}"
    else
        echo -e "${YELLOW}Lint warnings found (see app/build/reports/lint-results.html)${NC}"
    fi
}

# Function to build debug APK
build_debug() {
    echo -e "${YELLOW}Building debug APK...${NC}"
    ./gradlew assembleDebug --stacktrace
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Debug build successful${NC}"
        echo "APK location: app/build/outputs/apk/debug/"
    else
        echo -e "${RED}Debug build failed${NC}"
        exit 1
    fi
}

# Function to run unit tests
run_tests() {
    echo -e "${YELLOW}Running unit tests...${NC}"
    ./gradlew test
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}All tests passed${NC}"
    else
        echo -e "${RED}Some tests failed${NC}"
        echo "Test reports: app/build/reports/tests/"
    fi
}

# Function to check for common issues
check_common_issues() {
    echo -e "${YELLOW}Checking for common issues...${NC}"
    
    # Check for TODOs
    TODO_COUNT=$(grep -r "TODO\|FIXME\|XXX" app/src/main/java/ --include="*.java" | wc -l)
    if [ $TODO_COUNT -gt 0 ]; then
        echo -e "${YELLOW}Found $TODO_COUNT TODO/FIXME comments${NC}"
    fi
    
    # Check for duplicate imports
    echo "Checking for duplicate imports..."
    find app/src/main/java -name "*.java" -exec grep -l "^import.*;" {} \; | while read file; do
        DUPLICATES=$(grep "^import" "$file" | sort | uniq -d)
        if [ ! -z "$DUPLICATES" ]; then
            echo -e "${YELLOW}Duplicate imports in $file${NC}"
        fi
    done
    
    echo -e "${GREEN}Common issues check complete${NC}"
}

# Main execution
main() {
    echo "Starting build process at $(date)"
    echo ""
    
    check_android_sdk
    clean_build
    check_common_issues
    run_lint
    build_debug
    run_tests
    
    echo ""
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}Build and test cycle completed successfully!${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo "Completed at $(date)"
}

# Run main function
main