package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

public enum ExpenseCategory {
    ALIMENTACAO("Alimentacao", "#16A34A", "utensils", "Mercado, restaurantes, delivery e alimentos"),
    MORADIA("Moradia", "#2563EB", "home", "Aluguel, condominio, manutencao e despesas da casa"),
    TRANSPORTE("Transporte", "#F97316", "car", "Combustivel, transporte publico, aplicativos e manutencao"),
    SAUDE("Saude", "#DC2626", "heart-pulse", "Consultas, remedios, exames e plano de saude"),
    EDUCACAO("Educacao", "#7C3AED", "graduation-cap", "Cursos, livros, assinaturas educacionais e mensalidades"),
    LAZER("Lazer", "#DB2777", "ticket", "Passeios, viagens, eventos, jogos e entretenimento"),
    SERVICOS("Servicos", "#0891B2", "wifi", "Internet, telefone, streaming, energia e servicos contratados"),
    COMPRAS("Compras", "#CA8A04", "shopping-bag", "Roupas, eletronicos, presentes e itens pessoais"),
    TRABALHO("Trabalho", "#475569", "briefcase", "Ferramentas, deslocamentos e custos ligados ao trabalho"),
    PETS("Pets", "#65A30D", "paw-print", "Racao, veterinario, banho e itens de animais"),
    OUTROS("Outros", "#64748B", "circle-help", "Gastos que nao se encaixam nas demais categorias");

    private final String name;
    private final String color;
    private final String icon;
    private final String description;

    ExpenseCategory(String name, String color, String icon, String description) {
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.description = description;
    }

    public String getKey() {
        return name();
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
