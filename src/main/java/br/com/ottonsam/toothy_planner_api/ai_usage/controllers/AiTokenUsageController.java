package br.com.ottonsam.toothy_planner_api.ai_usage.controllers;

import br.com.ottonsam.toothy_planner_api.ai_usage.dtos.AiTokenUsageResponse;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.GetCurrentAiTokenUsageUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-usage")
public class AiTokenUsageController {

    private final GetCurrentAiTokenUsageUseCase getCurrentAiTokenUsageUseCase;

    public AiTokenUsageController(GetCurrentAiTokenUsageUseCase getCurrentAiTokenUsageUseCase) {
        this.getCurrentAiTokenUsageUseCase = getCurrentAiTokenUsageUseCase;
    }

    @GetMapping("/current")
    AiTokenUsageResponse current() {
        return getCurrentAiTokenUsageUseCase.execute();
    }
}
