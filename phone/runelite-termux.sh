#!/data/data/com.termux/files/usr/bin/bash
# Opens RuneLite with Claude's plugins in the Termux:X11 app. The "runelite" command that phone/setup-termux.sh adds
# runs the newest copy of this script from the repo.
DISTRO="debian"

# A credentials.properties file from the PC (for Jagex accounts) can be dropped in the phone's Downloads folder.
# Move it into Debian's RuneLite folder so it isn't left lying around.
for CREDS in "$HOME/storage/downloads/credentials.properties" "/sdcard/Download/credentials.properties"; do
	if [ -f "$CREDS" ]; then
		proot-distro login "$DISTRO" -- bash -c "mkdir -p /root/.runelite && cat > /root/.runelite/credentials.properties" < "$CREDS" \
			&& rm -f "$CREDS" && echo "Moved your Jagex account login into RuneLite."
		break
	fi
done

# Start a fresh X11 display and bring the Termux:X11 app to the front.
# Matched on the start of the X server's process name only, because this script runs as "bash -c <its text>", and a
# looser pattern would match the script's own process and stop it.
pkill -f '^termux-x11 ' >/dev/null 2>&1
termux-x11 :0 >/dev/null 2>&1 &
sleep 3
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1

# Set Termux:X11 up like the mobile game the first time: full screen in landscape, the screen kept on, and the display
# at half the phone's resolution so the game's text and buttons come out twice as big. Done once, so changes made in
# Termux:X11's own settings stick. It can only change settings while the app is open, hence after the line above.
PREFS_DONE="$HOME/.config/runelite-phone/termux-x11-prefs-v1"
if [ ! -f "$PREFS_DONE" ]; then
	sleep 2
	if timeout 10 termux-x11-preference "fullscreen"="true" "hideCutout"="true" "forceOrientation"="landscape" \
		"displayResolutionMode"="scaled" "displayScale"="200" "screenIdleTimeout"="never" >/dev/null 2>&1; then
		mkdir -p "$(dirname "$PREFS_DONE")" && touch "$PREFS_DONE"
	else
		echo "Couldn't change Termux:X11's settings, so it may not be full screen. Try running runelite again."
	fi
fi

proot-distro login "$DISTRO" --shared-tmp -- env DISPLAY=:0 bash /root/RunescapeClaude/phone/run-in-debian.sh
