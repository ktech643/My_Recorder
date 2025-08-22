#!/system/bin/sh

# MMT Extended Core
MODDIR=${0%/*}
TMPDIR=/data/local/tmp/checkmate_install
rm -rf "$TMPDIR"
mkdir -p "$TMPDIR"

ui_print " "
ui_print " CHECKMATE INSTALLATION "

# 0. Zygisk Enforcement
check_zygisk() {
  if [ "$(magisk --sqlite 'SELECT value FROM settings WHERE key="zygisk"' 2>/dev/null)" != "1" ]; then
    ui_print " Enabling Zygisk..."
    magisk --sqlite "UPDATE settings SET value=1 WHERE key='zygisk'"
    touch "${MODDIR}/.need_reboot"
  //  abort "Zygisk required! Reboot and re-flash"
  fi
}

check_zygisk

# 1. Dependency Management
install_lsposed() {
  ui_print " Checking LSPosed..."
  if ! magisk --sqlite "SELECT 1 FROM modules WHERE name='LSPosed'" 2>/dev/null; then
    ui_print " Installing LSPosed..."
    unzip -oj "$MODDIR/LSPosed.zip" -d "$TMPDIR/lsposed" >/dev/null || abort "LSPosed extraction failed"
    sh "$TMPDIR/lsposed/customize.sh" > "$TMPDIR/lsposed.log" 2>&1 || abort "LSPosed install failed"
    ui_print " LSPosed installed"
  else
    ui_print " LSPosed found"
  fi
}

install_sqlite3() {
  ui_print " Checking SQLite3..."
  if ! command -v sqlite3 >/dev/null; then
    unzip -oj "$MODDIR/sqlite3.zip" -d "$TMPDIR/sqlite" >/dev/null || abort "SQLite3 extraction failed"
    
    ARCH=$(uname -m)
    case $ARCH in
      armv7l) BIN=sqlite3-arm ;;
      aarch64) BIN=sqlite3-arm64 ;;
      x86_64) BIN=sqlite3-x64 ;;
      *) abort "Unsupported arch: $ARCH" ;;
    esac
    
    mkdir -p "$MODDIR/system/bin"
    cp "$TMPDIR/sqlite/$BIN" "$MODDIR/system/bin/sqlite3"
    chmod 0755 "$MODDIR/system/bin/sqlite3"
    ui_print " SQLite3 deployed"
  else
    ui_print " SQLite3 found"
  fi
}

# 2. Installation Flow
{
  install_lsposed
  install_sqlite3

  # 3. Play Protect Bypass
  ui_print " Applying Play Protect bypass..."
  unzip -oj "$MODDIR/disableplay.zip" -d "$TMPDIR/gms" >/dev/null || ui_print " GMS patch missing"
  if [ -f "$TMPDIR/gms/module.prop" ]; then
    sh "$TMPDIR/gms/customize.sh" > "$TMPDIR/gms.log" 2>&1 && ui_print " Play Protect bypassed" || ui_print " GMS patch failed"
  fi

  # 4. Cleanup
  rm -rf "$TMPDIR"
  ui_print " CheckMate installed"
} || {
  ui_print " Installation failed"
  abort "Check $TMPDIR for logs"
}