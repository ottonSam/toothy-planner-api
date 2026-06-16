# 005 - Diet Module

## Objetivo

Criar um modulo de dieta para permitir que o usuario cadastre metas nutricionais,
registre entradas alimentares em gramas ou porcoes, consulte alimentos ja
conhecidos para autocomplete e visualize metricas diarias de calorias e
macronutrientes.

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

## Modulo Diet

Criar um modulo de negocio para dieta seguindo a estrutura:

```text
br.com.ottonsam.toothy_planner_api.diet
├── controllers
├── entities
├── repositories
└── usecases
```

## Normalizacao De Alimentos

Regras:

- O nome do alimento deve ser salvo sempre em maiusculo.
- A comparacao de nomes deve ignorar maiusculas, minusculas e diferencas de
  espacamento.
- A normalizacao nao deve remover acentos nem `ç`.
- A busca por nome deve ser sempre limitada aos alimentos do usuario
  autenticado.

## Entidades

### Food

Representa um alimento conhecido do usuario.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `name`: texto obrigatorio, normalizado em maiusculo, preservando acentos e
  `ç`.
- `kcalPerGram`: valor decimal obrigatorio.
- `proteinPerGram`: valor decimal obrigatorio.
- `carbohydratePerGram`: valor decimal obrigatorio.
- `fatPerGram`: valor decimal obrigatorio.
- `kcalPerPortion`: valor decimal obrigatorio.
- `proteinPerPortion`: valor decimal obrigatorio.
- `carbohydratePerPortion`: valor decimal obrigatorio.
- `fatPerPortion`: valor decimal obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- O alimento deve pertencer ao usuario autenticado.
- `name` deve ser unico por usuario depois da normalizacao.
- Usuarios diferentes podem ter alimentos com o mesmo `name`.
- O alimento deve ser criado automaticamente ao registrar uma entrada alimentar
  cujo alimento ainda nao exista para o usuario.
- O usuario nao deve poder criar, editar ou remover alimentos manualmente.
- O usuario nao deve poder alterar manualmente valores nutricionais de um
  alimento.
- Os valores por grama devem representar as calorias e macros de `1g` do
  alimento.
- Os valores por porcao devem representar a media nutricional de uma porcao comum
  do alimento.
- A porcao nao precisa ter equivalencia em gramas.

### DietEntry

Representa uma entrada de alimento registrada na dieta do usuario.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `food`: obrigatorio.
- `entryDate`: date obrigatorio.
- `quantity`: valor decimal obrigatorio.
- `unit`: enum obrigatorio.
- `kcal`: valor decimal obrigatorio.
- `protein`: valor decimal obrigatorio.
- `carbohydrate`: valor decimal obrigatorio.
- `fat`: valor decimal obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Unidades:

- `GRAMS`: quantidade em gramas.
- `PORTIONS`: quantidade em porcoes.

Regras:

- A entrada deve pertencer ao usuario autenticado.
- `food` deve pertencer ao usuario autenticado.
- `entryDate` deve ser a data informada no payload.
- Se `entryDate` nao for informado, deve ser usada a data atual.
- `quantity` deve ser maior que zero.
- Quando `unit` for `GRAMS`, os campos calculados devem usar os valores por
  grama do alimento multiplicados por `quantity`.
- Quando `unit` for `PORTIONS`, os campos calculados devem usar os valores por
  porcao do alimento multiplicados por `quantity`.
- Os valores calculados da entrada devem ser persistidos para preservar o
  historico caso a base de alimentos seja recalculada no futuro.
- Ao criar uma entrada, a API deve buscar se o usuario ja possui um alimento com
  o nome informado.
- Se o alimento ja existir, a API deve usar os valores salvos e nao deve chamar a
  DeepSeek.
- Se o alimento nao existir, a API deve chamar a DeepSeek para obter os dados
  nutricionais e salvar o alimento antes de salvar a entrada.
- Se a chamada a DeepSeek falhar, a entrada nao deve ser salva.

### DietDefaultGoal

Representa a meta nutricional padrao do usuario.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `kcal`: valor decimal obrigatorio.
- `protein`: valor decimal obrigatorio.
- `carbohydrate`: valor decimal obrigatorio.
- `fat`: valor decimal obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- Deve existir no maximo uma meta padrao por usuario.
- A meta padrao deve pertencer ao usuario autenticado.
- Todos os valores de meta devem ser maiores ou iguais a zero.
- Ao alterar a meta padrao, a alteracao deve atualizar a meta do dia atual e de
  todos os dias futuros.
- A alteracao da meta padrao nao deve alterar metas de dias anteriores.
- Se ainda nao existir meta diaria para o dia atual ou para dias futuros, esses
  dias devem passar a usar a nova meta padrao quando consultados.

### DailyDietGoal

Representa a meta nutricional de um dia especifico.

Campos:

- `id`: UUID.
- `user`: obrigatorio.
- `goalDate`: date obrigatorio.
- `kcal`: valor decimal obrigatorio.
- `protein`: valor decimal obrigatorio.
- `carbohydrate`: valor decimal obrigatorio.
- `fat`: valor decimal obrigatorio.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- A meta diaria deve pertencer ao usuario autenticado.
- Deve existir no maximo uma meta diaria por usuario e `goalDate`.
- Todos os valores de meta devem ser maiores ou iguais a zero.
- A meta diaria pode variar por data.
- O usuario deve poder alterar a meta de uma data especifica sem alterar a meta
  padrao.
- Se uma data nao tiver meta diaria especifica, as metricas devem usar a meta
  padrao vigente do usuario.
- Se uma data nao tiver meta diaria especifica e o usuario tambem nao tiver meta
  padrao, as metricas devem retornar metas zeradas.

## Integracao Com DeepSeek

Adicionar uma integracao com DeepSeek para obter os dados nutricionais de um
alimento novo.

Regras:

- A chamada a DeepSeek deve acontecer no use case de criacao de entrada
  alimentar quando o alimento ainda nao existir para o usuario.
- A chave de API e configuracoes da DeepSeek devem ser lidas de variaveis de
  ambiente ou propriedades da aplicacao.
- Controllers nao devem conhecer detalhes da integracao com DeepSeek.
- Repositories nao devem conter regra de negocio de integracao.
- Se a DeepSeek falhar, o alimento nao deve ser salvo e a entrada nao deve ser
  criada.
- A resposta da API deve indicar erro de obtencao de dados nutricionais quando a
  integracao falhar.
- O alimento retornado pela DeepSeek deve ser salvo com `name` normalizado em
  maiusculo, preservando acentos e `ç`.
- A DeepSeek deve retornar valores nutricionais por `1g` e por porcao.
- A porcao deve ser retornada sempre pela DeepSeek.
- A porcao nao precisa conter equivalencia em gramas.
- A aplicacao deve exigir resposta estruturada em JSON da DeepSeek.

Formato esperado da resposta da DeepSeek:

```json
{
  "name": "OVO",
  "perGram": {
    "kcal": 1.43,
    "protein": 0.13,
    "carbohydrate": 0.01,
    "fat": 0.10
  },
  "portion": {
    "description": "1 ovo medio",
    "kcal": 68.00,
    "protein": 6.00,
    "carbohydrate": 0.50,
    "fat": 5.00
  }
}
```

## Metricas Diarias

Deve ser possivel visualizar metricas nutricionais por data.

Dados esperados:

- `date`;
- `goal`;
- `consumed`;
- `remaining`;
- `entries`.

Dados de `goal`:

- `kcal`;
- `protein`;
- `carbohydrate`;
- `fat`.

Dados de `consumed`:

- `kcal`;
- `protein`;
- `carbohydrate`;
- `fat`.

Dados de `remaining`:

- `kcal`;
- `protein`;
- `carbohydrate`;
- `fat`.

Regras:

- A data deve ser informada por query param.
- Se a data nao for informada, deve ser usada a data atual.
- `consumed` deve ser a soma das entradas da data.
- `goal` deve usar a meta diaria especifica da data quando ela existir.
- Se nao existir meta diaria especifica para a data, `goal` deve usar a meta
  padrao do usuario.
- Se nao existir meta diaria nem meta padrao, `goal` deve retornar valores
  zerados.
- `remaining` deve ser calculado como `goal - consumed`.
- Os valores de `remaining` podem ser negativos quando o usuario ultrapassar a
  meta.
- `entries` deve conter as entradas alimentares do dia.

## Endpoints

### Goals

- `GET /api/v1/diet/goals/default`
- `PUT /api/v1/diet/goals/default`
- `GET /api/v1/diet/goals/daily?date=2026-07-13`
- `PUT /api/v1/diet/goals/daily?date=2026-07-13`

Regras:

- A alteracao da meta padrao deve atualizar o dia atual e os proximos dias.
- A alteracao de meta diaria deve afetar apenas a data informada.

### Foods

- `GET /api/v1/diet/foods?name=ovo`
- `GET /api/v1/diet/foods/{foodId}`

Regras:

- A listagem por nome deve servir autocomplete/autoselect no frontend.
- O parametro `name` deve ser opcional.
- Se `name` nao for informado, devem ser retornados os alimentos do usuario.
- Se `name` for informado, devem ser retornados apenas alimentos cujo nome
  normalizado contenha o termo normalizado informado.
- A listagem deve ser ordenada por `name`.
- Nao deve existir endpoint para criar, editar ou remover alimentos manualmente.

### Entries

- `POST /api/v1/diet/entries`
- `GET /api/v1/diet/entries?date=2026-07-13`
- `GET /api/v1/diet/entries/{entryId}`
- `DELETE /api/v1/diet/entries/{entryId}`

### Metrics

- `GET /api/v1/diet/metrics?date=2026-07-13`

## Formatos De Request

### Atualizar Meta Padrao

```json
{
  "kcal": 2200.00,
  "protein": 160.00,
  "carbohydrate": 250.00,
  "fat": 70.00
}
```

### Atualizar Meta Diaria

```json
{
  "kcal": 1800.00,
  "protein": 150.00,
  "carbohydrate": 180.00,
  "fat": 60.00
}
```

### Criar Entrada Em Gramas

```json
{
  "foodName": "ovo",
  "quantity": 100.00,
  "unit": "GRAMS",
  "entryDate": "2026-07-13"
}
```

### Criar Entrada Em Porcoes

```json
{
  "foodName": "pao frances",
  "quantity": 2.00,
  "unit": "PORTIONS",
  "entryDate": "2026-07-13"
}
```

## Respostas Esperadas

### Alimento

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "name": "OVO",
  "kcalPerGram": 1.43,
  "proteinPerGram": 0.13,
  "carbohydratePerGram": 0.01,
  "fatPerGram": 0.10,
  "kcalPerPortion": 68.00,
  "proteinPerPortion": 6.00,
  "carbohydratePerPortion": 0.50,
  "fatPerPortion": 5.00,
  "portionDescription": "1 ovo medio",
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Entrada

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "food": {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "OVO",
    "portionDescription": "1 ovo medio"
  },
  "entryDate": "2026-07-13",
  "quantity": 2.00,
  "unit": "PORTIONS",
  "kcal": 136.00,
  "protein": 12.00,
  "carbohydrate": 1.00,
  "fat": 10.00,
  "createdAt": "2026-07-13T10:00:00",
  "updatedAt": "2026-07-13T10:00:00"
}
```

### Meta

```json
{
  "kcal": 2200.00,
  "protein": 160.00,
  "carbohydrate": 250.00,
  "fat": 70.00
}
```

### Metricas Diarias

```json
{
  "date": "2026-07-13",
  "goal": {
    "kcal": 2200.00,
    "protein": 160.00,
    "carbohydrate": 250.00,
    "fat": 70.00
  },
  "consumed": {
    "kcal": 136.00,
    "protein": 12.00,
    "carbohydrate": 1.00,
    "fat": 10.00
  },
  "remaining": {
    "kcal": 2064.00,
    "protein": 148.00,
    "carbohydrate": 249.00,
    "fat": 60.00
  },
  "entries": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "food": {
        "id": "00000000-0000-0000-0000-000000000000",
        "name": "OVO",
        "portionDescription": "1 ovo medio"
      },
      "entryDate": "2026-07-13",
      "quantity": 2.00,
      "unit": "PORTIONS",
      "kcal": 136.00,
      "protein": 12.00,
      "carbohydrate": 1.00,
      "fat": 10.00,
      "createdAt": "2026-07-13T10:00:00",
      "updatedAt": "2026-07-13T10:00:00"
    }
  ]
}
```

## Validacoes E Erros Esperados

- Retornar erro quando o usuario tentar acessar alimento, entrada ou meta de
  outro usuario.
- Retornar erro quando `foodName` nao for informado.
- Retornar erro quando `quantity` for menor ou igual a zero.
- Retornar erro quando `unit` nao for `GRAMS` ou `PORTIONS`.
- Retornar erro quando qualquer valor de meta for negativo.
- Retornar erro quando a DeepSeek falhar ao obter dados nutricionais de um
  alimento novo.
- Retornar erro quando a DeepSeek retornar resposta invalida ou incompleta.
- Nao chamar a DeepSeek quando o alimento ja existir para o usuario.
- Nao criar entrada quando a criacao automatica do alimento falhar.
- Nao permitir criacao, edicao ou remocao manual de alimentos.
