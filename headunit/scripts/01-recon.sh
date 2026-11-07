#!/usr/bin/env bash
# Step 1: find out what the unit actually is and what's running on it. READ-ONLY.
# Writes everything to headunit/recon/<timestamp>/ and prints a summary to paste back to Claude.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected

out="$HU_DIR/recon/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out"
grab() { local file="$1"; shift; ro_shell "$@" 2>&1 | tr -d '\r' >"$out/$file"; echo "  - $file"; }

echo "Collecting into $out"
# What the box really is
grab props.txt              'getprop'
grab cpuinfo.txt            'cat /proc/cpuinfo'
grab cpus.txt               'cat /sys/devices/system/cpu/possible /sys/devices/system/cpu/present'
grab meminfo.txt            'cat /proc/meminfo'
grab storage.txt            'df -h'
grab kernel.txt             'uname -a; cat /proc/version'
grab whoami.txt             'id'
# Every installed app and its state
grab pkgs_all.txt           'pm list packages -f -i -u'
grab pkgs_enabled.txt       'pm list packages -e'
grab pkgs_disabled.txt      'pm list packages -d'
grab pkgs_system.txt        'pm list packages -s'
grab pkgs_thirdparty.txt    'pm list packages -3'
grab pkgs_dump.txt          'dumpsys package packages'
grab default_apps.txt       'cmd role list-roles-holders 2>/dev/null; settings get secure default_input_method; settings get secure voice_interaction_service; settings get secure assistant'
# What's running and what's eating CPU
grab processes.txt          'ps -A -o PID,PPID,USER,RSS,NAME'
grab top.txt                'top -b -n 1 -m 40'
grab services_running.txt   'dumpsys activity services'
grab webview.txt            'dumpsys webviewupdate'
# Boot-time scripts (where the auto-reinstall / silent-installer tricks live)
grab init_list.txt          'ls -la /system/etc/init /vendor/etc/init /odm/etc/init /system/bin/*.sh /system/xbin/*.sh /vendor/bin/*.sh /system/etc/*.sh /oem 2>/dev/null'
grab init_rc.txt            'for f in /system/etc/init/*.rc /vendor/etc/init/*.rc /odm/etc/init/*.rc /*.rc; do echo "===== $f"; cat "$f" 2>/dev/null; done'
grab init_sh.txt            'for f in /system/bin/*.sh /system/xbin/*.sh /vendor/bin/*.sh /system/etc/*.sh; do echo "===== $f"; cat "$f" 2>/dev/null; done'
# Root / debug exposure
grab su_check.txt           'ls -la /system/bin/su /system/xbin/su /sbin/su /vendor/bin/su /system/xbin/daemonsu 2>/dev/null; ls -la /data/local/tmp 2>/dev/null'
# Settings (read only; used later for the undo file)
grab settings_global.txt    'settings list global'
grab settings_secure.txt    'settings list secure'
grab settings_system.txt    'settings list system'
# Network: who is the unit talking to right now
grab net_ip.txt             'ip addr'
grab net_tcp.txt            'cat /proc/net/tcp /proc/net/tcp6'
grab net_udp.txt            'cat /proc/net/udp /proc/net/udp6'

# ---- Summary (computed on the laptop) ----
p() { grep -m1 "\[$1\]" "$out/props.txt" | sed 's/.*: \[\(.*\)\]/\1/'; }
cores="$(grep -c '^processor' "$out/cpuinfo.txt")"
memkb="$(awk '/MemTotal/ {print $2}' "$out/meminfo.txt")"
availkb="$(awk '/MemAvailable/ {print $2}' "$out/meminfo.txt")"
listening="$(awk 'NR>1 && $4=="0A" {print $2}' "$out/net_tcp.txt" | sort -u | tr '\n' ' ')"

{
  echo "=== HEAD UNIT RECON SUMMARY ($(date '+%F %T')) ==="
  echo "Listing claims:      Android 15, 8-core, 4GB/64GB"
  echo "Model / brand:       $(p ro.product.model) / $(p ro.product.brand) / $(p ro.product.manufacturer)"
  echo "Board / platform:    $(p ro.product.board) / $(p ro.board.platform) / $(p ro.hardware)"
  echo "Android (real):      $(p ro.build.version.release)  (SDK $(p ro.build.version.sdk))"
  echo "Security patch:      $(p ro.build.version.security_patch)"
  echo "Build:               $(p ro.build.display.id)  |  $(p ro.build.fingerprint)"
  echo "Custom fw props:     $(grep -iE '\[(ro\.(fyt|ts|syu|sys\.(fyt|ts))|persist\.(fyt|syu|ts))' "$out/props.txt" | head -5 | tr '\n' ' ')"
  echo "CPU cores (real):    $cores   ($(grep -m1 -iE 'Hardware|model name' "$out/cpuinfo.txt" | cut -d: -f2-))"
  echo "RAM total/avail:     $((memkb/1024)) MB / $((availkb/1024)) MB"
  echo "Debug exposure:      ro.secure=$(p ro.secure) ro.debuggable=$(p ro.debuggable) ro.adb.secure=$(p ro.adb.secure) adb shell runs as: $(cut -d' ' -f1 "$out/whoami.txt")"
  echo "su binaries:         $(grep -cE '/(su|daemonsu)$' "$out/su_check.txt") found"
  echo "WebView:             $(grep -m1 -i 'Current WebView package' "$out/webview.txt")"
  echo "Packages:            $(wc -l <"$out/pkgs_all.txt") total, $(wc -l <"$out/pkgs_enabled.txt") enabled, $(wc -l <"$out/pkgs_disabled.txt") disabled, $(wc -l <"$out/pkgs_thirdparty.txt") user-installed"
  echo "Listening TCP ports: ${listening:-none}"
  echo
  echo "--- PROTECTED (CarPlay/Android Auto/radio/camera/SWC/CAN/BT/core; never touched) ---"
  sed 's/^package://' "$out/pkgs_enabled.txt" | grep -iE "$PROTECT_RE" | sort
  echo
  echo "--- EVERYTHING ELSE ENABLED (Claude sorts these into keep / junk / leave alone) ---"
  sed 's/^package://' "$out/pkgs_enabled.txt" | grep -viE "$PROTECT_RE" | sort
  echo
  echo "--- USER-INSTALLED APPS ---"
  sed 's/^package://' "$out/pkgs_thirdparty.txt" | sort
  echo
  echo "--- BOOT SCRIPT LINES THAT INSTALL APPS, SET DEFAULTS OR RUN AS ROOT ---"
  grep -nE 'pm install|pm enable|\.apk|ime set|default_input_method|settings put|setenforce|/su( |$)|chmod (0?777|[46]755)|exec_background|seclabel u:r:su' "$out/init_rc.txt" "$out/init_sh.txt" | sed "s|$out/||" | head -60
  echo
  echo "--- TOP CPU RIGHT NOW ---"
  sed -n '1,25p' "$out/top.txt"
} | tee "$out/SUMMARY.txt"

echo
echo "Done. Nothing on the unit was changed."
echo "Send Claude the file:  $out/SUMMARY.txt"
echo "(The rest of the folder stays on your laptop. It's backup material and has your Bluetooth/Wi-Fi names in it.)"
