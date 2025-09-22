// frontend/src/ScoreCard.jsx
import React from "react";
import { getMatchEvents } from "./api/soccerApi"; // returns events array

const BADGE_SIZE = "h-16 w-16";
const CARD_PAD = "p-5";

// Map Football API league names → Odds API keys
const leagueMap = {
  "Premier League": "soccer_epl",
  "Ligue 1": "soccer_france_ligue_one",
  "Serie A": "soccer_italy_serie_a",
  "La Liga": "soccer_spain_la_liga",
  "Bundesliga": "soccer_germany_bundesliga",
};

const initials = (name = "") =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();

const Crest = ({ name, url, highlight }) => {
  const [broken, setBroken] = React.useState(false);

  const baseClasses = `${BADGE_SIZE} rounded-full object-contain bg-white ring-1 ring-gray-200`;
  const highlightClasses = highlight
    ? "ring-2 ring-yellow-300 drop-shadow-[0_0_8px_rgba(250,204,21,0.8)]"
    : "";

  return !url || broken ? (
    <div
      className={`${BADGE_SIZE} rounded-full bg-gray-200 flex items-center justify-center text-sm font-bold ${highlightClasses}`}
    >
      {initials(name)}
    </div>
  ) : (
    <img
      src={url}
      alt={`${name} crest`}
      className={`${baseClasses} ${highlightClasses}`}
      onError={() => setBroken(true)}
      draggable={false}
    />
  );
};

const fmtLocalTime = (iso) =>
  iso
    ? new Date(iso).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })
    : "";

// Render events under each team
function TeamEvents({ events, teamName }) {
  const teamEvents = events.filter((e) => e.team === teamName);
  if (!teamEvents.length) return null;

  return (
    <div className="text-xs text-gray-600 text-center leading-tight mt-1 space-y-0.5">
      {teamEvents.map((ev, i) => (
        <div key={i}>
          {ev.minute}
          {ev.extra ? `+${ev.extra}` : ""}′ –{" "}
          {ev.player || "Unknown"} {ev.type} {ev.detail}
        </div>
      ))}
    </div>
  );
}

export default function ScoreCard({ match, isFavorite, favTeam }) {
  const {
    id,
    home = {},
    away = {},
    status = {},
    kickoffIso = "",
    league = "",
    homeId,
    awayId,
  } = match || {};

  const [timeline, setTimeline] = React.useState([]);
  const [loading, setLoading] = React.useState(false);
  const [open, setOpen] = React.useState(false);

  // AI state
  const [aiPreview, setAiPreview] = React.useState("");
  const [aiSummary, setAiSummary] = React.useState("");
  const [aiLoading, setAiLoading] = React.useState(false);

  // Odds state
  const [odds, setOdds] = React.useState(null);
  const [oddsLoading, setOddsLoading] = React.useState(false);

  let phase = (status.phase || "NS").toUpperCase();
  if (phase === "FINISHED") phase = "FT"; // normalize
  const centerTop =
    phase === "IN_PLAY"
      ? status.elapsed != null
        ? `${status.elapsed}′`
        : "LIVE"
      : phase === "PAUSED"
      ? "HT"
      : fmtLocalTime(kickoffIso) || "—";

  async function loadDetails() {
    try {
      setLoading(true);
      const events = await getMatchEvents(id);
      console.log("🎯 Raw events for match", id, events);
      setTimeline(events || []);
      setOpen((v) => !v);
    } finally {
      setLoading(false);
    }
  }

  async function fetchAi(endpoint, setter, current) {
    if (current) {
      setter("");
      return;
    }

    try {
      setAiLoading(true);

      let url;
      if (endpoint === "preview") {
        url =
          `https://soccer-tracker-extension.onrender.com/api/ai/preview` +
          `?home=${encodeURIComponent(home.name || "")}` +
          `&away=${encodeURIComponent(away.name || "")}` +
          `&kickoff=${encodeURIComponent(kickoffIso || "")}` +
          `&league=${encodeURIComponent(league || "")}` +
          `&homeId=${homeId ?? 0}` +
          `&awayId=${awayId ?? 0}`;
      } else {
        url =
          `https://soccer-tracker-extension.onrender.com/api/ai/summary` +
          `?matchId=${id}` +
          `&home=${encodeURIComponent(home.name || "")}` +
          `&homeScore=${home.score ?? 0}` +
          `&away=${encodeURIComponent(away.name || "")}` +
          `&awayScore=${away.score ?? 0}`;
      }

      const res = await fetch(url);
      const text = await res.text();
      setter(text);
    } catch (err) {
      setter("⚠️ Failed to load AI response");
    } finally {
      setAiLoading(false);
    }
  }

  async function loadOdds() {
    if (odds) {
      setOdds(null); // toggle hide if already showing
      return;
    }

    setOddsLoading(true);
    try {
      const mappedLeague =
        leagueMap[league] || league.toLowerCase().replace(/\s+/g, "_");
      const url =
        `https://soccer-tracker-extension.onrender.com/api/scores/odds` +
        `?league=${encodeURIComponent(mappedLeague)}` +
        `&home=${encodeURIComponent(home.name || "")}` +
        `&away=${encodeURIComponent(away.name || "")}`;

      const res = await fetch(url);
      const data = await res.json();
      console.log("🎲 Odds response", data);
      setOdds(data);
    } catch (err) {
      console.error("⚠️ Failed to load odds", err);
      setOdds({ error: "Failed to fetch odds" });
    } finally {
      setOddsLoading(false);
    }
  }

  return (
    <div
      className={`border-2 rounded-3xl ${CARD_PAD} bg-white shadow-sm
                  transition-all duration-200 ease-out
                  hover:border-yellow-400 hover:shadow-lg hover:-translate-y-0.5
                  focus:border-yellow-400 focus:shadow-lg focus:-translate-y-0.5
                  ${isFavorite ? "ring-4 ring-yellow-400" : "border-gray-300"}`}
    >
      {/* Header */}
      <div className="mb-3 flex items-center justify-between text-sm text-gray-600">
        <span className="font-medium">{league || "League"}</span>
        <span className="rounded-full bg-gray-100 px-3 py-1 text-gray-700">
          {phase}
        </span>
      </div>

      {/* 3-column layout */}
      <div className="grid grid-cols-3 items-start">
        {/* Home side */}
        <div className="flex flex-col items-center gap-2">
          <Crest
            name={home.name || "Home"}
            url={home.logo}
            highlight={home.name === favTeam}
          />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">
            {home.name || "Home"}
          </div>
          {open && <TeamEvents events={timeline} teamName={home.name} />}
        </div>

        {/* Center */}
        <div className="flex flex-col items-center gap-1 col-span-1 w-full">
          <div className="text-lg font-medium text-gray-900">{centerTop}</div>
          <div className="text-2xl font-normal tabular-nums">
            {home.score ?? 0}–{away.score ?? 0}
          </div>

          {/* Pre-Match button */}
          {phase === "NS" && (
            <button
              onClick={() => fetchAi("preview", setAiPreview, aiPreview)}
              className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50"
              disabled={aiLoading}
            >
              {aiLoading ? "Loading…" : aiPreview ? "Hide Pre-Match" : "Pre-Match"}
            </button>
          )}

          {/* Post-Match button */}
          {phase === "FT" && (
            <button
              onClick={() => fetchAi("summary", setAiSummary, aiSummary)}
              className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50"
              disabled={aiLoading}
            >
              {aiLoading ? "Loading…" : aiSummary ? "Hide Post-Match" : "Post-Match"}
            </button>
          )}

          {/* Details button */}
          <button
            onClick={loadDetails}
            disabled={loading}
            className="mt-2 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50 disabled:opacity-60"
          >
            {loading ? "Loading…" : open ? "Hide Details" : "Details"}
          </button>

          {/* Odds button */}
          <button
            onClick={loadOdds}
            disabled={oddsLoading}
            className="mt-2 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50 disabled:opacity-60"
          >
            {oddsLoading ? "Loading…" : odds ? "Hide Odds" : "Show Odds"}
          </button>

          {/* Odds display */}
{/* Odds display */}
{odds && !odds.error && (() => {
  // Step 1: calculate implied probabilities
  const implied = Object.entries(odds).map(([team, price]) => ({
    team,
    price,
    prob: 1 / price,
  }));

  // Step 2: normalize so total = 100%
  const totalProb = implied.reduce((sum, o) => sum + o.prob, 0);
  const normalized = implied.map(o => ({
    ...o,
    pct: (o.prob / totalProb) * 100,
  }));

  // Step 3: find the favorite (highest %)
  const maxPct = Math.max(...normalized.map(o => o.pct));

  // Step 4: render
  return (
    <div className="mt-2 text-xs text-gray-700 text-center space-y-1">
      {normalized.map(({ team, price, pct }) => {
        const isFavorite = pct === maxPct;
        return (
          <div
            key={team}
            className={isFavorite ? "font-bold text-yellow-600" : ""}
          >
            {team}: <span className="font-medium">{price}</span>{" "}
            <span className="text-gray-500">
              ({pct.toFixed(1)}%)
            </span>
          </div>
        );
      })}
    </div>
  );
})()}

          {odds && odds.error && (
            <div className="mt-2 text-xs text-red-500 text-center">{odds.error}</div>
          )}
        </div>

        {/* Away side */}
        <div className="flex flex-col items-center gap-2">
          <Crest
            name={away.name || "Away"}
            url={away.logo}
            highlight={away.name === favTeam}
          />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">
            {away.name || "Away"}
          </div>
          {open && <TeamEvents events={timeline} teamName={away.name} />}
        </div>
      </div>

      {/* AI text */}
      {aiPreview && (
        <div className="mt-3 text-sm text-gray-700 text-center italic max-w-[600px] mx-auto whitespace-pre-line leading-relaxed px-4">
          {aiPreview}
        </div>
      )}
      {aiSummary && (
        <div className="mt-3 text-sm text-gray-700 text-center italic max-w-[600px] mx-auto whitespace-pre-line leading-relaxed px-4">
          {aiSummary}
        </div>
      )}
    </div>
  );
}
