// frontend/src/ScoreCard.jsx
import React from "react";

const BADGE_SIZE = "h-16 w-16";
const CARD_PAD   = "p-5";

const initials = (name = "") =>
  name.split(" ").filter(Boolean).slice(0, 2).map(w => w[0]).join("").toUpperCase();

const Crest = ({ name, url }) => {
  const [broken, setBroken] = React.useState(false);
  const showImg = !!url && !broken;
  return showImg ? (
    <img
      src={url}
      alt={`${name} crest`}
      className={`${BADGE_SIZE} object-contain rounded-full bg-white ring-1 ring-gray-200`}
      onError={() => setBroken(true)}
      draggable={false}
    />
  ) : (
    <div className={`${BADGE_SIZE} rounded-full bg-gray-200 flex items-center justify-center text-sm font-bold`}>
      {initials(name)}
    </div>
  );
};

const fmtLocalTime = (utcIso) => {
  if (!utcIso) return "";
  const d = new Date(utcIso);
  return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
};

export default function ScoreCard({ match }) {
  const {
    home = "Home",
    away = "Away",
    homeCrest,
    awayCrest,
    competition = {},
    statusShort = "NS",
    utcDate = "",
    score = {},
  } = match || {};

  const homeScore = score.home ?? 0;
  const awayScore = score.away ?? 0;
  const compName  = competition.name || "League";
  const timeLocal = fmtLocalTime(utcDate);

  return (
    <div
      className={`border-2 border-gray-300 rounded-3xl ${CARD_PAD} bg-white shadow-sm
                  transition-all duration-200 ease-out
                  hover:border-yellow-400 hover:shadow-lg hover:-translate-y-0.5
                  focus:border-yellow-400 focus:shadow-lg focus:-translate-y-0.5
                  cursor-pointer outline-none`}
      role="button"
      tabIndex={0}
    >
      {/* Header */}
      <div className="flex items-center justify-between text-sm text-gray-600 mb-3">
        <span className="font-medium">{compName}</span>
        <span className="px-3 py-1 rounded-full bg-gray-100 text-gray-700">
          {statusShort}
        </span>
      </div>

      {/* 3-column layout */}
      <div className="grid grid-cols-3 items-center">
        {/* Home */}
        <div className="flex flex-col items-center gap-2">
          <Crest name={home} url={homeCrest} />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">{home}</div>
        </div>

        {/* Center */}
        <div className="flex flex-col items-center gap-2">
          <div className="text-lg font-medium text-gray-900">{timeLocal || "—"}</div>
          <div className="text-2xl font-normal tabular-nums">{homeScore}–{awayScore}</div>
        </div>

        {/* Away */}
        <div className="flex flex-col items-center gap-2">
          <Crest name={away} url={awayCrest} />
          <div className="text-sm font-bold text-gray-900 text-center leading-tight">{away}</div>
        </div>
      </div>
    </div>
  );
}
