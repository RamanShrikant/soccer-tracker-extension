package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // so frontend can call it
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/preview/{matchId}")
    public String getPreMatch(@PathVariable String matchId) {
        return aiService.getPreMatchAnalysis(matchId);
    }

    @GetMapping("/summary/{matchId}")
    public String getPostMatch(@PathVariable String matchId) {
        return aiService.getPostMatchSummary(matchId);
    }
}
