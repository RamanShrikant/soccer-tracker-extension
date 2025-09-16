// frontend/src/ScoreCard.jsx
export default function ScoreCard({ match }) {
  const {
    home = "Home",
    away = "Away",
    league = {},
    statusShort = "NS",
    kickoffLocal = "",
    score = {},
    elapsed = 0,
  } = match || {};

  const homeScore = score.home ?? 0;
  const awayScore = score.away ?? 0;

  return (
    <div className="border rounded p-3">
      <div className="flex justify-between items-center">
        <div className="font-semibold">
          {home} <span className="opacity-60">vs</span> {away}
        </div>
        <div className="text-lg font-bold">
          {homeScore}–{awayScore}
        </div>
      </div>

      <div className="text-xs mt-1 opacity-70">
        {league.name || league.slug || "League"} • {statusShort}
        {statusShort !== "NS" ? ` • ${elapsed}'` : ""} • {kickoffLocal}
      </div>
    </div>
  );
}
