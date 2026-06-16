package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public final class FoodNameNormalizer {

    private FoodNameNormalizer() {}

    public static String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food name is required");
        }
        var normalized =
                name.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food name is required");
        }
        return normalized;
    }
}
