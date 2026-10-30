#!/data/data/com.termux/files/usr/bin/bash
# Sets up RuneLite with Claude's plugins on an Android phone. Run it inside the Termux app (see phone/README.md).
# It installs a Debian Linux inside Termux, Java and Git there, downloads this repo, builds it once, and adds a
# "runelite" command that opens the game in the Termux:X11 app. Running it again updates everything.
set -e

REPO_URL="https://github.com/tristinhill26-bit/RunescapeClaude.git"
BRANCH="claude/runelite-117hd-lighting-dnp1u3"
DISTRO="debian"

step() { echo; echo "==> $*"; }

# Wrapped in a function so bash reads the whole script before running any of it. It may arrive on standard input
# (curl ... | bash), and the commands below must not read from there.
main() {
	step "Updating Termux packages..."
	pkg update -y -o Dpkg::Options::=--force-confnew
	pkg upgrade -y -o Dpkg::Options::=--force-confnew

	step "Installing the X11 display server and the Linux installer..."
	pkg install -y x11-repo
	pkg install -y termux-x11-nightly proot-distro

	# Everything inside Debian is reached through proot-distro rather than its folder on disk, because where
	# proot-distro keeps that folder differs between versions.
	if ! proot-distro login "$DISTRO" -- true >/dev/null 2>&1; then
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
	proot-distro login "$DISTRO" -- bash -c "mkdir -p /root/.config/openbox && cp /root/RunescapeClaude/phone/openbox-rc.xml /root/.config/openbox/rc.xml"

	step "Building once now so the first launch is quicker (can take 10+ minutes on a phone)..."
	proot-distro login "$DISTRO" -- bash -c "cd /root/RunescapeClaude && ./gradlew --no-daemon testClasses"

	step "Adding the 'runelite' command..."
	proot-distro login "$DISTRO" -- cat /root/RunescapeClaude/phone/runelite-termux.sh > "$PREFIX/bin/runelite"
	chmod +x "$PREFIX/bin/runelite"

	echo
	echo "All set. Open the Termux:X11 app once, come back to Termux and type:  runelite"
}

main </dev/null
