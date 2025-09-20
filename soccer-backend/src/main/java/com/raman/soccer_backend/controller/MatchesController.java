package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.ScoresService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
public class MatchesController {

    private final ScoresService scores;

    public MatchesController(ScoresService scores) {
        this.scores = scores;
    }

    // Matches frontend: /api/scores/fixtures
    @GetMapping("/fixtures")
    public List<Map<String, Object>> todayMatches() {
        return scores.getTodayMatches();
    }

    // Matches frontend: /api/scores/fixtures/events?fixture=12345
    @GetMapping("/fixtures/events")
    public List<Map<String, Object>> getEvents(@RequestParam("fixture") String matchId) {
        return scores.getMatchEvents(matchId);
    }
}
