package br.com.ottonsam.toothy_planner_api.goal.entities;

public enum GoalType {
    LONG_TERM("Longo prazo"),
    MEDIUM_TERM("Medio prazo"),
    CALENDAR("Calendário");

    private final String label;

    GoalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
