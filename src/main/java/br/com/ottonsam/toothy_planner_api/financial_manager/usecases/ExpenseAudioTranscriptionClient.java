package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

public interface ExpenseAudioTranscriptionClient {

    String transcribe(String audioBase64, String contentType);
}
