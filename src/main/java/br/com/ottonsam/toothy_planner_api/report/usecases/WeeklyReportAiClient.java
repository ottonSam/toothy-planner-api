package br.com.ottonsam.toothy_planner_api.report.usecases;

import java.util.UUID;

public interface WeeklyReportAiClient {

    String generate(UUID userId, String prompt);
}
