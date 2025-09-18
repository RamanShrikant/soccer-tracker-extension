// Dynamically detect backend port
// If you're running locally, backend will usually be 8080 or 8081
// In production, you can replace this with your deployed API URL
const BACKEND_PORT = process.env.REACT_APP_BACKEND_PORT || 8080;
const API_BASE = `http://localhost:${BACKEND_PORT}/prefs`;

export async function getPrefs(userId) {
  const res = await fetch(`${API_BASE}/${userId}`);
  if (!res.ok) throw new Error("Failed to fetch preferences");
  return res.json();
}

export async function savePref(userId, prefType, valueName) {
  const res = await fetch(
    `${API_BASE}/${userId}?prefType=${prefType}&valueName=${encodeURIComponent(valueName)}`,
    { method: "POST" }
  );
  if (!res.ok) throw new Error("Failed to save preference");
}
