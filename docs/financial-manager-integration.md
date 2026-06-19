# Financial Manager - Guia de integracao

Documento de contrato para integracao com o modulo financeiro da Toothy Planner
API.

## Base e autenticacao

- Base local: `http://localhost:8080`
- Prefixo do modulo: `/api/v1/financial-manager`
- Formato: `application/json`
- Datas: `YYYY-MM-DD`
- Datas com horario: ISO-8601 com offset
- IDs: UUID em string
- Autenticacao: cookie HTTP-only `access_token`

Todas as rotas deste documento exigem autenticacao. Em clientes web, envie as
requisicoes com credenciais:

```ts
fetch(url, {
  credentials: "include"
});
```

Erros usam o formato:

```json
{
  "message": "Wallet not found"
}
```

Status mais comuns:

- `400 Bad Request`: campo obrigatorio ausente ou regra invalida.
- `401 Unauthorized`: cookie ausente, expirado ou invalido.
- `404 Not Found`: recurso inexistente ou pertencente a outro usuario.
- `409 Conflict`: nome duplicado ou exclusao bloqueada por relacionamentos.

## Fluxo recomendado

1. Crie as categorias.
2. Crie uma carteira com meta e dia de encerramento.
3. Registre gastos pontuais, parcelados ou recorrentes.
4. Liste os ciclos criados automaticamente.
5. Consulte as metricas da carteira ou de um ciclo.

Nao existe endpoint para criar ciclos. Eles sao criados automaticamente ao
registrar gastos, parcelas ou recorrencias.

## Enums

### ExpenseType

```json
["ONE_TIME", "INSTALLMENT", "RECURRING"]
```

O tipo nao e enviado pelo cliente:

- `ONE_TIME`: criado por `/expenses`.
- `INSTALLMENT`: gerado por `/installment-expenses`.
- `RECURRING`: gerado por `/recurring-expenses`.

## Respostas reutilizaveis

### ExpenseCategoryResponse

```json
{
  "id": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "utensils",
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

### ExpenseCategorySummaryResponse

Usada dentro de gastos, parcelamentos e recorrencias.

```json
{
  "id": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "utensils"
}
```

### ExpenseWalletResponse

```json
{
  "id": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "cycleEndDay": 15,
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

### ExpenseCycleResponse

```json
{
  "id": "3a354849-f809-40b8-805d-fb784de471ef",
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "referenceMonth": 7,
  "referenceYear": 2026,
  "startsAt": "2026-06-16",
  "endsAt": "2026-07-15",
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

O mes de referencia e o mes no qual o ciclo termina. Para uma carteira com
`cycleEndDay = 15`:

- `2026-07-13` pertence ao ciclo de julho.
- `2026-07-16` pertence ao ciclo de agosto.
- O ciclo de julho vai de `2026-06-16` ate `2026-07-15`.

Se o dia configurado nao existir no mes, o ultimo dia do mes e usado.

### ExpenseResponse

```json
{
  "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "cycleId": "3a354849-f809-40b8-805d-fb784de471ef",
  "category": {
    "id": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
    "name": "Alimentacao",
    "color": "#F97316",
    "icon": "utensils"
  },
  "description": "Mercado",
  "amount": 250.90,
  "expenseDate": "2026-07-13",
  "type": "ONE_TIME",
  "parentExpenseId": null,
  "installmentNumber": null,
  "installmentTotal": null,
  "recurrenceId": null,
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

Campos condicionais:

- `INSTALLMENT`: possui `parentExpenseId`, `installmentNumber` e
  `installmentTotal`.
- `RECURRING`: possui `recurrenceId`.
- `ONE_TIME`: esses campos sao `null`.

### InstallmentExpenseResponse

```json
{
  "id": "e1e315d0-8268-49f6-8f58-7b150237cc55",
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "category": {
    "id": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
    "name": "Alimentacao",
    "color": "#F97316",
    "icon": "utensils"
  },
  "description": "Notebook",
  "totalAmount": 3500.00,
  "installmentAmount": null,
  "installments": 10,
  "firstExpenseDate": "2026-07-16",
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

Quando a criacao usa `installmentAmount`, `totalAmount` e retornado como `null`.
Quando usa `totalAmount`, `installmentAmount` e retornado como `null`. O valor
calculado das parcelas deve ser obtido na listagem de gastos.

### RecurringExpenseResponse

```json
{
  "id": "ac895448-5d75-4ee3-a8c5-a19c98f0322d",
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "category": {
    "id": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
    "name": "Servicos",
    "color": "#0EA5E9",
    "icon": "wifi"
  },
  "description": "Internet",
  "amount": 99.90,
  "startsAt": "2026-07-13",
  "canceledAt": null,
  "active": true,
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

### ExpenseCycleMetricsResponse

```json
{
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "cycleId": "3a354849-f809-40b8-805d-fb784de471ef",
  "referenceMonth": 7,
  "referenceYear": 2026,
  "startsAt": "2026-06-16",
  "endsAt": "2026-07-15",
  "spendingGoal": 3000.00,
  "totalSpent": 250.90,
  "remainingAmount": 2749.10,
  "remainingDailyAmount": 916.37,
  "installmentTotalFromCurrentCycle": 0.00,
  "recurringMonthlyTotal": 0.00,
  "oneTimeTotal": 250.90
}
```

Calculos:

- `totalSpent`: soma de todos os gastos do ciclo.
- `remainingAmount`: `spendingGoal - totalSpent`.
- `remainingDailyAmount`: saldo dividido pelos dias restantes, incluindo o dia
  atual e o dia final. Para ciclo encerrado, retorna `0.00`.
- `installmentTotalFromCurrentCycle`: soma das parcelas do ciclo consultado em
  diante.
- `recurringMonthlyTotal`: soma das recorrencias atualmente ativas.
- `oneTimeTotal`: soma dos gastos `ONE_TIME` do ciclo.

Os saldos podem ser negativos quando a meta for ultrapassada.

### ExpenseWalletMetricsResponse

```json
{
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "cycleEndDay": 15,
  "currentCycle": {
    "id": "3a354849-f809-40b8-805d-fb784de471ef",
    "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
    "referenceMonth": 7,
    "referenceYear": 2026,
    "startsAt": "2026-06-16",
    "endsAt": "2026-07-15",
    "createdAt": "2026-07-13T09:00:00-03:00",
    "updatedAt": "2026-07-13T09:00:00-03:00"
  },
  "currentCycleMetrics": {
    "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
    "cycleId": "3a354849-f809-40b8-805d-fb784de471ef",
    "referenceMonth": 7,
    "referenceYear": 2026,
    "startsAt": "2026-06-16",
    "endsAt": "2026-07-15",
    "spendingGoal": 3000.00,
    "totalSpent": 250.90,
    "remainingAmount": 2749.10,
    "remainingDailyAmount": 916.37,
    "installmentTotalFromCurrentCycle": 0.00,
    "recurringMonthlyTotal": 0.00,
    "oneTimeTotal": 250.90
  },
  "activeRecurringMonthlyTotal": 0.00,
  "installmentTotalFromCurrentCycle": 0.00
}
```

Se ainda nao existir um ciclo persistido para a data atual, `currentCycle` e
`currentCycleMetrics` retornam `null`. Consultar metricas nao cria o ciclo.

## Endpoints de categorias

### POST /categories

Cria uma categoria.

Request:

```json
{
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "utensils"
}
```

Resposta: `201 Created` com `ExpenseCategoryResponse`.

Validacoes:

- Todos os campos sao obrigatorios e nao podem estar em branco.
- `name` e unico por usuario, ignorando maiusculas e minusculas.

### GET /categories

Resposta: `200 OK` com `ExpenseCategoryResponse[]`, ordenado por criacao.

### GET /categories/{categoryId}

Resposta: `200 OK` com `ExpenseCategoryResponse`.

### PUT /categories/{categoryId}

Usa o mesmo payload de criacao.

Resposta: `200 OK` com `ExpenseCategoryResponse`.

### DELETE /categories/{categoryId}

Resposta: `204 No Content`.

Retorna `409 Conflict` se a categoria estiver ligada a um gasto, parcelamento
ou recorrencia.

## Endpoints de carteiras

### POST /wallets

Request:

```json
{
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "cycleEndDay": 15
}
```

Resposta: `201 Created` com `ExpenseWalletResponse`.

Validacoes:

- `description` e obrigatoria e unica por usuario, ignorando caixa.
- `spendingGoal` deve ser maior que zero.
- `cycleEndDay` deve estar entre `1` e `31`.

### GET /wallets

Resposta: `200 OK` com `ExpenseWalletResponse[]`, ordenado por criacao.

### GET /wallets/{walletId}

Resposta: `200 OK` com `ExpenseWalletResponse`.

### PUT /wallets/{walletId}

Usa o mesmo payload de criacao.

Resposta: `200 OK` com `ExpenseWalletResponse`.

Nota de integracao: a implementacao atual atualiza o `cycleEndDay` usado em
novos calculos, mas nao recalcula as datas de ciclos ja persistidos.

### DELETE /wallets/{walletId}

Resposta: `204 No Content`.

Retorna `409 Conflict` se houver ciclos, gastos, parcelamentos ou recorrencias.

### GET /wallets/{walletId}/metrics

Resposta: `200 OK` com `ExpenseWalletMetricsResponse`.

## Endpoints de ciclos

### GET /wallets/{walletId}/cycles

Resposta: `200 OK` com `ExpenseCycleResponse[]`, ordenado por ano e mes.

### GET /wallets/{walletId}/cycles/{cycleId}

Resposta: `200 OK` com `ExpenseCycleResponse`.

### GET /wallets/{walletId}/cycles/{cycleId}/metrics

Resposta: `200 OK` com `ExpenseCycleMetricsResponse`.

### GET /wallets/{walletId}/cycles/{cycleId}/expenses

Resposta: `200 OK` com `ExpenseResponse[]`.

Retorna somente os gastos associados ao ciclo informado, incluindo os tipos
`ONE_TIME`, `INSTALLMENT` e `RECURRING`. A lista e ordenada por `expenseDate` e
depois por `createdAt`. Um ciclo sem gastos retorna `[]`.

Nao existe `POST`, `PUT` ou `DELETE` de ciclo.

## Endpoints de gastos

### POST /wallets/{walletId}/expenses

Cria um gasto pontual.

Request:

```json
{
  "categoryId": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "description": "Mercado",
  "amount": 250.90,
  "expenseDate": "2026-07-13"
}
```

Resposta: `201 Created` com `ExpenseResponse` e `type = "ONE_TIME"`.

O cliente nao envia `cycleId`. O ciclo e encontrado ou criado a partir de
`expenseDate`.

### GET /wallets/{walletId}/expenses

Resposta: `200 OK` com `ExpenseResponse[]`.

A lista contem todos os tipos de gasto e e ordenada por `expenseDate` e
`createdAt`.

### GET /wallets/{walletId}/expenses/{expenseId}

Resposta: `200 OK` com `ExpenseResponse`.

### PUT /wallets/{walletId}/expenses/{expenseId}

Usa o mesmo payload de criacao. Se `expenseDate` mudar, o ciclo e recalculado.

Resposta: `200 OK` com `ExpenseResponse`.

Para alterar parcelas ou recorrencias futuras em lote, prefira os endpoints dos
recursos pai.

### DELETE /wallets/{walletId}/expenses/{expenseId}

Resposta: `204 No Content`.

Remove somente o gasto informado.

## Endpoints de parcelamentos

### POST /wallets/{walletId}/installment-expenses

O cliente deve enviar exatamente uma forma de valor.

Por valor total:

```json
{
  "categoryId": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "description": "Notebook",
  "totalAmount": 3500.00,
  "installments": 10,
  "firstExpenseDate": "2026-07-16"
}
```

Por valor da parcela:

```json
{
  "categoryId": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "description": "Academia",
  "installmentAmount": 120.00,
  "installments": 12,
  "firstExpenseDate": "2026-07-13"
}
```

Resposta: `201 Created` com `InstallmentExpenseResponse`.

Regras:

- `installments` deve ser maior que zero.
- Valores devem ser maiores que zero.
- Nao envie `totalAmount` e `installmentAmount` juntos.
- Com `totalAmount`, cada parcela e calculada com arredondamento para cima em
  duas casas. Exemplo: `100 / 3` gera tres parcelas de `33.34`.
- Cada parcela vira um `ExpenseResponse` com `type = "INSTALLMENT"`.
- O `parentExpenseId` de cada parcela e o ID do parcelamento.
- Os ciclos futuros sao criados automaticamente.

### GET /wallets/{walletId}/installment-expenses

Resposta: `200 OK` com `InstallmentExpenseResponse[]`.

### GET /wallets/{walletId}/installment-expenses/{installmentExpenseId}

Resposta: `200 OK` com `InstallmentExpenseResponse`.

### PUT /wallets/{walletId}/installment-expenses/{installmentExpenseId}

Usa o mesmo formato de criacao.

Resposta: `200 OK` com `InstallmentExpenseResponse`.

A atualizacao remove e recria as parcelas cujos ciclos terminam na data atual
ou depois dela. Parcelas de ciclos encerrados sao preservadas.

### DELETE /wallets/{walletId}/installment-expenses/{installmentExpenseId}

Resposta: `204 No Content`.

Remove o parcelamento e todas as parcelas associadas, inclusive historicas.

## Endpoints de recorrencias

### POST /wallets/{walletId}/recurring-expenses

Request:

```json
{
  "categoryId": "190e5224-d8f7-4df8-8a6f-2aa38efcf8ad",
  "description": "Internet",
  "amount": 99.90,
  "startsAt": "2026-07-13"
}
```

Resposta: `201 Created` com `RecurringExpenseResponse`.

A recorrencia gera um gasto no ciclo inicial e nos ciclos posteriores ja
existentes. Novos ciclos criados futuramente tambem recebem o gasto.

### GET /wallets/{walletId}/recurring-expenses

Resposta: `200 OK` com `RecurringExpenseResponse[]`.

A lista inclui recorrencias ativas e canceladas.

### GET /wallets/{walletId}/recurring-expenses/{recurringExpenseId}

Resposta: `200 OK` com `RecurringExpenseResponse`.

### PUT /wallets/{walletId}/recurring-expenses/{recurringExpenseId}

Usa o mesmo payload de criacao.

Resposta: `200 OK` com `RecurringExpenseResponse`.

Somente recorrencias ativas podem ser alteradas. Gastos de ciclos que terminam
na data atual ou depois dela sao removidos e recriados com os novos dados.

### POST /wallets/{walletId}/recurring-expenses/{recurringExpenseId}/cancel

Request:

```json
{
  "cycleId": "3a354849-f809-40b8-805d-fb784de471ef"
}
```

Resposta: `200 OK` com `RecurringExpenseResponse`, `active = false` e
`canceledAt` igual ao inicio do ciclo selecionado.

O gasto recorrente do ciclo selecionado e preservado. Gastos da mesma
recorrencia em ciclos posteriores sao removidos.

Nao existe endpoint de exclusao definitiva de recorrencia.

## Erros relevantes

```text
Category name already exists
Category is associated with expenses
Wallet description already exists
Wallet is associated with cycles or expenses
Inform either total amount or installment amount, but not both
Recurring expense is canceled
Category not found
Wallet not found
Cycle not found
Expense not found
Installment expense not found
Recurring expense not found
```
