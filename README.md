# Gully Scorer — Android app (v1.2)

An offline scoring app for gully cricket. It tracks overs, balls, runs, wides, no-balls, free hits and wickets, and saves every match automatically. Each day gets its own matchday report, and the app keeps season stats across all your days.

**New in 1.2**
- 🏏 **Six rule is optional**: in match setup, choose whether a six is *6 runs*, *Out (0 runs)* or *6 runs + out*. On a free hit a six always counts as 6 runs.

**New in 1.1**
- 🤝 **Common player**: one player can bat and bowl for both teams.
- 📈 **Season stats**: all-time, 30-day and 7-day leaderboards, records, a count of Player of the Day awards, and player profiles.
- 🎤 **Voice scoring**: tap the mic and say "four", "chauka", "wide one", "no ball two", "bowled", "caught", "undo" and so on.

---

## Build the APK with GitHub Actions (recommended)

The **Build APK** workflow runs on every push to `main` and publishes the app under **Releases**.
- If **`GullyCricket.zip`** is in the repository root, the app is built **from the zip**. To update the app, upload a new zip on github.com with **Add file → Upload files**, using the same name.
- If there's no zip, the app is built from the files in the repository.

**Get the app:** on the repository home page, go to **Releases → latest "Gully Scorer build N" → GullyScorer.apk**, then open it on your phone.
Every build has a higher version number and is signed with the same key (`app/gully-debug.keystore`). That means each new APK **installs over the old one and keeps your saved matches**.

> If you already installed a build that was signed with a different key, uninstall it once. Back up your matches first with Home → Backup.

**One-time codespace setup** (puts the workflow file in place):
```bash
cd /workspaces/$(basename "$PWD")
git fetch origin && git reset --hard origin/main
rm -rf /tmp/gc && unzip -q -o GullyCricket.zip -d /tmp/gc && cp -rT /tmp/gc/GullyCricket . && rm -rf /tmp/gc
chmod +x gradlew build-apk.sh .devcontainer/setup-android.sh
git add -A && git commit -m "Install Gully Scorer package" && git push
```

## Build in GitHub Codespaces (optional)
In the codespace terminal, run `bash .devcontainer/setup-android.sh` and then `./build-apk.sh`. When it finishes, right-click **GullyScorer.apk → Download**. The script picks Java 17 or 21 automatically.

## Alternative: Android Studio
Use **File → Open** → choose this folder → wait for Gradle sync → **Run ▶**, or **Build → Build APK(s)**.

## How it's built
- `app/src/main/assets/index.html` is the whole app: plain HTML, CSS and JS that works offline. To change the app, edit this file and rebuild.
- `MainActivity.java` shows that page in a WebView and stores the data in the WebView's local storage. It also connects the page to Android for these features:
  - **Voice**: uses Android's speech recogniser in Indian English (`en-IN`), so Hinglish words like *chauka*, *chhakka* and *ek* work.
  - **Share**: sends the day card image or the scorecard text through Android's share sheet (WhatsApp and others).
  - **Backup**: shares all your matches as a `.json` file. **Restore** opens the file picker.

## Voice commands
| Say | Records |
|---|---|
| dot · zero · no run | 0 |
| single · one · ek | 1 |
| two · double · do | 2 |
| three · teen | 3 |
| four · chauka · boundary | 4 |
| six · sixer · chhakka | 6 |
| wide · wide one (1 run ran) | Wide (+ runs ran) |
| no ball · no ball four | No-ball (+ runs off bat) |
| bye two · leg bye | Byes / leg byes |
| bowled | Wicket, recorded straight away |
| caught · run out · stumped · one tip one hand · out | Opens the wicket screen with that type picked so you can confirm |
| undo · swap strike | Undo last ball · swap batters |

Every voice entry shows a confirmation like 🎤 "chauka" → 4 runs. If it mishears, say "undo".
