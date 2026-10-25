#!/usr/bin/env bash
# Step 4: apply the change list the owner approved on 2026-10-25. CHANGES THE UNIT, every change reversible.
# - Refuses to run without a finished backup (02-backup.sh).
# - Saves each item's current state and writes an undo script BEFORE changing it.
# - Never touches the protected list or the current home screen.
# - Disables only (pm disable-user). Nothing is uninstalled.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected

backup="$(latest_backup)" || true
[[ -n $backup ]] || die "No finished backup found. Run 02-backup.sh first and let it complete."

DISABLE=(
  com.autolauncher.motorcar.free       # Car Launcher FREE: ads, mic + location, not the home screen
  com.ex.dabplayer.pad                 # DAB radio: no DAB in the US
  com.android.soundrecorder            # preinstalled microphone recorder
  com.netflix.mediaclient              # vendor-sideloaded copy, never updated
  com.spotify.music                    # vendor-sideloaded copy, never updated
  com.here.app.maps                    # vendor-sideloaded copy, never updated
  com.google.android.youtube           # not used on the unit
  com.google.android.googlequicksearchbox  # Google app / "Hey Google"; Android Auto uses the phone's assistant
  com.google.android.feedback          # Google feedback reporter
  com.imotor.ams                       # vendor firmware updater (imotoros.com.cn) + device-info service
  com.imotor.imtsmartadapter           # bridge for the (already disabled) TXZ voice assistant; contacts + call log
  com.imotor.oneinstall                # vendor APK-install popup
  com.imotor.memory                    # "memory cleaner" screen
)
ENABLE=(
  com.imotor.fmam                      # FM/AM radio app (vendor had it off; radio doesn't work)
)
# Matched by the protected pattern only because of the word "launcher"; approved explicitly by the owner.
APPROVED_OVERRIDE="com.autolauncher.motorcar.free"
DNS_HOST="dns.adguard-dns.com"

run="$HU_DIR/changes/$(date +%Y%m%d-%H%M%S)"; mkdir -p "$run"
undo="$run/undo.sh"
home="$(ro_shell 'pm resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tr -d '\r' | tail -1)"
ro_shell 'pm list packages -d' | tr -d '\r' | sed 's/^package://' | sort >"$run/before_disabled.txt"
ro_shell 'pm list packages' | tr -d '\r' | sed 's/^package://' | sort >"$run/before_all.txt"
prev_mode="$(ro_shell 'settings get global private_dns_mode' | tr -d '\r')"
prev_spec="$(ro_shell 'settings get global private_dns_specifier' | tr -d '\r')"
printf 'mode=%s\nspecifier=%s\n' "$prev_mode" "$prev_spec" >"$run/before_private_dns.txt"

echo "Backup:      $backup"
echo "Home screen: $home (never touched)"
echo
echo "Will DISABLE:"; printf '  %s\n' "${DISABLE[@]}"
echo "Will ENABLE:";  printf '  %s\n' "${ENABLE[@]}"
echo "Will SET:     Private DNS -> $DNS_HOST   (now: $prev_mode $prev_spec)"
echo
read -r -p "Type YES to apply: " ok
[[ $ok == YES ]] || die "Not applied. Nothing changed."

cat >"$undo" <<EOF
#!/usr/bin/env bash
# Undo for change run $(basename "$run"). Restores every item to its state before 03-apply.sh.
source "$HU_DIR/scripts/common.sh"
need_adb; ensure_connected
EOF
chmod +x "$undo"
undo_add() { echo "$1" >>"$undo"; }   # appended in order; each line restores one item

done_list=(); skipped=()
for pkg in "${DISABLE[@]}"; do
  if ! grep -qx "$pkg" "$run/before_all.txt"; then skipped+=("$pkg (not installed)"); continue; fi
  if [[ ${home%%/*} == "$pkg" ]]; then skipped+=("$pkg (is the home screen)"); continue; fi
  if [[ $pkg =~ $PROTECT_RE && $pkg != "$APPROVED_OVERRIDE" ]]; then skipped+=("$pkg (protected)"); continue; fi
  if grep -qx "$pkg" "$run/before_disabled.txt"; then skipped+=("$pkg (already disabled)"); continue; fi
  undo_add "rw_shell 'pm enable $pkg'"
  res="$(rw_shell "pm disable-user --user 0 $pkg")"
  echo "  disable $pkg -> $res"; done_list+=("disabled $pkg")
done
for pkg in "${ENABLE[@]}"; do
  if ! grep -qx "$pkg" "$run/before_all.txt"; then skipped+=("$pkg (not installed)"); continue; fi
  if ! grep -qx "$pkg" "$run/before_disabled.txt"; then skipped+=("$pkg (already enabled)"); continue; fi
  undo_add "rw_shell 'pm disable-user --user 0 $pkg'"
  res="$(rw_shell "pm enable $pkg")"
  echo "  enable  $pkg -> $res"; done_list+=("enabled $pkg")
done

# Private DNS
if [[ $prev_mode == null || -z $prev_mode ]]; then undo_add "rw_shell 'settings delete global private_dns_mode'"
else undo_add "rw_shell 'settings put global private_dns_mode $prev_mode'"; fi
if [[ $prev_spec == null || -z $prev_spec ]]; then undo_add "rw_shell 'settings delete global private_dns_specifier'"
else undo_add "rw_shell 'settings put global private_dns_specifier $prev_spec'"; fi
rw_shell "settings put global private_dns_specifier $DNS_HOST" >/dev/null
rw_shell "settings put global private_dns_mode hostname" >/dev/null
done_list+=("private DNS -> $DNS_HOST")
undo_add 'echo "Undo complete."'

# ---- Verify (read-only) ----
sleep 5
{
  echo "=== APPLY RESULT ($(date '+%F %T')) ==="
  echo "Changes:"; printf '  %s\n' "${done_list[@]}"
  echo "Skipped:"; printf '  %s\n' "${skipped[@]:-none}"
  echo
  echo "State now:"
  for pkg in "${DISABLE[@]}" "${ENABLE[@]}"; do
    st="$(ro_shell "pm list packages -d $pkg" | tr -d '\r' | grep -qx "package:$pkg" && echo disabled || echo enabled)"
    printf '  %-45s %s\n' "$pkg" "$st"
  done
  echo "  private DNS: $(ro_shell 'settings get global private_dns_mode' | tr -d '\r') $(ro_shell 'settings get global private_dns_specifier' | tr -d '\r')"
  echo
  echo "DNS check (0.0.0.0 = blocked by AdGuard):"
  for h in www.google.com iotapi.adups.com iotapi-new.abupdate.com uiotapi-new.abupdate.com piotapi-new.abupdate.com \
           www.imotoros.com.cn cms.carletter.cn; do
    ip="$(ro_shell "ping -c 1 -W 2 $h" 2>&1 | tr -d '\r' | sed -n 's/^PING [^ ]* (\([^)]*\)).*/\1/p' | head -1)"
    printf '  %-28s %s\n' "$h" "${ip:-no answer}"
  done
  echo
  echo "Undo everything from this run:  bash $undo"
} | tee "$run/RESULT.txt"
echo
echo "Now test in the car: Android Auto, CarPlay, steering wheel buttons, backup camera, a Bluetooth call,"
echo "the climate pop-up and the FM radio. Then send Claude: $run/RESULT.txt"
