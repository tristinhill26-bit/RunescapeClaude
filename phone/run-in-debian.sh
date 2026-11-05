#!/bin/bash
# Runs inside Debian on the phone: updates the plugins, then builds and opens RuneLite without 117 HD.
cd /root/RunescapeClaude || exit 1

pgrep -x openbox >/dev/null || (openbox >/dev/null 2>&1 &)

echo "Checking for updates..."
PULL_FAILED=
if ! git pull --ff-only; then
	PULL_FAILED=1
	echo "Couldn't update, so opening the version you already have."
fi

# Pick up any changes to the window settings from this update (they apply from the next launch).
cp -f phone/openbox-rc.xml /root/.config/openbox/rc.xml 2>/dev/null

echo "Building and opening RuneLite. The first start after an update takes a while on a phone..."
if ! ./gradlew runLite && [ -n "$PULL_FAILED" ]; then
	echo "Trying again with the RuneLite version that's already downloaded, in case you're offline..."
	./gradlew runLite --offline
fi
