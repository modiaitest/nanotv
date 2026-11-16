#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GH_BIN="/opt/homebrew/bin/gh"
NPX_BIN="/opt/homebrew/bin/npx"
export PATH="/opt/homebrew/bin:$PATH"

echo "========================================================"
echo "  NanoTV — Automated Cloud & Play Store Bundle Prep"
echo "========================================================"

# 1. GitHub Repo + Free HTTPS Privacy Policy Hosting (GitHub Pages)
if ! "$GH_BIN" auth status >/dev/null 2>&1; then
    echo ""
    echo "👉 Step 1/3: Sign into GitHub in your browser..."
    "$GH_BIN" auth login -h github.com -p https -w
fi

GH_USER=$("$GH_BIN" api user -q .login)
if ! git remote get-url origin >/dev/null 2>&1; then
    echo "Creating public GitHub repository ${GH_USER}/nanotv..."
    "$GH_BIN" repo create nanotv --public --source=. --remote=origin --push
else
    git push -u origin main
fi

"$GH_BIN" api -X POST "repos/${GH_USER}/nanotv/pages" -f "source[branch]=main" -f "source[path]=/" >/dev/null 2>&1 || true
PRIVACY_URL="https://${GH_USER}.github.io/nanotv/PRIVACY_POLICY.html"
echo "✅ GitHub Repo: https://github.com/${GH_USER}/nanotv"
echo "✅ Live Privacy Policy URL (for Play Console): ${PRIVACY_URL}"

# 2. Cloudflare Worker Edge Proxy Deployment (Zero API Key in APK!)
echo ""
echo "👉 Step 2/3: Deploying NanoTV Cloudflare Worker Edge Proxy..."
cd "${SCRIPT_DIR}/backend"

WHOAMI_OUT=$("$NPX_BIN" --yes wrangler whoami 2>&1 || true)
if echo "$WHOAMI_OUT" | grep -Eqi "not authenticated|login"; then
    echo "Opening Cloudflare login in your browser..."
    "$NPX_BIN" --yes wrangler login
fi

# Deploy Worker interactively first (allows choosing a free *.workers.dev subdomain if first time)
rm -f /tmp/nanotv_wrangler_deploy.log
/usr/bin/script -q /tmp/nanotv_wrangler_deploy.log "$NPX_BIN" --yes wrangler deploy

WORKER_URL=$(grep -Eo 'https://[a-zA-Z0-9._-]+\.workers\.dev' /tmp/nanotv_wrangler_deploy.log | head -n 1 || true)

# Upload YOUTUBE_API_KEY secret after Worker exists
PARENT_PROPS="${SCRIPT_DIR}/../local.properties"
SAVED_YT_KEY=""
if [ -f "$PARENT_PROPS" ]; then
    SAVED_YT_KEY=$(sed -n 's/^YOUTUBE_API_KEY=//p' "$PARENT_PROPS" | head -n 1)
fi

if [ -n "$SAVED_YT_KEY" ]; then
    printf "%s" "$SAVED_YT_KEY" | "$NPX_BIN" --yes wrangler secret put YOUTUBE_API_KEY
else
    "$NPX_BIN" --yes wrangler secret put YOUTUBE_API_KEY
fi

cd "$SCRIPT_DIR"
if [ -n "$WORKER_URL" ]; then
    echo "✅ Live Cloudflare Edge Proxy: ${WORKER_URL}"
    if grep -q "^NANO_BACKEND_URL=" "${SCRIPT_DIR}/local.properties" 2>/dev/null; then
        sed -i '' "s|^NANO_BACKEND_URL=.*|NANO_BACKEND_URL=${WORKER_URL}|" "${SCRIPT_DIR}/local.properties"
    else
        echo "NANO_BACKEND_URL=${WORKER_URL}" >> "${SCRIPT_DIR}/local.properties"
    fi
fi

# 3. Rebuild Final Signed Play Store Release App Bundle (.aab) & Debug APK
echo ""
echo "👉 Step 3/3: Building signed Google Play App Bundle (.aab) wired to your live backend..."
"${SCRIPT_DIR}/gradlew" bundleRelease assembleDebug

# Copy refreshed AAB to Antigravity browser upload staging if directory exists
UPLOAD_STAGE="/Users/abhishekmodi/.gemini/antigravity/brain/84e70a8f-dfb6-4ea4-a795-f0d8da2999e6/browser_sandbox/uploads"
if [ -d "$UPLOAD_STAGE" ]; then
    cp "${SCRIPT_DIR}/app/build/outputs/bundle/release/app-release.aab" "${UPLOAD_STAGE}/app-release.aab"
fi

echo ""
echo "========================================================"
echo "🎉 ALL CLOUD & PLAY STORE ARTIFACTS READY!"
echo "   • Signed Play Store Bundle: ${SCRIPT_DIR}/app/build/outputs/bundle/release/app-release.aab"
echo "   • Play Store Graphics Dir:  ${SCRIPT_DIR}/playstore-assets/"
echo "   • Copy-Paste Listing Text:  ${SCRIPT_DIR}/PLAY_STORE_LISTING.md"
echo "   • Privacy Policy URL:       ${PRIVACY_URL}"
echo "========================================================"
