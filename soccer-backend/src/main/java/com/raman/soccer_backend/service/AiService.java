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

    // ✅ Post-match recap
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

    // ✅ Pre-match analysis with odds support
    public String getPreMatchAnalysis(String homeName, String awayName,
                                      String kickoff, String league,
                                      int homeId, int awayId,
                                      String oddsJson) {

        // Get recent form
        List<String> homeForm = scoresService.getRecentForm(homeId, 5);
        List<String> awayForm = scoresService.getRecentForm(awayId, 5);

        // Get head-to-head
        List<Map<String, Object>> h2h = scoresService.getHeadToHead(homeId, awayId, 5);

        StringBuilder facts = new StringBuilder("Upcoming match: ")
                .append(homeName).append(" vs ").append(awayName).append(".\n");

        facts.append("Kickoff: ").append(kickoff).append(", League: ").append(league).append("\n");

        facts.append("Recent form: ")
                .append(homeName).append(" -> ").append(String.join("", homeForm))
                .append(", ").append(awayName).append(" -> ").append(String.join("", awayForm)).append("\n");

        if (!h2h.isEmpty()) {
            facts.append("Head-to-head (last ").append(h2h.size()).append(" meetings):\n");
            for (Map<String, Object> m : h2h) {
                Map<String, Object> home = (Map<String, Object>) m.get("home");
                Map<String, Object> away = (Map<String, Object>) m.get("away");
                facts.append(home.get("name")).append(" ")
                        .append(home.get("score"))
                        .append(" – ")
                        .append(away.get("score")).append(" ")
                        .append(away.get("name"))
                        .append(" (").append(m.get("kickoffIso")).append(")\n");
            }
        }

        // Odds (sent from frontend)
        if (oddsJson != null && !oddsJson.isBlank()) {
            facts.append("Betting odds (raw JSON): ").append(oddsJson).append("\n");
        }

        return callOpenAi(
            "You are given structured football data. " +
            "Write a 3–5 sentence pre-match preview ONLY using this data:\n\n" + facts +
            "\nRules:\n" +
            "1. Do NOT invent results, players, or stats that are not listed.\n" +
            "2. Summarize head-to-head exactly as provided.\n" +
            "3. Mention betting odds if available, noting which side is the favorite.\n" +
            "4. Stay concise, neutral, and insightful.\n" +
            "5. 3–5 sentences maximum."
        );
    }

    // 🔧 Helper
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
