package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseTextRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseTextResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseSource;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseTextUseCase {

    private final ExpenseTextAiClient aiClient;
    private final ExpenseUseCase expenseUseCase;
    private final InstallmentExpenseUseCase installmentExpenseUseCase;
    private final RecurringExpenseUseCase recurringExpenseUseCase;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    public ExpenseTextUseCase(
            ExpenseTextAiClient aiClient,
            ExpenseUseCase expenseUseCase,
            InstallmentExpenseUseCase installmentExpenseUseCase,
            RecurringExpenseUseCase recurringExpenseUseCase,
            ExpenseRepository expenseRepository,
            Clock clock) {
        this.aiClient = aiClient;
        this.expenseUseCase = expenseUseCase;
        this.installmentExpenseUseCase = installmentExpenseUseCase;
        this.recurringExpenseUseCase = recurringExpenseUseCase;
        this.expenseRepository = expenseRepository;
        this.clock = clock;
    }

    public ExpenseTextResponse create(UUID walletId, ExpenseTextRequest request) {
        var text = requiredText(request);
        var referenceDate = request.referenceDate() == null ? LocalDate.now(clock) : request.referenceDate();
        var classification = aiClient.classify(text, referenceDate);
        validateClassification(classification);

        return switch (classification.type()) {
            case ONE_TIME -> createOneTime(walletId, classification, referenceDate);
            case INSTALLMENT -> createInstallment(walletId, classification, referenceDate);
            case RECURRING -> createRecurring(walletId, classification, referenceDate);
        };
    }

    private ExpenseTextResponse createOneTime(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate) {
        var expenseDate = classification.expenseDate() == null ? referenceDate : classification.expenseDate();
        var expense = expenseUseCase.create(
                walletId,
                new ExpenseRequest(
                        classification.category(), classification.description(), classification.amount(), expenseDate),
                ExpenseSource.AI_TEXT);
        return new ExpenseTextResponse(ExpenseTextType.ONE_TIME.name(), expense, null, null, List.of(expense));
    }

    private ExpenseTextResponse createInstallment(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate) {
        var firstExpenseDate =
                classification.firstExpenseDate() == null ? referenceDate : classification.firstExpenseDate();
        var installmentExpense = installmentExpenseUseCase.createEntity(
                walletId,
                new InstallmentExpenseRequest(
                        classification.category(),
                        classification.description(),
                        installmentTotalAmount(classification),
                        installmentAmount(classification),
                        classification.installments(),
                        firstExpenseDate),
                ExpenseSource.AI_TEXT);
        var generatedExpenses =
                expenseRepository
                        .findAllByParentExpenseIdOrderByExpenseDateAscCreatedAtAsc(installmentExpense.getId())
                        .stream()
                        .map(ExpenseResponse::from)
                        .toList();
        return new ExpenseTextResponse(
                ExpenseTextType.INSTALLMENT.name(),
                null,
                InstallmentExpenseResponse.from(installmentExpense),
                null,
                generatedExpenses);
    }

    private ExpenseTextResponse createRecurring(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate) {
        var startsAt = classification.startsAt() == null ? referenceDate : classification.startsAt();
        var recurringExpense = recurringExpenseUseCase.createEntity(
                walletId,
                new RecurringExpenseRequest(
                        classification.category(), classification.description(), classification.amount(), startsAt),
                ExpenseSource.AI_TEXT);
        var generatedExpenses =
                expenseRepository
                        .findAllByRecurrenceIdOrderByExpenseDateAscCreatedAtAsc(recurringExpense.getId())
                        .stream()
                        .map(ExpenseResponse::from)
                        .toList();
        return new ExpenseTextResponse(
                ExpenseTextType.RECURRING.name(),
                null,
                null,
                RecurringExpenseResponse.from(recurringExpense),
                generatedExpenses);
    }

    private String requiredText(ExpenseTextRequest request) {
        if (request == null || request.text() == null || request.text().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense text is required");
        }
        return request.text().trim();
    }

    private void validateClassification(ExpenseTextClassification classification) {
        if (classification == null) {
            throw classificationError("response did not contain a classification", null);
        }
        if (classification.type() == null) {
            throw classificationError("type is required", classification);
        }
        if (classification.category() == null) {
            throw classificationError("category is required", classification);
        }
        if (classification.description() == null
                || classification.description().trim().isEmpty()) {
            throw classificationError("description is required", classification);
        }
        validateByType(classification);
    }

    private void validateByType(ExpenseTextClassification classification) {
        switch (classification.type()) {
            case ONE_TIME -> {
                if (classification.amount() == null) {
                    throw classificationError("ONE_TIME requires amount", classification);
                }
            }
            case INSTALLMENT -> {
                var totalAmount = installmentTotalAmount(classification);
                var installmentAmount = installmentAmount(classification);
                if (classification.installments() == null) {
                    throw classificationError("INSTALLMENT requires installments", classification);
                }
                if (totalAmount == null && installmentAmount == null) {
                    throw classificationError(
                            "INSTALLMENT requires totalAmount, installmentAmount or amount", classification);
                }
                if (totalAmount != null && installmentAmount != null) {
                    throw classificationError(
                            "INSTALLMENT must contain only one of totalAmount or installmentAmount/amount",
                            classification);
                }
            }
            case RECURRING -> {
                if (classification.amount() == null) {
                    throw classificationError("RECURRING requires amount", classification);
                }
            }
        }
    }

    private BigDecimal installmentTotalAmount(ExpenseTextClassification classification) {
        return classification.totalAmount();
    }

    private BigDecimal installmentAmount(ExpenseTextClassification classification) {
        if (classification.installmentAmount() != null) {
            return classification.installmentAmount();
        }
        if (classification.totalAmount() == null) {
            return classification.amount();
        }
        return null;
    }

    private ApiException classificationError(String reason, ExpenseTextClassification classification) {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "DeepSeek expense classification failed: " + reason + classificationSummary(classification));
    }

    private String classificationSummary(ExpenseTextClassification classification) {
        if (classification == null) {
            return "";
        }
        return " [type=%s, category=%s, amount=%s, totalAmount=%s, installmentAmount=%s, installments=%s, expenseDate=%s, firstExpenseDate=%s, startsAt=%s]"
                .formatted(
                        classification.type(),
                        classification.category(),
                        classification.amount(),
                        classification.totalAmount(),
                        classification.installmentAmount(),
                        classification.installments(),
                        classification.expenseDate(),
                        classification.firstExpenseDate(),
                        classification.startsAt());
    }
}
