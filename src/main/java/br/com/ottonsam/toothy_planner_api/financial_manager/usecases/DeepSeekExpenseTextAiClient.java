package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekExpenseTextAiClient implements ExpenseTextAiClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final long timeoutSeconds;

    public DeepSeekExpenseTextAiClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.timeout-seconds:60}") long timeoutSeconds) {
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .callTimeout(Duration.ofSeconds(this.timeoutSeconds + 15))
                .build();
        this.objectMapper = objectMapper.copy();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public ExpenseTextClassification classify(String text, LocalDate referenceDate) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek API key is not configured");
        }
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "model",
                    model,
                    "messages",
                    List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(text, referenceDate))),
                    "temperature",
                    0.1));
            var request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (var response = httpClient.newCall(request).execute()) {
                var responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage(response.code(), responseBody));
                }
                return parseContent(extractContent(objectMapper.readTree(responseBody)));
            }
        } catch (SocketTimeoutException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "DeepSeek expense classification timed out after %d seconds".formatted(timeoutSeconds));
        } catch (IOException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "DeepSeek expense classification failed: request or response could not be read (%s)"
                            .formatted(exception.getClass().getSimpleName()));
        }
    }

    private String systemPrompt() {
        return """
                Voce classifica textos de gastos financeiros. Retorne somente JSON valido, sem markdown.

                Categorias permitidas:
                __CATEGORIES__

                Regras:
                - Use type ONE_TIME para gasto pontual.
                - Use type INSTALLMENT quando o texto indicar parcelamento.
                - Use type RECURRING quando o texto indicar recorrencia, assinatura, plano, mensalidade ou cobranca fixa.
                - Se uma recorrencia nao informar dia, use referenceDate como startsAt.
                - Se um gasto pontual nao informar data, use referenceDate como expenseDate.
                - Se um parcelamento nao informar data inicial, use referenceDate como firstExpenseDate.
                - Para parcelamento, preencha installmentAmount quando o texto informar valor da parcela; preencha totalAmount quando informar valor total. Nunca preencha ambos.
                - Para textos como "12 vezes de 199" ou "12x de 199", use installmentAmount = 199 e installments = 12.
                - Para type use exatamente ONE_TIME, INSTALLMENT ou RECURRING; nunca retorne tipo em portugues.
                - Valores monetarios devem ser numeros decimais em reais.
                - Datas devem estar em ISO-8601 yyyy-MM-dd.

                Formato obrigatorio:
                {
                  "type": "ONE_TIME|INSTALLMENT|RECURRING",
                  "category": "ALIMENTACAO|MORADIA|TRANSPORTE|SAUDE|EDUCACAO|LAZER|SERVICOS|COMPRAS|TRABALHO|PETS|OUTROS",
                  "description": "descricao curta",
                  "amount": 0.00,
                  "expenseDate": "yyyy-MM-dd",
                  "totalAmount": null,
                  "installmentAmount": null,
                  "installments": null,
                  "firstExpenseDate": null,
                  "startsAt": null
                }
                """.replace("__CATEGORIES__", categoriesPrompt());
    }

    private String categoriesPrompt() {
        return Arrays.stream(ExpenseCategory.values())
                .map(category -> "- %s: %s".formatted(category.getKey(), category.getDescription()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String userPrompt(String text, LocalDate referenceDate) {
        return """
                referenceDate: __REFERENCE_DATE__
                text: __TEXT__
                """.replace("__REFERENCE_DATE__", referenceDate.toString()).replace("__TEXT__", text);
    }

    private String errorMessage(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "DeepSeek authentication failed";
        }
        var deepSeekMessage = deepSeekMessage(responseBody);
        if (deepSeekMessage == null || deepSeekMessage.isBlank()) {
            return "DeepSeek expense classification failed";
        }
        return "DeepSeek expense classification failed: " + deepSeekMessage;
    }

    private String deepSeekMessage(String responseBody) {
        try {
            return objectMapper
                    .readTree(responseBody)
                    .path("error")
                    .path("message")
                    .asText();
        } catch (IOException exception) {
            return "";
        }
    }

    private String extractContent(JsonNode response) {
        var content =
                response.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek expense classification failed");
        }
        return content;
    }

    private ExpenseTextClassification parseContent(String content) {
        JsonNode json;
        try {
            json = objectMapper.readTree(stripMarkdownFence(content));
        } catch (IOException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "DeepSeek expense classification response is invalid: content is not valid JSON");
        }
        try {
            return new ExpenseTextClassification(
                    requiredType(json, "type"),
                    requiredCategory(json, "category"),
                    requiredText(json, "description"),
                    optionalDecimal(json, "amount"),
                    optionalDate(json, "expenseDate"),
                    optionalDecimal(json, "totalAmount"),
                    optionalDecimal(json, "installmentAmount"),
                    optionalInteger(json, "installments"),
                    optionalDate(json, "firstExpenseDate"),
                    optionalDate(json, "startsAt"));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "DeepSeek expense classification response is invalid: " + exception.getMessage());
        }
    }

    private String stripMarkdownFence(String content) {
        var trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        var firstLineEnd = trimmed.indexOf('\n');
        var lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private ExpenseTextType requiredType(JsonNode node, String field) {
        var rawValue = requiredText(node, field);
        var value = normalizedEnumText(rawValue);
        if (value.contains("INSTALL") || value.contains("PARCEL")) {
            return ExpenseTextType.INSTALLMENT;
        }
        if (value.contains("RECUR") || value.contains("RECOR") || value.contains("MENSAL")) {
            return ExpenseTextType.RECURRING;
        }
        if (value.contains("ONE") || value.contains("PONTUAL") || value.contains("UNICO")) {
            return ExpenseTextType.ONE_TIME;
        }
        try {
            return ExpenseTextType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "%s must be ONE_TIME, INSTALLMENT or RECURRING, received '%s'".formatted(field, rawValue));
        }
    }

    private ExpenseCategory requiredCategory(JsonNode node, String field) {
        var rawValue = requiredText(node, field);
        try {
            return ExpenseCategory.valueOf(normalizedEnumText(rawValue));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("%s is invalid, received '%s'".formatted(field, rawValue));
        }
    }

    private String normalizedEnumText(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String requiredText(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private BigDecimal optionalDecimal(JsonNode node, String field) {
        var value = node.path(field);
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            try {
                return new BigDecimal(value.asText().trim().replace(",", "."));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(field + " must be a decimal number");
            }
        }
        return null;
    }

    private Integer optionalInteger(JsonNode node, String field) {
        var value = node.path(field);
        if (value.isNumber()) {
            return value.intValue();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(field + " must be an integer");
            }
        }
        return null;
    }

    private LocalDate optionalDate(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value == null || value.isBlank() || node.path(field).isNull()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be an ISO date yyyy-MM-dd");
        }
    }
}
