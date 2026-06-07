package br.com.ottonsam.toothy_planner_api.activity.entities;

public enum WeekDay {
    SUNDAY("Domingo"),
    MONDAY("Segunda-feira"),
    TUESDAY("Terça-feira"),
    WEDNESDAY("Quarta-feira"),
    THURSDAY("Quinta-feira"),
    FRIDAY("Sexta-feira"),
    SATURDAY("Sábado");

    private final String label;

    WeekDay(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
