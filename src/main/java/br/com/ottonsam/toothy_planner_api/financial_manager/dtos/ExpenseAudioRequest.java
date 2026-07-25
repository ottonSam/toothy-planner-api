package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.time.LocalDate;

public record ExpenseAudioRequest(String audioBase64, String contentType, LocalDate referenceDate) {}
