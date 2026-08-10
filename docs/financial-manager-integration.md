# Financial Manager - Guia de integracao

Contrato de integracao do modulo financeiro da Toothy Planner API.

## Base

- Base local: `http://localhost:8080`
- Prefixo: `/api/v1/financial-manager`
- Formato: `application/json`
- Autenticacao: cookie HTTP-only `access_token`
- Datas: `YYYY-MM-DD`

Todas as rotas exigem autenticacao.

## Conceitos

Categorias sao globais e fixas. O frontend deve usar `GET /categories` para
preencher seletores, mas nao pode criar, editar ou excluir categorias.

Carteiras agrupam ciclos e gastos. Cada carteira tem:

- `startsAt`: data inicial da primeira janela de ciclo.
- `targetSpendingDay`: dia alvo para consumir a meta mensal.
- `spendingGoal`: meta de gasto do ciclo.

Ciclos nao possuem rota de criacao. Eles sao criados automaticamente quando um
gasto, parcela ou recorrencia e registrado para uma data da carteira.

Exemplo: carteira com `startsAt = 2026-07-28`.

- O primeiro ciclo vai de `2026-07-28` ate `2026-08-27`.
- Um gasto em `2026-08-27` fica no ciclo de agosto.
- Um gasto em `2026-08-28` fica no ciclo de setembro.
- Se `targetSpendingDay = 10`, `remainingDailyAmount` considera os dias ate
  `2026-08-10` no ciclo de agosto.

## Enums

### ExpenseCategory

```json
[
  "ALIMENTACAO",
  "MORADIA",
  "TRANSPORTE",
  "SAUDE",
  "EDUCACAO",
  "LAZER",
  "SERVICOS",
  "COMPRAS",
  "TRABALHO",
  "PETS",
  "OUTROS"
]
```

### ExpenseType

```json
["ONE_TIME", "INSTALLMENT", "RECURRING"]
```

### ExpenseSource

```json
["MANUAL", "AI_TEXT", "AI_AUDIO"]
```

`AI_AUDIO` e mantido apenas para compatibilidade com gastos historicos. Novos
gastos criados a partir de texto digitado ou ditado usam `AI_TEXT`.

## Categorias

### GET /categories

Retorna categorias fixas com metadados para UI.

```json
[
  {
    "key": "ALIMENTACAO",
    "name": "Alimentacao",
    "color": "#16A34A",
    "icon": "utensils",
    "description": "Mercado, restaurantes, delivery e alimentos"
  }
]
```

## Carteiras

### POST /wallets

```json
{
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "startsAt": "2026-07-28",
  "targetSpendingDay": 10
}
```

Resposta:

```json
{
  "id": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "startsAt": "2026-07-28",
  "targetSpendingDay": 10,
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

Rotas:

- `GET /wallets`
- `GET /wallets/{walletId}`
- `PUT /wallets/{walletId}`
- `DELETE /wallets/{walletId}`
- `GET /wallets/{walletId}/metrics`

`description` deve ser unica por usuario.

## Ciclos

Rotas:

- `GET /wallets/{walletId}/cycles`
- `GET /wallets/{walletId}/cycles/{cycleId}`
- `GET /wallets/{walletId}/cycles/{cycleId}/metrics`
- `GET /wallets/{walletId}/cycles/{cycleId}/expenses`

Resposta de ciclo:

```json
{
  "id": "3a354849-f809-40b8-805d-fb784de471ef",
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "referenceMonth": 8,
  "referenceYear": 2026,
  "startsAt": "2026-07-28",
  "endsAt": "2026-08-27",
  "targetSpendingDate": "2026-08-10",
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

## Gastos pontuais

### POST /wallets/{walletId}/expenses

```json
{
  "category": "ALIMENTACAO",
  "description": "Mercado",
  "amount": 250.90,
  "expenseDate": "2026-08-09"
}
```

Gastos podem ser listados, consultados, editados e excluidos:

- `GET /wallets/{walletId}/expenses`
- `GET /wallets/{walletId}/expenses/{expenseId}`
- `PUT /wallets/{walletId}/expenses/{expenseId}`
- `DELETE /wallets/{walletId}/expenses/{expenseId}`

A edicao manual permite alterar `category`, `description`, `amount` e
`expenseDate`.

## Criacao por texto

### POST /wallets/{walletId}/expenses/text

```json
{
  "text": "gastei 32 reais no mercado e 20 reais na farmacia",
  "referenceDate": "2026-07-13"
}
```

`referenceDate` e opcional. Quando omitido, a API usa a data atual.

A API chama DeepSeek uma vez para separar, normalizar e classificar todos os
gastos do texto. As classificacoes sao validadas antes da persistencia e sao
processadas em lotes internos de ate 25 itens, com limite de 50 gastos por
texto. A operacao e atomica: qualquer falha impede a criacao de todos os itens.

Resposta:

```json
{
  "expenseCount": 2,
  "generatedExpenseCount": 2,
  "items": [
    {
      "sourceText": "32 reais no mercado",
      "type": "ONE_TIME",
      "expense": {
        "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
        "description": "Mercado",
        "amount": 32.00,
        "expenseDate": "2026-07-13",
        "type": "ONE_TIME",
        "source": "AI_TEXT"
      },
      "installmentExpense": null,
      "recurringExpense": null,
      "generatedExpenses": [
        {
          "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
          "description": "Mercado",
          "amount": 32.00,
          "expenseDate": "2026-07-13",
          "type": "ONE_TIME",
          "source": "AI_TEXT"
        }
      ]
    },
    {
      "sourceText": "20 reais na farmacia",
      "type": "ONE_TIME",
      "expense": {
        "id": "b9ced251-761e-4793-823a-bba1a3932cc4",
        "description": "Farmacia",
        "amount": 20.00,
        "expenseDate": "2026-07-13",
        "type": "ONE_TIME",
        "source": "AI_TEXT"
      },
      "installmentExpense": null,
      "recurringExpense": null,
      "generatedExpenses": [
        {
          "id": "b9ced251-761e-4793-823a-bba1a3932cc4",
          "description": "Farmacia",
          "amount": 20.00,
          "expenseDate": "2026-07-13",
          "type": "ONE_TIME",
          "source": "AI_TEXT"
        }
      ]
    }
  ],
  "generatedExpenses": [
    {
      "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
      "description": "Mercado",
      "amount": 32.00,
      "expenseDate": "2026-07-13",
      "type": "ONE_TIME",
      "source": "AI_TEXT"
    },
    {
      "id": "b9ced251-761e-4793-823a-bba1a3932cc4",
      "description": "Farmacia",
      "amount": 20.00,
      "expenseDate": "2026-07-13",
      "type": "ONE_TIME",
      "source": "AI_TEXT"
    }
  ]
}
```

Cada item preserva em `sourceText` o trecho que originou a classificacao. Para
itens parcelados, `installmentExpense` vem preenchido e `generatedExpenses`
contem as parcelas. Para itens recorrentes, `recurringExpense` vem preenchido e
`generatedExpenses` contem as recorrencias dos ciclos existentes. A lista
agregada na raiz contem todas as ocorrencias criadas, na ordem dos itens.

Texto sem gasto identificavel retorna `422`. Falhas ou respostas invalidas da
IA retornam `502`. Nenhum gasto e persistido nesses casos.

## Ditado de gastos no frontend

O backend nao recebe nem transcreve audio. O frontend pode usar reconhecimento
de fala para preencher um campo editavel e deve enviar o texto revisado pelo
usuario para `POST /wallets/{walletId}/expenses/text`.

Nao existe endpoint `/expenses/audio`. Textos obtidos por ditado seguem as
mesmas validacoes e regras de classificacao dos textos digitados e os gastos
criados possuem `source = AI_TEXT`.

## Parceladas

### POST /wallets/{walletId}/installment-expenses

Com valor total:

```json
{
  "category": "COMPRAS",
  "description": "Notebook",
  "totalAmount": 3500.00,
  "installments": 10,
  "firstExpenseDate": "2026-08-09"
}
```

Com valor da parcela:

```json
{
  "category": "COMPRAS",
  "description": "Celular",
  "installmentAmount": 199.00,
  "installments": 12,
  "firstExpenseDate": "2026-08-09"
}
```

A API aceita `totalAmount` ou `installmentAmount`, nunca ambos. Quando usa
`totalAmount`, divide pelo numero de parcelas com arredondamento para cima nos
centavos.

Rotas:

- `GET /wallets/{walletId}/installment-expenses`
- `GET /wallets/{walletId}/installment-expenses/{installmentExpenseId}`
- `PUT /wallets/{walletId}/installment-expenses/{installmentExpenseId}`
- `DELETE /wallets/{walletId}/installment-expenses/{installmentExpenseId}`

Parcelas geradas possuem `type = INSTALLMENT` e `parentExpenseId` com o id do
registro pai.

## Recorrentes

### POST /wallets/{walletId}/recurring-expenses

```json
{
  "category": "SERVICOS",
  "description": "Internet",
  "amount": 99.90,
  "startsAt": "2026-08-06"
}
```

Rotas:

- `GET /wallets/{walletId}/recurring-expenses`
- `GET /wallets/{walletId}/recurring-expenses/{recurringExpenseId}`
- `PUT /wallets/{walletId}/recurring-expenses/{recurringExpenseId}`
- `POST /wallets/{walletId}/recurring-expenses/{recurringExpenseId}/cancel`

Cancelamento:

```json
{
  "cycleId": "3a354849-f809-40b8-805d-fb784de471ef"
}
```

Ao excluir uma ocorrencia recorrente pelo endpoint de gastos, a recorrencia pai
e cancelada a partir daquele ciclo e todas as ocorrencias daquele ciclo em
diante sao removidas.

## Metricas

`ExpenseCycleMetricsResponse`:

```json
{
  "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
  "cycleId": "3a354849-f809-40b8-805d-fb784de471ef",
  "referenceMonth": 8,
  "referenceYear": 2026,
  "startsAt": "2026-07-28",
  "endsAt": "2026-08-27",
  "targetSpendingDate": "2026-08-10",
  "spendingGoal": 3000.00,
  "totalSpent": 250.90,
  "remainingAmount": 2749.10,
  "remainingDailyAmount": 211.47,
  "spentUntilTargetDate": 250.90,
  "spentAfterTargetDate": 0.00,
  "installmentTotalFromCurrentCycle": 0.00,
  "recurringMonthlyTotal": 99.90,
  "oneTimeTotal": 250.90,
  "spendingByCategory": [
    {
      "category": {
        "key": "ALIMENTACAO",
        "name": "Alimentacao",
        "color": "#16A34A",
        "icon": "utensils"
      },
      "totalSpent": 250.90,
      "percentage": 100.00
    }
  ]
}
```

`remainingDailyAmount` retorna `null` quando a data atual ja passou da
`targetSpendingDate` ou do fim do ciclo. `spentAfterTargetDate` retorna `null`
quando o ciclo ja terminou.
