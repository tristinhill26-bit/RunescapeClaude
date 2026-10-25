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
PROTECT_RE='carplay|carlink|zlink|autokit|tlink|easyconn|projection|gearhead|androidauto|android\.auto|radio|tuner|\.fm|camera|reverse|backcar|vehicle|clw|carinfo|avm|dvr|canbus|\.can\.|canbox|decoder|mcu|swc|steer|keyadapter|\.bt|bluetooth|phone|dialer|telecom|contacts|launcher|systemui|settings|inputmethod|keyboard|webview|gms|gsf|vending|^android$|com\.android\.(providers|server|shell|phone|bluetooth|location|media|externalstorage|packageinstaller|permissioncontroller|nfc|wifi|networkstack|se|defcontainer|keychain|certinstaller|inputdevices|proxyhandler|statementservice|sharedstoragebackup|backupconfirm|vpndialogs|cellbroadcast)'

# ---- Network helpers: decode /proc/net/tcp{,6} and map socket owners to apps ----
uid_to_pkg() {
  local u="$1"
  (( u < 10000 )) && { echo "system uid $u"; return; }
  [[ -n ${UIDMAP:-} ]] || UIDMAP="$(ro_shell 'pm list packages -U' | tr -d '\r' | sed 's/^package://')"
  echo "$UIDMAP" | awk -v u="uid:$u" '$2==u {print $1}' | paste -sd, -
}

# /proc/net stores addresses as little-endian 32-bit words in hex.
hex2ip() {
  local h="$1" w g out="" i
  if [[ ${#h} -eq 8 ]]; then
    printf '%d.%d.%d.%d' "0x${h:6:2}" "0x${h:4:2}" "0x${h:2:2}" "0x${h:0:2}"; return
  fi
  [[ ${h:0:24} == 0000000000000000FFFF0000 ]] && { hex2ip "${h:24:8}"; return; }
  [[ $h == 00000000000000000000000000000000 ]] && { echo "[::]"; return; }
  for i in 0 1 2 3; do
    w="${h:i*8:8}"; g="${w:6:2}${w:4:2}${w:2:2}${w:0:2}"
    out+="$(printf '%x:%x' "0x${g:0:4}" "0x${g:4:4}"):"
  done
  echo "[${out%:}]"
}

net_snapshot() {
  ro_shell 'cat /proc/net/tcp /proc/net/tcp6' | tr -d '\r' | awk '$4=="0A"||$4=="01" {print $2, $3, $4, $8}' |
  while read -r loc rem st uid; do
    local lip lport rip rport
    lip="$(hex2ip "${loc%%:*}")"; lport=$((16#${loc##*:}))
    rip="$(hex2ip "${rem%%:*}")"; rport=$((16#${rem##*:}))
    if [[ $st == 0A ]]; then printf '  LISTEN       %-28s uid %-6s %s\n' "$lip:$lport" "$uid" "$(uid_to_pkg "$uid")"
    else printf '  CONNECTED -> %-28s uid %-6s %s\n' "$rip:$rport" "$uid" "$(uid_to_pkg "$uid")"; fi
  done | sort -u
}

# ---- Write access: only 03-apply.sh and undo scripts use this. Every call is logged as CHANGE. ----
rw_shell() {
  printf '%s  CHANGE adb shell %s\n' "$(date '+%F %T')" "$*" >>"$LOG_FILE"
  adb -s "$(serial)" shell "$*" </dev/null | tr -d '\r'
}

latest_backup() {
  local b; b="$(ls -d "$HU_DIR"/backup/*/ 2>/dev/null | tail -1)"
  [[ -n $b && -f $b/SUMMARY.txt && -f $b/MANIFEST.sha256 ]] && echo "${b%/}"
}
