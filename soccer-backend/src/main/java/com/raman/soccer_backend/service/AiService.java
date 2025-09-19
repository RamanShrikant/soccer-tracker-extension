package com.raman.soccer_backend.service;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAI;
import com.openai.types.chat.ChatCompletionCreateParams;
import com.openai.types.chat.ChatCompletion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final OpenAIClient client;

    public AiService(@Value("${openai.api.key}") String apiKey) {
        this.client = OpenAI.builder()
                .apiKey(apiKey)
                .build();
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
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o-mini")
                .addMessage(ChatCompletionCreateParams.Message.ofUser(prompt))
                .maxTokens(150L)
                .temperature(0.7)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        return completion.getChoices().get(0).getMessage().getContent();
    }
}
