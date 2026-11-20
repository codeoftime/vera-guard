# VERA Guard

Fall/crash detection, silent SOS, an AI Responder Brief, nearby hospitals, Safe Journey with a server-side dead-man's-switch,
and "Ask VERA" first-aid chat. Native Android app (Kotlin + Jetpack Compose), Node/Express backend, public website (EN / हिन्दी / ಕನ್ನಡ).

```
android/   Kotlin app (min SDK 26, target 34, Hilt, Room on SQLCipher, EncryptedSharedPreferences)
backend/   Node 22 + Express + SQLite (node:sqlite), watchdog with DB-backed timers, website in backend/public
tools/     gen_strings.py: generates and verifies the en/hi/kn string resources
.github/   CI that builds, tests and publishes VERA-Guard.apk
```

> **Not a medical or emergency service.** VERA Guard assists but cannot guarantee that help arrives. In an emergency call 112 / 108.

## 1. Build the APK

Requirements: JDK 17, Android SDK 34 (platform + build-tools). Android Studio (Koala or newer) installs both.

```bash
cd android
./gradlew :app:testDebugUnitTest :app:assembleRelease -PbackendUrl=https://YOUR-BACKEND-URL
# output:
#   android/app/build/outputs/apk/release/VERA-Guard.apk
```

* `-PbackendUrl` is baked into the app as the default server. It can also be changed later in **Profile → Settings → Server URL**.
* Without a release keystore the APK is signed with the local debug key, which is fine for sideloading. To use your own key set
  `VERA_KEYSTORE`, `VERA_KEYSTORE_PASSWORD`, `VERA_KEY_ALIAS`, `VERA_KEY_PASSWORD` before building.
* **No machine with the SDK?** Push this repo to GitHub. `.github/workflows/build-apk.yml` runs the backend tests, the Android unit tests
  and `assembleRelease`, uploads the APK as a workflow artifact and publishes it as the release **latest**:
  `https://github.com/<you>/<repo>/releases/download/latest/VERA-Guard.apk`.
  Set the repository variable `BACKEND_URL` first (Settings → Secrets and variables → Actions → Variables), or pass it when running the workflow by hand.
  For updates that install over an older build keep the same signing key: add secrets `VERA_KEYSTORE_B64` (base64 of your .jks) and `VERA_KEYSTORE_PASSWORD`
  (the key alias must be `vera`).

Install on a phone: open the APK link in the phone's browser, allow "install unknown apps" for that browser when asked, then open the file.

## 2. Run the backend

```bash
cd backend
npm ci
npm test                      # 19 tests
cp .env.example .env          # optional: add keys (see below)
PUBLIC_URL=http://localhost:8080 npm start
```

Verify: `curl localhost:8080/health` returns `{"ok":true,...}`; open http://localhost:8080 for the website.

| Variable | Purpose |
|---|---|
| `PUBLIC_URL` | Base URL used in SMS tracking links (required in production) |
| `DB_PATH` | SQLite file. Use a persistent disk: the watchdog timers live in this DB and survive restarts |
| `LLM_API_KEY`, `LLM_MODEL` | AI summary and Ask VERA. Without a key the brief is fully deterministic and chat uses offline first-aid cards |
| `TWILIO_SID/TOKEN/FROM` | Server-sent SMS (journey watchdog / overdue alerts). Without them messages are only written to the `outbox` table |
| `PLACES_PROVIDER` | `osm` (default, free Overpass) or `google` with `GOOGLE_PLACES_KEY` |
| `APK_URL` | Where `/VERA-Guard.apk` comes from if the file is not in `backend/public/`. The server downloads and caches it |

### Deploy (Render example)

`render.yaml` and `backend/Dockerfile` are included. In Render: New → Blueprint → select the repo, then set `PUBLIC_URL` (e.g. `https://vera-guard.onrender.com`)
and `APK_URL` (the GitHub release asset above). The **Download APK** button on the home page then serves the APK from your own domain.
Any Docker host works the same way. The free Render plan sleeps and has no persistent disk: fine for a demo, not for the dead-man's-switch.

## 3. Verify each part

1. **Backend**: `npm test` passes; `/health` works; open `/` and switch language EN / हिन्दी / ಕನ್ನಡ.
2. **App install**: install the APK, open it, grant the checklist items on Home (notifications, location, SMS first).
3. **Profile**: add your details and a contact; Sharing tab shows exactly what responders would see.
4. **Demo mode** (Profile → Settings → Demo mode, enter your own number): **it never calls 112 / 108** and only messages your demo number.
   * Home → *Simulate fall* with the severity slider: countdown screen, "I am okay" cancels, otherwise the escalation pipeline runs.
   * Open the tracking link from the SMS: the Responder Brief page. Use *Acknowledge* and watch "Responder on the way" appear in the app.
5. **Silent SOS**: shake the phone (about 4 hard shakes in 3 s), the home-screen widget, the quick-settings tile, or the notification button.
   Set a cancel PIN and a duress PIN under Profile → Security. The duress PIN shows "cancelled" while the alert continues silently.
6. **Safe Journey**: Journey tab, search a destination, choose a follower, start. In demo mode use *Simulate route deviation* (check-in prompt, then alert)
   and *Simulate phone dying* (the **server** alerts followers after the watchdog time; 20 s in demo mode).
7. **Ask VERA**: ask a first-aid question; switch the phone to airplane mode and ask again to see the offline answer.

## 4. Keep protection alive (battery optimization)

Android vendors aggressively stop background apps. In the app, Home → checklist → *Battery optimization off*, then:

* **Xiaomi / Redmi / POCO (MIUI, HyperOS)**: Settings → Apps → Manage apps → VERA Guard → *Autostart* ON; *Battery saver* → **No restrictions**;
  in Recents long-press the app card and choose **Lock**.
* **Samsung (One UI)**: Settings → Battery → Background usage limits → *Never sleeping apps* → add VERA Guard; also turn off *Put unused apps to sleep*.
* **OnePlus (OxygenOS)**: Settings → Apps → VERA Guard → Battery usage → **Allow background activity**; enable *Auto-launch*; lock the app in Recents.
* **Realme / Oppo (ColorOS)**: Settings → Apps → App management → VERA Guard → *Auto-start* ON and Battery → **Allow background activity**; lock in Recents.
* All: location permission **Allow all the time**, and allow *full-screen notifications* (Android 14).

## 5. Known limitations (honest list)

* **Not compiled in the authoring sandbox.** The Android sources were written and reviewed without an Android SDK, so the first build may need small fixes.
  The backend (19 tests) and the detector thresholds (simulated in Python against fall / crash / walking / running / bus traces) were verified. Run the unit tests first.
* Fall detection uses phone sensors only; real-world accuracy depends on how the phone is carried. Thresholds are constants scaled by the sensitivity setting.
* Android does not let apps dial emergency numbers silently. After an unanswered fall alert the alert screen tries `CALL` for 112 and always shows a large manual **CALL** button.
  Silent triggers never auto-dial the ambulance (the call screen would reveal the phone). Demo mode never dials 112 / 108.
* The second SOS call (30 s later) only happens if the first call has ended; Android does not tell apps whether a call was answered.
* Background audio recording may be blocked by Android 11+ and some vendors; it is best effort. Clips are encrypted on device before upload. Photo capture is not implemented.
* Voice keyword ("help me", "बचाओ", "ಸಹಾಯ") uses the system speech recogniser: experimental and battery-hungry; may need internet.
* "Pick destination on map" is not implemented (search only). The tracking page polls every 10 s (no WebSocket).
* The public OSRM demo server and Overpass are rate-limited; point `OSRM_URL` / `OVERPASS_URL` at your own instances or use `PLACES_PROVIDER=google` for production.
* The landing page uses a CSS phone mock-up, not real screenshots.
* `usesCleartextTraffic` is on so you can test against `http://192.168.x.x`; turn it off in `AndroidManifest.xml` for a production build with an https backend.

## Privacy

Profile fields are shared only when enabled under **Profile → Sharing**; location is shared only during an active alert or journey. Data on the phone is encrypted
(Keystore-backed preferences, SQLCipher database). **Profile → Security → Delete all my data** wipes the phone and removes sessions/journeys from the server.
Sessions are also purged by server retention (`SESSION_TTL_HOURS`, default 72 h).
