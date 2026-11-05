# HP & Prayer phone app

Shows your current and max Hitpoints and Prayer from RuneLite on your phone, updated every game tick. The Hitpoints
bar turns orange below half and red below a quarter, and the Prayer bar lights up with **ON** while a prayer is active.

It works with the **Phone HP & Prayer** plugin in this repo, which runs a tiny web server inside RuneLite on your PC.
The phone connects to it over your home Wi-Fi; nothing goes through the internet.

## 1. Turn on the plugin

The plugin is loaded with the others when you start RuneLite with `start-runelite.bat`. Open the plugin list, search
for **Phone HP & Prayer** and turn it on. When you log in, a chat message tells you the address to use, e.g.

> Phone HP & Prayer: in the phone app, connect to 192.168.1.23, port 8765.

The first time, Windows may ask whether Java can use the network. Tick **Private networks** (and **Public networks** too if
the PC's network is set to Public) and click **Allow**.
If you missed that prompt, search Windows for *Allow an app through Windows Firewall* and tick *Private* for Java
(OpenJDK Platform binary).

To check the connection without the app, open `http://192.168.1.23:8765/` (your address) in the phone's browser. You
should see your stats as text.

## 2. Install the app

Every change to the app is built automatically by GitHub and published as the **phone-app** release:

1. On your phone, open
   [github.com/tristinhill26-bit/RunescapeClaude/releases/tag/phone-app](https://github.com/tristinhill26-bit/RunescapeClaude/releases/tag/phone-app)
   (sign in to GitHub if the repository is private).
2. Tap **HP-and-Prayer.apk** to download it, then open the download.
3. Android asks whether your browser may install unknown apps. Allow it, go back, and tap **Install**.
   Samsung may also show a *Auto Blocker* message; it can be turned off for the install under
   **Settings > Security and privacy > Auto Blocker**.

Later versions install over the old one, because every build is signed with the same key (`debug.keystore`).

## 3. Connect

Open **HP & Prayer**, type the address from the chat message (e.g. `192.168.1.23`, port `8765`) and tap
**Connect**. The app remembers it, keeps the screen on while open, and reconnects by itself if RuneLite restarts or
the Wi-Fi drops. Tap **PC** to change the address later.

If it says *No answer*, check the phone is on the same Wi-Fi as the PC (not mobile data or a guest network), and that
Windows Firewall allows Java. If it says *refused*, the plugin is off or uses a different port.

## Building it yourself

Open the `android-app` folder in Android Studio and press Run, or with the Android SDK installed:

```
cd android-app
./gradlew assembleRelease
```

The APK is in `app/build/outputs/apk/release/`.

## How the connection works

The plugin listens on port 8765 (changeable in its settings) on every network address of the PC:

- `GET /events` is a [Server-Sent Events](https://developer.mozilla.org/docs/Web/API/Server-sent_events) stream. Each
  update is one line, `data: {"loggedIn":true,"name":"...","hp":45,"maxHp":99,"prayer":12,"maxPrayer":77,"prayerActive":true}`,
  sent whenever a value changes, plus a `: ping` line every 5 seconds so both sides notice a dead connection.
- `GET /` or `GET /stats` answers once with the latest JSON.

Anyone on your Wi-Fi can read these numbers, and nothing else: the server only sends stats and ignores anything sent
to it.
