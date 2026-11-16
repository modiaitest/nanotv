const YT_BASE = "https://www.googleapis.com/youtube/v3";
const MIN_NON_SHORT_SECONDS = 76;

function parseIsoDuration(iso) {
  if (!iso) return 0;
  const m = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/.exec(iso.trim());
  if (!m) return 0;
  return (
    Number(m[1] || 0) * 86400 +
    Number(m[2] || 0) * 3600 +
    Number(m[3] || 0) * 60 +
    Number(m[4] || 0)
  );
}

function formatDuration(secs) {
  if (secs <= 0) return "";
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  const pad = (n) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

function isShort(secs, title = "", desc = "") {
  if (secs > 0 && secs < MIN_NON_SHORT_SECONDS) return true;
  const text = `${title} ${desc}`.toLowerCase();
  return text.includes("#shorts") || text.includes("#short");
}

function bestThumb(thumbnails, fallbackVid = "") {
  return (
    thumbnails?.high?.url ||
    thumbnails?.standard?.url ||
    thumbnails?.maxres?.url ||
    thumbnails?.medium?.url ||
    thumbnails?.default?.url ||
    (fallbackVid ? `https://i.ytimg.com/vi/${fallbackVid}/hqdefault.jpg` : "")
  );
}

async function cachedYtGet(pathAndQuery, apiKey, ttlSeconds, ctx) {
  const cacheKeyUrl = `https://nanotv-edge-cache.internal/${pathAndQuery}`;
  const cache = caches.default;
  const cacheReq = new Request(cacheKeyUrl, { method: "GET" });
  const hit = await cache.match(cacheReq);
  if (hit) return hit.json();

  const sep = pathAndQuery.includes("?") ? "&" : "?";
  const upstreamUrl = `${YT_BASE}/${pathAndQuery}${sep}key=${encodeURIComponent(apiKey)}`;
  const resp = await fetch(upstreamUrl);
  if (!resp.ok) {
    throw new Error(`YouTube upstream error ${resp.status}`);
  }
  const data = await resp.json();
  const toCache = new Response(JSON.stringify(data), {
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": `public, max-age=${ttlSeconds}`,
    },
  });
  ctx.waitUntil(cache.put(cacheReq, toCache));
  return data;
}

function jsonResponse(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    },
  });
}

export default {
  async fetch(request, env, ctx) {
    if (request.method === "OPTIONS") {
      return jsonResponse({ ok: true });
    }

    const url = new URL(request.url);
    if (url.pathname === "/v1/health") {
      return jsonResponse({ ok: true, service: "nanotv-api-proxy" });
    }

    const apiKey = env.YOUTUBE_API_KEY;
    if (!apiKey) {
      return jsonResponse({ ok: false, error: "YOUTUBE_API_KEY secret not configured on Worker" }, 500);
    }

    try {
      if (request.method === "GET" && url.pathname === "/v1/resolve-handle") {
        const handle = (url.searchParams.get("handle") || "").trim();
        if (!handle) return jsonResponse({ ok: false, error: "Missing handle" }, 400);
        const data = await cachedYtGet(
          `channels?part=id,snippet&forHandle=${encodeURIComponent(handle)}`,
          apiKey,
          86400,
          ctx
        );
        const ch = data.items?.[0];
        if (!ch) return jsonResponse({ ok: false, error: `Channel ${handle} not found` }, 404);
        return jsonResponse({
          ok: true,
          channelId: ch.id,
          title: ch.snippet?.title || handle,
        });
      }

      if (request.method === "GET" && url.pathname === "/v1/search") {
        const q = (url.searchParams.get("q") || "").trim();
        if (!q) return jsonResponse([]);
        const searchData = await cachedYtGet(
          `search?part=snippet&type=video&safeSearch=strict&videoEmbeddable=true&maxResults=18&q=${encodeURIComponent(q)}`,
          apiKey,
          3600,
          ctx
        );
        const ids = [...new Set((searchData.items || []).map((i) => i.id?.videoId).filter(Boolean))];
        if (ids.length === 0) return jsonResponse([]);

        const vData = await cachedYtGet(
          `videos?part=snippet,contentDetails,status&id=${encodeURIComponent(ids.join(","))}`,
          apiKey,
          14400,
          ctx
        );
        const results = (vData.items || [])
          .map((v) => {
            const secs = parseIsoDuration(v.contentDetails?.duration);
            if (v.status?.embeddable === false || v.status?.privacyStatus === "private") return null;
            if (secs <= 0 || isShort(secs, v.snippet?.title, v.snippet?.description)) return null;
            return {
              videoId: v.id,
              title: v.snippet?.title || "",
              channelId: v.snippet?.channelId || "",
              channelTitle: v.snippet?.channelTitle || "",
              durationSeconds: secs,
              durationFormatted: formatDuration(secs),
              thumbnailUrl: bestThumb(v.snippet?.thumbnails, v.id),
            };
          })
          .filter(Boolean);
        return jsonResponse(results);
      }

      if (request.method === "POST" && url.pathname === "/v1/sync") {
        const body = await request.json();
        const sources = Array.isArray(body.sources) ? body.sources.slice(0, 40) : [];

        const shelves = [];
        const allVideoIds = new Set();

        for (const src of sources) {
          if (src.sourceType === "VIDEO" && src.targetId) {
            allVideoIds.add(src.targetId);
            let shelf = shelves.find((s) => s.playlistId === `vid_shelf_${src.shelfTitle}`);
            if (!shelf) {
              shelf = {
                playlistId: `vid_shelf_${src.shelfTitle}`,
                title: src.shelfTitle || "Parent Hand-Picked",
                description: "Parent-approved videos",
                videoIds: [],
              };
              shelves.push(shelf);
            }
            shelf.videoIds.push(src.targetId);
          } else if (src.sourceType === "CHANNEL" && src.targetId) {
            const uploadsId = src.targetId.startsWith("UC")
              ? "UU" + src.targetId.slice(2)
              : src.targetId;
            const itemsData = await cachedYtGet(
              `playlistItems?part=contentDetails&playlistId=${encodeURIComponent(uploadsId)}&maxResults=40`,
              apiKey,
              14400,
              ctx
            ).catch(() => ({ items: [] }));
            const vids = (itemsData.items || [])
              .map((i) => i.contentDetails?.videoId)
              .filter(Boolean);
            vids.forEach((id) => allVideoIds.add(id));
            shelves.push({
              playlistId: `ch_${src.targetId}`,
              title: src.shelfTitle || "Channel",
              description: "Approved channel shelf",
              videoIds: vids,
            });
          } else if (src.sourceType === "PLAYLIST" && src.targetId) {
            const itemsData = await cachedYtGet(
              `playlistItems?part=contentDetails&playlistId=${encodeURIComponent(src.targetId)}&maxResults=50`,
              apiKey,
              14400,
              ctx
            ).catch(() => ({ items: [] }));
            const vids = (itemsData.items || [])
              .map((i) => i.contentDetails?.videoId)
              .filter(Boolean);
            vids.forEach((id) => allVideoIds.add(id));
            shelves.push({
              playlistId: `pl_${src.targetId}`,
              title: src.shelfTitle || "Playlist",
              description: "Approved playlist shelf",
              videoIds: vids,
            });
          }
        }

        const idArray = [...allVideoIds];
        const validVideos = new Map();
        const channelIds = new Set();

        for (let i = 0; i < idArray.length; i += 50) {
          const chunk = idArray.slice(i, i + 50);
          const vData = await cachedYtGet(
            `videos?part=snippet,contentDetails,status&id=${encodeURIComponent(chunk.join(","))}`,
            apiKey,
            21600,
            ctx
          );
          for (const v of vData.items || []) {
            const secs = parseIsoDuration(v.contentDetails?.duration);
            if (v.status?.embeddable === false || v.status?.privacyStatus === "private") continue;
            if (secs <= 0 || isShort(secs, v.snippet?.title, v.snippet?.description)) continue;
            validVideos.set(v.id, { dto: v, secs });
            if (v.snippet?.channelId) channelIds.add(v.snippet.channelId);
          }
        }

        const chArray = [...channelIds];
        const channelsMap = new Map();
        for (let i = 0; i < chArray.length; i += 50) {
          const chunk = chArray.slice(i, i + 50);
          const cData = await cachedYtGet(
            `channels?part=id,snippet,brandingSettings&id=${encodeURIComponent(chunk.join(","))}`,
            apiKey,
            86400,
            ctx
          );
          for (const c of cData.items || []) {
            channelsMap.set(c.id, c);
          }
        }

        const channelsOut = [...channelsMap.values()].map((c) => ({
          channelId: c.id,
          title: c.snippet?.title || "Creator",
          description: c.snippet?.description || "",
          avatarUrl: bestThumb(c.snippet?.thumbnails),
          bannerUrl: c.brandingSettings?.image?.bannerExternalUrl || bestThumb(c.snippet?.thumbnails),
        }));

        const videosOut = [...validVideos.values()].map(({ dto, secs }) => {
          const ch = channelsMap.get(dto.snippet?.channelId);
          return {
            videoId: dto.id,
            title: dto.snippet?.title || "",
            description: dto.snippet?.description || "",
            thumbnailUrl: bestThumb(dto.snippet?.thumbnails, dto.id),
            channelId: dto.snippet?.channelId || "",
            channelTitle: dto.snippet?.channelTitle || "",
            channelAvatarUrl: bestThumb(ch?.snippet?.thumbnails),
            durationSeconds: secs,
            durationFormatted: formatDuration(secs),
            publishedAt: dto.snippet?.publishedAt || "",
          };
        });

        const playlistsOut = [];
        for (const s of shelves) {
          const approvedIds = s.videoIds.filter((id) => validVideos.has(id)).slice(0, 25);
          if (approvedIds.length > 0) {
            const firstVid = validVideos.get(approvedIds[0]).dto;
            playlistsOut.push({
              playlistId: s.playlistId,
              title: s.title,
              description: s.description,
              thumbnailUrl: bestThumb(firstVid.snippet?.thumbnails, firstVid.id),
              videoIds: approvedIds,
            });
          }
        }

        return jsonResponse({
          ok: true,
          playlists: playlistsOut,
          videos: videosOut,
          channels: channelsOut,
        });
      }

      return jsonResponse({ ok: false, error: "Not found" }, 404);
    } catch (e) {
      return jsonResponse({ ok: false, error: String(e.message || e) }, 500);
    }
  },
};
