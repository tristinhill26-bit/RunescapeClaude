#!/usr/bin/env bash
# Step 0: connect to the head unit over Wi-Fi with adb. Changes nothing on the unit.
# Usage: ./00-connect.sh <head-unit-ip> [port]
source "$(dirname "$0")/common.sh"
need_adb

ip="${1:-}"; port="${2:-5555}"
# Accept "ip:port" exactly as the stereo's Wireless debugging screen shows it.
if [[ $ip == *:* ]]; then port="${ip##*:}"; ip="${ip%:*}"; fi
[[ -n $ip ]] || die "Usage: $0 <head-unit-ip> [port]   (find the IP under Settings > Wi-Fi > your network on the stereo)"
target="$ip:$port"

echo "Connecting to $target ..."
out="$(adb connect "$target" 2>&1)"; echo "$out"

state="$(adb -s "$target" get-state 2>&1)"
case "$state" in
  device)
    echo "$target" >"$STATE_FILE"
    echo
    echo "Connected. Saved $target for the next scripts."
    echo "Model:   $(adb -s "$target" shell getprop ro.product.model | tr -d '\r')"
    echo "Android: $(adb -s "$target" shell getprop ro.build.version.release | tr -d '\r')"
    echo
    echo "Next: ./01-recon.sh"
    ;;
  *unauthorized*)
    echo
    echo "The stereo is asking you to allow USB debugging. Tick 'Always allow from this computer',"
    echo "press OK on the stereo's screen, then run this script again."
    exit 1
    ;;
  *)
    echo
    echo "Couldn't connect. Things to try, in order:"
    echo "  1. Make sure Developer options > USB debugging (and 'ADB over network', if the unit has it) is ON."
    echo "  2. Check the stereo and laptop are on the same Wi-Fi and the IP is right."
    echo "     Some phone hotspots block devices from talking to each other. If it times out, try home Wi-Fi or a travel router."
    echo "  3. If the stereo is Android 11 or newer, use Developer options > Wireless debugging > 'Pair device with pairing code':"
    echo "       adb pair <ip>:<pairing-port>    (enter the 6-digit code)"
    echo "     then run:  $0 $ip <port shown on the Wireless debugging screen>"
    echo "  4. Still stuck? Paste this whole output back to Claude."
    exit 1
    ;;
esac
