package com.raman.soccer_backend.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final OpenAiService service;
    private final ScoresService scoresService;

    public AiService(@Value("${openai.api.key}") String apiKey, ScoresService scoresService) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("❌ No OpenAI API key found! Check your Render env variable.");
        } else {
            System.out.println("✅ OpenAI API key loaded (length: " + apiKey.length() + ")");
        }
        this.service = new OpenAiService(apiKey);
        this.scoresService = scoresService;
    }

    // ✅ Post-match recap using events + result
    public String getPostMatchSummary(String matchId, String homeName, int homeScore,
                                      String awayName, int awayScore) {
        List<Map<String, Object>> events = scoresService.getMatchEvents(matchId);

        StringBuilder facts = new StringBuilder("Match result: ")
                .append(homeName).append(" ").append(homeScore)
                .append(" – ").append(awayScore).append(" ").append(awayName).append(".\n");

        if (!events.isEmpty()) {
            facts.append("Events:\n");
            for (Map<String, Object> ev : events) {
                facts.append(ev.get("minute")).append("' ")
                        .append(ev.get("team")).append(" - ")
                        .append(ev.get("player")).append(" (")
                        .append(ev.get("type")).append(": ")
                        .append(ev.get("detail")).append(")\n");
            }
        }

        return callOpenAi("Write a 2–4 sentence post-match summary:\n" + facts);
    }

    // ✅ Pre-match preview using form + head-to-head + standings
    public String getPreMatchSummary(int leagueId, int season,
                                     int homeId, String homeName,
                                     int awayId, String awayName) {

        // Recent form
        List<String> homeForm = scoresService.getRecentForm(homeId, 5);
        List<String> awayForm = scoresService.getRecentForm(awayId, 5);

        // Head-to-head
        List<Map<String, Object>> h2h = scoresService.getHeadToHead(homeId, awayId, 5);

        // Standings
        Map<String, Object> homeStanding = scoresService.getTeamStanding(leagueId, season, homeId);
        Map<String, Object> awayStanding = scoresService.getTeamStanding(leagueId, season, awayId);

        StringBuilder facts = new StringBuilder("Upcoming match: ")
                .append(homeName).append(" vs ").append(awayName).append(".\n");

        facts.append("Recent form (last 5): ")
                .append(homeName).append(" -> ").append(String.join("", homeForm))
                .append(", ").append(awayName).append(" -> ").append(String.join("", awayForm)).append("\n");

        if (!h2h.isEmpty()) {
            facts.append("Head-to-head (last ").append(h2h.size()).append(" meetings):\n");
            for (Map<String, Object> m : h2h) {
                facts.append(m.get("home")).append(" ")
                        .append(((Map<?, ?>) m.get("home")).get("score"))
                        .append(" – ")
                        .append(((Map<?, ?>) m.get("away")).get("score"))
                        .append(" ").append(((Map<?, ?>) m.get("away")).get("name"))
                        .append(" (").append(m.get("kickoffIso")).append(")\n");
            }
        }

        if (homeStanding != null && awayStanding != null) {
            facts.append("Standings: ")
                    .append(homeName).append(" #").append(homeStanding.get("rank"))
                    .append(" (").append(homeStanding.get("points")).append(" pts), ")
                    .append(awayName).append(" #").append(awayStanding.get("rank"))
                    .append(" (").append(awayStanding.get("points")).append(" pts).\n");
        }

        return callOpenAi("Write a 3–5 sentence pre-match preview using this data:\n" + facts);
    }

    // 🔧 Utility method to call OpenAI
    private String callOpenAi(String prompt) {
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", "You are a football commentator. Be concise but insightful."),
                        new ChatMessage("user", prompt)
                ))
                .maxTokens(250)
                .build();

        return service.createChatCompletion(req)
                .getChoices().get(0).getMessage().getContent();
    }
}
