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
    

    public ScoresService(@Value("${apifootball.key:}") String apiKey) {
        System.out.println("🔑 Loaded API key (length): " + (apiKey == null ? "null" : apiKey.length()));
        this.client = RestClient.builder()
                .baseUrl("https://v3.football.api-sports.io")
                .defaultHeader("x-apisports-key", apiKey)
                .build();
    }

    // ✅ Fetch all matches for today
public List<Map<String, Object>> getTodayMatches() {
    String today = LocalDate.now().toString();
    System.out.println("🕒 getTodayMatches() using date = " + today);

    try {
        JsonNode root = client.get()
                .uri("/fixtures?date=" + today + "&timezone=UTC")
                .retrieve()
                .body(JsonNode.class);

        List<Map<String, Object>> matches = new ArrayList<>();
        for (JsonNode m : root.path("response")) {
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
                    .uri("/fixtures?id=" + matchId)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode arr = root.path("response");
            if (arr.isArray() && arr.size() > 0) {
                return parseMatch(arr.get(0));
            }
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error fetching match by ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Fetch timeline / events for a match
    public List<Map<String, Object>> getMatchEvents(String matchId) {
        try {
            JsonNode root = client.get()
                    .uri("/fixtures/events?fixture=" + matchId)
                    .retrieve()
                    .body(JsonNode.class);

            List<Map<String, Object>> events = new ArrayList<>();
            for (JsonNode ev : root.path("response")) {
                Map<String, Object> event = new HashMap<>();
                event.put("minute", ev.path("time").path("elapsed").asInt(0));
                event.put("extra", ev.path("time").path("extra").isInt() ? ev.path("time").path("extra").asInt() : null);
                event.put("type", ev.path("type").asText());
                event.put("detail", ev.path("detail").asText());
                event.put("player", ev.path("player").path("name").asText());
                event.put("team", ev.path("team").path("name").asText());
                events.add(event);
            }
            return events;

        } catch (Exception e) {
            System.err.println("❌ Error fetching match events: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ✅ Helper to parse one match JSON into a Map
    private Map<String, Object> parseMatch(JsonNode m) {
        Map<String, Object> match = new HashMap<>();
        match.put("id", m.path("fixture").path("id").asText());
        match.put("league", m.path("league").path("name").asText());
        match.put("kickoffIso", m.path("fixture").path("date").asText());

        match.put("leagueId", m.path("league").path("id").asInt());
        match.put("season", m.path("league").path("season").asInt());
        match.put("homeId", m.path("teams").path("home").path("id").asInt());
        match.put("awayId", m.path("teams").path("away").path("id").asInt());

        Map<String, Object> home = new HashMap<>();
        home.put("name", m.path("teams").path("home").path("name").asText());
        home.put("logo", m.path("teams").path("home").path("logo").asText(null));
        home.put("score", m.path("goals").path("home").isInt()
                ? m.path("goals").path("home").asInt()
                : null);

        Map<String, Object> away = new HashMap<>();
        away.put("name", m.path("teams").path("away").path("name").asText());
        away.put("logo", m.path("teams").path("away").path("logo").asText(null));
        away.put("score", m.path("goals").path("away").isInt()
                ? m.path("goals").path("away").asInt()
                : null);

        match.put("home", home);
        match.put("away", away);

        return match;
    }

    // ✅ Get last N matches for a team (recent form)
    public List<String> getRecentForm(int teamId, int lastN) {
        try {
            JsonNode root = client.get()
                    .uri("/fixtures?team=" + teamId + "&last=" + lastN)
                    .retrieve()
                    .body(JsonNode.class);

            List<String> form = new ArrayList<>();
            for (JsonNode f : root.path("response")) {
                String result;
                int homeGoals = f.path("goals").path("home").asInt();
                int awayGoals = f.path("goals").path("away").asInt();
                boolean isHome = f.path("teams").path("home").path("id").asInt() == teamId;

                if (homeGoals == awayGoals) {
                    result = "D"; // draw
                } else if ((isHome && homeGoals > awayGoals) || (!isHome && awayGoals > homeGoals)) {
                    result = "W"; // win
                } else {
                    result = "L"; // loss
                }

                form.add(result);
            }
            return form;

        } catch (Exception e) {
            System.err.println("❌ Error fetching recent form: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ✅ Get current league standings
    public Map<String, Object> getTeamStanding(int leagueId, int season, int teamId) {
        try {
            JsonNode root = client.get()
                    .uri("/standings?league=" + leagueId + "&season=" + season)
                    .retrieve()
                    .body(JsonNode.class);

            for (JsonNode league : root.path("response")) {
                JsonNode table = league.path("league").path("standings").get(0); // usually array of arrays
                for (JsonNode row : table) {
                    if (row.path("team").path("id").asInt() == teamId) {
                        Map<String, Object> standing = new HashMap<>();
                        standing.put("rank", row.path("rank").asInt());
                        standing.put("points", row.path("points").asInt());
                        standing.put("played", row.path("all").path("played").asInt());
                        return standing;
                    }
                }
            }
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error fetching standings: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Fetch head-to-head results between two teams
    public List<Map<String, Object>> getHeadToHead(int homeId, int awayId, int last) {
        try {
            JsonNode root = client.get()
                    .uri("/fixtures/headtohead?h2h=" + homeId + "-" + awayId + "&last=" + last)
                    .retrieve()
                    .body(JsonNode.class);

            List<Map<String, Object>> matches = new ArrayList<>();
            for (JsonNode m : root.path("response")) {
                matches.add(parseMatch(m));
            }
            return matches;

        } catch (Exception e) {
            System.err.println("❌ Error fetching head-to-head: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
