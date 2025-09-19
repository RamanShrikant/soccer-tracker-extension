package com.raman.soccer_backend.service;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AiService {

    private final OpenAiService openAiService;

    public AiService(@Value("${openai.api.key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
    }

    public String getPreMatchAnalysis(String matchId) {
        String prompt = "Write a fun, generic pre-match preview for a random soccer game in 2 sentences.";
        return askOpenAi(prompt);
    }

    public String getPostMatchSummary(String matchId) {
        String prompt = "Write a short, generic post-match recap for a random soccer game in 2 sentences.";
        return askOpenAi(prompt);
    }

    private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(Arrays.asList(new ChatMessage("user", prompt)))
                .maxTokens(150)
                .temperature(0.7)
                .build();

        return openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}
