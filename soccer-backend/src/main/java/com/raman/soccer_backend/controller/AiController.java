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

    // ✅ Pre-match preview — send team names, kickoff, league
    @GetMapping("/preview")
    public String getPreMatch(@RequestParam String home,
                              @RequestParam String away,
                              @RequestParam String kickoff,
                              @RequestParam String league) {
        return aiService.getPreMatchAnalysis(home, away, kickoff, league);
    }

    // ✅ Post-match recap — send team names + scores
    @GetMapping("/summary")
    public String getPostMatch(@RequestParam String home,
                               @RequestParam int homeScore,
                               @RequestParam String away,
                               @RequestParam int awayScore) {
        return aiService.getPostMatchSummary(home, homeScore, away, awayScore);
    }
}
