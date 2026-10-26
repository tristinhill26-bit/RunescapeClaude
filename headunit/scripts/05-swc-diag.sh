#!/usr/bin/env bash
# Steering wheel controls diagnosis. READ-ONLY.
# VW MK5/MK6 (incl. 2013 Jetta SportWagen) sends wheel buttons over CAN bus through the CAN decoder box,
# so this checks: which CAN protocol/car model the unit is set to, and whether anything arrives
# (MCU/CAN log lines or key events) while you press the buttons for 30 seconds.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected

out="$HU_DIR/recon/swc-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$out"
RE='can|car_?type|cartype|car_?model|carmodel|protocol|canbox|swc|steer|wheel|key_?learn|vehicle|decoder|mcu'

{
  echo "=== STEERING WHEEL DIAG ($(date '+%F %T')) ==="
  echo
  echo "--- CAN / vehicle configuration (props) ---"
  ro_shell 'getprop' | tr -d '\r' | grep -iE "$RE" | grep -viE 'cancel|scan|canonical|vendor\.camera' | sed 's/^/  /'
  echo
  echo "--- CAN / vehicle configuration (settings) ---"
  for ns in system global secure; do
    ro_shell "settings list $ns" | tr -d '\r' | grep -iE "$RE" | grep -viE 'cancel|scan' | sed "s/^/  [$ns] /"
  done
  echo
  echo "--- CLW / steering apps ---"
  for p in com.clw.vehicleinfos com.imotor.steerwheel; do
    echo "  $p: $(ro_shell "pm list packages -e $p" | tr -d '\r' | grep -qx "package:$p" && echo enabled || echo DISABLED)"
  done
  echo
  echo "--- Serial ports the MCU / CAN box can use ---"
  ro_shell 'ls -l /dev/ttyS* 2>/dev/null' | tr -d '\r' | sed 's/^/  /'
  echo
  echo "--- Input devices ---"
  ro_shell 'getevent -p 2>/dev/null' | tr -d '\r' | grep -E '^add device|name:' | sed 's/^/  /'
} | tee "$out/SWC.txt"

echo
echo ">>> Car on (ignition or ACC). In 5 seconds, press EACH steering wheel button 2-3 times, slowly:"
echo ">>> volume up, volume down, next, previous, phone, voice/mute, menu arrows. You have 30 seconds."
sleep 5
echo ">>> GO - press the buttons now."
ro_shell 'timeout 30 logcat -v time -T 1' >"$out/logcat_full.txt" 2>&1 &
lp=$!
ro_shell 'timeout 30 getevent -lt' >"$out/getevent.txt" 2>&1 &
gp=$!
wait $lp $gp
echo ">>> Done, you can stop."

{
  echo
  echo "--- Key events seen during the 30s (getevent) ---"
  grep -E 'EV_KEY' "$out/getevent.txt" | tr -d '\r' | sed 's/^/  /' | head -60
  echo "  (total EV_KEY lines: $(grep -c EV_KEY "$out/getevent.txt"))"
  echo
  echo "--- MCU / CAN / key log lines during the 30s ---"
  tr -d '\r' <"$out/logcat_full.txt" | grep -iE 'can|mcu|key|steer|swc|clw|vehicle|imotor|wheel' |
    grep -viE 'cancel|scan|canonical' | sed 's/^/  /' | head -120
  echo "  (total log lines captured: $(wc -l <"$out/logcat_full.txt"))"
} | tee -a "$out/SWC.txt"

echo
echo "Nothing on the unit was changed. Send Claude: $out/SWC.txt"
