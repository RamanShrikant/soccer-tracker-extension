// frontend/src/api/prefsApi.js
// Use deployed backend if available, fallback to localhost
const API_BASE =
  process.env.REACT_APP_API_BASE ||
  "https://soccer-tracker-extension.onrender.com/prefs";

export async function getPrefs(userId) {
  const res = await fetch(`${API_BASE}/${userId}`);
  if (!res.ok) throw new Error("Failed to fetch preferences");
  return res.json();
}

export async function savePref(userId, prefType, valueName) {
  const res = await fetch(
    `${API_BASE}/${userId}?prefType=${prefType}&valueName=${encodeURIComponent(
      valueName
    )}`,
    { method: "POST" }
  );
  if (!res.ok) throw new Error("Failed to save preference");
}

// ✅ New: delete all prefs for this user
export async function deletePrefs(userId) {
  const res = await fetch(`${API_BASE}/${userId}`, { method: "DELETE" });
  if (!res.ok) throw new Error("Failed to delete preferences");
}
