# NanoTV — Parent-Managed Curated TV Player for Android TV

**NanoTV** is a Play-Store-ready Android TV application built with **Kotlin + Jetpack Compose for TV**. It enables parents and guardians to curate a distraction-free household TV library from public channels, playlists, and full-length videos—protected behind a **4-digit Parent PIN** and **QR-code phone management**.

---

## Architecture Highlights

- **`app/` (`app.nanotv.tv`):** Native Android TV client with first-launch Parent Onboarding, salted `SHA-256` 4-digit Parent PIN protection, PIN-gated Local Wi-Fi QR Curation Portal (`:8485`), Shorts exclusion, D-pad pause timeline scrubbing, and a YouTube API ToS §III.E.4 compliant embedded player (`controls=1`, zero active-playback viewport overlays, external navigation containment, and `"Powered by YouTube"` attribution).
- **`backend/`:** Edge-cached **Cloudflare Worker** proxy that keeps `YOUTUBE_API_KEY` strictly in server-side secrets (never inside the client APK) and caches channel/playlist metadata across users.
- **Compliance & Release Docs:** See [PLAY_STORE_CHECKLIST.md](PLAY_STORE_CHECKLIST.md) and [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

---

## Quick Start (Local Build & Test)

```bash
cp local.properties.example local.properties
# Edit sdk.dir in local.properties (no API key needed in client!)
./gradlew testDebugUnitTest assembleDebug
./install-to-tv.sh <YOUR_ANDROID_TV_IP>
```
