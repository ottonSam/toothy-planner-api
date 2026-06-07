package br.com.ottonsam.toothy_planner_api.activity.entities;

public enum ActivityType {
    DAYS("Dias"),
    COUNT("Contagem"),
    TIME("Tempo");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
