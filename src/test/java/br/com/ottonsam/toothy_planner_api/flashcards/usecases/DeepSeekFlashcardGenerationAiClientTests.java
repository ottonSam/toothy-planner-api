package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DeepSeekFlashcardGenerationAiClientTests {

    private final DeepSeekFlashcardGenerationAiClient client =
            new DeepSeekFlashcardGenerationAiClient(new ObjectMapper(), null);

    @Test
    void requiresCardsArrayInSystemPrompt() {
        assertThat(client.systemPrompt()).contains("{\"cards\":[...]}");
    }

    @Test
    void parsesObjectAndArrayRootResponses() {
        var card = """
                {
                  "word": "airport",
                  "translation": "aeroporto",
                  "tags": ["travel"],
                  "examples": [
                    {
                      "text": "The airport is busy.",
                      "translation": "O aeroporto esta movimentado."
                    }
                  ]
                }
                """;

        var objectResult = client.parseContent(FlashcardDeckType.VOCABULARY, "{\"cards\":[" + card + "]}");
        var arrayResult = client.parseContent(FlashcardDeckType.VOCABULARY, "[" + card + "]");

        assertThat(objectResult.cards()).hasSize(1);
        assertThat(arrayResult.cards()).hasSize(1);
        assertThat(objectResult.cards().getFirst().word()).isEqualTo("airport");
        assertThat(arrayResult.cards().getFirst().word()).isEqualTo("airport");
    }
}
