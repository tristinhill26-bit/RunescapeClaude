#!/usr/bin/env bash
# Step 1c: what the unknown vendor apps and boot scripts actually do. READ-ONLY on the unit.
# Pulls a copy of each app's APK to this phone/laptop, lists its components, and extracts every
# web address baked into its code. Then reads the unexplained boot scripts and watches the
# network for 2 minutes.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected
command -v unzip >/dev/null 2>&1 || die "Needs unzip on this phone/laptop. In Termux: pkg install -y unzip"

recon="$(ls -d "$HU_DIR"/recon/*/ 2>/dev/null | tail -1)"
[[ -n $recon ]] || die "Run 01-recon.sh first."
out="$recon/DEEP.txt"; apkdir="$recon/apks"; mkdir -p "$apkdir"
echo "Writing $out (takes ~3 minutes)"

# Unknown vendor apps first, then the protected car apps for reference (we only look at those).
SUSPECTS="com.carletter.car com.imotor.ams com.imotor.oneinstall com.imotor.memory com.imotor.imtsmartadapter
com.imotor.bootvalidation com.imotor.dev com.android.factorymode com.autolauncher.motorcar.free"
REFERENCE="com.imotor.phoneconnect com.clw.vehicleinfos"
SCRIPTS="/system/bin/autorun.sh /system/bin/messi_run.sh /system/bin/ltlink.sh /system/bin/key.learn.sh
/system/bin/process_suspend.sh /system/bin/init_log_sh"
NATIVE="/system/bin/LinkSDKServer /system/bin/imtrvc /system/bin/gocsdk"

# Pull web addresses out of a file. Dex strings and native strings are plain bytes, so grep -a works.
hosts_in() {
  grep -ahoE 'https?://[A-Za-z0-9.-]+(:[0-9]+)?' "$@" 2>/dev/null | sed -E 's#https?://##' |
    grep -vE '^(schemas\.android\.com|www\.w3\.org|xml\.org|xmlpull\.org|ns\.adobe\.com|www\.apache\.org|localhost|127\.0\.0\.1|example\.com|www\.example\.com|purl\.org|java\.sun\.com)$' |
    sort | uniq -c | sort -rn | head -40
}

{
  echo "=== DEEP LOOK ($(date '+%F %T')) ==="
  for pkg in $SUSPECTS $REFERENCE; do
    echo
    echo "################ $pkg"
    d="$(ro_shell "dumpsys package $pkg" | tr -d '\r')"
    echo "$d" | grep -m3 -E 'sharedUser=|codePath=|versionName=' | sed 's/^ */  /'
    echo "  Components:"
    echo "$d" | grep -oE "$pkg/[A-Za-z0-9_.\$]+" | sort -u | sed 's/^/    /' | head -40
    echo "  Starts on: $(echo "$d" | grep -oE 'android\.intent\.action\.(BOOT_COMPLETED|LOCKED_BOOT_COMPLETED|MEDIA_MOUNTED|PACKAGE_REMOVED|PACKAGE_ADDED|USER_PRESENT|ACC_ON|SCREEN_ON)|com\.[a-z.]*\.(ACC_ON|ACCON|BOOT)[A-Z_]*' | sort -u | paste -sd' ' -)"
    echo "  Running services:"
    ro_shell "dumpsys activity services $pkg" | tr -d '\r' | grep -oE 'ServiceRecord\{[^}]*\}' | sed 's/^/    /' | sort -u | head -15
    mkdir -p "$apkdir/$pkg"
    ro_shell "pm path $pkg" </dev/null | tr -d '\r' | sed 's/^package://' | while read -r apk; do
      [[ -n $apk ]] && ro_pull "$apk" "$apkdir/$pkg/" </dev/null
    done
    tmp="$(mktemp -d)"
    for a in "$apkdir/$pkg"/*.apk; do [[ -f $a ]] && unzip -qo "$a" 'classes*.dex' 'lib/*' 'assets/*' -d "$tmp" 2>/dev/null; done
    echo "  Web addresses inside the app (count  host):"
    hosts_in -r "$tmp" | sed 's/^/    /'
    echo "  Native libraries: $(ls "$tmp"/lib/*/*.so 2>/dev/null | xargs -n1 basename 2>/dev/null | sort -u | paste -sd' ' -)"
    rm -rf "$tmp"
  done

  echo
  echo "################ NATIVE PROGRAMS"
  for bin in $NATIVE; do
    f="$apkdir/$(basename "$bin")"
    ro_pull "$bin" "$f"
    echo "  $bin  ($(wc -c <"$f" 2>/dev/null || echo ?) bytes)"
    hosts_in "$f" | sed 's/^/    /'
  done

  echo
  echo "################ BOOT SCRIPTS"
  for s in $SCRIPTS; do
    echo "----- $s"
    ro_shell "cat $s 2>/dev/null" | tr -d '\r' | head -120 | sed 's/^/  | /'
  done

  echo
  echo "################ PROPS OF INTEREST"
  grep -iE 'autotest|imotor|imt\.|persist\.sys\.(fyt|ts|.*cloud|.*ota|.*update)|ota|update' "$recon/props.txt" | sed 's/^/  /' | head -40

  echo
  echo "################ NETWORK WATCH (2 minutes, every 10s)"
  for i in $(seq 12); do net_snapshot; sleep 10; done | sort | uniq -c | sort -rn
} >"$out" 2>&1

cat "$out"
echo
echo "Done. Nothing on the unit was changed."
echo "Send Claude: $out"
