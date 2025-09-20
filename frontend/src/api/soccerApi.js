// frontend/src/api/soccerApi.js
// Talk only to backend; API key stays hidden in backend env vars

const BASE = "https://soccer-tracker-extension.onrender.com/api/scores";

// --- caches (2 minutes) ---
const FIXTURE_TTL_MS = 2 * 60 * 1000;
const EVENTS_TTL_MS  = 2 * 60 * 1000;
const fixturesCache = new Map();
const eventsCache   = new Map();

// Keep only: UCL + EPL + La Liga + Serie A + Bundesliga + Ligue 1
const KEEP_LEAGUE_IDS = new Set([2, 39, 140, 135, 78, 61]);

// =============== PUBLIC ===============

export async function getTodayFixtures(force = false) {
  const date = new Date().toISOString().slice(0, 10);
  const url = `${BASE}/fixtures?date=${date}&timezone=UTC`;

  const hit = fixturesCache.get(date);
  if (!force && hit && Date.now() - hit.ts < FIXTURE_TTL_MS) return hit.data;

  const json = await fetchJson(url);  
  const data = normalizeFixtures(json);
  fixturesCache.set(date, { data, ts: Date.now() });
  return data;
}

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
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return tryJson(res);
}

async function tryJson(res) {
  try { return await res.json(); } catch { return {}; }
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
      status: toStatus(f.fixture?.status),
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
