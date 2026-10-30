# RuneLite on an Android phone

This runs the same client and plugins as `start-runelite.bat`, on a phone, inside the Termux app. It has not been
tested on a real phone yet, so expect some rough edges.

**What you get:** Shadow Tiles, Mad Angel, ToA Helper, Fairy Ring Labels and Wiki Gear Loadouts.
**What you don't:** 117 HD, Interaction Lights and the high-visibility key. They need desktop OpenGL, which phones
don't have, so the phone launcher leaves them out and RuneLite uses its normal graphics.

Expect it to be slower than on the PC and fiddly to control by touch. A Bluetooth mouse, or Samsung DeX with a mouse,
makes it much easier.

## 1. Install the apps

1. **Termux** from F-Droid (https://f-droid.org/packages/com.termux/) or GitHub
   (https://github.com/termux/termux-app/releases). Use the `arm64-v8a` APK. The Play Store version is not
   recommended.
2. **Termux:X11** from GitHub: https://github.com/termux/termux-x11/releases, the `app-arm64-v8a-debug.apk` file
   from the newest "nightly" release.

Android will ask you to allow installing apps from your browser or F-Droid. Allow it.

## 2. Stop Android killing it in the background

Android can close Termux while a game runs in another app. On a Galaxy:

1. Settings > About phone > Software information, tap **Build number** 7 times to turn on Developer options.
2. Settings > Developer options, turn on **Disable child process restrictions**.
3. Settings > Apps > Termux > Battery, choose **Unrestricted**. Do the same for Termux:X11.

## 3. Run the setup

Open Termux and type these one at a time (press Enter after each):

```
termux-setup-storage
pkg install -y curl
curl -fsSL https://raw.githubusercontent.com/tristinhill26-bit/RunescapeClaude/claude/runelite-117hd-lighting-dnp1u3/phone/setup-termux.sh | bash
```

`termux-setup-storage` asks for file access. Allow it, it lets RuneLite pick up your login file in step 4.

Setup downloads about 1.5 GB and builds the plugins once, so plan on 20 to 40 minutes on Wi-Fi with the phone
plugged in. It ends with "All set". If it stops with an error, screenshot the last lines and send them to Claude.
Running the same `curl` line again picks up where it left off.

## 4. Jagex account login (skip if you log in with a username and password)

RuneLite started this way can't open the Jagex Launcher, so it needs the login file from your PC:

1. On the PC, look for `credentials.properties` in `C:\Users\<you>\.runelite\`.
   If it's not there, open **RuneLite (configure)** from the Start menu, type `--insecure-write-credentials` in
   the Client arguments box and save. Then start RuneLite once from the Jagex Launcher and close it. The file
   appears in that folder.
2. Copy `credentials.properties` to the phone's **Download** folder (Google Drive, USB cable or Quick Share).
3. The next `runelite` launch moves it into RuneLite for you.

Treat that file like a password. Anyone who has it can log in to your account. If you ever lose the phone, log out of
all sessions from the Jagex account page.

## 5. Play

1. Open the **Termux:X11** app once (it says "Not connected" until the next step, that's normal), then switch back to Termux.
2. Type `runelite` and press Enter.

It switches to Termux:X11, turns the screen sideways and RuneLite opens full screen. The first start takes a few minutes. Each launch checks for
updates first, like the PC launcher does.

To quit, close RuneLite from its own menu or go back to Termux and press `Ctrl` + `C` on the Termux key bar.

## Tips

- **Screen size:** the first launch sets Termux:X11 to full screen and landscape, and the **Phone Mode** plugin
  keeps RuneLite filling the screen and turns on **Stretched Mode** so the game scales up to fill it, in fixed or
  resizable mode. To make things bigger or smaller, change Display scale in Termux:X11 Preferences > Output (higher
  is bigger, 200% is the starting point).
- **Touch controls:** tap to click, hold your finger down for the right-click menu, and use two fingers for
  the scroll wheel (zoom). These need Termux:X11's Touchscreen input mode on **Simulated touchscreen**, which the first
  launch sets for you.
- **Keyboard:** swipe in from the left edge in Termux:X11 to show the keyboard and extra keys.
- **Don't turn on GPU or 117 HD** in RuneLite's plugin list on the phone. They need OpenGL and will crash or show a
  black screen. The phone keeps its own RuneLite settings, so if that happens, reset them: delete `/root/.runelite/settings.properties` inside Debian (`proot-distro login debian` then
  `rm /root/.runelite/settings.properties`).
