// frontend/src/api/soccerApi.js
// API-FOOTBALL adapter with key rotation, Big-5+UCL filter, and small caches.

const BASE = "https://v3.football.api-sports.io";

// 🔑 keys (in order). Rotates on limit.
const API_KEYS = [
  "966cf5e0c0155b46dbe384d1f13569a4",
  "f455f45a10472840b22e65810565fbcc",
];
let keyIndex = 0;

const USE_HEADER_KEY = true;
const HEADER_NAME = "x-apisports-key";

// --- caches (2 minutes) ---
const FIXTURE_TTL_MS = 2 * 60 * 1000;
const EVENTS_TTL_MS  = 2 * 60 * 1000;
const fixturesCache = new Map(); // date -> { data, ts }
const eventsCache   = new Map(); // matchId -> { timeline, ts }

// Keep only: UCL + EPL + La Liga + Serie A + Bundesliga + Ligue 1
const KEEP_LEAGUE_IDS = new Set([2, 39, 140, 135, 78, 61]);

// =============== PUBLIC ===============

export async function getTodayFixtures(force = false) {
  const date = new Date().toISOString().slice(0, 10); // YYYY-MM-DD (UTC)
  const url = `${BASE}/fixtures?date=${date}&timezone=UTC`;

  const hit = fixturesCache.get(date);
  if (!force && hit && Date.now() - hit.ts < FIXTURE_TTL_MS) return hit.data;

  const json = await fetchJson(url);
  const data = normalizeFixtures(json);
  fixturesCache.set(date, { data, ts: Date.now() });
  return data;
}

// Events are fetched on demand (used by the "Details" flow)
export async function getMatchEvents(matchId, { force = false } = {}) {
  const hit = eventsCache.get(matchId);
  if (!force && hit && Date.now() - hit.ts < EVENTS_TTL_MS) return hit;

  const url = `${BASE}/fixtures/events?fixture=${matchId}`;
  const evJson = await fetchJson(url);
  const timeline = normalizeEventsOnly(evJson);
  const data = { timeline, ts: Date.now() };
  eventsCache.set(matchId, data);
  return data;
}

// =============== INTERNAL FETCH ===============

async function fetchJson(url) {
  // try current key
  let res = await fetch(url, fetchInit());
  let json = await tryJson(res);

  // A) Real HTTP 429
  if (res.status === 429) {
    rotateKey();
    res = await fetch(url, fetchInit());
    json = await tryJson(res);
  }

  // B) API-FOOTBALL sometimes returns 200 with { errors: {...}, response: [] }
  if (hasApiFootballError(json)) {
    rotateKey();
    const res2 = await fetch(url, fetchInit());
    const json2 = await tryJson(res2);
    if (!hasApiFootballError(json2)) return json2;
    throw new Error(apiFootballErrorMessage(json2) || "API limit reached");
  }

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return json;
}

function fetchInit() {
  return USE_HEADER_KEY ? { headers: { [HEADER_NAME]: currentKey() } } : undefined;
}
function currentKey() { return API_KEYS[keyIndex]; }
function rotateKey()   {
  keyIndex = (keyIndex + 1) % API_KEYS.length;
  // console.log("[soccerApi] switched to key index:", keyIndex);
}

async function tryJson(res){ try { return await res.json(); } catch { return {}; } }
function hasApiFootballError(j){ const e=j?.errors; return e && Object.keys(e).length>0; }
function apiFootballErrorMessage(j){
  const e=j?.errors; if(!e) return "";
  return Object.values(e).filter(Boolean).join("; ");
}

// =============== NORMALIZERS ===============

function normalizeFixtures(raw) {
  const arr = Array.isArray(raw?.response) ? raw.response : [];
  return arr
    .filter(f => KEEP_LEAGUE_IDS.has(f.league?.id))
    .map((f) => ({
      id: f.fixture?.id,
      home: {
        name: f.teams?.home?.name ?? "",
        score: num(f.goals?.home),
        logo: str(f.teams?.home?.logo),
      },
      away: {
        name: f.teams?.away?.name ?? "",
        score: num(f.goals?.away),
        logo: str(f.teams?.away?.logo),
      },
      status: toStatus(f.fixture?.status), // { phase, elapsed, period }
      kickoffIso: f.fixture?.date ?? null,
      league: f.league?.name ?? "",
    }));
}

function normalizeEventsOnly(eventsRaw) {
  const arr = Array.isArray(eventsRaw?.response) ? eventsRaw.response : [];
  return arr.map((ev) => ({
    minute: num(ev.time?.elapsed ?? ev.minute ?? ev.min ?? 0),
    extra: ev.time?.extra ?? ev.extra ?? null,
    type: ev.type ?? ev.detail ?? ev.event ?? "",
    player: ev.player?.name ?? ev.player_name ?? "",
    team: ev.team?.name ?? ev.team_name ?? "",
  }));
}

// =============== HELPERS ===============

function toStatus(s = {}) {
  const code = (s.short ?? s.code ?? s.phase ?? s.state ?? s.status ?? "NS").toString();
  const elapsed = num(s.elapsed ?? s.minute ?? s.time ?? 0);
  const inPlay = /^(1H|2H|ET|P|LIVE)$/i.test(code);
  const paused = /^HT$/i.test(code);
  const finished = /^(FT|AET|PEN|CANC|ABD|PST|FT_PEN)$/i.test(code);
  const phase = inPlay ? "IN_PLAY" : paused ? "PAUSED" : finished ? "FINISHED" : code.toUpperCase();
  const period = /^1H|HT$/i.test(code) ? 1 : 2;
  return { phase, elapsed, period };
}
function num(x){ if(typeof x==="number")return x; if(x==null||x==="")return null; const n=Number(x); return Number.isNaN(n)?null:n; }
function str(x){ return typeof x==="string" && x.length>0 ? x : null; }
