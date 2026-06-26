package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryUnit;
import java.math.BigDecimal;

public record DietEntryUpdateRequest(BigDecimal quantity, DietEntryUnit unit) {}
