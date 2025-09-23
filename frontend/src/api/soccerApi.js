// frontend/src/api/soccerApi.js
const BASE = "https://soccer-tracker-extension.onrender.com/api/scores";

const FIXTURE_TTL_MS = 2 * 60 * 1000;
const EVENTS_TTL_MS = 2 * 60 * 1000;
const fixturesCache = new Map();
const eventsCache = new Map();

const KEEP_LEAGUE_IDS = new Set([2, 39, 140, 135, 78, 61]);

function num(x) {
  if (typeof x === "number") return x;
  if (x == null || x === "") return null;
  const n = Number(x);
  return Number.isNaN(n) ? null : n;
}

function toStatus(s = {}) {
  const code = (s.short ?? s.code ?? s.phase ?? s.state ?? s.status ?? "NS").toString();
  const elapsed = num(s.elapsed ?? s.minute ?? s.time ?? 0);

  console.log("🔍 toStatus input:", s, "-> parsed code:", code);

  const inPlay = /^(1H|2H|ET|P|LIVE)$/i.test(code);
  const paused = /^HT$/i.test(code);
  const finished = /^(FT|AET|PEN|CANC|ABD|PST|FT_PEN|FINISHED|MATCH_FINISHED)$/i.test(code);

  const phase = inPlay
    ? "IN_PLAY"
    : paused
    ? "PAUSED"
    : finished
    ? "FINISHED"
    : code.toUpperCase();

  const period = /^1H|HT$/i.test(code) ? 1 : 2;
  return { phase, elapsed, period };
}

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
  if (!force && hit && Date.now() - hit.ts < EVENTS_TTL_MS) return hit.timeline;

  const url = `${BASE}/fixtures/events?fixture=${matchId}`;
  const evJson = await fetchJson(url);
  const timeline = normalizeEventsOnly(evJson);
  eventsCache.set(matchId, { timeline, ts: Date.now() });
  return timeline;
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return tryJson(res);
}

async function tryJson(res) {
  try {
    return await res.json();
  } catch {
    return {};
  }
}

function normalizeFixtures(raw) {
  console.log("⚽ Raw fixtures from backend:", raw);
  const arr = Array.isArray(raw) ? raw : [];
  return arr
    .filter((f) => KEEP_LEAGUE_IDS.has(f.leagueId))
    .map((f) => {
      const parsedStatus = toStatus(f.status);
      console.log("📡 Fixture status parsed:", f.status, "=>", parsedStatus);
      return {
        id: f.id,
        home: f.home,
        away: f.away,
        status: parsedStatus,
        kickoffIso: f.kickoffIso,
        league: f.league,
        leagueId: f.leagueId,
      };
    });
}

function normalizeEventsOnly(eventsRaw) {
  const arr = Array.isArray(eventsRaw?.response)
    ? eventsRaw.response
    : Array.isArray(eventsRaw)
    ? eventsRaw
    : [];

  console.log("🎯 normalizeEventsOnly input:", eventsRaw, "=> parsed length:", arr.length);

  return arr.map((ev) => ({
    minute: num(ev.minute ?? ev.time?.elapsed ?? ev.min ?? 0),
    extra: ev.extra ?? ev.time?.extra ?? null,
    type: ev.type ?? ev.detail ?? ev.event ?? "",
    player: ev.player?.name ?? ev.player ?? ev.player_name ?? "Unknown",
    team: ev.team?.name ?? ev.team ?? ev.team_name ?? "",   // always string
    teamId: ev.team?.id ?? ev.teamId ?? ev.team_id ?? null,
    detail: ev.detail ?? "",
  }));
}
