package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.ScoresService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
public class MatchesController {

    private final ScoresService scores;

    @Value("${oddsapi.key}")
    private String oddsApiKey;

    public MatchesController(ScoresService scores) {
        this.scores = scores;
    }

    // Existing endpoints ------------------------

    @GetMapping("/fixtures")
    public List<Map<String, Object>> todayMatches() {
        return scores.getTodayMatches();
    }

    @GetMapping("/fixtures/events")
    public List<Map<String, Object>> getEvents(@RequestParam("fixture") String matchId) {
        return scores.getMatchEvents(matchId);
    }

// New Odds endpoint ------------------------
// Example: /api/scores/odds?league=uefa_champions_league&home=Barcelona&away=PSG
@GetMapping("/odds")
public Map<String, Object> getOdds(
        @RequestParam String league,
        @RequestParam String home,
        @RequestParam String away) {

    // Mapping from frontend → Odds API keys
    Map<String, String> leagueMap = Map.of(
        "premier_league", "soccer_epl",
        "la_liga", "soccer_spain_la_liga",
        "serie_a", "soccer_italy_serie_a",
        "ligue_1", "soccer_france_ligue_one",
        "bundesliga", "soccer_germany_bundesliga",
        "uefa_champions_league", "soccer_uefa_champs_league"
    );

    // Translate league key
    String oddsLeague = leagueMap.get(league.toLowerCase());
    if (oddsLeague == null) {
        return Map.of("error", "Unsupported league: " + league);
    }

    String url = String.format(
        "https://api.the-odds-api.com/v4/sports/%s/odds/?regions=eu&markets=h2h&apiKey=%s",
        oddsLeague,
        oddsApiKey
    );

    try {
        RestTemplate restTemplate = new RestTemplate();
        List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

        if (response == null) {
            return Map.of("error", "No data from Odds API");
        }

        for (Map<String, Object> game : response) {
            String homeTeam = (String) game.get("home_team");
            String awayTeamResp = (String) game.get("away_team");

            if (homeTeam.equalsIgnoreCase(home) && awayTeamResp.equalsIgnoreCase(away)) {
                Map<String, Object> simplified = new HashMap<>();
                List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) game.get("bookmakers");
                if (bookmakers != null && !bookmakers.isEmpty()) {
                    List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmakers.get(0).get("markets");
                    if (markets != null && !markets.isEmpty()) {
                        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) markets.get(0).get("outcomes");
                        if (outcomes != null) {
                            for (Map<String, Object> o : outcomes) {
                                simplified.put((String) o.get("name"), o.get("price"));
                            }
                        }
                    }
                }
                return simplified;
            }
        }

        return Map.of("error", "Match not found in Odds API");

    } catch (Exception e) {
        e.printStackTrace(); // shows stacktrace in Render logs
        return Map.of("error", "Exception while fetching odds: " + e.getMessage());
    }
}


}
