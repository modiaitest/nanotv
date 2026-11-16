# NanoTV — Google Play Console Copy-Paste Listing Pack

All files needed to upload and submit **NanoTV** (`app.nanotv.tv`) are already built locally:
- **Signed Release App Bundle (`.aab`):** `app/build/outputs/bundle/release/app-release.aab`
- **Play Store Graphics (`playstore-assets/`):**
  - App Icon (`512x512`): `playstore-assets/app-icon-512x512.png`
  - Feature Graphic (`1024x500`): `playstore-assets/feature-graphic-1024x500.png`
  - Android TV Banner (`1280x720`): `playstore-assets/tv-banner-1280x720.png`
  - Android TV 16:9 Screenshots (`1920x1080`):
    - `playstore-assets/tv-screenshot-1-home-1920x1080.png`
    - `playstore-assets/tv-screenshot-2-parent-pin-1920x1080.png`
    - `playstore-assets/tv-screenshot-3-qr-curate-1920x1080.png`

---

## 1. Main Store Listing Text (Trademark & Policy Safe)

- **App name (max 30 chars):**
  `NanoTV: Family Video Manager`

- **Short description (max 80 chars):**
  `Parent-curated TV shelves with a 4-digit PIN gate, phone QR setup & zero Shorts.`

- **Full description (max 4,000 chars):**
  ```text
  NanoTV is a parent-managed media organizer designed for parents and guardians (18+) to build a calm, curated television library on Android TV.

  Instead of an endless algorithmic feed or rapid-fire vertical clips, NanoTV lets parents choose the exact public channels, playlists, and full-length videos available on their living room TV—locked behind a 4-digit Parent PIN.

  KEY FEATURES FOR PARENTS
  • 4-Digit Parent PIN Protection: Channel curation, remote web management, and app settings are locked behind a salted SHA-256 Parent PIN set during initial setup.
  • Effortless Phone QR Curation: Scan the on-screen QR code from any phone or laptop on your home Wi-Fi to add or remove channels, playlists, and individual videos in seconds.
  • Full-Length Episodes Only (No Shorts): Short-form vertical clips and endless marathon live loops are automatically excluded so household viewing stays intentional.
  • Clean D-Pad Android TV Navigation: Built natively for Android TV remotes with organized shelves, creator pages,Continue Watching resume bars, Favorites, Watch Later, and local-only library search.
  • Zero Accounts or Tracking: Your PIN, approved shelves, and watch history stay locally on your Android TV device.

  IMPORTANT ATTRIBUTION & TERMS
  NanoTV uses YouTube API Services to display public metadata and play embedded videos via the official YouTube IFrame Player. NanoTV is an independent utility and is not affiliated with, endorsed by, or sponsored by YouTube or Google LLC. By using NanoTV, users agree to be bound by the YouTube Terms of Service (https://www.youtube.com/t/terms) and acknowledge the Google Privacy Policy (https://policies.google.com/privacy).
  ```

---

## 2. Google Play Console Questionnaire Answers

- **Category:** `Tools` or `Entertainment` (Tools / Parental Utility recommended)
- **Form Factors (Advanced Settings → Form Factors):**
  - Add **Android TV** → Select **"Opt-in to Android TV"** (upload `tv-banner-1280x720.png` and the three `1920x1080` TV screenshots).
- **Target Audience & Content:**
  - Target age group: **18 and over** (NanoTV is a parent-configured household media management tool with a mandatory Parent PIN setup gate).
  - Could your store listing unintentionally appeal to children? **No** (dark geometric midnight/cyan utility branding).
- **Ads Declaration:**
  - Does your app contain ads? Select **Yes** *(Note: Google Play policy states that if an app embeds third-party video players like YouTube IFrame Player which may serve creator video ads, selecting "Yes — embedded video player may display third-party video ads" prevents ad-badge review misunderstandings).*
- **Data Safety:**
  - Does your app collect or share any of the required user data types? **No** (no user accounts, no analytics SDKs, PIN & shelves stored locally on-device).
- **App Access (For Google Play Reviewers):**
  - Select *"All functionality is available without special access"* (or note: *"On first launch, the reviewer sets their own 4-digit Parent PIN on-screen during Step 1 of setup"*).
