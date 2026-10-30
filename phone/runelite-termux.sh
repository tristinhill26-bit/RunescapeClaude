#!/data/data/com.termux/files/usr/bin/bash
# Opens RuneLite with Claude's plugins in the Termux:X11 app. phone/setup-termux.sh installs this as "runelite".
DISTRO="debian"
ROOTFS="$PREFIX/var/lib/proot-distro/installed-rootfs/$DISTRO"

# A credentials.properties file from the PC (for Jagex accounts) can be dropped in the phone's Downloads folder.
# Move it into Debian's RuneLite folder so it isn't left lying around.
for CREDS in "$HOME/storage/downloads/credentials.properties" "/sdcard/Download/credentials.properties"; do
	if [ -f "$CREDS" ]; then
		mkdir -p "$ROOTFS/root/.runelite"
		mv -f "$CREDS" "$ROOTFS/root/.runelite/credentials.properties" && echo "Moved your Jagex account login into RuneLite."
		break
	fi
done

# Start a fresh X11 display and bring the Termux:X11 app to the front.
pkill -f com.termux.x11 >/dev/null 2>&1
termux-x11 :0 >/dev/null 2>&1 &
sleep 3
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1

proot-distro login "$DISTRO" --shared-tmp -- env DISPLAY=:0 bash /root/RunescapeClaude/phone/run-in-debian.sh
