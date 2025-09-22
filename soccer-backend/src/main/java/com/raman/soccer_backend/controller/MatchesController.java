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
    // Example: /api/scores/odds?league=soccer_epl
    @GetMapping("/odds")
    public Map<String, Object> getOdds(
            @RequestParam String league) {

        String url = String.format(
            "https://api.the-odds-api.com/v4/sports/%s/odds/?regions=eu&markets=h2h&apiKey=%s",
            league.toLowerCase(),
            oddsApiKey
        );

        RestTemplate restTemplate = new RestTemplate();
        List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

        Map<String, Object> simplified = new HashMap<>();
        if (response != null && !response.isEmpty()) {
            // Just grab first bookmaker to simplify
            Map<String, Object> firstBook = (Map<String, Object>) response.get(0);
List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmakers.get(0).get("markets");
if (markets != null && !markets.isEmpty()) {
    Map<String, Object> firstMarket = markets.get(0);
    List<Map<String, Object>> outcomes = (List<Map<String, Object>>) firstMarket.get("outcomes");
    if (outcomes != null) {
        for (Map<String, Object> o : outcomes) {
            String name = (String) o.get("name");
            Double price = (Double) o.get("price");
            simplified.put(name, price);
        }
    }
}

        }

        return simplified;
    }
}
