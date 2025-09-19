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
        this.service = new OpenAiService(apiKey);
    }

    public String getPreMatchAnalysis(String matchId) {
        return askOpenAi("Write a fun, generic pre-match preview for a random soccer game in 2 sentences.");
    }

    public String getPostMatchSummary(String matchId) {
        return askOpenAi("Write a short, generic post-match recap for a random soccer game in 2 sentences.");
    }

    private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo") // safest for Theokanning
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
