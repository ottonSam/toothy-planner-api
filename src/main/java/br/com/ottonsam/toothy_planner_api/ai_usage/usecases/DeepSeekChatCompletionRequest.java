package br.com.ottonsam.toothy_planner_api.ai_usage.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DeepSeekChatCompletionRequest(
        UUID userId,
        AiFeature feature,
        List<Map<String, String>> messages,
        double temperature,
        int maxTokens,
        String failureMessage,
        String timeoutMessage) {

    public DeepSeekChatCompletionRequest {
        messages = immutableMessages(messages);
    }

    @Override
    public List<Map<String, String>> messages() {
        return immutableMessages(messages);
    }

    private static List<Map<String, String>> immutableMessages(List<Map<String, String>> messages) {
        return messages.stream().map(Map::copyOf).toList();
    }
}
