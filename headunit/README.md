# Rimoody VW head unit: debloat, step by step

**Unit:** Rimoody 7" 4GB+64GB "USB" set for VW Golf/Jetta/Passat MK5/MK6 (Amazon B0G29F97PQ, bought Feb 4 2026).
The listing says **Android 15** in the bullets and **4 Core** in its own comparison table. One reviewer says it's really
Android 9 or 10 with locked-down USB debugging and no Play Store. Step 1 settles what it actually is.

## Guardrails (same as the Reddit post, plus one)

1. **Back up everything before changing anything.** Steps 0–2 are read-only. `common.sh` refuses any write
   command (`pm disable`, `settings put`, `rm`, `reboot`, `>` …) while they run.
2. **Show the list and wait for OK.** Nothing gets disabled until you've approved the exact list.
3. **Disable, don't uninstall.** Removing an APK comes later, only with your OK, and only after a restore script exists for it.
4. **Never touch** CarPlay, **Android Auto**, the radio, the backup camera, the steering wheel buttons, **Bluetooth calling,
   or the CAN-bus decoder / MCU services** (on a VW, the CAN box is what makes the steering wheel buttons work).
   Leave anything we can't identify alone.
5. **Never sign into anything** for you.
6. **Only install apps from their official GitHub releases**, with the SHA-256 checked.
7. *(added)* **Every change has an undo command**, and **no firmware flashing**. Bricking an in-dash unit means pulling the dash apart.

## Why not a brand-new OS?

On this hardware, building our own ROM would almost certainly **break Android Auto and CarPlay**, which is the thing you want to keep:

- **Android Auto / CarPlay here aren't part of Android.** They come from a licensed vendor projection app (usually ZLink, CarLink or
  AutoKit), and that license is tied to the vendor firmware. A clean Android build can't legally or practically include it.
- **The radio, camera, steering wheel and CAN box** are driven by the vendor's closed MCU apps and kernel drivers. These vendors
  don't publish kernel source, so a custom build would be missing the parts that make it a car stereo.
- **Flashing is the main way to brick it**, and some of these units can't be recovered without a vendor-signed image.

What we do instead gets you most of what a new OS would: we take the vendor OS down to the base system plus the car parts.
Spyware, updaters, silent installers and root backdoors get shut off, the vendor home screen is replaced with an open-source
one, and DNS ad/tracker blocking covers every app. Android Auto keeps working because we never touch it.

## How this works

The cloud Claude session that wrote these scripts can't reach your car. You run them from a laptop on the same Wi-Fi as the
stereo, then send back the `SUMMARY.txt` each one prints. You can also do what the Reddit poster did and open Claude Code on the
laptop in the driver's seat, point it at this folder and this README, and let it run the adb commands itself.

Needs: a laptop (macOS, Linux, or Windows with **Git Bash**) and Google's
[platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`). Every command is logged with a timestamp to `logs/`.

## Steps

| # | Step | Changes the unit? | Status |
|---|------|-------------------|--------|
| 0 | Turn on debugging, connect over Wi-Fi (`00-connect.sh`) | No | **← you are here** |
| 1 | Recon: real Android version, CPU, every app, boot scripts, su, network (`01-recon.sh`) | No | ready |
| 2 | Full backup: every APK, all settings, boot scripts, SHA-256 manifest (`02-backup.sh`) | No | ready |
| 3 | Claude sorts apps into keep / junk / not sure and you approve the list | No | after step 1 |
| 4 | Disable the approved junk, with an undo script | Yes, reversible | after your OK |
| 5 | Shut down silent installers, auto-reinstallers, the OTA updater and the su backdoor (originals saved first) | Yes, reversible | after your OK |
| 6 | Private DNS (AdGuard), WebView update, open-source launcher, apps from GitHub releases | Yes, reversible | after your OK |
| 7 | Verify: reboot, watch network connections, test AA/CarPlay/radio/camera/SWC/calls | No | |
| 8 | Lock down adb so only your laptop can connect | Yes, reversible | optional |

### Step 0: on the stereo

1. Connect the stereo to Wi-Fi: your home network if the car reaches it, otherwise a phone hotspot with the laptop on the same hotspot.
2. **Settings → About (or System → About) → tap "Build number" 7 times** to unlock Developer options.
   On many of these units, Developer options sit behind **Settings → Factory/Car settings → password**. Look in the manual or ask
   Rimoody support for the code. Codes people commonly report for these units include `8888`, `1234`, `0000` and `3368`, but they vary.
   Don't change anything else in the factory menu.
3. In **Developer options**, turn on **USB debugging** and, if it's there, **ADB over network / Network debugging**.
4. Find the stereo's IP under **Settings → Wi-Fi → (your network)**.

### Step 0–2: on the laptop

```bash
cd headunit/scripts
./00-connect.sh 192.168.x.x     # accept the "Allow debugging?" prompt on the stereo if it appears
./01-recon.sh                   # ~1 minute. Prints the summary
./02-backup.sh                  # can take a while over Wi-Fi. Leave the car running/ACC on
```

### No laptop: use an Android phone (Termux)

Install Termux from its [GitHub releases](https://github.com/termux/termux-app/releases), turn on the phone's hotspot,
connect the stereo to it, then in Termux:

```bash
pkg install -y android-tools git
git clone -b claude/pensive-lovelace-f9ez4m https://github.com/tristinhill26-bit/RunescapeClaude.git
cd RunescapeClaude/headunit/scripts
bash 00-connect.sh <stereo-ip>   # the real IP, without the < >
termux-wake-lock                 # keeps Android from killing Termux during the backup
bash 01-recon.sh
```

To hand a summary back, run `termux-setup-storage` once, then `cp ../recon/*/SUMMARY.txt ~/storage/downloads/` and attach the file.

Then send back both `SUMMARY.txt` files (`recon/<time>/SUMMARY.txt` and `backup/<time>/SUMMARY.txt`).
If `00-connect.sh` fails, send its output instead.

**Keep `recon/`, `backup/` and `logs/` on your laptop.** They're git-ignored, because they contain your device IDs and
Bluetooth/Wi-Fi names. Copy `backup/` to a USB stick as well.
