const API_BASE = "http://localhost:8080/prefs";

export async function getPrefs(userId) {
  const res = await fetch(`${API_BASE}/${userId}`);
  if (!res.ok) throw new Error("Failed to fetch preferences");
  return res.json();
}

export async function savePref(userId, prefType, valueName) {
  const res = await fetch(`${API_BASE}/${userId}?prefType=${prefType}&valueName=${encodeURIComponent(valueName)}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Failed to save preference");
}
