package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // ✅ Pre-match preview — now includes team IDs for H2H
    @GetMapping("/preview")
    public String getPreMatch(@RequestParam String home,
                              @RequestParam String away,
                              @RequestParam String kickoff,
                              @RequestParam String league,
                              @RequestParam int homeId,
                              @RequestParam int awayId) {
        return aiService.getPreMatchAnalysis(home, away, kickoff, league, homeId, awayId);
    }

    // ✅ Post-match recap — uses matchId to fetch events
    @GetMapping("/summary")
    public String getPostMatch(@RequestParam String matchId,
                               @RequestParam String home,
                               @RequestParam int homeScore,
                               @RequestParam String away,
                               @RequestParam int awayScore) {
        return aiService.getPostMatchSummary(matchId, home, homeScore, away, awayScore);
    }
}
