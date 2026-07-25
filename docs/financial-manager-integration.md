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
  "text": "fui ao mercado e gastei 32 reais",
  "referenceDate": "2026-07-13"
}
```

`referenceDate` e opcional. Quando omitido, a API usa a data atual.

A API chama DeepSeek, classifica categoria e tipo de gasto, cria o registro sem
pre-confirmacao e retorna o que foi criado.

Resposta para gasto pontual:

```json
{
  "type": "ONE_TIME",
  "expense": {
    "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
    "category": {
      "key": "ALIMENTACAO",
      "name": "Alimentacao",
      "color": "#16A34A",
      "icon": "utensils"
    },
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
      "category": {
        "key": "ALIMENTACAO",
        "name": "Alimentacao",
        "color": "#16A34A",
        "icon": "utensils"
      },
      "description": "Mercado",
      "amount": 32.00,
      "expenseDate": "2026-07-13",
      "type": "ONE_TIME",
      "source": "AI_TEXT"
    }
  ]
}
```

Para textos parcelados, `installmentExpense` vem preenchido e
`generatedExpenses` contem as parcelas criadas. Para textos recorrentes,
`recurringExpense` vem preenchido e `generatedExpenses` contem as recorrencias
geradas para ciclos existentes.

## Criacao por audio

### POST /wallets/{walletId}/expenses/audio

```json
{
  "audioBase64": "AAAA...",
  "contentType": "audio/webm",
  "referenceDate": "2026-07-13"
}
```

`audioBase64` deve conter o audio em Base64 dentro do JSON. O formato
preferencial para web e `audio/webm` com codec Opus, mas a API aceita:

- `audio/webm`
- `audio/ogg`
- `audio/wav`
- `audio/mpeg`
- `audio/mp4`

`contentType` pode conter parametros do navegador, como
`audio/webm;codecs=opus`; a API normaliza para o MIME type base.

Fluxo:

1. A API valida o Base64, o formato e o tamanho maximo configurado.
2. O audio e enviado ao servico interno `audio-transcriber` via Docker.
3. O texto transcrito e enviado ao mesmo fluxo DeepSeek usado por
   `/expenses/text`.
4. O gasto e criado diretamente como pontual, parcelado ou recorrente.

Resposta:

```json
{
  "transcribedText": "comprei um notebook parcelado em 12 vezes de 199 reais",
  "classification": {
    "type": "INSTALLMENT"
  },
  "expense": null,
  "installmentExpense": {
    "id": "e1e315d0-8268-49f6-8f58-7b150237cc55",
    "walletId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
    "description": "Notebook",
    "installmentAmount": 199.00,
    "installments": 12
  },
  "recurringExpense": null,
  "generatedExpenses": []
}
```

Gastos gerados por audio retornam `source = AI_AUDIO` e continuam editaveis
pelas rotas manuais existentes.

Erros esperados:

- `400`: `Audio content is required`
- `400`: `Audio content must be valid Base64`
- `400`: `Audio content type is required`
- `400`: `Audio content type is not supported`
- `400`: `Audio content exceeds maximum size`
- `502`: `Audio transcription returned empty text`
- `502`: `Audio transcription timed out`
- `502`: `Audio transcription service is unavailable`

O audio nao e salvo em banco, arquivo local, storage externo ou logs. O arquivo
temporario criado pelo servico de transcricao e removido apos o processamento.

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
