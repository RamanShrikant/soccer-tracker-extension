import { useEffect, useState } from "react";
import ScoreCard from "./ScoreCard";

function App() {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchScores = async () => {
    setLoading(true);
    try {
      const resp = await fetch("http://localhost:8081/scores/today?force=true");
      const data = await resp.json();
      setMatches(data);
    } catch (err) {
      console.error("Error fetching scores:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScores();
  }, []);

  return (
    <div className="p-4">
      <h1 className="text-lg font-bold mb-2">Today’s Matches</h1>
      <button
        onClick={fetchScores}
        className="px-3 py-1 bg-blue-600 text-white rounded"
      >
        Refresh
      </button>

      {loading && <p>Loading…</p>}

      <div className="mt-4 space-y-2">
        {matches.map(m => (
          <ScoreCard key={m.fixtureId} match={m} />
        ))}
      </div>
    </div>
  );
}

export default App;
