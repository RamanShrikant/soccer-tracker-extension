// frontend/src/App.js
import React from "react";
import ScoreCard from "./ScoreCard";
import { getTodayFixtures } from "./api/soccerApi";

export default function App() {
  const [fixtures, setFixtures] = React.useState([]);
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState("");

  async function load(force = false) {
    setLoading(true); setErr("");
    try {
      const rows = await getTodayFixtures(force); // [{id, home, away, status, kickoffIso, league}]
      setFixtures(rows);
    } catch (e) {
      setErr(e?.message || String(e));
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => { load(false); }, []);

  return (
    <div className="w-[480px] max-w-[520px] mx-auto px-4 py-3">
      <h1 className="mb-3 text-2xl font-bold">Today’s Matches</h1>

      <div className="mb-4 flex items-center gap-2">
        <button
          onClick={() => load(true)}
          className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          Refresh
        </button>
      </div>

      {loading && <p>Loading…</p>}

      {err && (
        <p className="mb-3 rounded-lg bg-yellow-100 px-3 py-2 text-sm text-yellow-900">
          {err}
        </p>
      )}

      {!loading && !err && fixtures.length === 0 && (
        <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-700">
          No matches to show (or the API limit was reached).
        </div>
      )}

      <div className="space-y-6">
        {fixtures.map(m => (
          <ScoreCard key={m.id} match={m} />
        ))}
      </div>
    </div>
  );
}
