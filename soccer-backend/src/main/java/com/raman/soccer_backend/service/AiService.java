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

    // ✅ Post-Match with events
    public String getPostMatchSummary(String matchId) {
        try {
            Map<String, Object> match = scoresService.getMatchById(matchId);
            if (match == null) {
                return "⚠️ Match not found for id " + matchId;
            }

            Map<String, Object> home = (Map<String, Object>) match.get("home");
            Map<String, Object> away = (Map<String, Object>) match.get("away");

            String homeName = home.get("name").toString();
            String awayName = away.get("name").toString();
            Integer homeScore = (Integer) home.get("score");
            Integer awayScore = (Integer) away.get("score");
            String league = match.get("league").toString();

            // get timeline
            List<Map<String, Object>> events = scoresService.getMatchEvents(matchId);

            StringBuilder sb = new StringBuilder();
            sb.append("Write a short 2–3 sentence post-match recap.\n");
            sb.append("League: ").append(league).append("\n");
            sb.append("Fixture: ").append(homeName).append(" vs ").append(awayName).append("\n");
            sb.append("Final Score: ").append(homeName).append(" ").append(homeScore)
              .append(" – ").append(awayScore).append(" ").append(awayName).append("\n");

            if (!events.isEmpty()) {
                sb.append("Events:\n");
                for (Map<String, Object> ev : events) {
                    String player = ev.get("player") != null ? ev.get("player").toString() : "Unknown";
                    String team = ev.get("team") != null ? ev.get("team").toString() : "Unknown";
                    String type = ev.get("type") != null ? ev.get("type").toString() : "";
                    Integer min = (Integer) ev.get("minute");
                    Integer extra = (Integer) ev.get("extra");
                    String minuteStr = (extra != null) ? min + "+" + extra : String.valueOf(min);
                    sb.append("- ").append(minuteStr).append("′: ")
                      .append(player).append(" (").append(team).append(") ").append(type).append("\n");
                }
            }

            return askOpenAi(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Failed to generate AI summary: " + e.getMessage();
        }
    }

    // ✅ Pre-Match with form + standings
    public String getPreMatchAnalysis(String matchId) {
        try {
            Map<String, Object> match = scoresService.getMatchById(matchId);
            if (match == null) {
                return "⚠️ Match not found for id " + matchId;
            }

            Map<String, Object> home = (Map<String, Object>) match.get("home");
            Map<String, Object> away = (Map<String, Object>) match.get("away");

            String homeName = home.get("name").toString();
            String awayName = away.get("name").toString();
            String league = match.get("league").toString();

            int leagueId = (int) match.get("leagueId");
            int season = (int) match.get("season");
            int homeId = (int) match.get("homeId");
            int awayId = (int) match.get("awayId");

            // last 5 results
            List<String> homeForm = scoresService.getRecentForm(homeId, 5);
            List<String> awayForm = scoresService.getRecentForm(awayId, 5);

            // standings
            Map<String, Object> homeStanding = scoresService.getTeamStanding(leagueId, season, homeId);
            Map<String, Object> awayStanding = scoresService.getTeamStanding(leagueId, season, awayId);

            StringBuilder sb = new StringBuilder();
            sb.append("Write a short 2–3 sentence pre-match preview. ")
              .append("Use only the information below. Do not invent details.\n\n");

            sb.append("League: ").append(league).append("\n");
            sb.append("Fixture: ").append(homeName).append(" vs ").append(awayName).append("\n");
            sb.append("Kickoff: ").append(match.get("kickoffIso")).append("\n\n");

            sb.append("Recent Form:\n");
            sb.append("- ").append(homeName).append(": ").append(homeForm).append("\n");
            sb.append("- ").append(awayName).append(": ").append(awayForm).append("\n\n");

            sb.append("Standings:\n");
            if (homeStanding != null) {
                sb.append("- ").append(homeName).append(": ")
                  .append(homeStanding.get("rank")).append("th place, ")
                  .append(homeStanding.get("points")).append(" pts\n");
            }
            if (awayStanding != null) {
                sb.append("- ").append(awayName).append(": ")
                  .append(awayStanding.get("rank")).append("th place, ")
                  .append(awayStanding.get("points")).append(" pts\n");
            }

            return askOpenAi(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Failed to generate AI preview: " + e.getMessage();
        }
    }

    private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(200)
                .temperature(0.7)
                .build();

        return service.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}
