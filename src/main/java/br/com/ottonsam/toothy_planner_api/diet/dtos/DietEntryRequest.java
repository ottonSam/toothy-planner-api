package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryUnit;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DietEntryRequest(String foodName, BigDecimal quantity, DietEntryUnit unit, LocalDate entryDate) {}
