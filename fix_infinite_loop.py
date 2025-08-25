# Read the file
with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'r') as f:
    lines = f.readlines()

# Find and remove the problematic verifyStoragePermissions call
new_lines = []
skip_next_two = 0

for i, line in enumerate(lines):
    if skip_next_two > 0:
        skip_next_two -= 1
        continue
    
    if 'verifyStoragePermissions(activity);' in line:
        # Replace the three-line block with a comment
        new_lines.append('        // Storage permissions are handled separately in continueWithActivation\n')
        new_lines.append('        // Removed verifyStoragePermissions to prevent infinite loop\n')
        skip_next_two = 2  # Skip the closing brace and delay line
        continue
    
    new_lines.append(line)

# Write the fixed content
with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'w') as f:
    f.writelines(new_lines)

print('Fixed infinite loop by removing verifyStoragePermissions call')
