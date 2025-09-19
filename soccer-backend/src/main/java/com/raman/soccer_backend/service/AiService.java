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

            String prompt = "Match result: " + homeName + " " + homeScore + " – " +
                    awayScore + " " + awayName + ". Write a short 2-sentence recap of this match.";

            return askOpenAi(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Failed to generate AI summary: " + e.getMessage();
        }
    }

    public String getPreMatchAnalysis(String matchId) {
        return askOpenAi("Write a fun, generic pre-match preview for a random soccer game in 2 sentences.");
    }

    private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(150)
                .temperature(0.7)
                .build();

        return service.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}
