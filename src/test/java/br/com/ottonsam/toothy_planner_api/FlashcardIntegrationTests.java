package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleData;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardGenerationAiClient;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardGenerationAiRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardGenerationAiResult;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.GeneratedFlashcardData;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FlashcardIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;
    private final FakeFlashcardGenerationAiClient aiClient;

    @Autowired
    FlashcardIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository,
            FlashcardGenerationAiClient aiClient) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.aiClient = (FakeFlashcardGenerationAiClient) aiClient;
    }

    @BeforeEach
    void resetAiClient() {
        aiClient.reset();
    }

    @Test
    void createsVocabularyDeckWithBatchesAndPassesAllGeneratedTermsToDeepSeek() throws Exception {
        var userCookie = login("flashcards-vocabulary@example.com");
        aiClient.autoGenerate();

        var jobId = createGenerationJob(userCookie, "Viagem", "viagem", "VOCABULARY", 250);
        awaitJob(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(250))
                .andExpect(jsonPath("$.createdCount").value(250))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batches.length()").value(3))
                .andExpect(jsonPath("$.batches[0].requestedCount").value(100))
                .andExpect(jsonPath("$.batches[1].requestedCount").value(100))
                .andExpect(jsonPath("$.batches[2].requestedCount").value(50));

        assertThat(aiClient.requests()).hasSize(13);
        assertThat(aiClient.requests().get(0).alreadyGeneratedTerms()).isEmpty();
        assertThat(aiClient.requests().get(0).requestedCount()).isEqualTo(20);
        assertThat(aiClient.requests().get(5).alreadyGeneratedTerms()).hasSize(100);
        assertThat(aiClient.requests().get(10).alreadyGeneratedTerms()).hasSize(200);
        assertThat(aiClient.requests().get(12).requestedCount()).isEqualTo(10);

        var deckId = jobDeckId(userCookie, jobId);
        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/generation-status", deckId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.deckId").value(deckId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batches.length()").value(3));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", deckId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(250))
                .andExpect(jsonPath("$.totalPages").value(25))
                .andExpect(jsonPath("$.content[0].word").value("word-1"))
                .andExpect(jsonPath("$.content[0].translation").value("traducao word-1"))
                .andExpect(jsonPath("$.content[0].examples[0].translation").exists())
                .andExpect(jsonPath("$.content[0].tags[0].name").value("viagem"));

        mockMvc.perform(get("/api/v1/flashcards/tags").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("viagem"));
    }

    @Test
    void discardsDuplicatesAndRetriesRemainingCards() throws Exception {
        var userCookie = login("flashcards-duplicates@example.com");
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(
                List.of(vocabulary("airport"), vocabulary("passport"), vocabulary("airport"))));
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("hotel"))));

        var jobId = createGenerationJob(userCookie, "Viagem duplicada", "viagem", "VOCABULARY", 3);
        awaitJob(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(3))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(aiClient.requests()).hasSize(2);
        assertThat(aiClient.requests().get(1).requestedCount()).isEqualTo(1);
        assertThat(aiClient.requests().get(1).alreadyGeneratedTerms()).containsExactlyInAnyOrder("airport", "passport");
    }

    @Test
    void retriesInvalidAiResponseAndCompletesGeneration() throws Exception {
        var userCookie = login("flashcards-retry@example.com");
        aiClient.enqueueFailures(1, "DeepSeek flashcard generation response is invalid");
        aiClient.autoGenerate();

        var jobId = createGenerationJob(userCookie, "Retry", "viagem", "VOCABULARY", 20);
        awaitJob(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(20))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batches[0].createdCount").value(20))
                .andExpect(jsonPath("$.batches[0].status").value("COMPLETED"));

        assertThat(aiClient.requests()).hasSize(2);
    }

    @Test
    void preservesCardsAndMarksPartialGenerationWhenLaterRequestsFail() throws Exception {
        var userCookie = login("flashcards-partial@example.com");
        aiClient.enqueue(request -> generatedVocabulary("partial", request.requestedCount()));
        aiClient.enqueueFailures(3, "DeepSeek flashcard generation response is invalid");

        var jobId = createGenerationJob(userCookie, "Partial", "viagem", "VOCABULARY", 200);
        awaitJob(userCookie, jobId);

        var job = getJson(userCookie, "/api/v1/flashcards/generation-jobs/%s".formatted(jobId));
        assertThat(job.path("createdCount").asInt()).isEqualTo(20);
        assertThat(job.path("status").asText()).isEqualTo("PARTIAL_COMPLETED");
        assertThat(job.path("batches").get(0).path("createdCount").asInt()).isEqualTo(20);
        assertThat(job.path("batches").get(0).path("status").asText()).isEqualTo("PARTIAL_COMPLETED");
        assertThat(job.path("batches").get(1).path("status").asText()).isEqualTo("CANCELED");

        var deckId = UUID.fromString(job.path("deckId").asText());
        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", deckId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void marksJobAndOpenBatchesWhenFirstAiRequestFails() throws Exception {
        var userCookie = login("flashcards-total-failure@example.com");
        aiClient.enqueueFailures(3, "DeepSeek flashcard generation response is invalid");

        var jobId = createGenerationJob(userCookie, "Failure", "viagem", "VOCABULARY", 200);
        awaitJob(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("DeepSeek flashcard generation response is invalid"))
                .andExpect(jsonPath("$.batches[0].status").value("FAILED"))
                .andExpect(jsonPath("$.batches[1].status").value("CANCELED"));
    }

    @Test
    void generatesIrregularVerbAndExpressionDecks() throws Exception {
        var userCookie = login("flashcards-types@example.com");

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(irregularVerb("go"))));
        var irregularJobId = createGenerationJob(userCookie, "Irregulares", "verbos comuns", "IRREGULAR_VERBS", 1);
        awaitJob(userCookie, irregularJobId);
        var irregularDeckId = jobDeckId(userCookie, irregularJobId);

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", irregularDeckId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("IRREGULAR_VERBS"))
                .andExpect(jsonPath("$.content[0].baseVerb").value("go"))
                .andExpect(jsonPath("$.content[0].pastSimple").value("went"))
                .andExpect(jsonPath("$.content[0].pastParticiple").value("gone"))
                .andExpect(jsonPath("$.content[0].translation").value("ir"));

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(expression("How are you?"))));
        var expressionJobId = createGenerationJob(userCookie, "Expressoes", "conversacao", "EXPRESSIONS", 1);
        awaitJob(userCookie, expressionJobId);
        var expressionDeckId = jobDeckId(userCookie, expressionJobId);

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", expressionDeckId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("EXPRESSIONS"))
                .andExpect(jsonPath("$.content[0].expression").value("How are you?"))
                .andExpect(jsonPath("$.content[0].translation").value("Como voce esta?"));
    }

    @Test
    void createsManualCardsForEveryDeckTypeAndPaginatesTenAtATime() throws Exception {
        var userCookie = login("flashcards-manual@example.com");

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"))));
        var vocabularyJobId = createGenerationJob(userCookie, "Manual vocabulary", "travel", "VOCABULARY", 1);
        awaitJob(userCookie, vocabularyJobId);
        var vocabularyDeckId = jobDeckId(userCookie, vocabularyJobId);

        for (var index = 1; index <= 11; index++) {
            var request = post("/api/v1/flashcards/decks/{deckId}/cards", vocabularyDeckId)
                    .cookie(userCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(
                            "word",
                            "manual-" + index,
                            "translation",
                            "manual traducao " + index,
                            "phonetic",
                            "/manual/",
                            "level",
                            "A1",
                            "usageNote",
                            "Manual card",
                            "examples",
                            List.of(Map.of(
                                    "text", "Manual example " + index,
                                    "translation", "Exemplo manual " + index)),
                            "tags",
                            List.of("manual", "travel"))));
            var result = mockMvc.perform(request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.active").value(true));
            if (index == 1) {
                result.andExpect(jsonPath("$.type").value("VOCABULARY"))
                        .andExpect(jsonPath("$.word").value("manual-1"))
                        .andExpect(jsonPath("$.examples[0].text").value("Manual example 1"))
                        .andExpect(jsonPath("$.tags.length()").value(2));
            }
        }

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", vocabularyDeckId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", vocabularyDeckId)
                        .param("page", "1")
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(irregularVerb("go"))));
        var irregularJobId = createGenerationJob(userCookie, "Manual verbs", "verbs", "IRREGULAR_VERBS", 1);
        awaitJob(userCookie, irregularJobId);
        var irregularDeckId = jobDeckId(userCookie, irregularJobId);
        mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", irregularDeckId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "baseVerb", "write",
                                "pastSimple", "wrote",
                                "pastParticiple", "written",
                                "translation", "escrever",
                                "phonetic", "/raɪt/",
                                "examples", List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IRREGULAR_VERBS"))
                .andExpect(jsonPath("$.phonetic").value("/raɪt/"))
                .andExpect(jsonPath("$.examples.length()").value(0));

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(expression("How are you?"))));
        var expressionJobId = createGenerationJob(userCookie, "Manual expressions", "conversation", "EXPRESSIONS", 1);
        awaitJob(userCookie, expressionJobId);
        var expressionDeckId = jobDeckId(userCookie, expressionJobId);
        mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", expressionDeckId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "expression", "See you later",
                                "translation", "Ate mais",
                                "phonetic", "/siː juː ˈleɪtər/",
                                "examples", List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPRESSIONS"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.phonetic").value("/siː juː ˈleɪtər/"));
    }

    @Test
    void validatesManualCardsAndPagination() throws Exception {
        var userCookie = login("flashcards-manual-validation@example.com");
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"))));
        var jobId = createGenerationJob(userCookie, "Validation", "travel", "VOCABULARY", 1);
        awaitJob(userCookie, jobId);
        var deckId = jobDeckId(userCookie, jobId);

        mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", deckId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "expression", "How are you?",
                                "translation", "Como voce esta?"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flashcard word is required"));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", deckId)
                        .param("page", "-1")
                        .cookie(userCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flashcard page must be greater than or equal to zero"));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/cards", deckId)
                        .param("size", "101")
                        .cookie(userCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flashcard page size must be between 1 and 100"));
    }

    @Test
    void editsManualCardWithoutResettingReviewHistory() throws Exception {
        var userCookie = login("flashcards-manual-history@example.com");
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"))));
        var jobId = createGenerationJob(userCookie, "History", "travel", "VOCABULARY", 1);
        awaitJob(userCookie, jobId);
        var deckId = jobDeckId(userCookie, jobId);

        var created = mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", deckId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word",
                                "ticket",
                                "translation",
                                "bilhete",
                                "examples",
                                List.of(Map.of(
                                        "text", "I bought a ticket.",
                                        "translation", "Eu comprei um bilhete.")),
                                "tags",
                                List.of("travel")))))
                .andExpect(status().isCreated())
                .andReturn();
        var cardId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("id")
                .asText());

        mockMvc.perform(post("/api/v1/flashcards/cards/{cardId}/answer", cardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", "GOOD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.correctCount").value(1));

        mockMvc.perform(put("/api/v1/flashcards/cards/{cardId}", cardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "EXPRESSIONS",
                                "word", "ticket edited",
                                "expression", "ignored expression",
                                "translation", "bilhete editado",
                                "active", false,
                                "examples", List.of(),
                                "tags", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("VOCABULARY"))
                .andExpect(jsonPath("$.word").value("ticket edited"))
                .andExpect(jsonPath("$.expression").isEmpty())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.examples.length()").value(0))
                .andExpect(jsonPath("$.tags.length()").value(0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.correctCount").value(1));
    }

    @Test
    void reviewsNextCardAnswersAndCalculatesMetrics() throws Exception {
        var userCookie = login("flashcards-review@example.com");
        aiClient.enqueue(
                request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"), vocabulary("hotel"))));
        var jobId = createGenerationJob(userCookie, "Review", "viagem", "VOCABULARY", 2);
        awaitJob(userCookie, jobId);
        var deckId = jobDeckId(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/review-ratings").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("AGAIN"))
                .andExpect(jsonPath("$[3].key").value("EASY"));

        var nextCard = mockMvc.perform(post("/api/v1/flashcards/review/next-card")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deckId", deckId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(BigDecimal.valueOf(50.00)))
                .andExpect(jsonPath("$.card.lastSeenAt").exists())
                .andReturn()
                .getResponse();
        var cardId = UUID.fromString(objectMapper
                .readTree(nextCard.getContentAsString())
                .path("card")
                .path("id")
                .asText());
        var cardsAfterSelection = getJson(userCookie, "/api/v1/flashcards/decks/%s/cards".formatted(deckId))
                .path("content");
        var seenCards = 0;
        for (var card : cardsAfterSelection) {
            if (!card.path("lastSeenAt").isNull()) {
                seenCards++;
                assertThat(card.path("id").asText()).isEqualTo(cardId.toString());
            }
        }
        assertThat(seenCards).isEqualTo(1);

        mockMvc.perform(post("/api/v1/flashcards/cards/{cardId}/answer", cardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", "AGAIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wrongCount").value(1))
                .andExpect(jsonPath("$.consecutiveWrong").value(1))
                .andExpect(jsonPath("$.difficulty").value(2.00))
                .andExpect(jsonPath("$.nextReviewAt").exists());

        mockMvc.perform(post("/api/v1/flashcards/cards/{cardId}/answer", cardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", "GOOD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.consecutiveCorrect").value(1))
                .andExpect(jsonPath("$.consecutiveWrong").value(0));

        mockMvc.perform(get("/api/v1/flashcards/metrics").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDecks").value(1))
                .andExpect(jsonPath("$.activeCards").value(2))
                .andExpect(jsonPath("$.reviewedToday").value(2))
                .andExpect(jsonPath("$.totalCorrect").value(1))
                .andExpect(jsonPath("$.totalWrong").value(1))
                .andExpect(jsonPath("$.accuracyRate").value(50.00))
                .andExpect(jsonPath("$.neverSeenCards").value(1));
    }

    @Test
    void selectsOnlyActiveCardsFromRequestedOwnedDeckAndReturnsNotFoundWhenNoneRemain() throws Exception {
        var userCookie = login("flashcards-weighted-owner@example.com");
        var otherCookie = login("flashcards-weighted-other@example.com");
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"))));
        var firstJobId = createGenerationJob(userCookie, "Weighted", "travel", "VOCABULARY", 1);
        awaitJob(userCookie, firstJobId);
        var firstDeckId = jobDeckId(userCookie, firstJobId);
        var generatedCard = getJson(userCookie, "/api/v1/flashcards/decks/%s/cards".formatted(firstDeckId))
                .path("content")
                .get(0);

        var manualCardResponse = mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", firstDeckId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", "hotel",
                                "translation", "hotel"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        var manualCardId = UUID.fromString(objectMapper
                .readTree(manualCardResponse.getContentAsString())
                .path("id")
                .asText());

        mockMvc.perform(put(
                                "/api/v1/flashcards/cards/{cardId}",
                                UUID.fromString(generatedCard.path("id").asText()))
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", generatedCard.path("word").asText(),
                                "translation", generatedCard.path("translation").asText(),
                                "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("restaurant"))));
        var secondJobId = createGenerationJob(userCookie, "Other deck", "food", "VOCABULARY", 1);
        awaitJob(userCookie, secondJobId);

        mockMvc.perform(post("/api/v1/flashcards/review/next-card")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deckId", firstDeckId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.id").value(manualCardId.toString()))
                .andExpect(jsonPath("$.score").value(BigDecimal.valueOf(50.00)));

        mockMvc.perform(post("/api/v1/flashcards/review/next-card")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deckId", firstDeckId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard deck not found"));

        mockMvc.perform(put("/api/v1/flashcards/cards/{cardId}", manualCardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", "hotel",
                                "translation", "hotel",
                                "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/flashcards/review/next-card")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deckId", firstDeckId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No flashcards available for review"));
    }

    @Test
    void isolatesDeckJobsAndCardsByUserAndAllowsManualEdit() throws Exception {
        var userCookie = login("flashcards-owner@example.com");
        var otherCookie = login("flashcards-other@example.com");
        aiClient.enqueue(request -> new FlashcardGenerationAiResult(List.of(vocabulary("airport"))));
        var jobId = createGenerationJob(userCookie, "Privado", "viagem", "VOCABULARY", 1);
        awaitJob(userCookie, jobId);
        var deckId = jobDeckId(userCookie, jobId);
        var cards = getJson(userCookie, "/api/v1/flashcards/decks/%s/cards".formatted(deckId));
        var cardId = UUID.fromString(cards.path("content").get(0).get("id").asText());

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard generation job not found"));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}", deckId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard deck not found"));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/generation-status", deckId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard generation job not found"));

        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/generation-status", UUID.randomUUID())
                        .cookie(userCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard generation job not found"));

        mockMvc.perform(get("/api/v1/flashcards/cards/{cardId}", cardId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard not found"));

        mockMvc.perform(post("/api/v1/flashcards/decks/{deckId}/cards", deckId)
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("word", "private", "translation", "privado"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard deck not found"));

        mockMvc.perform(put("/api/v1/flashcards/cards/{cardId}", cardId)
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("word", "private", "translation", "privado"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard not found"));

        mockMvc.perform(delete("/api/v1/flashcards/cards/{cardId}", cardId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flashcard not found"));

        mockMvc.perform(put("/api/v1/flashcards/cards/{cardId}", cardId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word",
                                "airport edited",
                                "translation",
                                "aeroporto editado",
                                "active",
                                true,
                                "examples",
                                List.of(Map.of(
                                        "text", "The airport is open.",
                                        "translation", "O aeroporto esta aberto."))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("airport edited"))
                .andExpect(jsonPath("$.translation").value("aeroporto editado"));

        mockMvc.perform(delete("/api/v1/flashcards/cards/{cardId}", cardId).cookie(userCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidGenerationAndMarksFailedJobWhenAiFails() throws Exception {
        var userCookie = login("flashcards-validation@example.com");

        mockMvc.perform(post("/api/v1/flashcards/decks/generate")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Invalido",
                                "context", "viagem",
                                "targetLanguage", "en",
                                "baseLanguage", "pt-BR",
                                "type", "VOCABULARY",
                                "cardCount", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flashcard generation card count must be greater than zero"));

        aiClient.enqueueFailures(3, "DeepSeek flashcard generation failed");
        var jobId = createGenerationJob(userCookie, "Falha", "viagem", "VOCABULARY", 1);
        awaitJob(userCookie, jobId);

        mockMvc.perform(get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("DeepSeek flashcard generation failed"));

        var deckId = jobDeckId(userCookie, jobId);
        mockMvc.perform(get("/api/v1/flashcards/decks/{deckId}/generation-status", deckId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.batches[0].status").value("FAILED"));
    }

    private UUID createGenerationJob(Cookie accessToken, String name, String context, String type, int cardCount)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/flashcards/decks/generate")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "context", context,
                                "targetLanguage", "en",
                                "baseLanguage", "pt-BR",
                                "type", type,
                                "cardCount", cardCount))))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse();
        return UUID.fromString(
                objectMapper.readTree(response.getContentAsString()).get("id").asText());
    }

    private UUID jobDeckId(Cookie accessToken, UUID jobId) throws Exception {
        var response = mockMvc.perform(
                        get("/api/v1/flashcards/generation-jobs/{jobId}", jobId).cookie(accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return UUID.fromString(objectMapper
                .readTree(response.getContentAsString())
                .get("deckId")
                .asText());
    }

    private JsonNode awaitJob(Cookie accessToken, UUID jobId) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        JsonNode job = null;
        while (System.nanoTime() < deadline) {
            job = getJson(accessToken, "/api/v1/flashcards/generation-jobs/%s".formatted(jobId));
            var status = job.path("status").asText();
            if (!"PENDING".equals(status) && !"RUNNING".equals(status)) {
                return job;
            }
            Thread.sleep(50);
        }
        return job;
    }

    private JsonNode getJson(Cookie accessToken, String path) throws Exception {
        var response = mockMvc.perform(get(path).cookie(accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return objectMapper.readTree(response.getContentAsString());
    }

    private Cookie login(String email) throws Exception {
        createAndActivateUser(email);
        var response = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return response.getCookie("access_token");
    }

    private void createAndActivateUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "Strong1!"))))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail(email).orElseThrow();
        var code = activationCodeRepository.findAll().stream()
                .filter(activationCode -> activationCode.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow()
                .getCode();

        mockMvc.perform(post("/api/v1/users/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static GeneratedFlashcardData vocabulary(String word) {
        return new GeneratedFlashcardData(
                word,
                null,
                null,
                null,
                null,
                "traducao " + word,
                null,
                "A1",
                List.of("viagem"),
                List.of(new FlashcardExampleData("Example with " + word + ".", "Exemplo com " + word + ".")),
                null);
    }

    private static GeneratedFlashcardData irregularVerb(String baseVerb) {
        return new GeneratedFlashcardData(
                null,
                baseVerb,
                "went",
                "gone",
                null,
                "ir",
                null,
                null,
                List.of("irregular-verbs"),
                List.of(new FlashcardExampleData("I went home.", "Eu fui para casa.")),
                null);
    }

    private static GeneratedFlashcardData expression(String expression) {
        return new GeneratedFlashcardData(
                null,
                null,
                null,
                null,
                expression,
                "Como voce esta?",
                null,
                null,
                List.of("greetings"),
                List.of(new FlashcardExampleData("Hi, Ana. How are you?", "Oi, Ana. Como voce esta?")),
                "Common greeting.");
    }

    private static FlashcardGenerationAiResult generatedVocabulary(String prefix, int count) {
        var cards = new ArrayList<GeneratedFlashcardData>();
        for (var index = 1; index <= count; index++) {
            cards.add(vocabulary(prefix + "-" + index));
        }
        return new FlashcardGenerationAiResult(cards);
    }

    static class FakeFlashcardGenerationAiClient implements FlashcardGenerationAiClient {

        private final ArrayDeque<Function<FlashcardGenerationAiRequest, FlashcardGenerationAiResult>> responses =
                new ArrayDeque<>();
        private final List<FlashcardGenerationAiRequest> requests = new ArrayList<>();
        private boolean autoGenerate;
        private int sequence;

        @Override
        public FlashcardGenerationAiResult generate(UUID userId, FlashcardGenerationAiRequest request) {
            requests.add(request);
            if (!responses.isEmpty()) {
                return responses.removeFirst().apply(request);
            }
            if (autoGenerate) {
                var cards = new ArrayList<GeneratedFlashcardData>();
                for (int index = 0; index < request.requestedCount(); index++) {
                    sequence++;
                    cards.add(vocabulary("word-" + sequence));
                }
                return new FlashcardGenerationAiResult(cards);
            }
            return new FlashcardGenerationAiResult(List.of());
        }

        void enqueue(Function<FlashcardGenerationAiRequest, FlashcardGenerationAiResult> response) {
            responses.add(response);
        }

        void autoGenerate() {
            autoGenerate = true;
        }

        void enqueueFailures(int count, String message) {
            for (var index = 0; index < count; index++) {
                responses.add(request -> {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, message);
                });
            }
        }

        List<FlashcardGenerationAiRequest> requests() {
            return List.copyOf(requests);
        }

        void reset() {
            responses.clear();
            requests.clear();
            autoGenerate = false;
            sequence = 0;
        }
    }

    @TestConfiguration
    static class TestFlashcardConfiguration {

        @Bean
        @Primary
        FlashcardGenerationAiClient flashcardGenerationAiClient() {
            return new FakeFlashcardGenerationAiClient();
        }

        @Bean
        @Primary
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
