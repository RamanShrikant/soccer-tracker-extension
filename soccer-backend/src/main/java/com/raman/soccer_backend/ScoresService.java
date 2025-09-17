package com.raman.soccer_backend;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Fetches today's Big-5 matches (PL, PD, SA, BL1, FL1, CL) from football-data.org.
 */
@Service
public class ScoresService {

  @Value("${sportsopendata.key:}")
  private String apiKey;

  private static final String BASE = "https://api.football-data.org/v4";
  // Football-Data competition codes for Big 5 + CL
  private static final String COMPETITIONS = "PL,PD,SA,BL1,FL1,CL";

  /** Called by ScoresController */
  public List<Map<String, Object>> fetchTodayBig5(boolean live, boolean force) {
    return fetchFootballDataToday();
  }

  private RestClient client() {
    return RestClient.builder()
        .baseUrl(BASE)
        .defaultHeader("X-Auth-Token", apiKey)
        .build();
  }

  private List<Map<String, Object>> fetchFootballDataToday() {
    System.out.println("apiKey len = " + (apiKey == null ? 0 : apiKey.length()));

    // Use UTC day; fetch ±1 day to avoid boundary misses, then filter back to today UTC
    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    LocalDate start = todayUtc.minusDays(1);
    LocalDate end   = todayUtc.plusDays(1);

    String url = String.format("/matches?dateFrom=%s&dateTo=%s&competitions=%s",
        start, end, COMPETITIONS);

    JsonNode root;
    try {
      root = client().get().uri(url).retrieve().body(JsonNode.class);
    } catch (Exception e) {
      System.out.println("Football-Data call failed: " + e.getMessage());
      return List.of();
    }

    int raw = root != null && root.has("matches") ? root.get("matches").size() : 0;
    System.out.println("API URL = " + url);
    System.out.println("matches (±1d) = " + raw);

    List<Map<String, Object>> out = new ArrayList<>();
    if (root == null || !root.has("matches")) return out;

    for (JsonNode m : root.get("matches")) {
      String utcIso = m.path("utcDate").asText("");
      if (utcIso.isEmpty()) continue;

      LocalDate matchDayUtc;
      try {
        matchDayUtc = Instant.parse(utcIso).atOffset(ZoneOffset.UTC).toLocalDate();
      } catch (Exception ex) {
        // skip malformed dates
        continue;
      }
      if (!matchDayUtc.equals(todayUtc)) continue; // keep only today (UTC)

      String status = m.path("status").asText("SCHEDULED");

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("fixtureId", m.path("id").asLong());
      row.put("utcDate", utcIso);
      row.put("statusShort", status);

      row.put("competition", Map.of(
          "code", m.path("competition").path("code").asText(""),
          "name", m.path("competition").path("name").asText("")
      ));

      // read team nodes once
      JsonNode homeTeam = m.path("homeTeam");
      JsonNode awayTeam = m.path("awayTeam");

      // core fields
      row.put("home", homeTeam.path("name").asText(""));
      row.put("away", awayTeam.path("name").asText(""));

      // <<< ADDED: crest URLs from the API (may be empty string if not present) >>>
      row.put("homeCrest", homeTeam.path("crest").asText(""));
      row.put("awayCrest", awayTeam.path("crest").asText(""));

      // scores: halfTime / fullTime
      int htH = m.path("score").path("halfTime").path("home").asInt(0);
      int htA = m.path("score").path("halfTime").path("away").asInt(0);
      int ftH = m.path("score").path("fullTime").path("home").asInt(0);
      int ftA = m.path("score").path("fullTime").path("away").asInt(0);

      // current shown score: prefer final, else half-time (your previous logic)
      int curH = ("FINISHED".equals(status) || ftH > 0 || ftA > 0) ? ftH : htH;
      int curA = ("FINISHED".equals(status) || ftH > 0 || ftA > 0) ? ftA : htA;

      row.put("score", Map.of(
          "home", curH,
          "away", curA,
          "ht", htH + "-" + htA,
          "ft", ftH + "-" + ftA
      ));

      out.add(row);
    }

    out.sort(Comparator.comparing(m -> String.valueOf(m.get("utcDate"))));
    System.out.println("matches (today UTC) = " + out.size());
    return out;
  }
}
