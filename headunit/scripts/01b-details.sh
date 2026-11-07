#!/usr/bin/env bash
# Step 1b: a closer look at what recon flagged. READ-ONLY.
# For every vendor/third-party app: where it lives, version, who installed it, which sensitive
# permissions it holds and whether it has an icon. Plus: which app is the home screen, who owns each
# open network port, and the boot script that installs an APK from USB sticks.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected

recon="$(ls -d "$HU_DIR"/recon/*/ 2>/dev/null | tail -1)"
[[ -n $recon ]] || die "Run 01-recon.sh first."
out="$recon/DETAILS.txt"
echo "Writing $out"


{
  echo "=== DETAILS ($(date '+%F %T')) ==="
  echo
  echo "--- HOME SCREEN / DEFAULTS ---"
  echo "Home:      $(ro_shell 'pm resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tr -d '\r' | tail -1)"
  echo "Keyboard:  $(ro_shell 'settings get secure default_input_method' | tr -d '\r')"
  echo "Assistant: $(ro_shell 'settings get secure assistant' | tr -d '\r')  voice: $(ro_shell 'settings get secure voice_interaction_service' | tr -d '\r')"
  echo "Private DNS: $(ro_shell 'settings get global private_dns_mode' | tr -d '\r') $(ro_shell 'settings get global private_dns_specifier' | tr -d '\r')"
  echo "Unknown sources / verifier: $(ro_shell 'settings get secure install_non_market_apps' | tr -d '\r') / $(ro_shell 'settings get global package_verifier_enable' | tr -d '\r')"
  echo
  echo "--- APP ICONS (what each app opens) ---"
  ro_shell 'pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER' | tr -d '\r' | grep '/' | sort
  echo
  echo "--- VENDOR + THIRD-PARTY APPS ---"
  echo "  (perms: MIC=record audio, LOC=location, PHONE=phone state, CALLLOG, SMS, INSTALL=can install apps, BOOT=starts at boot, NET=internet)"
  sed 's/^package://' "$recon/pkgs_enabled.txt" |
    grep -vE '^(android|com\.android\.|com\.google\.)' ; echo com.android.factorymode; echo com.android.soundrecorder
} >"$out.tmp"

# per-package details (appended after the header block)
pkgs="$(sed -n '/^--- VENDOR + THIRD-PARTY APPS ---/,$p' "$out.tmp" | tail -n +3 | sort -u)"
sed -n '1,/^  (perms:/p' "$out.tmp" >"$out"; rm -f "$out.tmp"
for pkg in $pkgs; do
  d="$(ro_shell "dumpsys package $pkg" | tr -d '\r')"
  path="$(echo "$d" | grep -m1 'codePath=' | sed 's/.*codePath=//')"
  ver="$(echo "$d" | grep -m1 'versionName=' | sed 's/.*versionName=//')"
  inst="$(echo "$d" | grep -m1 'installerPackageName=' | sed 's/.*installerPackageName=//')"
  perms=""
  for p in RECORD_AUDIO:MIC ACCESS_FINE_LOCATION:LOC READ_PHONE_STATE:PHONE READ_CALL_LOG:CALLLOG READ_SMS:SMS \
           INSTALL_PACKAGES:INSTALL RECEIVE_BOOT_COMPLETED:BOOT INTERNET:NET READ_CONTACTS:CONTACTS; do
    echo "$d" | grep -q "android.permission.${p%%:*}: granted=true" && perms+="${p##*:} "
  done
  printf '  %-42s %-38s v%-14s inst=%-12s %s\n' "$pkg" "$path" "${ver:-?}" "${inst:-none}" "$perms" >>"$out"
done

{
  echo
  echo "--- DISABLED PACKAGES (already off) ---"
  sed 's/^package:/  /' "$recon/pkgs_disabled.txt" | sort
  echo
  echo "--- NETWORK: open ports and live connections (3 snapshots, 10s apart) ---"
  for i in 1 2 3; do net_snapshot; [[ $i -lt 3 ]] && sleep 10; done | sort -u
  echo
  echo "--- USB AUTO-INSTALL SCRIPT (imotor-client / AutoTestClient.apk) ---"
  awk '/^===== /{f=$2; n=0} {n++} /imotor-client|AutoTestClient|pm install|\.apk/{print f": "n": "$0}' "$recon/init_sh.txt" | head -20
  file="$(awk '/^===== /{f=$2} /AutoTestClient/{print f; exit}' "$recon/init_sh.txt")"
  if [[ -n $file ]]; then
    echo "  Full script: $file"
    ro_shell "cat $file" | tr -d '\r' | sed 's/^/    | /'
    echo "  Who runs it:"
    grep -nB2 -A6 "$(basename "$file")" "$recon/init_rc.txt" | sed 's/^/    | /' | head -40
  fi
  echo
  echo "--- VENDOR SERVICES IN INIT (non-AOSP) ---"
  grep -nE '^service ' "$recon/init_rc.txt" | grep -viE ' (adbd|vold|netd|zygote|surfaceflinger|servicemanager|hwservicemanager|vndservicemanager|logd|lmkd|ueventd|healthd|installd|keystore|gatekeeper|statsd|storaged|tombstoned|wificond|media|audioserver|cameraserver|drm|incidentd|gpu|update_engine|apexd|traced|heapprofd|perfetto|bootstat|console|ril|gsid|credstore|iorap|mdns|mtpd|racoon|blank_screen|dumpstate|bpfloader|llkd|idmap|flags_health|snapuserd|wait_for_keymaster|system_suspend|usbd|cppreopts|otapreopt|ota|recovery|charger|vendor\.|android\.hardware|exec_)' | sed 's/^/  /'
  echo
  echo "--- WHAT THE TOP NATIVE PROCESSES ARE ---"
  for proc in LinkSDKServer gocsdk; do
    echo "  $proc: $(ro_shell "ls -l /proc/\$(pidof $proc)/exe 2>/dev/null; ls -l /system/bin/$proc /vendor/bin/$proc 2>/dev/null" | tr -d '\r' | awk '{print $NF}' | paste -sd' ' -)"
  done
} >>"$out"

cat "$out"
echo
echo "Done. Nothing on the unit was changed."
echo "Send Claude: $out"
