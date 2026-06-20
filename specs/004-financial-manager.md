# 004 - Financial Manager

## Objetivo

Criar um gerenciador financeiro para permitir que o usuario organize carteiras de
gastos, categorias, ciclos mensais, gastos pontuais, gastos parcelados e gastos
recorrentes, com calculo automatico de ciclos a partir do dia de fim definido na
carteira e metricas de acompanhamento financeiro.

## Regras Gerais

Todas as rotas devem iniciar com `/api/v1`.

Todas as mensagens de erro retornadas pela API devem ser em ingles.

Todas as rotas desta spec devem ser privadas e acessiveis somente com
autenticacao via JWT por cookie.

Todas as entidades devem pertencer ao usuario autenticado direta ou
indiretamente.

O usuario dono nao deve ser enviado no payload de criacao ou edicao. O usuario
deve sempre ser obtido a partir da autenticacao.

Controllers e repositories nao devem conter regra de negocio. A regra de negocio
deve ficar nos use cases.

As validacoes dos campos devem ser feitas na camada de entidade, informando as
mensagens de cada erro para garantir o padrao de linguagem definido pelo
projeto.

Devem ser criados DTOs quando necessario para separar entrada e saida de dados
das entidades de dominio.

Todas as rotas desta spec devem ter requests correspondentes na collection Bruno
versionada em `bruno/`, incluindo metodo, caminho, cookies, payloads e exemplos
de resposta esperados para testes manuais da API.

## Modulo Financial Manager

Criar um modulo de negocio para o gerenciador financeiro seguindo a estrutura:

```text
br.com.ottonsam.toothy_planner_api.financial_manager
├── controllers
├── entities
├── repositories
└── usecases
```

## Entidades

### ExpenseCategory

Representa uma categoria de gasto criada pelo usuario.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `name`: texto obrigatorio.
- `color`: texto obrigatorio.
- `icon`: texto obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- A categoria deve pertencer ao usuario autenticado.
- `name` deve ser unico por usuario.
- Usuarios diferentes podem ter categorias com o mesmo `name`.
- `color` deve representar a cor exibida para a categoria.
- `icon` deve representar o icone exibido para a categoria.
- Uma categoria so pode ser listada, visualizada, editada ou removida pelo
  usuario dono.
- Uma categoria nao pode ser removida se estiver associada a gastos existentes.

### ExpenseWallet

Representa uma carteira financeira do usuario. A carteira engloba os ciclos de
gastos e define a meta de gastos e o dia de fim de ciclo.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `description`: texto obrigatorio.
- `spendingGoal`: valor decimal obrigatorio.
- `cycleEndDay`: inteiro obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- A carteira deve pertencer ao usuario autenticado.
- `description` deve ser unica por usuario.
- Usuarios diferentes podem ter carteiras com a mesma `description`.
- `spendingGoal` deve ser maior que zero.
- `cycleEndDay` deve representar o dia do mes usado como fim do ciclo.
- `cycleEndDay` deve estar entre `1` e `31`.
- Uma carteira so pode ser listada, visualizada, editada ou removida pelo usuario
  dono.
- A alteracao de `cycleEndDay` deve recalcular ciclos a partir do atual e gastos ja criados.

### ExpenseCycle

Representa um ciclo mensal de gastos dentro de uma carteira.

Campos:

- `id`: UUID.
- `wallet`: obrigatoria.
- `referenceMonth`: inteiro obrigatorio.
- `referenceYear`: inteiro obrigatorio.
- `startsAt`: date obrigatorio.
- `endsAt`: date obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- O ciclo deve pertencer ao usuario autenticado indiretamente pela carteira.
- O ciclo nao deve ser criado por endpoint proprio.
- O ciclo deve ser criado automaticamente quando um gasto for registrado para uma
  data que pertence a ele.
- O ciclo deve ser criado automaticamente quando uma compra parcelada gerar
  parcelas para ciclos futuros.
- Deve existir apenas um ciclo por carteira, `referenceMonth` e
  `referenceYear`.
- `referenceMonth` e `referenceYear` devem representar o mes e ano de referencia
  do fim do ciclo.
- `endsAt` deve ser calculado a partir de `referenceMonth`, `referenceYear` e
  `wallet.cycleEndDay`.
- Se `wallet.cycleEndDay` nao existir no mes de referencia, `endsAt` deve ser o
  ultimo dia do mes de referencia.
- `startsAt` deve ser o dia seguinte ao `endsAt` calculado para o ciclo anterior
  da mesma carteira, mesmo que o ciclo anterior ainda nao esteja persistido.
- Exemplo: se a carteira tem `cycleEndDay` igual a `15`, o ciclo de julho cobre
  o periodo de `16/06` ate `15/07`.
- Exemplo: se a carteira tem `cycleEndDay` igual a `15`, um gasto em `13/07`
  deve ser associado ao ciclo de julho.
- Exemplo: se a carteira tem `cycleEndDay` igual a `15`, um gasto em `16/07`
  deve ser associado ao ciclo de agosto.

### Expense

Representa um gasto registrado em um ciclo.

Campos:

- `id`: UUID.
- `wallet`: obrigatoria.
- `cycle`: obrigatorio.
- `category`: obrigatoria.
- `description`: texto obrigatorio.
- `amount`: valor decimal obrigatorio.
- `expenseDate`: date obrigatorio.
- `type`: enum obrigatorio.
- `parentExpenseId`: UUID opcional.
- `installmentNumber`: inteiro opcional.
- `installmentTotal`: inteiro opcional.
- `recurrenceId`: UUID opcional.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Tipos de gasto:

- `ONE_TIME`: gasto pontual.
- `INSTALLMENT`: parcela de uma compra parcelada.
- `RECURRING`: gasto recorrente gerado para um ciclo.

Regras:

- O gasto deve pertencer ao usuario autenticado indiretamente pela carteira.
- `wallet`, `cycle` e `category` devem pertencer ao usuario autenticado.
- `amount` deve ser maior que zero.
- `expenseDate` deve determinar automaticamente o ciclo do gasto conforme o
  `cycleEndDay` da carteira.
- O cliente nao deve enviar `cycleId` na criacao de um gasto pontual.
- O ciclo deve ser encontrado ou criado automaticamente durante a criacao do
  gasto.
- Gastos do tipo `INSTALLMENT` devem ter `parentExpenseId`,
  `installmentNumber` e `installmentTotal`.
- Gastos do tipo `RECURRING` devem ter `recurrenceId`.
- `parentExpenseId` deve permitir editar ou cancelar uma compra parcelada em
  lote.

### InstallmentExpense

Representa a compra parcelada original que gera gastos do tipo `INSTALLMENT`.

Campos:

- `id`: UUID.
- `wallet`: obrigatoria.
- `category`: obrigatoria.
- `description`: texto obrigatorio.
- `totalAmount`: valor decimal opcional.
- `installmentAmount`: valor decimal opcional.
- `installments`: inteiro obrigatorio.
- `firstExpenseDate`: date obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- A compra parcelada deve pertencer ao usuario autenticado indiretamente pela
  carteira.
- Deve ser possivel cadastrar uma compra parcelada informando
  `totalAmount` e `installments`.
- Deve ser possivel cadastrar uma compra parcelada informando
  `installmentAmount` e `installments`.
- O cliente deve informar exatamente uma das opcoes:
  - `totalAmount` com `installments`;
  - `installmentAmount` com `installments`.
- `installments` deve ser maior que zero.
- Quando `totalAmount` for informado, o sistema deve calcular o valor de cada
  parcela dividindo `totalAmount` por `installments`.
- O valor calculado de cada parcela deve ser arredondado para cima ate os
  centavos.
- Quando `installmentAmount` for informado, todas as parcelas devem usar esse
  valor.
- Cada parcela deve gerar um gasto do tipo `INSTALLMENT`.
- Cada parcela deve ser associada ao ciclo correspondente a sua data.
- Os ciclos correspondentes as parcelas devem ser criados automaticamente quando
  ainda nao existirem.
- As parcelas devem ser geradas em ciclos mensais consecutivos, iniciando pelo
  ciclo correspondente a `firstExpenseDate`.
- Todas as parcelas geradas por uma mesma compra parcelada devem ter o mesmo
  `parentExpenseId`.
- Cada parcela deve preencher `installmentNumber` com sua posicao na sequencia.
- Cada parcela deve preencher `installmentTotal` com a quantidade total de
  parcelas.
- A edicao em lote da compra parcelada deve atualizar as parcelas futuras que
  ainda nao foram encerradas pelo ciclo.

### RecurringExpense

Representa uma configuracao de gasto recorrente que gera gastos nos ciclos.

Campos:

- `id`: UUID.
- `wallet`: obrigatoria.
- `category`: obrigatoria.
- `description`: texto obrigatorio.
- `amount`: valor decimal obrigatorio.
- `startsAt`: date obrigatorio.
- `canceledAt`: date opcional.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- A recorrencia deve pertencer ao usuario autenticado indiretamente pela carteira.
- `wallet` e `category` devem pertencer ao usuario autenticado.
- `amount` deve ser maior que zero.
- Gastos recorrentes devem ser considerados ativos enquanto `canceledAt` for
  nulo.
- Ao cadastrar uma recorrencia, deve ser gerado um gasto do tipo `RECURRING` no
  ciclo correspondente a `startsAt`.
- Gastos recorrentes devem ser adicionados em todos os ciclos posteriores ao
  registro da recorrencia.
- Como ciclos sao criados sob demanda, a recorrencia ativa deve gerar
  automaticamente o gasto recorrente quando um novo ciclo posterior for criado.
- O gasto recorrente gerado deve ter `recurrenceId`.
- Ao cancelar uma recorrencia em um ciclo, o sistema deve preencher
  `canceledAt` com uma data pertencente ao ciclo informado.
- Ao cancelar uma recorrencia, todos os gastos recorrentes da mesma recorrencia
  em ciclos seguintes devem ser removidos.
- O cancelamento nao deve remover gastos recorrentes de ciclos anteriores ao
  ciclo de cancelamento.
- Ao excluir diretamente um gasto do tipo `RECURRING`, o gasto selecionado e
  todos os gastos da mesma recorrencia nos ciclos seguintes devem ser removidos.
- A exclusao direta de um gasto `RECURRING` deve cancelar a recorrencia pai no
  ciclo selecionado, impedindo a geracao em novos ciclos.
- A exclusao direta deve preservar gastos da mesma recorrencia em ciclos
  anteriores e nao deve afetar outras recorrencias.
- A exclusao direta de gastos `ONE_TIME` e `INSTALLMENT` deve remover somente o
  gasto selecionado.

## Metricas Financeiras

Deve ser possivel visualizar metricas por carteira e por ciclo.

### Metricas Do Ciclo

Dados esperados:

- `walletId`;
- `cycleId`;
- `referenceMonth`;
- `referenceYear`;
- `startsAt`;
- `endsAt`;
- `spendingGoal`;
- `totalSpent`;
- `remainingAmount`;
- `remainingDailyAmount`;
- `installmentTotalFromCurrentCycle`;
- `recurringMonthlyTotal`;
- `oneTimeTotal`.

Regras:

- `totalSpent` deve ser a soma de todos os gastos do ciclo.
- `remainingAmount` deve ser calculado como `spendingGoal - totalSpent`.
- `remainingAmount` pode ser negativo.
- `remainingDailyAmount` deve ser calculado como `remainingAmount` dividido pela
  quantidade de dias restantes ate o fim do ciclo.
- A data atual deve ser considerada para calcular os dias restantes do ciclo.
- Se o ciclo ja estiver encerrado, `remainingDailyAmount` deve ser `0`.
- Se a data atual estiver antes do inicio do ciclo, os dias restantes devem ser
  contados de `startsAt` ate `endsAt`.
- `installmentTotalFromCurrentCycle` deve representar o total de gastos
  parcelados do ciclo atual em diante.
- Para calcular `installmentTotalFromCurrentCycle`, devem ser considerados os
  gastos do tipo `INSTALLMENT` no ciclo atual e em ciclos futuros da mesma
  carteira.
- `recurringMonthlyTotal` deve representar a soma mensal das recorrencias ativas
  da carteira.
- Apenas recorrencias ativas devem compor `recurringMonthlyTotal`.
- `oneTimeTotal` deve representar a soma de gastos pontuais do ciclo.

### Metricas Da Carteira

Dados esperados:

- `walletId`;
- `description`;
- `spendingGoal`;
- `cycleEndDay`;
- `currentCycle`;
- `currentCycleMetrics`;
- `activeRecurringMonthlyTotal`;
- `installmentTotalFromCurrentCycle`;

Regras:

- `currentCycle` deve ser o ciclo correspondente a data atual.
- Se o ciclo atual ainda nao existir, `currentCycle` deve ser nulo.
- Se `currentCycle` for nulo, `currentCycleMetrics` tambem deve ser nulo.
- A consulta de metricas da carteira nao deve criar ciclos automaticamente.
- `currentCycleMetrics` deve seguir as regras de metricas do ciclo.
- `activeRecurringMonthlyTotal` deve considerar apenas recorrencias ativas.
- `installmentTotalFromCurrentCycle` deve considerar gastos parcelados do ciclo
  atual em diante.

## Endpoints

### Categories

- `POST /api/v1/financial-manager/categories`
- `GET /api/v1/financial-manager/categories`
- `GET /api/v1/financial-manager/categories/{categoryId}`
- `PUT /api/v1/financial-manager/categories/{categoryId}`
- `DELETE /api/v1/financial-manager/categories/{categoryId}`

### Wallets

- `POST /api/v1/financial-manager/wallets`
- `GET /api/v1/financial-manager/wallets`
- `GET /api/v1/financial-manager/wallets/{walletId}`
- `PUT /api/v1/financial-manager/wallets/{walletId}`
- `DELETE /api/v1/financial-manager/wallets/{walletId}`
- `GET /api/v1/financial-manager/wallets/{walletId}/metrics`

### Cycles

- `GET /api/v1/financial-manager/wallets/{walletId}/cycles`
- `GET /api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}`
- `GET /api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/metrics`
- `GET /api/v1/financial-manager/wallets/{walletId}/cycles/{cycleId}/expenses`

Regras:

- Nao deve existir endpoint para criar ciclo manualmente.
- Ciclos devem ser criados automaticamente pelos fluxos de gastos, parcelas e
  recorrencias.
- A listagem de gastos do ciclo deve retornar gastos `ONE_TIME`, `INSTALLMENT`
  e `RECURRING`, ordenados por `expenseDate` e `createdAt`.
- A listagem deve retornar um array vazio quando o ciclo nao possuir gastos.

### Expenses

- `POST /api/v1/financial-manager/wallets/{walletId}/expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}`
- `PUT /api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}`
- `DELETE /api/v1/financial-manager/wallets/{walletId}/expenses/{expenseId}`

### Installment Expenses

- `POST /api/v1/financial-manager/wallets/{walletId}/installment-expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/installment-expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/installment-expenses/{installmentExpenseId}`
- `PUT /api/v1/financial-manager/wallets/{walletId}/installment-expenses/{installmentExpenseId}`
- `DELETE /api/v1/financial-manager/wallets/{walletId}/installment-expenses/{installmentExpenseId}`

### Recurring Expenses

- `POST /api/v1/financial-manager/wallets/{walletId}/recurring-expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/recurring-expenses`
- `GET /api/v1/financial-manager/wallets/{walletId}/recurring-expenses/{recurringExpenseId}`
- `PUT /api/v1/financial-manager/wallets/{walletId}/recurring-expenses/{recurringExpenseId}`
- `POST /api/v1/financial-manager/wallets/{walletId}/recurring-expenses/{recurringExpenseId}/cancel`

## Formatos De Request

### Criar Categoria

```json
{
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "utensils"
}
```

### Criar Carteira

```json
{
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "cycleEndDay": 15
}
```

### Criar Gasto Pontual

```json
{
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "description": "Mercado",
  "amount": 250.90,
  "expenseDate": "2026-07-13"
}
```

### Criar Compra Parcelada Por Valor Total

```json
{
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "description": "Notebook",
  "totalAmount": 3500.00,
  "installments": 10,
  "firstExpenseDate": "2026-07-16"
}
```

### Criar Compra Parcelada Por Valor Da Parcela

```json
{
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "description": "Academia",
  "installmentAmount": 120.00,
  "installments": 12,
  "firstExpenseDate": "2026-07-13"
}
```

### Criar Recorrencia

```json
{
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "description": "Internet",
  "amount": 99.90,
  "startsAt": "2026-07-13"
}
```

### Cancelar Recorrencia

```json
{
  "cycleId": "00000000-0000-0000-0000-000000000000"
}
```

## Respostas Esperadas

### Categoria

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "utensils",
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Carteira

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "cycleEndDay": 15,
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Ciclo

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "walletId": "00000000-0000-0000-0000-000000000000",
  "referenceMonth": 7,
  "referenceYear": 2026,
  "startsAt": "2026-06-16",
  "endsAt": "2026-07-15",
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Gasto

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "walletId": "00000000-0000-0000-0000-000000000000",
  "cycleId": "00000000-0000-0000-0000-000000000000",
  "category": {
    "id": "00000000-0000-0000-0000-000000000000",
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
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Metricas Do Ciclo

```json
{
  "walletId": "00000000-0000-0000-0000-000000000000",
  "cycleId": "00000000-0000-0000-0000-000000000000",
  "referenceMonth": 7,
  "referenceYear": 2026,
  "startsAt": "2026-06-16",
  "endsAt": "2026-07-15",
  "spendingGoal": 3000.00,
  "totalSpent": 250.90,
  "remainingAmount": 2749.10,
  "remainingDailyAmount": 916.37,
  "installmentTotalFromCurrentCycle": 3500.00,
  "recurringMonthlyTotal": 99.90,
  "oneTimeTotal": 250.90
}
```

## Validacoes E Erros Esperados

- Retornar erro quando o usuario tentar acessar categoria, carteira, ciclo,
  gasto, compra parcelada ou recorrencia de outro usuario.
- Retornar erro quando `name` de categoria ja existir para o usuario.
- Retornar erro quando `description` de carteira ja existir para o usuario.
- Retornar erro quando `spendingGoal` for menor ou igual a zero.
- Retornar erro quando `cycleEndDay` estiver fora do intervalo de `1` a `31`.
- Retornar erro quando `amount` for menor ou igual a zero.
- Retornar erro quando `installments` for menor ou igual a zero.
- Retornar erro quando a compra parcelada receber `totalAmount` e
  `installmentAmount` ao mesmo tempo.
- Retornar erro quando a compra parcelada nao receber `totalAmount` nem
  `installmentAmount`.
- Retornar erro quando uma categoria em uso for removida.
- Retornar erro quando uma carteira com ciclos ou gastos for removida.
