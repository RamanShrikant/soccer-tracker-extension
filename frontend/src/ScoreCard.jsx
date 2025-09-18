// frontend/src/ScoreCard.jsx
import React from "react";
import { getMatchEvents } from "./api/soccerApi"; // returns { timeline } (cached 2 min)

const BADGE_SIZE = "h-16 w-16";
const CARD_PAD = "p-5";

const initials = (name = "") =>
  name.split(" ").filter(Boolean).slice(0,2).map(w=>w[0]).join("").toUpperCase();

const Crest = ({ name, url }) => {
  const [broken, setBroken] = React.useState(false);
  return (!url || broken) ? (
    <div className={`${BADGE_SIZE} rounded-full bg-gray-200 flex items-center justify-center text-sm font-bold`}>
      {initials(name)}
    </div>
  ) : (
    <img
      src={url}
      alt={`${name} crest`}
      className={`${BADGE_SIZE} object-contain rounded-full bg-white ring-1 ring-gray-200`}
      onError={()=>setBroken(true)}
      draggable={false}
    />
  );
};

const fmtLocalTime = iso =>
  iso ? new Date(iso).toLocaleTimeString([], {hour:"numeric", minute:"2-digit"}) : "";

const onlyGoals = (tl=[]) => tl.filter(ev => /GOAL|PEN|OWN/i.test(ev.type || ""));

function ScorersLine({ goals }) {
  if (!goals?.length) return null;
  const txt = goals
    .map(g => `${g.player || "Unknown"} ${g.minute ?? 0}${g.extra ? `+${g.extra}` : ""}′`)
    .join(", ");
  return <div className="text-sm text-gray-700 text-center leading-tight">{txt}</div>;
}

export default function ScoreCard({ match }) {
  const { id, home={}, away={}, status={}, kickoffIso="", league="" } = match || {};

  const [timeline, setTimeline] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [open, setOpen] = React.useState(false); // show/hide details line

  // Center-top text: elapsed, HT, or kickoff time
  const phase = (status.phase || "NS").toUpperCase();
  const centerTop =
    phase === "IN_PLAY" ? (status.elapsed != null ? `${status.elapsed}′` : "LIVE")
    : phase === "PAUSED" ? "HT"
    : fmtLocalTime(kickoffIso) || "—";

  async function loadDetails() {
    if (timeline) { setOpen(v=>!v); return; } // already fetched; just toggle
    try {
      setLoading(true);
      const { timeline: tl } = await getMatchEvents(id); // 1 call; cached in api
      setTimeline(tl || []);
      setOpen(true);
    } finally {
      setLoading(false);
    }
  }

  const goals = onlyGoals(timeline || []);

  return (
    <div
      className={`border-2 border-gray-300 rounded-3xl ${CARD_PAD} bg-white shadow-sm
                  transition-all duration-200 ease-out
                  hover:border-yellow-400 hover:shadow-lg hover:-translate-y-0.5
                  focus:border-yellow-400 focus:shadow-lg focus:-translate-y-0.5`}
    >
      {/* Header */}
      <div className="mb-3 flex items-center justify-between text-sm text-gray-600">
        <span className="font-medium">{league || "League"}</span>
        <span className="rounded-full bg-gray-100 px-3 py-1 text-gray-700">{phase}</span>
      </div>

      {/* 3-column layout */}
      <div className="grid grid-cols-3 items-center">
        <div className="flex flex-col items-center gap-2">
          <Crest name={home.name || "Home"} url={home.logo} />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">{home.name || "Home"}</div>
        </div>

        <div className="flex flex-col items-center gap-1">
          <div className="text-lg font-medium text-gray-900">{centerTop}</div>
          <div className="text-2xl font-normal tabular-nums">{(home.score ?? 0)}–{(away.score ?? 0)}</div>

          {/* Details button / scorers */}
          {!open ? (
            <button
              onClick={loadDetails}
              disabled={loading}
              className="mt-1 rounded-xl border px-2 py-1 text-xs text-gray-700 hover:bg-gray-50 disabled:opacity-60"
              aria-label="Load match details"
            >
              {loading ? "Loading…" : "Details"}
            </button>
          ) : (
            <ScorersLine goals={goals} />
          )}
        </div>

        <div className="flex flex-col items-center gap-2">
          <Crest name={away.name || "Away"} url={away.logo} />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">{away.name || "Away"}</div>
        </div>
      </div>
    </div>
  );
}
