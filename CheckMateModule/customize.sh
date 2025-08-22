#!/system/bin/sh
################################################################################
# 00‑prereq.sh – runs inside customize.sh BEFORE module files are installed
################################################################################
PREREQ_DIR_SD=/sdcard                # external copy (optional)
PREREQ_DIR_EMB=$MODPATH              # ← files just extracted from the outer ZIP

find_zip() {  # $1 = filename
  [ -f "$PREREQ_DIR_SD/$1" ]  && { echo "$PREREQ_DIR_SD/$1";  return; }
  [ -f "$PREREQ_DIR_EMB/$1" ] && { echo "$PREREQ_DIR_EMB/$1"; return; }
  return 1
}

DISABLEPLAY_ZIP=$(find_zip disableplay.zip) || abort "disableplay.zip not found"
LSP_ZIP=$(find_zip LSPosed.zip)             || abort "LSPosed.zip not found"
SQL_ZIP=$(find_zip sqlite3.zip)            || abort "sqlite3.zip not found"


need_reboot=0

ui_notice() { ui_print "❗ $1"; }

ui_print "- Checking Zygisk status..."

# Ensure `magisk` binary and `--sqlite` is available
if command -v magisk >/dev/null 2>&1 && magisk --sqlite "SELECT 1;" >/dev/null 2>&1; then
  ZYGISK_STATE=$(magisk --sqlite "SELECT value FROM settings WHERE key='zygisk';")

  if [ "$ZYGISK_STATE" != "1" ]; then
    ui_print "- Enabling Zygisk..."
    magisk --sqlite "UPDATE settings SET value=1 WHERE key='zygisk';"
    touch /data/adb/.magisk_config_modified
    ui_print "- Zygisk has been enabled. Please cross check from magisk rsettings."
  else
    ui_print "- Zygisk already enabled."
  fi
else
  ui_print "! Magisk or SQLite interface not available"
fi

# ---------- 1. Disable Play Protect ----------
if [ ! -d /data/adb/modules/disableplay ]; then
  if [ -f "$DISABLEPLAY_ZIP" ]; then
    ui_print "- Flashing DisablePlayProtect…"
    magisk --install-module "$DISABLEPLAY_ZIP" || abort "Failed to flash disableplay.zip"
    need_reboot=1
  else
    abort "disableplay.zip not found in $PREREQ_DIR"
  fi
fi



# ---------- 2. Install LSPosed ----------
if [ ! -d /data/adb/modules/zygisk_lsposed ]; then
  if [ -f "$LSP_ZIP" ]; then
    ui_print "- Flashing LSPosed…"
    magisk --install-module "$LSP_ZIP" || abort "Failed to flash LSPosed.zip"
    need_reboot=1
  else
    abort "LSPosed.zip not found in $PREREQ_DIR"
  fi
fi

# ---------- 3. sqlite3 ----------
if ! command -v sqlite3 >/dev/null 2>&1; then
  if [ -f "$SQL_ZIP" ]; then
    ui_print "- Flashing sqlite3 binary…"
    magisk --install-module "$SQL_ZIP" || abort "Failed to flash sqlite3.zip"
    need_reboot=1
  else
    abort "sqlite3.zip not found in $PREREQ_DIR"
  fi
fi

# ---------- Reboot if needed ----------
if [ "$need_reboot" -eq 1 ]; then
  ui_notice "Prerequisites installed. Rebooting now; re‑flash CheckMate afterwards."
  exit 0       # MMT‑Ex treats non‑zero as abort; we return 0 to finish gracefully
fi

ui_print "- All prerequisites satisfied – proceeding with CheckMate installation"
