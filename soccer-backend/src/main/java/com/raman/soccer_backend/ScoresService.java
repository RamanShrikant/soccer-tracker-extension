package com.raman.soccer_backend;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ScoresService {

  @Value("${sportsopendata.key:}")
  private String sodKey;

  private static final ZoneId TZ = ZoneId.of("America/Toronto");

  public List<Map<String,Object>> fetchTodayBig5(boolean live, boolean force) {
    // Hard test: ONLY real API call, no sample fallback.
    return fetchLeagueForToday("premier-league");
  }

  private List<Map<String,Object>> fetchLeagueForToday(String leagueSlug) {
    String date = LocalDate.now(TZ).toString();

    RestClient http = RestClient.builder()
        .baseUrl("https://api.sportsopendata.net")
        .defaultHeader("X-Auth-Token", sodKey)   // <-- correct header
        .build();

    JsonNode root = http.get()
        .uri(u -> u
            .path("/v1/leagues/" + leagueSlug + "/seasons/2024/matches")
            .queryParam("date", date)
            .build())
        .retrieve()
        .body(JsonNode.class);

    JsonNode matches = root.at("/data/matches");   // adjust if their JSON differs
    List<Map<String,Object>> out = new ArrayList<>();

    if (matches.isArray()) {
      for (JsonNode m : matches) {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("fixtureId", m.path("id").asLong());
        row.put("kickoffLocal", m.path("date").asText());
        row.put("statusShort", m.path("status").asText("NS"));
        row.put("elapsed", m.path("elapsed").asInt(0));
        row.put("league", Map.of("slug", leagueSlug, "name", m.path("league").path("name").asText(leagueSlug)));
        row.put("home", m.path("home_team").asText());
        row.put("away", m.path("away_team").asText());
        row.put("score", Map.of(
            "home", m.path("result").path("home").asInt(0),
            "away", m.path("result").path("away").asInt(0),
            "ht",  m.path("result").path("ht").asText(""),
            "ft",  m.path("result").path("ft").asText("")
        ));
        out.add(row);
      }
    }

    return out; // if out is empty, you'll see [] (not the Arsenal demo)
  }
}
