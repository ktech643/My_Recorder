# post-fs-data.sh
#!/system/bin/sh

MODDIR=${0%/*}
LSPD_DB="/data/adb/lspd/config/modules.db"

# Wait for LSPosed initialization
until [ -f "$LSPD_DB" ]; do
    sleep 5
done

MODDIR=${0%/*}

# Create symlinks
ln -sf $MODDIR/system/bin/sqlite3 /system/bin/sqlite3
ln -sf $MODDIR/system/bin/sqlite3 /system/xbin/sqlite3

# Samsung-specific policies


magiskpolicy --live "allow system_server system_server process execmem"
magiskpolicy --live "allow zygote sqlite_stmt_file file { read write }"
magiskpolicy --live "allow untrusted_app knox_guard_service service_manager find"
# Set system context for Samsung
magiskpolicy --live "allow zygote apk_data_file file write"
# Samsung Knox compatibility
magiskpolicy --live "allow system_server knox_guard_service service_manager find"
magiskpolicy --live "allow zygote knox_keystore_file dir search"
magiskpolicy --live "allow untrusted_app knox_guard_service service_manager add"

# Fix memory permissions
magiskpolicy --live "allow system_server system_server process execmem"
magiskpolicy --live "allow hal_gnss_default_32_0 app_data_file file { read write }"
magiskpolicy --live "allow priv_app knox_analytics_service service_manager find"
magiskpolicy --live "allow system_server keystore_service service_manager add"

# Allow memory access for One UI
magiskpolicy --live "allow hal_graphics_allocator_default_32_0 system_file file { execute execute_no_trans }"

SQLITE_REQUIRED=3080000  # 3.8.0
SQLITE_INSTALLED=$(sqlite3 --version | awk '{print $1}' | tr -d '.')

if [ "$SQLITE_INSTALLED" -lt "$SQLITE_REQUIRED" ]; then
    echo "SQLite3 version mismatch: Required 3.8.0+, Found $(sqlite3 --version)" >&2
    abort
fi

# Enable our module in LSPosed
sqlite3 "$LSPD_DB" \
"INSERT OR REPLACE INTO modules (package_name, enabled, scope) VALUES \
('com.checkmate.xposed', 1, '{\"apps\":[\"com.checkmate.android\"],\"system\":true}');"