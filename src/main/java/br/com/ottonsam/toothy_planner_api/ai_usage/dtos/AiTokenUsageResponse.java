package br.com.ottonsam.toothy_planner_api.ai_usage.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AiTokenUsageResponse(BigDecimal remainingPercentage, OffsetDateTime periodEndsAt, boolean exhausted) {}
