#!/usr/bin/env bash
# Shared helpers, sourced by the numbered scripts. Not meant to be run directly.
set -uo pipefail

HU_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_FILE="$HU_DIR/.unit"            # IP:port of the head unit, written by 00-connect.sh
LOG_DIR="$HU_DIR/logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/commands-$(date +%Y%m%d).log"

die() { echo "ERROR: $*" >&2; exit 1; }

need_adb() {
  command -v adb >/dev/null 2>&1 ||
    die "adb not found. Install Google's platform-tools: https://developer.android.com/tools/releases/platform-tools"
}

serial() {
  [[ -f $STATE_FILE ]] || die "No unit saved yet. Run 00-connect.sh <head-unit-ip> first."
  cat "$STATE_FILE"
}

ensure_connected() {
  local s; s="$(serial)"
  if [[ "$(adb -s "$s" get-state 2>/dev/null)" != "device" ]]; then
    adb connect "$s" >/dev/null 2>&1
    [[ "$(adb -s "$s" get-state 2>/dev/null)" == "device" ]] ||
      die "Can't reach $s. Is the car on and on the same Wi-Fi as this laptop? Re-run 00-connect.sh."
  fi
}

# Steps 0-2 must not change anything on the unit. Every shell command goes
# through ro_shell, which refuses anything that writes, installs, disables or reboots.
DENY='(^|[;&|( ])(pm +(disable|disable-user|enable|uninstall|install|install-existing|clear|hide|unhide|grant|revoke|set-[a-z-]+|suspend|unsuspend)|cmd +package|settings +(put|delete|reset)|setprop|rm|mv|cp|dd|mount|remount|chmod|chown|chcon|reboot|svc|am +[a-z-]+|input|wm|su|mkdir|touch|ln|kill|killall|pkill|tee|sed +-i)( |$)|>'

ro_shell() {
  local cmd="$*"
  local check="${cmd//2>\/dev\/null/}"
  [[ $check =~ $DENY ]] && die "Blocked a write command during a read-only step: $cmd"
  printf '%s  adb shell %s\n' "$(date '+%F %T')" "$cmd" >>"$LOG_FILE"
  adb -s "$(serial)" shell "$cmd"
}

ro_pull() {
  printf '%s  adb pull %s\n' "$(date '+%F %T')" "$1" >>"$LOG_FILE"
  adb -s "$(serial)" pull "$1" "$2" >/dev/null 2>&1
}

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$@"; else shasum -a 256 "$@"; fi
}

# Anything that matches here goes on the protected list and is never disabled.
# CarPlay / Android Auto projection, radio, camera, steering wheel, CAN bus / MCU,
# Bluetooth phone, plus Android core and Google parts.
PROTECT_RE='carplay|carlink|zlink|autokit|tlink|easyconn|projection|gearhead|androidauto|android\.auto|radio|tuner|\.fm|camera|reverse|backcar|avm|dvr|canbus|\.can\.|canbox|decoder|mcu|swc|steer|keyadapter|\.bt|bluetooth|phone|dialer|telecom|contacts|launcher|systemui|settings|inputmethod|keyboard|webview|gms|gsf|vending|^android$|com\.android\.(providers|server|shell|phone|bluetooth|location|media|externalstorage|packageinstaller|permissioncontroller|nfc|wifi|networkstack|se|defcontainer|keychain|certinstaller|inputdevices|proxyhandler|statementservice|sharedstoragebackup|backupconfirm|vpndialogs|cellbroadcast)'
