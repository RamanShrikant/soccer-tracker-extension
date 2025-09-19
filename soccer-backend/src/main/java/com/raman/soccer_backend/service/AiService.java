package com.raman.soccer_backend.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    public String getPreMatchAnalysis(String matchId) {
        // TODO: Replace with real AI logic later
        return "Pre-match analysis for match " + matchId + ": Expect a tough contest!";
    }

    public String getPostMatchSummary(String matchId) {
        // TODO: Replace with real AI logic later
        return "Post-match summary for match " + matchId + ": Exciting game, key goals decided it.";
    }
}
