#!/data/data/com.termux/files/usr/bin/bash
# Sets up RuneLite with Claude's plugins on an Android phone. Run it inside the Termux app (see phone/README.md).
# It installs a Debian Linux inside Termux, Java and Git there, downloads this repo, builds it once, and adds a
# "runelite" command that opens the game in the Termux:X11 app. Running it again updates everything.
set -e

REPO_URL="https://github.com/tristinhill26-bit/RunescapeClaude.git"
BRANCH="claude/runelite-117hd-lighting-dnp1u3"
DISTRO="debian"
ROOTFS="$PREFIX/var/lib/proot-distro/installed-rootfs/$DISTRO"

step() { echo; echo "==> $*"; }

step "Updating Termux packages (answer with Enter if it asks anything)..."
pkg update -y -o Dpkg::Options::=--force-confnew
pkg upgrade -y -o Dpkg::Options::=--force-confnew

step "Installing the X11 display server and the Linux installer..."
pkg install -y x11-repo
pkg install -y termux-x11-nightly proot-distro

if [ ! -d "$ROOTFS/root" ]; then
	step "Installing Debian (a few hundred MB, this takes a while)..."
	proot-distro install "$DISTRO"
else
	step "Debian is already installed."
fi

step "Installing Java, Git and a window manager inside Debian..."
proot-distro login "$DISTRO" -- bash -c '
	set -e
	export DEBIAN_FRONTEND=noninteractive
	apt-get update
	apt-get install -y default-jdk git openbox fonts-dejavu-core ca-certificates
'

step "Downloading Claude's plugins..."
proot-distro login "$DISTRO" -- bash -c "
	set -e
	if [ -d /root/RunescapeClaude/.git ]; then
		cd /root/RunescapeClaude
		git fetch origin '$BRANCH'
		git checkout '$BRANCH'
		git pull --ff-only
	else
		git clone --branch '$BRANCH' '$REPO_URL' /root/RunescapeClaude
	fi
"

# Openbox opens every window full screen and without a title bar, so RuneLite fills the phone's screen.
mkdir -p "$ROOTFS/root/.config/openbox"
cp "$ROOTFS/root/RunescapeClaude/phone/openbox-rc.xml" "$ROOTFS/root/.config/openbox/rc.xml"

step "Building once now so the first launch is quicker (can take 10+ minutes on a phone)..."
proot-distro login "$DISTRO" -- bash -c "cd /root/RunescapeClaude && ./gradlew --no-daemon testClasses"

step "Adding the 'runelite' command..."
cp "$ROOTFS/root/RunescapeClaude/phone/runelite-termux.sh" "$PREFIX/bin/runelite"
chmod +x "$PREFIX/bin/runelite"

echo
echo "All set. Open the Termux:X11 app once, come back to Termux and type:  runelite"
