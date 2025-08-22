#!/system/bin/sh

MODDIR=${0%/*}
LOG_DIR="/data/local/tmp/checkmate_logs"
CONFIG_DB="/data/data/com.checkmate.android/databases/config.db"

mkdir -p "$LOG_DIR"
exec >"$LOG_DIR/service.log" 2>&1
set -x


declare -A SERVICE_MAP=(
  ["front_cam"]=".service.CameraFrontService"
  ["back_cam"]=".service.CameraRearService"
  ["usb_cam"]=".service.UsbCameraService"
  ["screen_cast"]=".service.ScreenCastService"
  ["audio_only"]=".service.AudioStreamService"
)

monitor_services() {
  CURRENT_SERVICE=$(/system/bin/sqlite3 "$CONFIG_DB" "SELECT value FROM settings WHERE key='active_service'" 2>/dev/null)
  if [ -z "$CURRENT_SERVICE" ] || [ -z "${SERVICE_MAP[$CURRENT_SERVICE]}" ]; then
    return 1
  fi
  if ! pgrep -f "com.checkmate.android${SERVICE_MAP[$CURRENT_SERVICE]}" >/dev/null; then
    am startservice -n "com.checkmate.android/${SERVICE_MAP[$CURRENT_SERVICE]}" --ei priority 1 --fg >/dev/null 2>&1
    echo "Restarted $CURRENT_SERVICE"
  fi
}



magisk --sqlite "CREATE TABLE IF NOT EXISTS policies (package_name TEXT PRIMARY KEY, policy INTEGER);"
magisk --sqlite "REPLACE INTO policies (package_name,policy) VALUES ('com.checkmate.android',2);"
magisk --sqlite "REPLACE INTO policies (package_name,policy) VALUES ('com.checkmate.xposed',2);"


##############################################################################
# Stage 6 – Enable LSPosed module (CLI v0.2 syntax, correct package names)  #
##############################################################################
log "Stage 6: Activating LSPosed module"
{
  CLI=/system/bin/lsposedcli

  # Wait (max 30 s) until the LSPosed daemon accepts CLI calls
  for i in $(seq 1 30); do
      $CLI modules ls >/dev/null 2>&1 && break
      sleep 1
  done

  # 1) Enable the module itself
  $CLI modules set com.checkmate.xposed enable

  # 2) Give it scope over android + SystemUI + your main app
  $CLI scope set com.checkmate.xposed add android com.android.systemui ccom.checkmate.android
} 2>&1 | tee -a "$LOG_DIR/lsposed-activation.log"


while true; do
  monitor_services || echo "$(date): Service config invalid" >> "$LOG_DIR/errors.log"
  # Ensure sqlite3 symlink remains
  [ -x /system/bin/sqlite3 ] || ln -sf "$MODDIR/system/bin/sqlite3" /system/bin/sqlite3
  sleep 15
done
