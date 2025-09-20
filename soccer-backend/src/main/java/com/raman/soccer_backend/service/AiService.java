package com.raman.soccer_backend.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    private final OpenAiService service;

    public AiService(@Value("${openai.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("❌ No OpenAI API key found! Check your Render env variable.");
        } else {
            System.out.println("✅ OpenAI API key loaded (length: " + apiKey.length() + ")");
        }
        this.service = new OpenAiService(apiKey);
    }

    // ✅ Post-match recap
    public String getPostMatchSummary(String homeName, int homeScore, String awayName, int awayScore) {
        String prompt = "Match result: " + homeName + " " + homeScore + " – " +
                awayScore + " " + awayName + ". Write a short 2-sentence recap of this match.";
        return askOpenAi(prompt);
    }

    // ✅ Pre-match preview
    public String getPreMatchAnalysis(String homeName, String awayName, String kickoffIso, String league) {
        String prompt = "Upcoming match: " + homeName + " vs " + awayName +
                " in the " + league + ". Kickoff at " + kickoffIso +
                ". Write a short 2-sentence preview for fans.";
        return askOpenAi(prompt);
    }

    // ✅ Ask GPT
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
