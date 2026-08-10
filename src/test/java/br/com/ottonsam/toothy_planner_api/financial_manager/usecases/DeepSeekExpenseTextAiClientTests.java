package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DeepSeekExpenseTextAiClientTests {

    private final DeepSeekExpenseTextAiClient client =
            new DeepSeekExpenseTextAiClient(new ObjectMapper(), "test-key", "http://localhost", "test-model", 5);

    @Test
    void parsesMultipleNormalizedExpensesInOrder() {
        var classifications = client.parseContent("""
                {
                  "expenses": [
                    {
                      "sourceText": "mercado de 80 reais",
                      "type": "pontual",
                      "category": "alimentacao",
                      "description": "Mercado",
                      "amount": "80,50",
                      "expenseDate": "2026-07-12"
                    },
                    {
                      "sourceText": "notebook em 3 vezes de 200",
                      "type": "parcelado",
                      "category": "compras",
                      "description": "Notebook",
                      "installmentAmount": 200,
                      "installments": 3,
                      "firstExpenseDate": "2026-07-13"
                    }
                  ]
                }
                """);

        assertThat(classifications).hasSize(2);
        assertThat(classifications.getFirst().type()).isEqualTo(ExpenseTextType.ONE_TIME);
        assertThat(classifications.getFirst().category()).isEqualTo(ExpenseCategory.ALIMENTACAO);
        assertThat(classifications.getFirst().amount()).isEqualByComparingTo(new BigDecimal("80.50"));
        assertThat(classifications.getFirst().expenseDate()).isEqualTo(LocalDate.parse("2026-07-12"));
        assertThat(classifications.get(1).type()).isEqualTo(ExpenseTextType.INSTALLMENT);
        assertThat(classifications.get(1).installments()).isEqualTo(3);
        assertThat(classifications.get(1).sourceText()).isEqualTo("notebook em 3 vezes de 200");
    }

    @Test
    void acceptsAnEmptyExpenseList() {
        assertThat(client.parseContent("{\"expenses\":[]}")).isEmpty();
    }

    @Test
    void rejectsInvalidAiJsonAndMissingExpenseArray() {
        assertThatThrownBy(() -> client.parseContent("not-json"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("content is not valid JSON");
        assertThatThrownBy(() -> client.parseContent("{}"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expenses must be an array");
    }
}
