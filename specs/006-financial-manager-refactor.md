# 006 - Financial Manager Refactor

## Objetivo

Refatorar o gerenciador financeiro para:

- manter carteiras separadas por usuario;
- fazer ciclos mensais a partir da data de inicio da carteira;
- permitir que a carteira tenha um dia alvo de gasto para calculo de saldo por
  dia;
- remover o cadastro manual de categorias;
- usar categorias base fixas e globais;
- permitir criacao de gastos a partir de texto livre com auxilio da DeepSeek;
- permitir que a IA identifique gastos pontuais, parcelados e recorrentes;
- manter os endpoints estruturados de criacao, edicao, listagem e remocao de
  gastos, parcelamentos e recorrencias.

Esta spec altera e substitui partes da spec `004 - Financial Manager`.

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

Devem ser criados DTOs quando necessario para separar entrada e saida de dados
das entidades de dominio.

As validacoes dos campos devem ser feitas na camada de entidade, informando as
mensagens de cada erro para garantir o padrao de linguagem definido pelo
projeto.

Todas as rotas novas ou alteradas devem ter requests correspondentes na
collection Bruno versionada em `bruno/`.

## Categorias Base

O cadastro manual de categorias deve ser removido.

Categorias devem ser fixas, globais e representadas por enum no dominio.

Nao deve existir criacao, edicao ou remocao manual de categorias por usuario.

Usuarios diferentes devem usar o mesmo conjunto de categorias base.

### ExpenseCategory

`ExpenseCategory` deixa de ser uma entidade persistida por usuario e passa a ser
um enum.

Categorias base iniciais:

- `ALIMENTACAO`
- `MORADIA`
- `TRANSPORTE`
- `SAUDE`
- `EDUCACAO`
- `LAZER`
- `SERVICOS`
- `COMPRAS`
- `TRABALHO`
- `PETS`
- `OUTROS`

Cada categoria deve ter metadados fixos para resposta da API:

- `key`: valor do enum.
- `name`: nome amigavel.
- `color`: cor sugerida para interface.
- `icon`: icone sugerido para interface.
- `description`: descricao curta para orientar a classificacao.

Esses metadados devem ser definidos no backend e usados nas respostas da API e
no prompt de classificacao da IA.

### Category Response

Exemplo:

```json
{
  "key": "ALIMENTACAO",
  "name": "Alimentacao",
  "color": "#F97316",
  "icon": "restaurant-outline",
  "description": "Mercado, restaurantes, delivery, padaria e comida em geral"
}
```

### Endpoints De Categoria

Deve existir apenas endpoint de listagem das categorias base:

- `GET /api/v1/financial-manager/categories`

Nao devem existir endpoints:

- `POST /api/v1/financial-manager/categories`
- `GET /api/v1/financial-manager/categories/{categoryId}`
- `PUT /api/v1/financial-manager/categories/{categoryId}`
- `DELETE /api/v1/financial-manager/categories/{categoryId}`

## Carteiras

Carteiras continuam separando ciclos e gastos do usuario.

### ExpenseWallet

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `description`: texto obrigatorio.
- `spendingGoal`: valor decimal obrigatorio.
- `startsAt`: date obrigatorio.
- `targetSpendingDay`: inteiro obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Campos removidos:

- `cycleEndDay`.

Regras:

- A carteira deve pertencer ao usuario autenticado.
- `description` deve ser unica por usuario.
- Usuarios diferentes podem ter carteiras com a mesma `description`.
- `spendingGoal` deve ser maior que zero.
- `startsAt` define o inicio do primeiro ciclo da carteira.
- Ciclos mensais devem ser calculados a partir de `startsAt`.
- `targetSpendingDay` representa o dia do mes ate o qual o usuario espera usar
  a meta de gastos da carteira.
- `targetSpendingDay` deve estar entre `1` e `31`.
- O dia alvo nao encerra o ciclo. Ele existe apenas para metricas.
- Uma carteira so pode ser listada, visualizada, editada ou removida pelo
  usuario dono.
- Alterar `startsAt` deve recalcular os ciclos e gastos associados conforme regra
  definida na implementacao da spec.
- Alterar `targetSpendingDay` deve recalcular metricas, mas nao deve mover
  gastos entre ciclos.

### Ciclos Da Carteira

Os ciclos deixam de ser calculados por dia de fim e passam a ser calculados pela
data de inicio da carteira.

Regras:

- O primeiro ciclo inicia em `wallet.startsAt`.
- Cada ciclo termina no dia anterior ao mesmo dia do mes seguinte.
- Exemplo: se `startsAt = 2026-07-28`, o primeiro ciclo cobre
  `2026-07-28` ate `2026-08-27`.
- O ciclo seguinte cobre `2026-08-28` ate `2026-09-27`.
- Se o dia de inicio nao existir no mes seguinte, o fim do ciclo deve ser
  ajustado para o dia anterior ao inicio calculado do proximo ciclo valido.
- O ciclo nao deve ser criado por endpoint proprio.
- O ciclo deve ser criado automaticamente quando um gasto for registrado para uma
  data que pertence a ele.
- O ciclo deve ser criado automaticamente quando uma compra parcelada gerar
  parcelas para ciclos futuros.
- Como ciclos sao criados sob demanda, recorrencias ativas devem gerar
  automaticamente o gasto recorrente quando um novo ciclo posterior for criado.

### Data Alvo De Gasto Do Ciclo

Cada ciclo deve ter uma data alvo calculada a partir de
`wallet.targetSpendingDay`.

Regras:

- `targetSpendingDate` deve ser uma data dentro do intervalo do ciclo.
- Para encontrar a data alvo, deve ser usada a primeira data dentro do ciclo cujo
  dia do mes seja igual a `targetSpendingDay`.
- Exemplo: carteira inicia em `2026-07-28`, ciclo vai de `2026-07-28` ate
  `2026-08-27`, `targetSpendingDay = 10`, entao `targetSpendingDate` e
  `2026-08-10`.
- Se nao existir uma data dentro do ciclo com esse dia do mes, a data alvo deve
  ser `cycle.endsAt`.
- A data alvo nao altera a associacao dos gastos ao ciclo.

## Gastos

### Expense

Campos:

- `id`: UUID.
- `wallet`: obrigatoria.
- `cycle`: obrigatorio.
- `category`: enum `ExpenseCategory` obrigatorio.
- `description`: texto obrigatorio.
- `amount`: valor decimal obrigatorio.
- `expenseDate`: date obrigatorio.
- `type`: enum obrigatorio.
- `parentExpenseId`: UUID opcional.
- `installmentNumber`: inteiro opcional.
- `installmentTotal`: inteiro opcional.
- `recurrenceId`: UUID opcional.
- `source`: enum obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Tipos de gasto:

- `ONE_TIME`: gasto pontual.
- `INSTALLMENT`: parcela de uma compra parcelada.
- `RECURRING`: gasto recorrente gerado para um ciclo.

Fontes de criacao:

- `MANUAL`: gasto criado por endpoint estruturado.
- `AI_TEXT`: gasto criado a partir de texto livre classificado pela IA.

Regras:

- O gasto deve pertencer ao usuario autenticado indiretamente pela carteira.
- `wallet` e `cycle` devem pertencer ao usuario autenticado.
- `category` deve ser uma das categorias base.
- `amount` deve ser maior que zero.
- `expenseDate` deve determinar automaticamente o ciclo do gasto conforme
  `wallet.startsAt`.
- O cliente nao deve enviar `cycleId` na criacao de um gasto pontual.
- O ciclo deve ser encontrado ou criado automaticamente durante a criacao do
  gasto.
- Gastos do tipo `INSTALLMENT` devem ter `parentExpenseId`,
  `installmentNumber` e `installmentTotal`.
- Gastos do tipo `RECURRING` devem ter `recurrenceId`.
- Gastos criados por IA devem poder ser editados manualmente depois.
- A edicao manual de um gasto deve permitir alterar:
  - `category`;
  - `description`;
  - `amount`;
  - `expenseDate`.
- Ao alterar `expenseDate`, o ciclo do gasto deve ser recalculado.
- A exclusao manual de gastos deve continuar disponivel.

### InstallmentExpense

`InstallmentExpense` deve usar `category` como enum `ExpenseCategory`.

As regras da spec `004` para parcelamento continuam validas, com a troca de
`categoryId` por `category`.

Ao criar por IA, o texto deve ser interpretado e o fluxo existente de criacao de
compra parcelada deve ser reutilizado pelo use case.

Exemplo:

```text
comprei algo parcelado em 12 vezes de 199
```

Resultado esperado:

- `type = INSTALLMENT`;
- `installments = 12`;
- `installmentAmount = 199.00`;
- devem ser geradas 12 parcelas de `199.00`;
- cada parcela deve ser associada ao ciclo correspondente.

### RecurringExpense

`RecurringExpense` deve usar `category` como enum `ExpenseCategory`.

As regras da spec `004` para recorrencias continuam validas, com a troca de
`categoryId` por `category`.

Ao criar por IA, o texto deve ser interpretado e o fluxo existente de criacao de
recorrencia deve ser reutilizado pelo use case.

Exemplo:

```text
contratei um plano de internet de 100 reais todo dia 6
```

Resultado esperado:

- `type = RECURRING`;
- `amount = 100.00`;
- `category = SERVICOS`;
- `startsAt` deve usar o proximo dia 6 coerente com a data de referencia usada
  na criacao.

Se a IA identificar gasto recorrente, mas o usuario nao informar o dia de
recorrencia, `startsAt` deve ser a data atual.

## Criacao De Gastos Por Texto Com IA

Deve ser criado um fluxo para receber um texto livre de gasto e criar o registro
automaticamente.

### Endpoint

- `POST /api/v1/financial-manager/wallets/{walletId}/expenses/text`

Request:

```json
{
  "text": "fui ao mercado e gastei 32 reais",
  "referenceDate": "2026-07-24"
}
```

Regras do request:

- `text` e obrigatorio.
- `text` deve ter conteudo nao vazio.
- `referenceDate` e opcional.
- Se `referenceDate` nao for informado, deve ser usada a data atual do sistema.
- `referenceDate` deve ser usada para resolver datas relativas ou ausentes no
  texto.

O endpoint nao deve retornar pre-visualizacao para confirmacao.

O endpoint deve criar imediatamente o gasto pontual, compra parcelada ou
recorrencia identificado pela IA.

### Resposta

A resposta deve indicar qual fluxo foi criado.

Exemplo para gasto pontual:

```json
{
  "type": "ONE_TIME",
  "expense": {
    "id": "00000000-0000-0000-0000-000000000000"
  }
}
```

Exemplo para compra parcelada:

```json
{
  "type": "INSTALLMENT",
  "installmentExpense": {
    "id": "00000000-0000-0000-0000-000000000000"
  },
  "expenses": [
    {
      "id": "00000000-0000-0000-0000-000000000000"
    }
  ]
}
```

Exemplo para recorrencia:

```json
{
  "type": "RECURRING",
  "recurringExpense": {
    "id": "00000000-0000-0000-0000-000000000000"
  },
  "expense": {
    "id": "00000000-0000-0000-0000-000000000000"
  }
}
```

Os objetos internos devem usar os mesmos DTOs completos ja retornados pelos
endpoints estruturados.

### Integracao Com DeepSeek

Deve ser usada a integracao existente com DeepSeek.

Deve ser criado um novo cliente/use case especifico para classificacao de gastos
financeiros por texto.

Deve existir um novo system prompt para este fluxo.

O prompt deve orientar a DeepSeek a:

- retornar apenas JSON valido;
- nao retornar markdown;
- classificar o tipo do gasto como `ONE_TIME`, `INSTALLMENT` ou `RECURRING`;
- escolher uma das categorias base;
- extrair descricao normalizada e curta;
- extrair valor monetario;
- extrair data do gasto quando existir;
- usar `referenceDate` quando a data nao existir ou for relativa;
- identificar quantidade de parcelas;
- diferenciar valor total parcelado de valor de cada parcela;
- identificar recorrencias;
- identificar dia de recorrencia quando informado;
- usar data atual quando uma recorrencia nao informar dia ou data de inicio;
- retornar nivel de confianca;
- retornar motivo curto da classificacao para auditoria.

Formato esperado da resposta da IA:

```json
{
  "type": "ONE_TIME",
  "category": "ALIMENTACAO",
  "description": "Mercado",
  "amount": 32.00,
  "expenseDate": "2026-07-24",
  "installments": null,
  "totalAmount": null,
  "installmentAmount": null,
  "recurringDay": null,
  "startsAt": null,
  "confidence": 0.94,
  "reason": "Texto menciona mercado e valor unico."
}
```

Regras:

- Se a DeepSeek falhar, nada deve ser criado.
- Se a resposta da DeepSeek for invalida, nada deve ser criado.
- Se a categoria retornada nao existir no enum, nada deve ser criado.
- Se o tipo retornado nao existir, nada deve ser criado.
- Se os campos obrigatorios para o tipo identificado estiverem ausentes, nada
  deve ser criado.
- O backend nao deve confiar cegamente nos valores da IA. Todas as validacoes das
  entidades e use cases existentes devem continuar sendo aplicadas.

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
- `targetSpendingDate`;
- `spendingGoal`;
- `totalSpent`;
- `spentUntilTargetDate`;
- `spentAfterTargetDate`;
- `remainingAmount`;
- `remainingDailyAmount`;
- `installmentTotalFromCurrentCycle`;
- `recurringMonthlyTotal`;
- `oneTimeTotal`;
- `spendingByCategory`.

Regras:

- `totalSpent` deve ser a soma de todos os gastos do ciclo.
- `spentUntilTargetDate` deve somar gastos do ciclo com `expenseDate` menor ou
  igual a `targetSpendingDate`.
- `spentAfterTargetDate` deve somar gastos do ciclo com `expenseDate` maior que
  `targetSpendingDate`.
- `remainingAmount` deve ser calculado como `spendingGoal - totalSpent`.
- `remainingAmount` pode ser negativo.
- `remainingDailyAmount` deve ser calculado como `remainingAmount` dividido pela
  quantidade de dias restantes ate `targetSpendingDate`.
- A data atual deve ser considerada para calcular os dias restantes ate a data
  alvo.
- Se a data atual estiver antes do inicio do ciclo, os dias restantes devem ser
  contados de `startsAt` ate `targetSpendingDate`.
- Se a data atual estiver depois de `targetSpendingDate`, `remainingDailyAmount`
  deve ser `null`.
- Se o ciclo ja estiver encerrado, `remainingDailyAmount` deve ser `null`.
- `spentAfterTargetDate` tambem deve ser `null` quando o ciclo ja estiver
  encerrado.
- `installmentTotalFromCurrentCycle` deve representar o total de gastos
  parcelados do ciclo atual em diante.
- `recurringMonthlyTotal` deve representar a soma mensal das recorrencias ativas
  da carteira.
- `oneTimeTotal` deve representar a soma de gastos pontuais do ciclo.
- `spendingByCategory` deve continuar representando o total gasto por categoria
  no ciclo e a porcentagem da categoria sobre `totalSpent`.

### Metricas Da Carteira

Dados esperados:

- `walletId`;
- `description`;
- `spendingGoal`;
- `startsAt`;
- `targetSpendingDay`;
- `currentCycle`;
- `currentCycleMetrics`;
- `activeRecurringMonthlyTotal`;
- `installmentTotalFromCurrentCycle`.

Regras:

- `currentCycle` deve ser o ciclo correspondente a data atual.
- Se o ciclo atual ainda nao existir, `currentCycle` deve ser nulo.
- Se `currentCycle` for nulo, `currentCycleMetrics` tambem deve ser nulo.
- A consulta de metricas da carteira nao deve criar ciclos automaticamente.
- `currentCycleMetrics` deve seguir as regras de metricas do ciclo.

## Endpoints Estruturados Mantidos

Os endpoints estruturados devem continuar existindo:

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

### Expenses

- `POST /api/v1/financial-manager/wallets/{walletId}/expenses`
- `POST /api/v1/financial-manager/wallets/{walletId}/expenses/text`
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

### Criar Carteira

```json
{
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "startsAt": "2026-07-28",
  "targetSpendingDay": 10
}
```

### Criar Gasto Pontual Estruturado

```json
{
  "category": "ALIMENTACAO",
  "description": "Mercado",
  "amount": 250.90,
  "expenseDate": "2026-07-29"
}
```

### Criar Gasto Por Texto

```json
{
  "text": "fui ao mercado e gastei 32 reais",
  "referenceDate": "2026-07-29"
}
```

### Criar Compra Parcelada Estruturada

```json
{
  "category": "COMPRAS",
  "description": "Notebook",
  "installmentAmount": 199.00,
  "installments": 12,
  "firstExpenseDate": "2026-07-29"
}
```

### Criar Recorrencia Estruturada

```json
{
  "category": "SERVICOS",
  "description": "Internet",
  "amount": 100.00,
  "startsAt": "2026-08-06"
}
```

## Respostas Esperadas

### Carteira

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "description": "Carteira pessoal",
  "spendingGoal": 3000.00,
  "startsAt": "2026-07-28",
  "targetSpendingDay": 10,
  "createdAt": "2026-07-24T10:00:00",
  "updatedAt": "2026-07-24T10:00:00"
}
```

### Ciclo

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "walletId": "00000000-0000-0000-0000-000000000000",
  "referenceMonth": 8,
  "referenceYear": 2026,
  "startsAt": "2026-07-28",
  "endsAt": "2026-08-27",
  "targetSpendingDate": "2026-08-10",
  "createdAt": "2026-07-24T10:00:00",
  "updatedAt": "2026-07-24T10:00:00"
}
```

### Gasto

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "walletId": "00000000-0000-0000-0000-000000000000",
  "cycleId": "00000000-0000-0000-0000-000000000000",
  "category": {
    "key": "ALIMENTACAO",
    "name": "Alimentacao",
    "color": "#F97316",
    "icon": "restaurant-outline"
  },
  "description": "Mercado",
  "amount": 32.00,
  "expenseDate": "2026-07-29",
  "type": "ONE_TIME",
  "source": "AI_TEXT",
  "parentExpenseId": null,
  "installmentNumber": null,
  "installmentTotal": null,
  "recurrenceId": null,
  "createdAt": "2026-07-24T10:00:00",
  "updatedAt": "2026-07-24T10:00:00"
}
```

### Metricas Do Ciclo

```json
{
  "walletId": "00000000-0000-0000-0000-000000000000",
  "cycleId": "00000000-0000-0000-0000-000000000000",
  "referenceMonth": 8,
  "referenceYear": 2026,
  "startsAt": "2026-07-28",
  "endsAt": "2026-08-27",
  "targetSpendingDate": "2026-08-10",
  "spendingGoal": 3000.00,
  "totalSpent": 250.90,
  "spentUntilTargetDate": 180.00,
  "spentAfterTargetDate": 70.90,
  "remainingAmount": 2749.10,
  "remainingDailyAmount": 392.73,
  "installmentTotalFromCurrentCycle": 2388.00,
  "recurringMonthlyTotal": 100.00,
  "oneTimeTotal": 250.90,
  "spendingByCategory": [
    {
      "category": {
        "key": "ALIMENTACAO",
        "name": "Alimentacao",
        "color": "#F97316",
        "icon": "restaurant-outline"
      },
      "totalSpent": 250.90,
      "percentage": 100.00
    }
  ]
}
```

## Migracao Do Modelo Atual

A implementacao desta spec deve prever migracao do modelo atual para o modelo
novo.

Regras:

- `ExpenseCategoryEntity` deve deixar de ser a fonte da categoria de gastos.
- Gastos, parcelamentos e recorrencias existentes devem passar a armazenar o enum
  `ExpenseCategory`.
- Categorias existentes devem ser convertidas para uma categoria base quando o
  nome for claramente equivalente.
- Categorias existentes sem equivalencia clara devem ser migradas para
  `OUTROS`.
- A migracao nao deve apagar gastos existentes.
- Os endpoints antigos de CRUD manual de categorias devem ser removidos ou
  desativados conforme estrategia de versionamento do projeto.

## Validacoes E Erros Esperados

- Retornar erro quando o usuario tentar acessar carteira, ciclo, gasto, compra
  parcelada ou recorrencia de outro usuario.
- Retornar erro quando `description` de carteira ja existir para o usuario.
- Retornar erro quando `spendingGoal` for menor ou igual a zero.
- Retornar erro quando `startsAt` nao for informado.
- Retornar erro quando `targetSpendingDay` estiver fora do intervalo de `1` a
  `31`.
- Retornar erro quando `category` nao existir no enum de categorias base.
- Retornar erro quando `amount` for menor ou igual a zero.
- Retornar erro quando `installments` for menor ou igual a zero.
- Retornar erro quando a compra parcelada receber `totalAmount` e
  `installmentAmount` ao mesmo tempo.
- Retornar erro quando a compra parcelada nao receber `totalAmount` nem
  `installmentAmount`.
- Retornar erro quando texto livre de gasto estiver vazio.
- Retornar erro quando a DeepSeek falhar.
- Retornar erro quando a DeepSeek retornar JSON invalido.
- Retornar erro quando a DeepSeek retornar uma classificacao incompleta ou
  inconsistente.

## Cenarios De Teste Esperados

- Criar carteira com `startsAt` e `targetSpendingDay`.
- Criar ciclos mensais a partir de `startsAt`.
- Associar gastos ao ciclo correto usando a data de inicio da carteira.
- Calcular `targetSpendingDate` dentro do ciclo.
- Calcular `remainingDailyAmount` ate `targetSpendingDate`.
- Retornar `remainingDailyAmount = null` quando a data atual passar da data
  alvo.
- Calcular `spentAfterTargetDate`.
- Retornar `spentAfterTargetDate = null` quando o ciclo estiver encerrado.
- Listar categorias base fixas.
- Garantir que nao existe criacao, edicao ou remocao manual de categorias.
- Criar gasto pontual estruturado usando categoria enum.
- Editar manualmente categoria, descricao, valor e data de um gasto.
- Excluir gasto manualmente.
- Criar gasto pontual por texto via DeepSeek.
- Criar compra parcelada por texto via DeepSeek e gerar todas as parcelas.
- Criar recorrencia por texto via DeepSeek.
- Criar recorrencia por texto sem dia informado usando a data atual.
- Nao criar nada quando a DeepSeek falhar.
- Nao criar nada quando a resposta da DeepSeek for invalida.
- Garantir isolamento de usuario em carteiras, ciclos, gastos, parcelamentos e
  recorrencias.
