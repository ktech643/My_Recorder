with open('app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java', 'r') as f:
    lines = f.readlines()

# Find getInstance method and add initialize before it
new_lines = []
for i, line in enumerate(lines):
    if 'public static DynamicSettingsManager getInstance(Context context)' in line:
        # Add initialize method before getInstance
        new_lines.append('    /**\n')
        new_lines.append('     * Initialize the DynamicSettingsManager\n')
        new_lines.append('     * Should be called from Application.onCreate()\n')
        new_lines.append('     */\n')
        new_lines.append('    public static void initialize(Context context) {\n')
        new_lines.append('        getInstance(context);\n')
        new_lines.append('    }\n')
        new_lines.append('    \n')
    new_lines.append(line)

with open('app/src/main/java/com/checkmate/android/util/DynamicSettingsManager.java', 'w') as f:
    f.writelines(new_lines)

print("Added initialize method")
