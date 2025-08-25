with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'r') as f:
    content = f.read()

# Remove the problematic verifyStoragePermissions call
old_block = """        new Handler().postDelayed(() -> {
            verifyStoragePermissions(activity);
        }, 400);"""

new_block = """        // Storage permissions are handled separately in continueWithActivation
        // Removed verifyStoragePermissions to prevent infinite loop"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'w') as f:
    f.write(content)

print('Fixed')
