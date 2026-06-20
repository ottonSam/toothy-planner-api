package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.report.usecases.WeeklyReportAiClient;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    @Autowired
    FinancialManagerIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
    }

    @Test
    void managesCategoriesAndWalletsOnlyForAuthenticatedUser() throws Exception {
        var userCookie = login("financial-owner@example.com");
        var otherCookie = login("financial-other@example.com");

        var categoryId = createCategory(userCookie, "Alimentacao");
        var walletId = createWallet(userCookie, "Carteira pessoal", 3000, 15);

        mockMvc.perform(post("/api/v1/financial-manager/categories")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "alimentacao", "color", "#111111", "icon", "copy"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category name already exists"));

        mockMvc.perform(post("/api/v1/financial-manager/wallets")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                Map.of("description", "carteira pessoal", "spendingGoal", 1000, "cycleEndDay", 15))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Wallet description already exists"));

        mockMvc.perform(get("/api/v1/financial-manager/categories/{categoryId}", categoryId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}", walletId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));

        mockMvc.perform(put("/api/v1/financial-manager/categories/{categoryId}", categoryId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Mercado", "color", "#22C55E", "icon", "shopping-cart"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercado"));

        mockMvc.perform(put("/api/v1/financial-manager/wallets/{walletId}", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description", "Carteira principal",
                                "spendingGoal", 3500,
                                "cycleEndDay", 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Carteira principal"))
                .andExpect(jsonPath("$.cycleEndDay").value(20));

        var unusedCategoryId = createCategory(userCookie, "Transporte");
        mockMvc.perform(delete("/api/v1/financial-manager/categories/{categoryId}", unusedCategoryId)
                        .cookie(userCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void createsCyclesFromExpenseDatesAndCalculatesCycleMetrics() throws Exception {
        var userCookie = login("financial-cycles@example.com");
        var categoryId = createCategory(userCookie, "Mercado");
        var walletId = createWallet(userCookie, "Carteira ciclos", 3000, 15);

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId",
                                categoryId,
                                "description",
                                "Mercado julho",
                                "amount",
                                250.90,
                                "expenseDate",
                                "2026-07-13"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ONE_TIME"))
                .andExpect(jsonPath("$.expenseDate").value("2026-07-13"));

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId",
                                categoryId,
                                "description",
                                "Mercado agosto",
                                "amount",
                                100.00,
                                "expenseDate",
                                "2026-07-16"))))
                .andExpect(status().isCreated());

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        assertThat(cycles).hasSize(2);
        assertThat(cycles.get(0).get("referenceMonth").asInt()).isEqualTo(7);
        assertThat(cycles.get(0).get("startsAt").asText()).isEqualTo("2026-06-16");
        assertThat(cycles.get(0).get("endsAt").asText()).isEqualTo("2026-07-15");
        assertThat(cycles.get(1).get("referenceMonth").asInt()).isEqualTo(8);
        assertThat(cycles.get(1).get("startsAt").asText()).isEqualTo("2026-07-16");
        assertThat(cycles.get(1).get("endsAt").asText()).isEqualTo("2026-08-15");

        var julyCycleId = UUID.fromString(cycles.get(0).get("id").asText());
        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics",
                                walletId,
                                julyCycleId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(250.90))
                .andExpect(jsonPath("$.remainingAmount").value(2749.10))
                .andExpect(jsonPath("$.remainingDailyAmount").value(916.37))
                .andExpect(jsonPath("$.oneTimeTotal").value(250.90));

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}/metrics", walletId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentCycle.referenceMonth").value(7))
                .andExpect(jsonPath("$.currentCycleMetrics.totalSpent").value(250.90));
    }

    @Test
    void createsInstallmentsRecurringExpensesAndCancelsFollowingRecurringCycles() throws Exception {
        var userCookie = login("financial-installments@example.com");
        var categoryId = createCategory(userCookie, "Servicos");
        var walletId = createWallet(userCookie, "Carteira recorrente", 1000, 15);

        var recurringExpenseId =
                createRecurringExpense(userCookie, walletId, categoryId, "Internet", 99.90, "2026-07-13");
        createInstallmentExpenseByTotal(userCookie, walletId, categoryId, "Compra parcelada", 100.00, 3, "2026-07-13");

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
    void deletingRecurringExpenseRemovesCurrentAndFollowingOccurrences() throws Exception {
        var userCookie = login("financial-delete-recurring@example.com");
        var otherCookie = login("financial-delete-recurring-other@example.com");
        var categoryId = createCategory(userCookie, "Recorrencias para excluir");
        var walletId = createWallet(userCookie, "Carteira exclusao recorrente", 2000, 15);

        createExpense(userCookie, walletId, categoryId, "Criar julho", 1.00, "2026-07-13");
        createExpense(userCookie, walletId, categoryId, "Criar agosto", 1.00, "2026-07-16");
        createExpense(userCookie, walletId, categoryId, "Criar setembro", 1.00, "2026-08-16");
        createExpense(userCookie, walletId, categoryId, "Criar outubro", 1.00, "2026-09-16");

        var deletedRecurrenceId =
                createRecurringExpense(userCookie, walletId, categoryId, "Internet excluida", 99.90, "2026-07-13");
        createRecurringExpense(userCookie, walletId, categoryId, "Streaming mantido", 49.90, "2026-07-13");

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

        mockMvc.perform(delete(
                                "/api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}",
                                walletId,
                                augustRecurringExpenseId)
                        .cookie(userCookie))
                .andExpect(status().isNoContent());

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

        createExpense(userCookie, walletId, categoryId, "Criar novembro", 1.00, "2026-10-16");
        var expensesWithNovember =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByDescription(expensesWithNovember, "Internet excluida"))
                .isEqualTo(1);
        assertThat(countByDescription(expensesWithNovember, "Streaming mantido"))
                .isEqualTo(5);
    }

    @Test
    void deletingOneTimeAndInstallmentExpensesRemainsIndividual() throws Exception {
        var userCookie = login("financial-delete-individual@example.com");
        var categoryId = createCategory(userCookie, "Exclusoes individuais");
        var walletId = createWallet(userCookie, "Carteira exclusoes individuais", 2000, 15);
        var firstOneTimeId = createExpense(userCookie, walletId, categoryId, "Pontual excluido", 10.00, "2026-07-13");
        createExpense(userCookie, walletId, categoryId, "Pontual mantido", 20.00, "2026-07-13");
        createInstallmentExpenseByTotal(
                userCookie, walletId, categoryId, "Parcelas individuais", 90.00, 3, "2026-07-13");

        var expenses = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        var installmentId = findExpenseIdByInstallmentNumber(expenses, "Parcelas individuais", 2);

        deleteExpense(userCookie, walletId, firstOneTimeId);
        deleteExpense(userCookie, walletId, installmentId);

        var expensesAfterDelete =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/expenses".formatted(walletId));
        assertThat(countByDescription(expensesAfterDelete, "Pontual excluido")).isZero();
        assertThat(countByDescription(expensesAfterDelete, "Pontual mantido")).isEqualTo(1);
        assertThat(countByDescription(expensesAfterDelete, "Parcelas individuais"))
                .isEqualTo(2);
    }

    @Test
    void listsAllExpenseTypesFromCycleInDateOrderAndEnforcesOwnership() throws Exception {
        var userCookie = login("financial-cycle-expenses@example.com");
        var otherCookie = login("financial-cycle-expenses-other@example.com");
        var categoryId = createCategory(userCookie, "Gastos do ciclo");
        var walletId = createWallet(userCookie, "Carteira por ciclo", 2000, 15);

        createRecurringExpense(userCookie, walletId, categoryId, "Recorrente", 90.00, "2026-07-11");
        createInstallmentExpenseByTotal(userCookie, walletId, categoryId, "Parcelado", 200.00, 2, "2026-07-12");
        createExpense(userCookie, walletId, categoryId, "Pontual primeiro", 50.00, "2026-07-13");
        createExpense(userCookie, walletId, categoryId, "Pontual segundo", 60.00, "2026-07-13");
        createExpense(userCookie, walletId, categoryId, "Outro ciclo", 25.00, "2026-07-16");

        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var julyCycleId = findCycleId(cycles, 7, 2026);
        var julyExpenses = getJson(
                userCookie, "/api/v1/financial-manager/wallets/%s/cycles/%s/expenses".formatted(walletId, julyCycleId));

        assertThat(julyExpenses).hasSize(4);
        assertThat(julyExpenses.findValuesAsText("type"))
                .containsExactly("RECURRING", "INSTALLMENT", "ONE_TIME", "ONE_TIME");
        assertThat(julyExpenses.findValuesAsText("expenseDate"))
                .containsExactly("2026-07-11", "2026-07-12", "2026-07-13", "2026-07-13");
        assertThat(julyExpenses.findValuesAsText("description"))
                .containsExactly("Recorrente", "Parcelado", "Pontual primeiro", "Pontual segundo")
                .doesNotContain("Outro ciclo");

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses",
                                walletId,
                                julyCycleId)
                        .cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses",
                                walletId,
                                UUID.randomUUID())
                        .cookie(userCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cycle not found"));

        var secondWalletId = createWallet(userCookie, "Outra carteira por ciclo", 1000, 15);
        createExpense(userCookie, secondWalletId, categoryId, "Gasto outra carteira", 10.00, "2026-07-13");
        var secondWalletCycles =
                getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(secondWalletId));
        var secondWalletCycleId = findCycleId(secondWalletCycles, 7, 2026);

        mockMvc.perform(get(
                                "/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses",
                                walletId,
                                secondWalletCycleId)
                        .cookie(userCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cycle not found"));
    }

    @Test
    void returnsEmptyListWhenCycleHasNoExpenses() throws Exception {
        var userCookie = login("financial-empty-cycle@example.com");
        var categoryId = createCategory(userCookie, "Categoria temporaria");
        var walletId = createWallet(userCookie, "Carteira ciclo vazio", 1000, 15);
        var expenseId = createExpense(userCookie, walletId, categoryId, "Gasto temporario", 10.00, "2026-07-13");
        var cycles = getJson(userCookie, "/api/v1/financial-manager/wallets/%s/cycles".formatted(walletId));
        var cycleId = findCycleId(cycles, 7, 2026);

        mockMvc.perform(delete("/api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}", walletId, expenseId)
                        .cookie(userCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses", walletId, cycleId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsInvalidInstallmentAmountChoiceAndUsedCategoryDeletion() throws Exception {
        var userCookie = login("financial-validation@example.com");
        var categoryId = createCategory(userCookie, "Compras");
        var walletId = createWallet(userCookie, "Carteira validacoes", 1000, 15);

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/installment-expenses", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId",
                                categoryId,
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

        mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId",
                                categoryId,
                                "description",
                                "Compra",
                                "amount",
                                50,
                                "expenseDate",
                                "2026-07-13"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/financial-manager/categories/{categoryId}", categoryId)
                        .cookie(userCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category is associated with expenses"));
    }

    private UUID createCategory(Cookie accessToken, String name) throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/categories")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "color", "#F97316", "icon", "utensils"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createWallet(Cookie accessToken, String description, int spendingGoal, int cycleEndDay)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description", description,
                                "spendingGoal", spendingGoal,
                                "cycleEndDay", cycleEndDay))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createExpense(
            Cookie accessToken, UUID walletId, UUID categoryId, String description, double amount, String expenseDate)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/expenses", walletId)
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", categoryId,
                                "description", description,
                                "amount", amount,
                                "expenseDate", expenseDate))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createRecurringExpense(
            Cookie accessToken, UUID walletId, UUID categoryId, String description, double amount, String startsAt)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/financial-manager/wallets/{walletId}/recurring-expenses", walletId)
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", categoryId,
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
            UUID categoryId,
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
                                        "categoryId", categoryId,
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

    private UUID findExpenseIdByInstallmentNumber(JsonNode expenses, String description, int installmentNumber) {
        return java.util.stream.StreamSupport.stream(expenses.spliterator(), false)
                .filter(expense -> description.equals(expense.get("description").asText()))
                .filter(expense -> expense.get("installmentNumber").asInt() == installmentNumber)
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

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        WeeklyReportAiClient weeklyReportAiClient() {
            return prompt -> "# Relatorio Semanal de Desempenho\n\n## Resumo\nRelatorio gerado.";
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
