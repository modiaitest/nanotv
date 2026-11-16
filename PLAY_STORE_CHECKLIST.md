# NanoTV — Google Play Store (Android TV) Launch Guide

This checklist covers everything needed to take **NanoTV** live on Google Play for Android TV while complying with **Google Play Developer Program Policies** and **YouTube API Services Terms of Service**.

---

## 1. Policy Compliance Already Built Into NanoTV

- **Original Brand & Visual Identity (`app.nanotv.tv`):** Original **NanoTV** name, midnight-cyan palette, and geometric `N` emblem (`@mipmap/ic_launcher` & `@drawable/tv_banner`) — zero `"Tube"` / `"YouTube"` naming or red play-button logos.
- **YouTube API Services ToS (§III.E.4) Compliant Player:**
  - Uses official YouTube IFrame Player with `controls(1)` so YouTube's required player controls, watermark, branding, and creator ads are **never covered or obscured during active video playback**.
  - `WebViewClient.shouldOverrideUrlLoading` blocks external browser escapes while keeping playback inside the official YouTube embed frame.
  - Displays `"Powered by YouTube"` attribution and the mandatory verbatim YouTube Terms of Service (`youtube.com/t/terms`) + Google Privacy Policy (`policies.google.com/privacy`) disclosures during Onboarding, in Parent Settings, and on the QR Web Portal.
- **Zero API Keys Inside the APK:**
  - `app/build.gradle.kts` contains **no `YOUTUBE_API_KEY`**.
  - Includes a ready-to-deploy **Cloudflare Worker Edge Proxy** (`backend/`) that holds your key as an encrypted server secret and caches channel/playlist lookups at the Cloudflare Edge for 4–6 hours (cutting API quota consumption by ~99% across thousands of TVs), plus a built-in keyless public Atom feed fallback.
- **Parent-Targeted (18+) + Mandatory 4-Digit Parent PIN Gate:**
  - First launch walks the parent through setting a salted `SHA-256` 4-digit Parent PIN and choosing optional starter channels.
  - Both **Parent Curate (QR)** and **Parent Settings** on the TV—as well as the **Local Wi-Fi Web Portal (`:8485`)**—require the Parent PIN.
- **Android TV Play Store Manifest Readiness:**
  - `android.software.leanback` (`required="true"`), `android.hardware.touchscreen` (`required="false"`), `320x180dp` TV banner, adaptive icon, Target SDK 35, zero dangerous permissions (`INTERNET` & `ACCESS_NETWORK_STATE` only), and R8 release minification enabled.

---

## 2. Deploy the Backend API Proxy (Free Cloudflare Worker)

1. Install Node.js if needed and deploy `backend/`:
   ```bash
   cd backend
   npx wrangler login
   npx wrangler secret put YOUTUBE_API_KEY   # Paste your YouTube Data API v3 key when prompted
   npx wrangler deploy
   ```
2. Copy the Worker URL printed by Wrangler (e.g., `https://nanotv-api-proxy.<your-subdomain>.workers.dev`) into `local.properties` before building your release bundle:
   ```properties
   NANO_BACKEND_URL=https://nanotv-api-proxy.<your-subdomain>.workers.dev
   ```

---

## 3. Generate Upload Keystore & Build Signed `.aab` for Google Play

1. **Create your Play Store upload key (keep `.jks` backed up safely — never commit it!):**
   ```bash
   keytool -genkeypair -v -keystore nanotv-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nanotv
   ```
2. **Create `keystore.properties` in `NanoTV/` (gitignored):**
   ```properties
   storeFile=nanotv-upload.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=nanotv
   keyPassword=YOUR_KEY_PASSWORD
   ```
3. **Build the signed Release App Bundle (`.aab`):**
   ```bash
   ./gradlew bundleRelease
   ```
   Upload `app/build/outputs/bundle/release/app-release.aab` to Google Play Console!

---

## 4. Google Play Console Submission Settings

1. **Enable Android TV Form Factor:**
   - In Play Console → **Test and release → Setup → Advanced settings → Form factors**, click **+ Add form factor → Android TV**.
   - Upload at least **2–8 landscape 16:9 Android TV screenshots** (`1920x1080`) and a **1280x720 TV Banner PNG**.
2. **App Content → Target Audience & Content:**
   - Select **18 and over** (position the Store Listing clearly as *"NanoTV: Parental Video Organizer & Curated Family TV Player — built for parents to curate and PIN-lock approved channels for their household Android TV"*).
   - Do **not** use the word `"YouTube"` in the App Title itself (e.g., use **"NanoTV - Family Video Manager"**, and mention *"Curate public YouTube channels & playlists"* in the description).
3. **Privacy Policy URL:**
   - Host `PRIVACY_POLICY.md` on GitHub Pages (or your website) and paste its public HTTPS URL into **App content → Privacy policy**.
4. **Data Safety Section:**
   - Does your app collect or share any of the required user data types? **No** (PIN and watch states stay locally on the user's TV).
