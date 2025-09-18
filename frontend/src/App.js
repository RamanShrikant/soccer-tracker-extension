// App.js
import React, { useEffect, useState } from "react";
import ScoreCard from "./ScoreCard";
import { getTodayFixtures } from "./api/soccerApi";
import { getPrefs, savePref } from "./api/prefsApi";

const USER_ID = "u123"; // static for now

export default function App() {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [favouriteClub, setFavouriteClub] = useState(null);
  const [favouriteLeague, setFavouriteLeague] = useState(null);

  // Load saved preferences
  useEffect(() => {
    async function fetchPrefs() {
      try {
        const prefs = await getPrefs(USER_ID);
        prefs.forEach((p) => {
          if (p.prefType === "TEAM") setFavouriteClub(p.valueName);
          if (p.prefType === "LEAGUE") setFavouriteLeague(p.valueName);
        });
      } catch (err) {
        console.error("Failed to fetch preferences", err);
      }
    }
    fetchPrefs();
  }, []);

  // Load matches on mount
  useEffect(() => {
    const loadMatches = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getTodayFixtures();
        setMatches(data);
      } catch (err) {
        setError("Failed to load matches");
      } finally {
        setLoading(false);
      }
    };
    loadMatches();
  }, []);

  const handleClubChange = async (e) => {
    const value = e.target.value;
    setFavouriteClub(value);
    if (value) await savePref(USER_ID, "TEAM", value);
  };

  const handleLeagueChange = async (e) => {
    const value = e.target.value;
    setFavouriteLeague(value);
    if (value) await savePref(USER_ID, "LEAGUE", value);
  };

  // Reset preferences
  const resetPreferences = () => {
    setFavouriteClub(null);
    setFavouriteLeague(null);
    // not saving empty to DB right now (could add deletePref API later)
  };

  const sortedMatches = [...matches].sort((a, b) => {
    const aFav =
      a.home.name === favouriteClub ||
      a.away.name === favouriteClub ||
      a.league === favouriteLeague;
    const bFav =
      b.home.name === favouriteClub ||
      b.away.name === favouriteClub ||
      b.league === favouriteLeague;
    return aFav === bFav ? 0 : aFav ? -1 : 1;
  });

  return (
    <div className="p-4 w-[400px]">
      <h1 className="text-xl font-bold mb-3">Today’s Matches</h1>

      <div className="mb-4 space-y-2">
        <div>
          <label className="block text-sm font-medium">Favourite Club</label>
          <select
            value={favouriteClub || ""}
            onChange={handleClubChange}
            className="w-full border p-2 rounded"
          >
            <option value="">-- Select Club --</option>
            {matches.map((m) => (
              <React.Fragment key={m.id}>
                <option value={m.home.name}>{m.home.name}</option>
                <option value={m.away.name}>{m.away.name}</option>
              </React.Fragment>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium">Favourite League</label>
          <select
            value={favouriteLeague || ""}
            onChange={handleLeagueChange}
            className="w-full border p-2 rounded"
          >
            <option value="">-- Select League --</option>
            {["Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1", "UEFA Champions League"].map(
              (league) => (
                <option key={league} value={league}>
                  {league}
                </option>
              )
            )}
          </select>
        </div>

        {/* Reset Button */}
        <button
          onClick={resetPreferences}
          className="mt-2 px-3 py-1 bg-gray-500 text-white rounded"
        >
          Reset Preferences
        </button>
      </div>

      {loading && <p>Loading matches...</p>}
      {error && <p className="text-red-500">{error}</p>}

      <div className="space-y-4">
        {sortedMatches.length === 0 ? (
          <p>No matches to show</p>
        ) : (
          sortedMatches.map((match) => (
            <ScoreCard
              key={match.id}
              match={match}
              isFavourite={
                match.home.name === favouriteClub ||
                match.away.name === favouriteClub ||
                match.league === favouriteLeague
              }
            />
          ))
        )}
      </div>
    </div>
  );
}
