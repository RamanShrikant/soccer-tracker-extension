// frontend/src/ScoreCard.jsx
import React from "react";
import { getMatchEvents } from "./api/soccerApi"; // returns { timeline }

const BADGE_SIZE = "h-16 w-16";
const CARD_PAD = "p-5";

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

const onlyGoals = (tl = []) => tl.filter((ev) => /GOAL|PEN|OWN/i.test(ev.type || ""));

function TeamScorers({ goals, teamName }) {
  const teamGoals = goals.filter((g) => g.team === teamName);
  if (!teamGoals.length) return null;

  return (
    <div className="text-xs text-gray-600 text-center leading-tight mt-1">
      {teamGoals
        .map(
          (g) =>
            `${g.player || "Unknown"} ${g.minute ?? 0}${
              g.extra ? `+${g.extra}` : ""
            }′`
        )
        .join(", ")}
    </div>
  );
}

export default function ScoreCard({ match, isFavorite, favTeam }) {
  const { id, home = {}, away = {}, status = {}, kickoffIso = "", league = "" } =
    match || {};

  const [timeline, setTimeline] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [open, setOpen] = React.useState(false);

  // NEW: AI state
  const [aiPreview, setAiPreview] = React.useState("");
  const [aiSummary, setAiSummary] = React.useState("");
  const [aiLoading, setAiLoading] = React.useState(false);

  const phase = (status.phase || "NS").toUpperCase();
  const centerTop =
    phase === "IN_PLAY"
      ? status.elapsed != null
        ? `${status.elapsed}′`
        : "LIVE"
      : phase === "PAUSED"
      ? "HT"
      : fmtLocalTime(kickoffIso) || "—";

  async function loadDetails() {
    if (timeline) {
      setOpen((v) => !v);
      return;
    }
    try {
      setLoading(true);
      const { timeline: tl } = await getMatchEvents(id);
      setTimeline(tl || []);
      setOpen(true);
    } finally {
      setLoading(false);
    }
  }

  async function fetchAi(endpoint, setter) {
    try {
      setAiLoading(true);
      const res = await fetch(
        `https://soccer-tracker-extension.onrender.com/api/ai/${endpoint}/${id}`
      );
      const text = await res.text();
      setter(text);
    } catch (err) {
      setter("⚠️ Failed to load AI response");
    } finally {
      setAiLoading(false);
    }
  }

  const goals = onlyGoals(timeline || []);

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
          {open && <TeamScorers goals={goals} teamName={home.name} />}
        </div>

        {/* Center */}
        <div className="flex flex-col items-center gap-1">
          <div className="text-lg font-medium text-gray-900">{centerTop}</div>
          <div className="text-2xl font-normal tabular-nums">
            {home.score ?? 0}–{away.score ?? 0}
          </div>

          {/* Pre-Match button */}
          {phase === "NS" && (
            <>
              <button
                onClick={() => fetchAi("preview", setAiPreview)}
                className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50"
                disabled={aiLoading}
              >
                {aiLoading ? "Loading…" : "Pre-Match"}
              </button>
              <div className="mt-1 text-xs text-gray-600 text-center">
                {aiPreview}
              </div>
            </>
          )}

          {/* Post-Match button */}
          {phase === "FT" && (
            <>
              <button
                onClick={() => fetchAi("summary", setAiSummary)}
                className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50"
                disabled={aiLoading}
              >
                {aiLoading ? "Loading…" : "Post-Match"}
              </button>
              <div className="mt-1 text-xs text-gray-600 text-center">
                {aiSummary}
              </div>
            </>
          )}

          {/* Details button */}
          {!open && phase !== "FT" && (
            <button
              onClick={loadDetails}
              disabled={loading}
              className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50 disabled:opacity-60"
              aria-label="Load match details"
            >
              {loading ? "Loading…" : "Details"}
            </button>
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
          {open && <TeamScorers goals={goals} teamName={away.name} />}
        </div>
      </div>
    </div>
  );
}
