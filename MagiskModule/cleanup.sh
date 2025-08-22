cd CheckMateModule

# List of files to process (replace with actual paths)
FILES=(

"META-INF/com//google/android/updater-script"
"META-INF/com/google/android/update-binary"

"system/addon.d/51-com.checkmate.android.sh"
"system/app/CheckMateXposed/CheckMateXposed.apk",

"system/priv-app/CheckMateApp/CheckMateApp.apk",

"system/bin/lspcli",
"system/etc/init/checkmate.rc",

"system/etc/selinux/checkmate.te",
"system/etc/sysconfig/config-com.checkmate.android.xml",
"system/etc/surfaceflinger_feature.conf",
"system/etc/audio/audio_policy_configuration.xml",
"system/etc/permissions/privapp-permissions-com.checkmate.android.xml",

"customize.sh"
"post-fs-data.sh"
"service.sh"
"sepolicy.rule"
"module.prop"
"post-fs-data.sh"
"sepolicy.rule"
"service.sh"
)

# Convert all files to LF + ASCII
for file in "${FILES[@]}"; do
echo "Processing $file..."

# Remove non-printable ASCII characters
tr -cd '\11\12\15\40-\176' < "$file" > temp.txt
mv temp.txt "$file"

# Force LF line endings
dos2unix "$file"

# Verify final format
echo "=== Verification ==="
file "$file"
cat -v "$file" | grep -q '\^M' || echo "- Clean LF endings"
LC_ALL=C grep --color='auto' -n "[^ -~]" "$file" && echo "- WARNING: Non-ASCII detected"
done
