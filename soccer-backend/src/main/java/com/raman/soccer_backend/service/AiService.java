package com.raman.soccer_backend.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    public String getPreMatchAnalysis(String matchId) {
        // Pretend this is your "prompt" template
        return "Pre-match analysis for match " + matchId 
            + ": Summarize expected key players, team form, and style.";
    }

    public String getPostMatchSummary(String matchId) {
        // Pretend this is your "prompt" template
        return "Post-match summary for match " + matchId 
            + ": Include final score, goal scorers, and highlight moment.";
    }
}
