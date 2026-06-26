# Diet - Guia de integracao

Documento de contrato para integracao com o modulo de dieta da Toothy Planner
API.

## Base e autenticacao

- Base local: `http://localhost:8080`
- Prefixo do modulo: `/api/v1/diet`
- Formato: `application/json`
- Datas: `YYYY-MM-DD`
- Datas com horario: ISO-8601 com offset
- IDs: UUID em string
- Autenticacao: cookie HTTP-only `access_token`

Todas as rotas exigem autenticacao. Em clientes web:

```ts
fetch(url, {
  credentials: "include"
});
```

Erros usam o formato:

```json
{
  "message": "Diet entry not found"
}
```

Status mais comuns:

- `400 Bad Request`: payload, enum, data ou valor invalido.
- `401 Unauthorized`: cookie ausente, expirado ou invalido.
- `404 Not Found`: alimento ou entrada inexistente ou de outro usuario.
- `502 Bad Gateway`: falha ao consultar ou interpretar a resposta da DeepSeek.

## Fluxo recomendado

1. Configure a meta nutricional padrao.
2. Consulte alimentos por nome para preencher o autocomplete.
3. Registre uma entrada usando o nome escolhido ou digitado.
4. Consulte as metricas do dia.
5. Edite ou remova entradas quando precisar corrigir quantidade ou unidade.
6. Use uma meta diaria quando uma data precisar fugir do padrao.

O cliente nao cria alimentos diretamente. O primeiro registro de um nome novo
faz a API consultar a DeepSeek e salvar os valores nutricionais.

## Enum DietEntryUnit

```json
["GRAMS", "PORTIONS"]
```

- `GRAMS`: multiplica a quantidade pelos valores nutricionais de `1g`.
- `PORTIONS`: multiplica a quantidade pelos valores de uma porcao media.

A porcao nao possui equivalencia obrigatoria em gramas.

## Normalizacao de alimentos

Antes da busca ou criacao, o nome:

- tem espacos externos removidos;
- tem sequencias de espacos reduzidas para um espaco;
- e convertido para maiusculas;
- preserva acentos e `ç`.

Exemplos:

```text
"  maçã  " -> "MAÇÃ"
"pão   francês" -> "PÃO FRANCÊS"
```

`MAÇÃ` e `maca` sao nomes diferentes. A busca parcial ignora maiusculas e
minusculas, mas respeita acentos e `ç`.

Os alimentos sao isolados por usuario. O mesmo nome pode possuir valores
independentes para usuarios diferentes.

## Respostas reutilizaveis

### DietGoalResponse

```json
{
  "kcal": 2200.00,
  "protein": 160.00,
  "carbohydrate": 250.00,
  "fat": 70.00
}
```

Todos os macros sao expressos em gramas.

### FoodResponse

```json
{
  "id": "949357e2-97dd-4dbf-b546-1751eb583537",
  "name": "MAÇÃ",
  "kcalPerGram": 0.5200,
  "proteinPerGram": 0.0030,
  "carbohydratePerGram": 0.1400,
  "fatPerGram": 0.0020,
  "kcalPerPortion": 95.0000,
  "proteinPerPortion": 0.5000,
  "carbohydratePerPortion": 25.0000,
  "fatPerPortion": 0.3000,
  "portionDescription": "1 unidade media",
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

### FoodSummaryResponse

Usada dentro de uma entrada:

```json
{
  "id": "949357e2-97dd-4dbf-b546-1751eb583537",
  "name": "MAÇÃ",
  "portionDescription": "1 unidade media"
}
```

### DietEntryResponse

```json
{
  "id": "93caee6f-ab4a-4d88-a236-425dd9951440",
  "food": {
    "id": "949357e2-97dd-4dbf-b546-1751eb583537",
    "name": "MAÇÃ",
    "portionDescription": "1 unidade media"
  },
  "entryDate": "2026-07-13",
  "quantity": 2.00,
  "unit": "PORTIONS",
  "kcal": 190.00,
  "protein": 1.00,
  "carbohydrate": 50.00,
  "fat": 0.60,
  "createdAt": "2026-07-13T09:00:00-03:00",
  "updatedAt": "2026-07-13T09:00:00-03:00"
}
```

Os valores calculados sao arredondados para duas casas com `HALF_UP` e
persistidos na entrada. Alteracoes futuras na fonte nutricional nao modificam
entradas antigas.

### DietMetricsResponse

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
    "kcal": 190.00,
    "protein": 1.00,
    "carbohydrate": 50.00,
    "fat": 0.60
  },
  "remaining": {
    "kcal": 2010.00,
    "protein": 159.00,
    "carbohydrate": 200.00,
    "fat": 69.40
  },
  "entries": [
    {
      "id": "93caee6f-ab4a-4d88-a236-425dd9951440",
      "food": {
        "id": "949357e2-97dd-4dbf-b546-1751eb583537",
        "name": "MAÇÃ",
        "portionDescription": "1 unidade media"
      },
      "entryDate": "2026-07-13",
      "quantity": 2.00,
      "unit": "PORTIONS",
      "kcal": 190.00,
      "protein": 1.00,
      "carbohydrate": 50.00,
      "fat": 0.60,
      "createdAt": "2026-07-13T09:00:00-03:00",
      "updatedAt": "2026-07-13T09:00:00-03:00"
    }
  ]
}
```

Calculos:

- `consumed`: soma das entradas da data.
- `goal`: meta diaria da data, ou meta padrao, ou valores zerados.
- `remaining`: `goal - consumed`.
- `remaining` pode ser negativo.

## Endpoints de metas

### GET /goals/default

Resposta: `200 OK` com `DietGoalResponse`.

Quando ainda nao existe meta padrao, retorna:

```json
{
  "kcal": 0.00,
  "protein": 0.00,
  "carbohydrate": 0.00,
  "fat": 0.00
}
```

### PUT /goals/default

Request:

```json
{
  "kcal": 2200.00,
  "protein": 160.00,
  "carbohydrate": 250.00,
  "fat": 70.00
}
```

Resposta: `200 OK` com `DietGoalResponse`.

Regras:

- Todos os campos sao obrigatorios.
- Todos os valores devem ser maiores ou iguais a zero.
- A alteracao atualiza a meta do dia atual.
- Metas diarias ja persistidas para hoje ou datas futuras sao sobrescritas.
- Metas de dias anteriores sao preservadas.
- Datas futuras sem meta especifica passam a usar a nova meta padrao.

### GET /goals/daily?date=2026-07-13

O query param `date` e opcional. Sem ele, a API usa a data atual.

Resposta: `200 OK` com `DietGoalResponse`.

Resolucao da meta:

1. Meta diaria da data.
2. Meta padrao do usuario.
3. Valores zerados.

### PUT /goals/daily?date=2026-07-13

Usa o mesmo payload da meta padrao.

Resposta: `200 OK` com `DietGoalResponse`.

O query param `date` e opcional e usa a data atual quando omitido. A operacao
altera somente a meta da data informada.

## Endpoints de alimentos

### GET /foods

Lista todos os alimentos do usuario em ordem alfabetica.

Resposta: `200 OK` com `FoodResponse[]`.

### GET /foods?name=maç

Filtra alimentos cujo nome normalizado contenha o termo informado. Essa rota
deve ser usada pelo autocomplete/autoselect.

Resposta: `200 OK` com `FoodResponse[]`.

Exemplo de uso:

```ts
const query = encodeURIComponent("maç");
const response = await fetch(`/api/v1/diet/foods?name=${query}`, {
  credentials: "include"
});
const foods = await response.json();
```

O parametro deve ser enviado com URL encoding quando possuir espacos, acentos
ou `ç`.

### GET /foods/{foodId}

Resposta: `200 OK` com `FoodResponse`.

Nao existem endpoints para criar, editar ou remover alimentos manualmente.

## Endpoints de entradas

### POST /entries

Entrada em gramas:

```json
{
  "foodName": "Maçã",
  "quantity": 100.00,
  "unit": "GRAMS",
  "entryDate": "2026-07-13"
}
```

Entrada em porcoes:

```json
{
  "foodName": "Pão francês",
  "quantity": 2.00,
  "unit": "PORTIONS",
  "entryDate": "2026-07-13"
}
```

Resposta: `201 Created` com `DietEntryResponse`.

Regras:

- `foodName` e obrigatorio.
- `quantity` deve ser maior que zero.
- `unit` deve ser `GRAMS` ou `PORTIONS`.
- `entryDate` e opcional; sem ele, usa a data atual.
- Se o alimento existir para o usuario, seus dados salvos sao reutilizados.
- Se nao existir, a API consulta a DeepSeek antes de criar a entrada.
- Se a consulta falhar, nem o alimento nem a entrada sao criados.

O cliente deve enviar o nome, e nao o `foodId`, mesmo quando seleciona um item do
autocomplete.

### GET /entries

Lista entradas da data atual.

Resposta: `200 OK` com `DietEntryResponse[]`.

### GET /entries?date=2026-07-13

Lista entradas da data informada, ordenadas por criacao.

Resposta: `200 OK` com `DietEntryResponse[]`.

### GET /entries/{entryId}

Resposta: `200 OK` com `DietEntryResponse`.

### PUT /entries/{entryId}

Edita a quantidade e a unidade de uma entrada existente.

Request:

```json
{
  "quantity": 150.00,
  "unit": "GRAMS"
}
```

Resposta: `200 OK` com `DietEntryResponse`.

Regras:

- A entrada deve pertencer ao usuario autenticado.
- `quantity` deve ser maior que zero.
- `unit` deve ser `GRAMS` ou `PORTIONS`.
- O alimento e a data da entrada nao sao alterados.
- Os valores de `kcal`, `protein`, `carbohydrate` e `fat` sao recalculados com
  base nos dados nutricionais ja salvos do alimento.
- A edicao nao consulta a DeepSeek e nao cria alimento novo.

### DELETE /entries/{entryId}

Resposta: `204 No Content`.

## Endpoint de metricas

### GET /metrics?date=2026-07-13

O query param `date` e opcional. Sem ele, usa a data atual.

Resposta: `200 OK` com `DietMetricsResponse`.

Uma data sem entradas retorna `consumed` zerado e `entries: []`. A meta continua
seguindo a resolucao diaria, padrao ou zerada.

## Integracao com DeepSeek

A integracao e interna ao backend. O frontend nao envia chave, modelo ou dados
nutricionais.

A consulta acontece somente quando o usuario registra um nome que ainda nao
existe em sua base. A API solicita:

- kcal, proteina, carboidrato e gordura por `1g`;
- descricao e valores nutricionais de uma porcao comum.

Falhas retornam `502 Bad Gateway`. Mensagens possiveis:

```text
DeepSeek API key is not configured
DeepSeek authentication failed
DeepSeek nutrition lookup failed
DeepSeek nutrition response is invalid
```

Depois que o alimento e salvo, entradas futuras com o mesmo nome normalizado nao
fazem nova consulta.

## Erros relevantes

```text
Food name is required
Food not found
Diet entry not found
Diet entry quantity must be greater than zero
Diet entry unit is required
Diet goal kcal must be greater than or equal to zero
Diet goal protein must be greater than or equal to zero
Diet goal carbohydrate must be greater than or equal to zero
Diet goal fat must be greater than or equal to zero
Invalid request body
Invalid request parameter
```
