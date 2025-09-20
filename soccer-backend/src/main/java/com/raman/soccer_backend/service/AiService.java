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

    // ✅ Post-match recap using real events
    public String getPostMatchSummary(String matchId, String homeName, int homeScore, String awayName, int awayScore) {
        List<Map<String, Object>> events = scoresService.getMatchEvents(matchId);

        StringBuilder facts = new StringBuilder("Match result: ")
                .append(homeName).append(" ").append(homeScore).append(" – ")
                .append(awayScore).append(" ").append(awayName).append(".\n");

        if (!events.isEmpty()) {
            facts.append("Events:\n");
            for (Map<String, Object> ev : events) {
                String type = (String) ev.get("type");
                String detail = (String) ev.get("detail");
                String player = (String) ev.get("player");
                String team = (String) ev.get("team");
                int minute = ev.get("minute") != null ? (Integer) ev.get("minute") : 0;

                if ("Goal".equalsIgnoreCase(type)) {
                    facts.append("- ").append(player).append(" scored for ").append(team)
                            .append(" (").append(minute).append("′)\n");
                } else if ("Card".equalsIgnoreCase(type)) {
                    facts.append("- ").append(player).append(" received a ").append(detail)
                            .append(" (").append(team).append(", ").append(minute).append("′)\n");
                } else if ("subst".equalsIgnoreCase(type)) {
                    facts.append("- Substitution for ").append(team).append(": ").append(detail)
                            .append(" (").append(minute).append("′)\n");
                }
            }
        }

        String prompt = facts + "\nWrite a concise 2-sentence recap of this match, using only the listed events. Do not invent players or goals.";

        return askOpenAi(prompt);
    }

    // ✅ Pre-match preview using H2H
    public String getPreMatchAnalysis(String homeName, String awayName, String kickoffIso, String league,
                                      int homeId, int awayId) {
        List<Map<String, Object>> h2h = scoresService.getHeadToHead(homeId, awayId, 5);

        StringBuilder facts = new StringBuilder("Upcoming match: ")
                .append(homeName).append(" vs ").append(awayName)
                .append(" in the ").append(league)
                .append(". Kickoff at ").append(kickoffIso).append(".\n");

        if (!h2h.isEmpty()) {
            int homeWins = 0, awayWins = 0, draws = 0;
            for (Map<String, Object> m : h2h) {
                int hs = (int) m.get("homeScore");
                int as = (int) m.get("awayScore");
                if (hs == as) draws++;
                else if (m.get("home").equals(homeName) && hs > as) homeWins++;
                else if (m.get("away").equals(homeName) && as > hs) homeWins++;
                else if (m.get("home").equals(awayName) && hs > as) awayWins++;
                else if (m.get("away").equals(awayName) && as > hs) awayWins++;
            }
            facts.append("Head-to-head (last 5): ")
                 .append(homeName).append(" ").append(homeWins).append("W, ")
                 .append(awayName).append(" ").append(awayWins).append("W, ")
                 .append(draws).append("D.\n");
        }

        String prompt = facts + "Write a short 2-sentence preview for fans, highlighting the head-to-head record but not inventing details.";

        return askOpenAi(prompt);
    }

    // ✅ Ask GPT
    private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(200)
                .temperature(0.5)
                .build();

        return service.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}
