package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseTextAiClient;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseTextClassification;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseTextType;
import br.com.ottonsam.toothy_planner_api.report.usecases.WeeklyReportAiClient;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FinancialManagerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;
    private final FakeExpenseTextAiClient expenseTextAiClient;

    @Autowired
    FinancialManagerIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository,
            ExpenseTextAiClient expenseTextAiClient) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.expenseTextAiClient = (FakeExpenseTextAiClient) expenseTextAiClient;
    }

    @BeforeEach
    void resetAiClients() {
        expenseTextAiClient.reset();
    }

    @Test
    void listsFixedCategoriesAndManagesWalletsOnlyForAuthenticatedUser() throws Exception {
        var userCookie = login("financial-owner@example.com");
        var otherCookie = login("financial-other@example.com");

        mockMvc.perform(get("/api/v1/financial-manager/categories").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("ALIMENTACAO"))
                .andExpect(jsonPath("$[0].name").value("Alimentacao"))
                .andExpect(jsonPath("$[0].color").exists())
                .andExpect(jsonPath("$[0].icon").exists());

        mockMvc.perform(post("/api/v1/financial-manager/categories")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Mercado", "color", "#111111", "icon", "copy"))))
                .andExpect(status().isMethodNotAllowed());

        var walletId = createWallet(userCookie, "Carteira pessoal", 3000, "2026-06-16", 15);

        mockMvc.perform(post("/api/v1/financial-manager/wallets")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description",
                                "carteira pessoal",
                                "spendingGoal",
                                1000,
                                "startsAt",
                                "2026-06-16",
                                "targetSpendingDay",
                                15))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Wallet description already exists"));

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}", walletId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));

        mockMvc.perform(put("/api/v1/financial-manager/wallets/{walletId}", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description",
                                "Carteira principal",
                                "spendingGoal",
                                3500,
                                "startsAt",
                                "2026-06-16",
                                "targetSpendingDay",
                                10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Carteira principal"))
                .andExpect(jsonPath("$.startsAt").value("2026-06-16"))
                .andExpect(jsonPath("$.targetSpendingDay").value(10));
    }

    @Test
    void createsCyclesFromWalletStartDateAndCalculatesTargetMetrics() throws Exception {
        var userCookie = login("financial-cycles@example.com");
        var walletId = createWallet(userCookie, "Carteira ciclos", 3000, "2026-06-16", 15);

        createExpense(userCookie, walletId, "ALIMENTACAO", "Mercado julho", 250.90, "2026-07-13");
        createExpense(userCookie, walletId, "ALIMENTACAO", "Mercado agosto", 100.00, "2026-07-16");

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        assertThat(cycles).hasSize(2);
        assertThat(cycles.get(0).get("referenceMonth").asInt()).isEqualTo(7);
        assertThat(cycles.get(0).get("startsAt").asText()).isEqualTo("2026-06-16");
        assertThat(cycles.get(0).get("endsAt").asText()).isEqualTo("2026-07-15");
        assertThat(cycles.get(0).get("targetSpendingDate").asText()).isEqualTo("2026-07-15");
        assertThat(cycles.get(1).get("referenceMonth").asInt()).isEqualTo(8);
        assertThat(cycles.get(1).get("startsAt").asText()).isEqualTo("2026-07-16");
        assertThat(cycles.get(1).get("endsAt").asText()).isEqualTo("2026-08-15");

        var julyCycleId = findCycleId(cycles, 7, 2026);
        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics",
                                walletId,
                                julyCycleId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(250.90))
                .andExpect(jsonPath("$.remainingAmount").value(2749.10))
                .andExpect(jsonPath("$.remainingDailyAmount").value(916.37))
                .andExpect(jsonPath("$.spentUntilTargetDate").value(250.90))
                .andExpect(jsonPath("$.spentAfterTargetDate").value(0.00))
                .andExpect(jsonPath("$.oneTimeTotal").value(250.90));

        var anchoredWalletId = createWallet(userCookie, "Carteira dia 28", 2000, "2026-07-28", 10);
        createExpense(userCookie, anchoredWalletId, "SERVICOS", "Inicio", 10.00, "2026-07-28");
        createExpense(userCookie, anchoredWalletId, "SERVICOS", "Fim", 20.00, "2026-08-27");
        createExpense(userCookie, anchoredWalletId, "SERVICOS", "Outro ciclo", 30.00, "2026-08-28");

        var anchoredCycles =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(anchoredWalletId));
        assertThat(anchoredCycles).hasSize(2);
        assertThat(anchoredCycles.get(0).get("referenceMonth").asInt()).isEqualTo(8);
        assertThat(anchoredCycles.get(0).get("startsAt").asText()).isEqualTo("2026-07-28");
        assertThat(anchoredCycles.get(0).get("endsAt").asText()).isEqualTo("2026-08-27");
        assertThat(anchoredCycles.get(0).get("targetSpendingDate").asText()).isEqualTo("2026-08-10");
        assertThat(anchoredCycles.get(1).get("referenceMonth").asInt()).isEqualTo(9);
    }

    @Test
    void calculatesCycleSpendingByCategoryForChart() throws Exception {
        var userCookie = login("financial-category-chart@example.com");
        var otherCookie = login("financial-category-chart-other@example.com");
        var walletId = createWallet(userCookie, "Carteira grafico categorias", 3000, "2026-06-16", 15);

        createExpense(userCookie, walletId, "ALIMENTACAO", "Mercado", 100.00, "2026-07-10");
        createInstallmentExpenseByTotal(
                userCookie, walletId, "ALIMENTACAO", "Compra parcelada", 100.00, 2, "2026-07-11");
        createRecurringExpense(userCookie, walletId, "ALIMENTACAO", "Assinatura", 50.00, "2026-07-12");
        createExpense(userCookie, walletId, "SAUDE", "Farmacia", 100.00, "2026-07-13");
        createExpense(userCookie, walletId, "TRANSPORTE", "Taxi", 100.00, "2026-07-14");
        createExpense(userCookie, walletId, "ALIMENTACAO", "Outro ciclo", 300.00, "2026-07-16");

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var julyCycleId = findCycleId(cycles, 7, 2026);

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics",
                                walletId,
                                julyCycleId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(400.00))
                .andExpect(jsonPath("$.spendingByCategory.length()").value(3))
                .andExpect(jsonPath("$.spendingByCategory[0].category.key").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.spendingByCategory[0].totalSpent").value(200.00))
                .andExpect(jsonPath("$.spendingByCategory[0].percentage").value(50.00))
                .andExpect(jsonPath("$.spendingByCategory[1].category.key").value("SAUDE"))
                .andExpect(jsonPath("$.spendingByCategory[1].totalSpent").value(100.00))
                .andExpect(jsonPath("$.spendingByCategory[1].percentage").value(25.00))
                .andExpect(jsonPath("$.spendingByCategory[2].category.key").value("TRANSPORTE"));

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics",
                                walletId,
                                julyCycleId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    @Test
    void createsInstallmentsRecurringExpensesAndCancelsFollowingRecurringCycles() throws Exception {
        var userCookie = login("financial-installments@example.com");
        var walletId = createWallet(userCookie, "Carteira recorrente", 1000, "2026-06-16", 15);

        var recurringExpenseId =
                createRecurringExpense(userCookie, walletId, "SERVICOS", "Internet", 99.90, "2026-07-13");
        createInstallmentExpenseByTotal(userCookie, walletId, "SERVICOS", "Compra parcelada", 100.00, 3, "2026-07-13");

        var expenses = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByType(expenses, "INSTALLMENT")).isEqualTo(3);
        assertThat(countByType(expenses, "RECURRING")).isEqualTo(3);
        assertThat(expenses.findValues("amount").stream().map(JsonNode::asText)).contains("33.34", "99.9");

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var julyCycleId = findCycleId(cycles, 7, 2026);

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics",
                                walletId,
                                julyCycleId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentTotalFromCurrentCycle").value(100.02))
                .andExpect(jsonPath("$.recurringMonthlyTotal").value(99.90));

        mockMvc.perform(post(
                                "/api/v1/financial-manager/wallets/{walletId}/recurring-expenses/{recurringExpenseId}/cancel",
                                walletId,
                                recurringExpenseId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("cycleId", julyCycleId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        var expensesAfterCancel =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByType(expensesAfterCancel, "RECURRING")).isEqualTo(1);

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}/metrics", walletId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRecurringMonthlyTotal").value(0.00));
    }

    @Test
    void deletesRecurringOccurrenceFromCurrentCycleForward() throws Exception {
        var userCookie = login("financial-delete-recurring@example.com");
        var otherCookie = login("financial-delete-recurring-other@example.com");
        var walletId = createWallet(userCookie, "Carteira exclusao recorrente", 2000, "2026-06-16", 15);

        createExpense(userCookie, walletId, "SERVICOS", "Criar julho", 1.00, "2026-07-13");
        createExpense(userCookie, walletId, "SERVICOS", "Criar agosto", 1.00, "2026-07-16");
        createExpense(userCookie, walletId, "SERVICOS", "Criar setembro", 1.00, "2026-08-16");
        createExpense(userCookie, walletId, "SERVICOS", "Criar outubro", 1.00, "2026-09-16");

        var deletedRecurrenceId =
                createRecurringExpense(userCookie, walletId, "SERVICOS", "Internet excluida", 99.90, "2026-07-13");
        createRecurringExpense(userCookie, walletId, "SERVICOS", "Streaming mantido", 49.90, "2026-07-13");

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var augustCycleId = findCycleId(cycles, 8, 2026);
        var expenses = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        var augustRecurringExpenseId = findExpenseId(expenses, "Internet excluida", augustCycleId);

        mockMvc.perform(delete(
                                "/api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}",
                                walletId,
                                augustRecurringExpenseId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Expense not found"));

        deleteExpense(userCookie, walletId, augustRecurringExpenseId);

        var expensesAfterDelete =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByDescription(expensesAfterDelete, "Internet excluida")).isEqualTo(1);
        assertThat(countByDescription(expensesAfterDelete, "Streaming mantido")).isEqualTo(4);

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/recurring-expenses/{recurringExpenseId}",
                                walletId,
                                deletedRecurrenceId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.canceledAt").value("2026-07-16"));

        createExpense(userCookie, walletId, "SERVICOS", "Criar novembro", 1.00, "2026-10-16");
        var expensesWithNovember =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByDescription(expensesWithNovember, "Internet excluida"))
                .isEqualTo(1);
        assertThat(countByDescription(expensesWithNovember, "Streaming mantido"))
                .isEqualTo(5);
    }

    @Test
    void listsAllExpenseTypesFromCycleAndSupportsIndividualManualEdit() throws Exception {
        var userCookie = login("financial-cycle-expenses@example.com");
        var otherCookie = login("financial-cycle-expenses-other@example.com");
        var walletId = createWallet(userCookie, "Carteira por ciclo", 2000, "2026-06-16", 15);

        createRecurringExpense(userCookie, walletId, "SERVICOS", "Recorrente", 90.00, "2026-07-11");
        createInstallmentExpenseByTotal(userCookie, walletId, "COMPRAS", "Parcelado", 200.00, 2, "2026-07-12");
        var expenseId = createExpense(userCookie, walletId, "ALIMENTACAO", "Pontual primeiro", 50.00, "2026-07-13");
        createExpense(userCookie, walletId, "ALIMENTACAO", "Pontual segundo", 60.00, "2026-07-13");
        createExpense(userCookie, walletId, "ALIMENTACAO", "Outro ciclo", 25.00, "2026-07-16");

        mockMvc.perform(put("/api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}", walletId, expenseId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category", "SAUDE",
                                "description", "Farmacia editada",
                                "amount", 70.00,
                                "expenseDate", "2026-07-13"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.key").value("SAUDE"))
                .andExpect(jsonPath("$.description").value("Farmacia editada"))
                .andExpect(jsonPath("$.amount").value(70.00));

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var julyCycleId = findCycleId(cycles, 7, 2026);
        var julyExpenses = getJson(
                userCookie, "/api/v1/financial-manager/wallets/%s/cycles/%s/expenses".formatted(walletId, julyCycleId));

        assertThat(julyExpenses).hasSize(4);
        assertThat(julyExpenses.findValuesAsText("type"))
                .containsExactly("RECURRING", "INSTALLMENT", "ONE_TIME", "ONE_TIME");
        assertThat(julyExpenses.findValuesAsText("description"))
                .containsExactly("Recorrente", "Parcelado", "Farmacia editada", "Pontual segundo");

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses",
                                walletId,
                                julyCycleId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    @Test
    void createsExpensesFromTextUsingAiClassification() throws Exception {
        var userCookie = login("financial-ai-text@example.com");
        var walletId = createWallet(userCookie, "Carteira IA", 2000, "2026-06-16", 15);

        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.ONE_TIME,
                ExpenseCategory.ALIMENTACAO,
                "Mercado",
                BigDecimal.valueOf(32),
                null,
                null,
                null,
                null,
                null,
                null));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "fui ao mercado e gastei 32 reais"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseCount").value(1))
                .andExpect(jsonPath("$.generatedExpenseCount").value(1))
                .andExpect(jsonPath("$.items[0].type").value("ONE_TIME"))
                .andExpect(jsonPath("$.items[0].expense.category.key").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.items[0].expense.source").value("AI_TEXT"))
                .andExpect(jsonPath("$.items[0].expense.expenseDate").value("2026-07-13"))
                .andExpect(jsonPath("$.generatedExpenses.length()").value(1));

        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.INSTALLMENT,
                ExpenseCategory.COMPRAS,
                "Compra parcelada",
                BigDecimal.valueOf(199),
                null,
                null,
                null,
                12,
                LocalDate.parse("2026-07-13"),
                null));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "comprei algo parcelado em 12 vezes de 199"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].type").value("INSTALLMENT"))
                .andExpect(
                        jsonPath("$.items[0].installmentExpense.category.key").value("COMPRAS"))
                .andExpect(jsonPath("$.generatedExpenses.length()").value(12))
                .andExpect(jsonPath("$.generatedExpenses[0].source").value("AI_TEXT"));

        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.RECURRING,
                ExpenseCategory.SERVICOS,
                "Internet",
                BigDecimal.valueOf(100),
                null,
                null,
                null,
                null,
                null,
                null));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "contratei um plano de internet de 100 reais"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].type").value("RECURRING"))
                .andExpect(jsonPath("$.items[0].recurringExpense.startsAt").value("2026-07-13"))
                .andExpect(jsonPath("$.generatedExpenses[0].source").value("AI_TEXT"));
    }

    @Test
    void separatesNormalizesAndCreatesMixedExpensesFromOneText() throws Exception {
        var userCookie = login("financial-ai-text-batch@example.com");
        var walletId = createWallet(userCookie, "Carteira IA em lote", 4000, "2026-06-16", 15);

        expenseTextAiClient.enqueueAll(List.of(
                new ExpenseTextClassification(
                        ExpenseTextType.RECURRING,
                        ExpenseCategory.SERVICOS,
                        "Streaming",
                        BigDecimal.valueOf(30),
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDate.parse("2026-07-13"),
                        "streaming mensal de 30 reais"),
                new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.ALIMENTACAO,
                        "Mercado",
                        BigDecimal.valueOf(80),
                        LocalDate.parse("2026-07-12"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "mercado de 80 reais ontem"),
                new ExpenseTextClassification(
                        ExpenseTextType.INSTALLMENT,
                        ExpenseCategory.COMPRAS,
                        "Notebook",
                        BigDecimal.valueOf(200),
                        null,
                        null,
                        null,
                        3,
                        LocalDate.parse("2026-07-13"),
                        null,
                        "notebook em 3 vezes de 200")));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "text",
                                "Assinei streaming por 30 mensais, ontem gastei 80 no mercado e comprei um notebook em 3 vezes de 200",
                                "referenceDate",
                                "2026-07-13"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseCount").value(3))
                .andExpect(jsonPath("$.generatedExpenseCount").value(7))
                .andExpect(jsonPath("$.items[0].sourceText").value("streaming mensal de 30 reais"))
                .andExpect(jsonPath("$.items[0].type").value("RECURRING"))
                .andExpect(jsonPath("$.items[0].generatedExpenses.length()").value(3))
                .andExpect(jsonPath("$.items[0].generatedExpenses[2].source").value("AI_TEXT"))
                .andExpect(jsonPath("$.items[1].expense.description").value("Mercado"))
                .andExpect(jsonPath("$.items[2].type").value("INSTALLMENT"))
                .andExpect(jsonPath("$.items[2].generatedExpenses.length()").value(3))
                .andExpect(jsonPath("$.generatedExpenses.length()").value(7));
    }

    @Test
    void createsTextExpensesAcrossInternalBatchesAndRejectsMoreThanTheLimit() throws Exception {
        var userCookie = login("financial-ai-text-limit@example.com");
        var walletId = createWallet(userCookie, "Carteira IA limite", 10000, "2026-06-16", 15);
        var classifications = IntStream.rangeClosed(1, 26)
                .mapToObj(index -> new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.OUTROS,
                        "Gasto " + index,
                        BigDecimal.valueOf(index),
                        LocalDate.parse("2026-07-13"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "gasto " + index))
                .toList();
        expenseTextAiClient.enqueueAll(classifications);

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "26 gastos identificados"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseCount").value(26))
                .andExpect(jsonPath("$.generatedExpenseCount").value(26))
                .andExpect(jsonPath("$.items[25].expense.description").value("Gasto 26"));

        expenseTextAiClient.enqueueAll(IntStream.rangeClosed(1, 51)
                .mapToObj(index -> new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.OUTROS,
                        "Excedente " + index,
                        BigDecimal.ONE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .toList());

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "51 gastos identificados"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Expense text supports at most 50 expenses"));

        var expenses = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(expenses).hasSize(26);
    }

    @Test
    void rejectsTextExpensesForAWalletOwnedByAnotherUser() throws Exception {
        var ownerCookie = login("financial-ai-owner@example.com");
        var otherCookie = login("financial-ai-other@example.com");
        var walletId = createWallet(ownerCookie, "Carteira privada IA", 1000, "2026-06-16", 15);
        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.ONE_TIME,
                ExpenseCategory.ALIMENTACAO,
                "Mercado",
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "mercado de 10 reais"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void doesNotExposeTheRemovedAudioExpenseEndpoint() throws Exception {
        var userCookie = login("financial-audio-removed@example.com");
        var walletId = createWallet(userCookie, "Carteira sem audio", 1000, "2026-06-16", 15);

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/audio", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("audioBase64", "AAAA", "contentType", "audio/webm"))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsInvalidRequestsAndDoesNotSaveWhenAiFails() throws Exception {
        var userCookie = login("financial-validation@example.com");
        var walletId = createWallet(userCookie, "Carteira validacoes", 1000, "2026-06-16", 15);

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Expense text is required"));

        expenseTextAiClient.enqueueAll(List.of());
        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "Hoje foi um dia tranquilo"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No expenses were identified in the text"));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/installment-expenses", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category",
                                "COMPRAS",
                                "description",
                                "Invalido",
                                "totalAmount",
                                100,
                                "installmentAmount",
                                10,
                                "installments",
                                10,
                                "firstExpenseDate",
                                "2026-07-13"))))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message").value("Inform either total amount or installment amount, but not both"));

        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.INSTALLMENT,
                ExpenseCategory.COMPRAS,
                "Compra parcelada invalida",
                BigDecimal.valueOf(199),
                null,
                null,
                null,
                null,
                null,
                null));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "comprei algo parcelado de 199"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(containsString("INSTALLMENT requires installments")))
                .andExpect(jsonPath("$.message").value(containsString("amount=199")))
                .andExpect(jsonPath("$.message").value(containsString("installments=null")));

        expenseTextAiClient.enqueueAll(List.of(
                new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.ALIMENTACAO,
                        "Gasto valido",
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.OUTROS,
                        "Gasto invalido",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "um gasto valido e outro sem valor"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(containsString("ONE_TIME requires amount")));

        expenseTextAiClient.enqueue(new ExpenseTextClassification(
                ExpenseTextType.ONE_TIME,
                ExpenseCategory.OUTROS,
                "Gasto negativo",
                BigDecimal.valueOf(-1),
                null,
                null,
                null,
                null,
                null,
                null));
        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "gasto negativo"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(containsString("amount must be greater than zero")));

        expenseTextAiClient.enqueueAll(List.of(
                new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.ALIMENTACAO,
                        "Primeiro gasto",
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                new ExpenseTextClassification(
                        ExpenseTextType.ONE_TIME,
                        ExpenseCategory.OUTROS,
                        "Gasto anterior a carteira",
                        BigDecimal.ONE,
                        LocalDate.parse("2026-06-15"),
                        null,
                        null,
                        null,
                        null,
                        null)));
        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "um gasto valido e outro anterior a carteira"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Expense date must be on or after wallet start date"));

        expenseTextAiClient.failNext();
        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses/text", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "gastei 50 reais"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("DeepSeek expense classification failed: upstream unavailable"));

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private UUID createWallet(
            Cookie accessToken, String description, int spendingGoal, String startsAt, int targetSpendingDay)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description", description,
                                "spendingGoal", spendingGoal,
                                "startsAt", startsAt,
                                "targetSpendingDay", targetSpendingDay))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createExpense(
            Cookie accessToken, UUID walletId, String category, String description, double amount, String expenseDate)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category", category,
                                "description", description,
                                "amount", amount,
                                "expenseDate", expenseDate))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createRecurringExpense(
            Cookie accessToken, UUID walletId, String category, String description, double amount, String startsAt)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/recurring-expenses", walletId)
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "category", category,
                                "description", description,
                                "amount", amount,
                                "startsAt", startsAt))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createInstallmentExpenseByTotal(
            Cookie accessToken,
            UUID walletId,
            String category,
            String description,
            double totalAmount,
            int installments,
            String firstExpenseDate)
            throws Exception {
        var response = mockMvc.perform(
                        post("/api/v1/financial-manager/wallets/{walletId}/installment-expenses", walletId)
                                .cookie(accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of(
                                        "category", category,
                                        "description", description,
                                        "totalAmount", totalAmount,
                                        "installments", installments,
                                        "firstExpenseDate", firstExpenseDate))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private JsonNode getJson(Cookie accessToken, String path) throws Exception {
        var response = mockMvc.perform(get(path).cookie(accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return objectMapper.readTree(response.getContentAsString());
    }

    private void deleteExpense(Cookie accessToken, UUID walletId, UUID expenseId) throws Exception {
        mockMvc.perform(delete("/api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}", walletId, expenseId)
                        .cookie(accessToken))
                .andExpect(status().isNoContent());
    }

    private long countByType(JsonNode expenses, String type) {
        return java.util.stream.StreamSupport.stream(expenses.spliterator(), false)
                .filter(expense -> type.equals(expense.get("type").asText()))
                .count();
    }

    private long countByDescription(JsonNode expenses, String description) {
        return java.util.stream.StreamSupport.stream(expenses.spliterator(), false)
                .filter(expense -> description.equals(expense.get("description").asText()))
                .count();
    }

    private UUID findExpenseId(JsonNode expenses, String description, UUID cycleId) {
        return java.util.stream.StreamSupport.stream(expenses.spliterator(), false)
                .filter(expense -> description.equals(expense.get("description").asText()))
                .filter(expense ->
                        cycleId.toString().equals(expense.get("cycleId").asText()))
                .map(expense -> UUID.fromString(expense.get("id").asText()))
                .findFirst()
                .orElseThrow();
    }

    private UUID findCycleId(JsonNode cycles, int referenceMonth, int referenceYear) {
        return java.util.stream.StreamSupport.stream(cycles.spliterator(), false)
                .filter(cycle -> cycle.get("referenceMonth").asInt() == referenceMonth)
                .filter(cycle -> cycle.get("referenceYear").asInt() == referenceYear)
                .map(cycle -> UUID.fromString(cycle.get("id").asText()))
                .findFirst()
                .orElseThrow();
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
        assertThat(userRepository.findByEmail(email).orElseThrow().isActive()).isTrue();
    }

    private UUID readId(String responseBody) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return UUID.fromString(jsonNode.get("id").asText());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    static class FakeExpenseTextAiClient implements ExpenseTextAiClient {

        private final ArrayDeque<List<ExpenseTextClassification>> classifications = new ArrayDeque<>();
        private boolean failNext;

        @Override
        public List<ExpenseTextClassification> classify(UUID userId, String text, LocalDate referenceDate) {
            if (failNext) {
                failNext = false;
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY, "DeepSeek expense classification failed: upstream unavailable");
            }
            return classifications.removeFirst();
        }

        void enqueue(ExpenseTextClassification classification) {
            classifications.add(List.of(classification));
        }

        void enqueueAll(List<ExpenseTextClassification> batch) {
            classifications.add(List.copyOf(batch));
        }

        void failNext() {
            failNext = true;
        }

        void reset() {
            classifications.clear();
            failNext = false;
        }
    }

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        ExpenseTextAiClient expenseTextAiClient() {
            return new FakeExpenseTextAiClient();
        }

        @Bean
        @Primary
        WeeklyReportAiClient weeklyReportAiClient() {
            return (userId, prompt) -> "# Relatorio Semanal de Desempenho\n\n## Resumo\nRelatorio gerado.";
        }

        @Bean
        @Primary
        ProfileImageStorage profileImageStorage() {
            return new ProfileImageStorage() {
                private final HashMap<String, ProfileImageData> images = new HashMap<>();

                @Override
                public String store(UUID userId, ProfileImagePayload image) {
                    var key = "users/%s/profile-image/test.%s".formatted(userId, image.extension());
                    images.put(key, new ProfileImageData(image.content(), image.contentType()));
                    return key;
                }

                @Override
                public Optional<ProfileImageData> load(String key) {
                    return Optional.ofNullable(images.get(key));
                }

                @Override
                public void delete(String key) {
                    images.remove(key);
                }
            };
        }
    }
}
