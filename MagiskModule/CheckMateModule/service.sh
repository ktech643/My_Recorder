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

while true; do
  monitor_services || echo "$(date): Service config invalid" >> "$LOG_DIR/errors.log"
  # Ensure sqlite3 symlink remains
  [ -x /system/bin/sqlite3 ] || ln -sf "$MODDIR/system/bin/sqlite3" /system/bin/sqlite3
  sleep 60
done
