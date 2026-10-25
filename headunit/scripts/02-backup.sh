#!/usr/bin/env bash
# Step 2: back up everything before anything changes. READ-ONLY on the unit.
# Pulls every APK (including split APKs), the full package state, every setting and
# the boot scripts, then writes a SHA-256 manifest. Over Wi-Fi this can take a while.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected

out="$HU_DIR/backup/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out/apks" "$out/state" "$out/system_files"
echo "Backing up to $out"

# State: what's enabled/disabled/default right now. A later restore puts this back.
ro_shell 'pm list packages -u'        | tr -d '\r' >"$out/state/pkgs_all.txt"
ro_shell 'pm list packages -e'        | tr -d '\r' >"$out/state/pkgs_enabled.txt"
ro_shell 'pm list packages -d'        | tr -d '\r' >"$out/state/pkgs_disabled.txt"
for ns in global secure system; do
  ro_shell "settings list $ns"        | tr -d '\r' >"$out/state/settings_$ns.txt"
done
ro_shell 'getprop'                    | tr -d '\r' >"$out/state/props.txt"

# Every APK
total=$(wc -l <"$out/state/pkgs_all.txt"); n=0; failed=0
while read -r line; do
  pkg="${line#package:}"; [[ -z $pkg ]] && continue
  n=$((n+1)); printf '\r  APKs: %d/%d  %-60s' "$n" "$total" "$pkg"
  mkdir -p "$out/apks/$pkg"
  ro_shell "pm path $pkg" </dev/null | tr -d '\r' | sed 's/^package://' | while read -r apk; do
    [[ -n $apk ]] && ro_pull "$apk" "$out/apks/$pkg/" </dev/null
  done
  [[ -n "$(ls -A "$out/apks/$pkg" 2>/dev/null)" ]] || { failed=$((failed+1)); echo "$pkg" >>"$out/state/apk_pull_failed.txt"; }
done <"$out/state/pkgs_all.txt"
echo

# Boot scripts and init files, so any later edit can be undone byte for byte
for path in /system/etc/init /vendor/etc/init /system/build.prop /vendor/build.prop; do
  ro_pull "$path" "$out/system_files/" || true
done
ro_shell 'ls /system/bin/*.sh /system/xbin/*.sh /vendor/bin/*.sh /system/etc/*.sh 2>/dev/null' | tr -d '\r' |
  while read -r f; do [[ -n $f ]] && ro_pull "$f" "$out/system_files/" </dev/null; done

# Integrity manifest
( cd "$out" && find . -type f ! -name MANIFEST.sha256 -print0 | sort -z | xargs -0 sha256sum 2>/dev/null >MANIFEST.sha256 ||
  find . -type f ! -name MANIFEST.sha256 -print0 | sort -z | xargs -0 shasum -a 256 >MANIFEST.sha256 )

apks=$(find "$out/apks" -name '*.apk' | wc -l)
size=$(du -sh "$out" | cut -f1)
{
  echo "=== BACKUP SUMMARY ($(date '+%F %T')) ==="
  echo "Packages: $total   APK files pulled: $apks   Packages with no APK pulled: $failed   Size: $size"
  [[ -f $out/state/apk_pull_failed.txt ]] && { echo "Could not pull:"; sed 's/^/  /' "$out/state/apk_pull_failed.txt"; }
  echo "Manifest: $(wc -l <"$out/MANIFEST.sha256") files hashed"
} | tee "$out/SUMMARY.txt"
echo
echo "Done. Nothing on the unit was changed. Copy $out somewhere safe (a USB stick or cloud drive)."
echo "Send Claude the file:  $out/SUMMARY.txt"
