package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.ScoresService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/matches")
public class MatchesController {

    private final ScoresService scores;

    public MatchesController(ScoresService scores) {
        this.scores = scores;
    }

    @GetMapping("/today")
    public List<Map<String, Object>> todayMatches() {
        return scores.getTodayMatches();
    }
}
