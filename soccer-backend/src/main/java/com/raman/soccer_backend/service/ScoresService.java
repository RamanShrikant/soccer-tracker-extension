package com.raman.soccer_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.*;

@Service
public class ScoresService {

    private final RestClient client;

    // ✅ Debug API key length when service starts
    public ScoresService(@Value("${sportsopendata.key:}") String apiKey) {
        System.out.println("🔑 Loaded API key (length): " + (apiKey == null ? "null" : apiKey.length()));
        this.client = RestClient.builder()
                .baseUrl("https://api.football-data.org/v4")
                .defaultHeader("X-Auth-Token", apiKey)
                .build();
    }

    // ✅ Fetch all matches for today
    public List<Map<String, Object>> getTodayMatches() {
        String today = LocalDate.now().toString();

        try {
            JsonNode root = client.get()
                    .uri("/matches?competitions=PL,PD,SA,BL1,FL1,CL&dateFrom=" + today + "&dateTo=" + today)
                    .retrieve()
                    .body(JsonNode.class);

            List<Map<String, Object>> matches = new ArrayList<>();
            for (JsonNode m : root.path("matches")) {
                matches.add(parseMatch(m));
            }

            return matches;

        } catch (Exception e) {
            System.err.println("❌ Error fetching matches: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ✅ Fetch a single match directly by ID
    public Map<String, Object> getMatchById(String matchId) {
        try {
            JsonNode root = client.get()
                    .uri("/matches/" + matchId)
                    .retrieve()
                    .body(JsonNode.class);

            // API already returns the match at the top-level
            return parseMatch(root);

        } catch (Exception e) {
            System.err.println("❌ Error fetching match by ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Helper to parse one match JSON into a Map
    private Map<String, Object> parseMatch(JsonNode m) {
        Map<String, Object> match = new HashMap<>();
        match.put("id", m.path("id").asText());
        match.put("league", m.path("competition").path("name").asText());
        match.put("kickoffIso", m.path("utcDate").asText());

        Map<String, Object> home = new HashMap<>();
        home.put("name", m.path("homeTeam").path("name").asText());
        home.put("logo", m.path("homeTeam").path("crest").asText(null));
        home.put("score", m.path("score").path("fullTime").path("home").isInt()
                ? m.path("score").path("fullTime").path("home").asInt()
                : null);

        Map<String, Object> away = new HashMap<>();
        away.put("name", m.path("awayTeam").path("name").asText());
        away.put("logo", m.path("awayTeam").path("crest").asText(null));
        away.put("score", m.path("score").path("fullTime").path("away").isInt()
                ? m.path("score").path("fullTime").path("away").asInt()
                : null);

        match.put("home", home);
        match.put("away", away);

        return match;
    }
}
