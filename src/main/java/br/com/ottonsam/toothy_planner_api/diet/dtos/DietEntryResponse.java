package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryEntity;
import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DietEntryResponse(
        UUID id,
        FoodSummaryResponse food,
        LocalDate entryDate,
        BigDecimal quantity,
        DietEntryUnit unit,
        BigDecimal kcal,
        BigDecimal protein,
        BigDecimal carbohydrate,
        BigDecimal fat,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DietEntryResponse from(DietEntryEntity entry) {
        return new DietEntryResponse(
                entry.getId(),
                FoodSummaryResponse.from(entry.getFood()),
                entry.getEntryDate(),
                entry.getQuantity(),
                entry.getUnit(),
                entry.getKcal(),
                entry.getProtein(),
                entry.getCarbohydrate(),
                entry.getFat(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
