package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseTextItemResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseTextUseCase {

    private static final int BATCH_SIZE = 25;
    private static final int MAX_EXPENSES_PER_TEXT = 50;

    private final ExpenseTextAiClient aiClient;
    private final ExpenseUseCase expenseUseCase;
    private final InstallmentExpenseUseCase installmentExpenseUseCase;
    private final RecurringExpenseUseCase recurringExpenseUseCase;
    private final ExpenseRepository expenseRepository;
    private final ExpenseWalletUseCase walletUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ExpenseTextUseCase(
            ExpenseTextAiClient aiClient,
            ExpenseUseCase expenseUseCase,
            InstallmentExpenseUseCase installmentExpenseUseCase,
            RecurringExpenseUseCase recurringExpenseUseCase,
            ExpenseRepository expenseRepository,
            ExpenseWalletUseCase walletUseCase,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.aiClient = aiClient;
        this.expenseUseCase = expenseUseCase;
        this.installmentExpenseUseCase = installmentExpenseUseCase;
        this.recurringExpenseUseCase = recurringExpenseUseCase;
        this.expenseRepository = expenseRepository;
        this.walletUseCase = walletUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public ExpenseTextResponse create(UUID walletId, ExpenseTextRequest request) {
        return create(walletId, request, ExpenseSource.AI_TEXT);
    }

    ExpenseTextResponse create(UUID walletId, ExpenseTextRequest request, ExpenseSource source) {
        var text = requiredText(request);
        var referenceDate = request.referenceDate() == null ? LocalDate.now(clock) : request.referenceDate();
        walletUseCase.findOwned(walletId, currentUserProvider.get().getId());
        var classifications = aiClient.classify(text, referenceDate);
        validateClassifications(classifications);

        var items = new ArrayList<ExpenseTextItemResponse>(classifications.size());
        classifications.forEach(ignored -> items.add(null));
        createInBatches(walletId, classifications, items, referenceDate, source, false);
        createInBatches(walletId, classifications, items, referenceDate, source, true);
        var generatedExpenses = items.stream()
                .flatMap(item -> item.generatedExpenses().stream())
                .toList();
        return new ExpenseTextResponse(items.size(), generatedExpenses.size(), items, generatedExpenses);
    }

    private void createInBatches(
            UUID walletId,
            List<ExpenseTextClassification> classifications,
            List<ExpenseTextItemResponse> items,
            LocalDate referenceDate,
            ExpenseSource source,
            boolean recurringPhase) {
        var indexes = new ArrayList<Integer>();
        for (int index = 0; index < classifications.size(); index++) {
            var isRecurring = classifications.get(index).type() == ExpenseTextType.RECURRING;
            if (isRecurring == recurringPhase) {
                indexes.add(index);
            }
        }
        for (int start = 0; start < indexes.size(); start += BATCH_SIZE) {
            var end = Math.min(start + BATCH_SIZE, indexes.size());
            for (var index : indexes.subList(start, end)) {
                items.set(index, createItem(walletId, classifications.get(index), referenceDate, source));
            }
        }
    }

    private ExpenseTextItemResponse createItem(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate, ExpenseSource source) {
        return switch (classification.type()) {
            case ONE_TIME -> createOneTime(walletId, classification, referenceDate, source);
            case INSTALLMENT -> createInstallment(walletId, classification, referenceDate, source);
            case RECURRING -> createRecurring(walletId, classification, referenceDate, source);
        };
    }

    private ExpenseTextItemResponse createOneTime(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate, ExpenseSource source) {
        var expenseDate = classification.expenseDate() == null ? referenceDate : classification.expenseDate();
        var expense = expenseUseCase.create(
                walletId,
                new ExpenseRequest(
                        classification.category(), classification.description(), classification.amount(), expenseDate),
                source);
        return new ExpenseTextItemResponse(
                classification.sourceText().trim(),
                ExpenseTextType.ONE_TIME.name(),
                expense,
                null,
                null,
                List.of(expense));
    }

    private ExpenseTextItemResponse createInstallment(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate, ExpenseSource source) {
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
                source);
        var generatedExpenses =
                expenseRepository
                        .findAllByParentExpenseIdOrderByExpenseDateAscCreatedAtAsc(installmentExpense.getId())
                        .stream()
                        .map(ExpenseResponse::from)
                        .toList();
        return new ExpenseTextItemResponse(
                classification.sourceText().trim(),
                ExpenseTextType.INSTALLMENT.name(),
                null,
                InstallmentExpenseResponse.from(installmentExpense),
                null,
                generatedExpenses);
    }

    private ExpenseTextItemResponse createRecurring(
            UUID walletId, ExpenseTextClassification classification, LocalDate referenceDate, ExpenseSource source) {
        var startsAt = classification.startsAt() == null ? referenceDate : classification.startsAt();
        var recurringExpense = recurringExpenseUseCase.createEntity(
                walletId,
                new RecurringExpenseRequest(
                        classification.category(), classification.description(), classification.amount(), startsAt),
                source);
        var generatedExpenses =
                expenseRepository
                        .findAllByRecurrenceIdOrderByExpenseDateAscCreatedAtAsc(recurringExpense.getId())
                        .stream()
                        .map(ExpenseResponse::from)
                        .toList();
        return new ExpenseTextItemResponse(
                classification.sourceText().trim(),
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

    private void validateClassifications(List<ExpenseTextClassification> classifications) {
        if (classifications == null) {
            throw classificationError("response did not contain expenses", null);
        }
        if (classifications.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No expenses were identified in the text");
        }
        if (classifications.size() > MAX_EXPENSES_PER_TEXT) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Expense text supports at most %d expenses".formatted(MAX_EXPENSES_PER_TEXT));
        }
        classifications.forEach(this::validateClassification);
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
        if (classification.sourceText() == null
                || classification.sourceText().trim().isEmpty()) {
            throw classificationError("sourceText is required", classification);
        }
        validateByType(classification);
    }

    private void validateByType(ExpenseTextClassification classification) {
        switch (classification.type()) {
            case ONE_TIME -> {
                if (classification.amount() == null) {
                    throw classificationError("ONE_TIME requires amount", classification);
                }
                if (classification.amount().signum() <= 0) {
                    throw classificationError("ONE_TIME amount must be greater than zero", classification);
                }
            }
            case INSTALLMENT -> {
                var totalAmount = installmentTotalAmount(classification);
                var installmentAmount = installmentAmount(classification);
                if (classification.installments() == null) {
                    throw classificationError("INSTALLMENT requires installments", classification);
                }
                if (classification.installments() <= 0) {
                    throw classificationError("INSTALLMENT installments must be greater than zero", classification);
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
                if ((totalAmount != null && totalAmount.signum() <= 0)
                        || (installmentAmount != null && installmentAmount.signum() <= 0)) {
                    throw classificationError("INSTALLMENT amount must be greater than zero", classification);
                }
            }
            case RECURRING -> {
                if (classification.amount() == null) {
                    throw classificationError("RECURRING requires amount", classification);
                }
                if (classification.amount().signum() <= 0) {
                    throw classificationError("RECURRING amount must be greater than zero", classification);
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
