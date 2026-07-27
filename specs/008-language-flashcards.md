# 008 - Language Flashcards

## Objetivo

Criar um modulo de flashcards para estudo de linguas.

O modulo deve permitir que o usuario crie decks proprios, gere cards com apoio
do DeepSeek e estude usando uma recomendacao baseada em dificuldade, erros,
revisao vencida e tempo desde a ultima visualizacao do card.

## Regras Gerais

Todas as rotas devem iniciar com `/api/v1`.

Todas as mensagens de erro retornadas pela API devem ser em ingles.

Todas as rotas do modulo devem ser privadas e acessiveis somente com
autenticacao via JWT por cookie.

O usuario dono dos decks, tags, cards, jobs e respostas de revisao deve ser
obtido a partir da autenticacao.

Controllers e repositories nao devem conter regra de negocio. A regra de
negocio deve ficar nos use cases.

Devem ser criados DTOs para separar entrada e saida de dados das entidades de
dominio.

As validacoes dos campos devem ser feitas na camada de entidade quando forem
validacoes do dominio, informando mensagens em ingles.

Decks e tags devem ser especificos por usuario.

Cards devem pertencer a um deck e ao usuario dono do deck.

O modulo deve utilizar a integracao existente com DeepSeek para geracao dos
cards.

## Tipos De Deck

Devem existir os seguintes tipos de deck:

- `VOCABULARY`
- `IRREGULAR_VERBS`
- `EXPRESSIONS`

## Decks

Um deck representa uma colecao de flashcards de um usuario.

Campos sugeridos:

- `id`
- `userId`
- `name`
- `context`
- `targetLanguage`
- `baseLanguage`
- `type`
- `createdAt`
- `updatedAt`

Regras:

- `name` e obrigatorio.
- `context` e obrigatorio para decks gerados por IA.
- `targetLanguage` e obrigatorio.
- `baseLanguage` e obrigatorio.
- `type` e obrigatorio.
- O deck deve ser criado automaticamente ao iniciar uma geracao por IA.
- O deck deve ficar editavel depois de criado.
- O usuario nao pode acessar decks de outro usuario.

## Tags

Tags servem para organizar cards dentro dos decks.

Campos sugeridos:

- `id`
- `userId`
- `name`
- `createdAt`
- `updatedAt`

Regras:

- Tags devem ser especificas por usuario.
- A IA pode sugerir tags para os cards gerados.
- Tags sugeridas pela IA devem ser criadas automaticamente para o usuario quando
  ainda nao existirem.
- Tags devem ser reutilizadas quando ja existirem para o usuario.
- `name` deve ser unico por usuario, ignorando maiusculas e minusculas.
- O usuario nao pode acessar tags de outro usuario.

## Cards

Todo card deve possuir campos comuns:

- `id`
- `userId`
- `deckId`
- `type`
- `tags`
- `examples`
- `usageNote`
- `active`
- `lastSeenAt`
- `lastReviewedAt`
- `nextReviewAt`
- `reviewCount`
- `correctCount`
- `wrongCount`
- `consecutiveCorrect`
- `consecutiveWrong`
- `difficulty`
- `createdAt`
- `updatedAt`

Regras:

- `lastSeenAt` deve ser atualizado quando o card for entregue em uma sessao de
  revisao.
- `lastReviewedAt` deve ser atualizado quando o usuario responder ao card.
- `nextReviewAt` deve ser usado para identificar cards vencidos para revisao.
- `difficulty` deve aumentar quando o usuario errar ou marcar dificuldade.
- `difficulty` deve diminuir gradualmente quando o usuario acertar.
- Cards inativos nao devem aparecer no fluxo de revisao.
- O usuario nao pode acessar cards de outro usuario.

## Exemplos Dos Cards

Todo card gerado por IA deve trazer exemplos.

Campos sugeridos para exemplo:

- `text`
- `translation`

Regras:

- Cada card deve ter pelo menos 1 exemplo.
- Cada exemplo deve ter traducao no idioma base.
- Para palavras, verbos ou expressoes com uso ambiguo, a IA deve retornar 2 ou
  3 exemplos contrastivos quando fizer sentido.
- Exemplos devem ser naturais no idioma alvo.
- Exemplos nao devem ser artificiais apenas para encaixar o termo.
- A resposta revelada ao usuario deve exibir a traducao principal e os exemplos
  traduzidos.

Exemplo para palavra:

```json
{
  "word": "stop",
  "translation": "parar",
  "examples": [
    {
      "text": "I stopped smoking last year.",
      "translation": "Eu parei de fumar no ano passado."
    },
    {
      "text": "I stopped to smoke outside.",
      "translation": "Eu parei para fumar la fora."
    }
  ],
  "usageNote": "After stop, the gerund means ending an action; the infinitive means pausing in order to do something."
}
```

## Cards De Vocabulario

Cards do tipo `VOCABULARY` devem representar uma palavra ou termo.

Campos especificos:

- `word`
- `translation`
- `phonetic`
- `level`

Regras:

- `word` e obrigatorio.
- `translation` e obrigatoria.
- `phonetic` e opcional.
- `level` e opcional.
- A deduplicacao deve usar `word` como termo alvo.

## Cards De Verbos Irregulares

Cards do tipo `IRREGULAR_VERBS` devem representar verbos irregulares.

Campos especificos:

- `baseVerb`
- `pastSimple`
- `pastParticiple`
- `translation`

Regras:

- `baseVerb` e obrigatorio.
- `pastSimple` e obrigatorio.
- `pastParticiple` e obrigatorio.
- `translation` e obrigatoria.
- A deduplicacao deve usar `baseVerb` como termo alvo.
- O verso do card deve exibir passado simples, participio passado, traducao e
  exemplos traduzidos.

Exemplo:

```json
{
  "baseVerb": "go",
  "pastSimple": "went",
  "pastParticiple": "gone",
  "translation": "ir",
  "examples": [
    {
      "text": "I went to the airport yesterday.",
      "translation": "Eu fui ao aeroporto ontem."
    },
    {
      "text": "She has gone home.",
      "translation": "Ela foi para casa."
    }
  ]
}
```

## Cards De Expressoes

Cards do tipo `EXPRESSIONS` devem representar trechos comuns no idioma alvo.

Campos especificos:

- `expression`
- `translation`

Regras:

- `expression` e obrigatoria.
- `translation` e obrigatoria.
- A deduplicacao deve usar `expression` como termo alvo.
- A frente do card deve exibir a expressao.
- O verso do card deve exibir a traducao e exemplos traduzidos.
- Expressoes geradas pela IA devem ser naturais, comuns e uteis.
- A IA nao deve gerar traducoes literais artificiais como expressoes.

Exemplo:

```json
{
  "expression": "How are you?",
  "translation": "Como voce esta?",
  "examples": [
    {
      "text": "Hi, John. How are you?",
      "translation": "Oi, John. Como voce esta?"
    }
  ],
  "usageNote": "Common greeting used in casual and professional contexts."
}
```

## Geracao Assincrona Por IA

A criacao de decks por IA deve ser assincrona.

O usuario deve enviar:

- `name`
- `context`
- `targetLanguage`
- `baseLanguage`
- `type`
- `cardCount`

Exemplo:

```json
{
  "name": "Viagem",
  "context": "viagem",
  "targetLanguage": "en",
  "baseLanguage": "pt-BR",
  "type": "VOCABULARY",
  "cardCount": 200
}
```

Fluxo:

1. API cria o deck do usuario.
2. API cria um job principal com status `PENDING`.
3. API divide a geracao em lotes de no maximo 100 cards.
4. Cada lote deve ser processado como um batch do job.
5. O processamento deve chamar DeepSeek em cada batch.
6. A API deve validar a resposta do DeepSeek.
7. A API deve criar cards, tags e associacoes validas.
8. O job deve atualizar progresso conforme os batches avancam.

O endpoint de criacao da geracao deve retornar `202 Accepted`.

## Jobs E Batches

Deve existir um job principal de geracao.

Campos sugeridos do job:

- `id`
- `userId`
- `deckId`
- `type`
- `context`
- `targetLanguage`
- `baseLanguage`
- `requestedCount`
- `createdCount`
- `status`
- `errorMessage`
- `createdAt`
- `startedAt`
- `finishedAt`
- `updatedAt`

Statuses do job:

- `PENDING`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELED`
- `PARTIAL_COMPLETED`

Cada job deve ser dividido em batches.

Campos sugeridos do batch:

- `id`
- `jobId`
- `batchNumber`
- `requestedCount`
- `createdCount`
- `status`
- `errorMessage`
- `createdAt`
- `startedAt`
- `finishedAt`
- `updatedAt`

Regras:

- Cada batch deve ter no maximo 100 cards solicitados.
- Para `cardCount = 250`, devem ser criados 3 batches: 100, 100 e 50.
- O job deve ser `COMPLETED` quando todos os batches forem concluidos sem erro e
  a quantidade criada for igual a quantidade solicitada.
- O job deve ser `PARTIAL_COMPLETED` quando finalizar com menos cards criados do
  que o solicitado, mas com pelo menos 1 card criado.
- O job deve ser `FAILED` quando nenhum card puder ser criado ou quando ocorrer
  erro irrecuperavel.
- Deve ser possivel consultar o status do job.
- O usuario nao pode acessar jobs ou batches de outro usuario.

## Deduplicacao Na Geracao

A geracao por IA nao deve criar cards repetidos no mesmo deck.

Regras:

- Em todo batch, a API deve enviar ao DeepSeek a lista completa de todos os
  termos alvo ja criados no deck.
- Para `VOCABULARY`, a lista deve conter todas as palavras ja criadas.
- Para `IRREGULAR_VERBS`, a lista deve conter todos os verbos base ja criados.
- Para `EXPRESSIONS`, a lista deve conter todas as expressoes ja criadas.
- Essa lista deve ser enviada como `alreadyGeneratedTerms`.
- A regra de nao repetir termos de `alreadyGeneratedTerms` deve estar explicita
  no system prompt.
- O backend deve validar duplicidade mesmo quando a IA receber a lista completa.
- Se a IA retornar termos duplicados, a API deve descartar os duplicados.
- Se um batch criar menos cards do que solicitado por causa de duplicidade, a
  API deve tentar completar o saldo restante com nova chamada ao DeepSeek.
- A retentativa deve incluir em `alreadyGeneratedTerms` todos os termos criados
  antes do batch e todos os termos validos criados nas tentativas anteriores.
- Deve haver limite configuravel de retentativas por batch.

## System Prompt Do DeepSeek

O system prompt para geracao de flashcards deve exigir JSON valido e estruturado.

O system prompt deve conter explicitamente a regra:

```text
You are generating flashcards for a language-learning deck.

Never generate a card whose target term is present in alreadyGeneratedTerms.
The alreadyGeneratedTerms list contains every target term already created for this deck across all previous batches.
This rule is mandatory even if the term is highly relevant to the requested context.
All returned target terms must be unique within the response and must not appear in alreadyGeneratedTerms.
```

Outras regras obrigatorias do prompt:

- Retornar somente JSON valido, sem markdown.
- Respeitar `targetLanguage` e `baseLanguage`.
- Gerar exatamente `requestedCount` cards unicos quando possivel.
- Gerar cards relacionados ao `context`.
- Gerar tags uteis para cada card.
- Gerar pelo menos 1 exemplo traduzido para cada card.
- Gerar 2 ou 3 exemplos quando houver ambiguidade relevante.
- Nao repetir termos dentro da mesma resposta.
- Nao inventar campos fora do contrato esperado.

## Contrato De Resposta Da IA

Resposta esperada para `VOCABULARY`:

```json
{
  "cards": [
    {
      "word": "airport",
      "translation": "aeroporto",
      "phonetic": null,
      "level": "A1",
      "tags": ["viagem", "aeroporto"],
      "examples": [
        {
          "text": "The airport is very busy today.",
          "translation": "O aeroporto esta muito cheio hoje."
        }
      ],
      "usageNote": null
    }
  ]
}
```

Resposta esperada para `IRREGULAR_VERBS`:

```json
{
  "cards": [
    {
      "baseVerb": "go",
      "pastSimple": "went",
      "pastParticiple": "gone",
      "translation": "ir",
      "tags": ["irregular-verbs"],
      "examples": [
        {
          "text": "I went to the airport yesterday.",
          "translation": "Eu fui ao aeroporto ontem."
        }
      ],
      "usageNote": null
    }
  ]
}
```

Resposta esperada para `EXPRESSIONS`:

```json
{
  "cards": [
    {
      "expression": "How are you?",
      "translation": "Como voce esta?",
      "tags": ["greetings"],
      "examples": [
        {
          "text": "Hi, Ana. How are you?",
          "translation": "Oi, Ana. Como voce esta?"
        }
      ],
      "usageNote": "Common greeting."
    }
  ]
}
```

## Proximo Card Para Revisao

A revisao deve funcionar de forma incremental.

O frontend deve solicitar o proximo card a ser revisado, a API deve calcular o
score dos cards disponiveis e sortear um card usando o score como peso.

Nao deve ser usado `GET` para selecionar o proximo card quando isso atualizar
`lastSeenAt`.

Endpoint conceitual:

### POST /api/v1/flashcards/review/next-card

Request:

```json
{
  "deckId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8"
}
```

Campos:

- `deckId`: opcional. Quando omitido, a API deve selecionar o proximo card entre
  todos os decks do usuario.

Response:

```json
{
  "card": {
    "id": "93288efd-a6d5-4a25-ac6c-c115551ffe8c",
    "deckId": "26697c4f-3eef-4821-a6d5-a1d09caa5ff8",
    "type": "VOCABULARY",
    "word": "airport",
    "translation": "aeroporto",
    "examples": [
      {
        "text": "The airport is very busy today.",
        "translation": "O aeroporto esta muito cheio hoje."
      }
    ],
    "usageNote": null
  },
  "score": 42.5
}
```

Regras:

- A API deve selecionar apenas cards ativos do usuario.
- A API deve calcular o score dos cards candidatos.
- A API deve sortear o card proporcionalmente ao score de cada candidato.
- Cards com score menor devem continuar tendo chance de ser selecionados.
- A API deve atualizar `lastSeenAt` do card retornado.
- Cards nunca vistos devem receber score alto.
- Cards vistos recentemente devem receber penalidade no score para evitar
  repeticao imediata.
- Quando `deckId` for informado, o deck deve pertencer ao usuario autenticado.
- Quando nao houver card disponivel, a API deve retornar resposta clara com
  status adequado.

## Registro De Respostas

O usuario deve registrar o resultado de revisao de cada card.

Ratings:

- `AGAIN`: errou.
- `HARD`: acertou com dificuldade.
- `GOOD`: acertou.
- `EASY`: acertou com facilidade.

Deve existir uma rota para listar os ratings possiveis, permitindo que o
frontend use somente valores validos do enum.

Endpoint conceitual:

### GET /api/v1/flashcards/review-ratings

Response:

```json
[
  {
    "key": "AGAIN",
    "name": "Again",
    "description": "The answer was wrong"
  },
  {
    "key": "HARD",
    "name": "Hard",
    "description": "The answer was correct but difficult"
  },
  {
    "key": "GOOD",
    "name": "Good",
    "description": "The answer was correct"
  },
  {
    "key": "EASY",
    "name": "Easy",
    "description": "The answer was correct and easy"
  }
]
```

Endpoint conceitual:

### POST /api/v1/flashcards/cards/{cardId}/answer

Request:

```json
{
  "rating": "AGAIN"
}
```

Regras:

- A resposta deve ser registrada diretamente para um card do usuario.
- O card deve pertencer ao usuario autenticado.
- Ao responder, a API deve atualizar `lastReviewedAt`.
- Ao responder, a API deve atualizar `reviewCount`.
- `AGAIN` deve incrementar `wrongCount` e `consecutiveWrong`.
- `HARD`, `GOOD` e `EASY` devem incrementar `correctCount` e
  `consecutiveCorrect`.
- Erro deve resetar `consecutiveCorrect`.
- Acerto deve resetar `consecutiveWrong`.
- A API deve recalcular `difficulty`.
- A API deve recalcular `nextReviewAt`.

## Algoritmo De Recomendacao

A recomendacao deve priorizar:

1. Cards vencidos para revisao.
2. Cards com mais erros.
3. Cards com maior dificuldade.
4. Cards com erros consecutivos.
5. Cards nunca vistos.
6. Cards ha mais tempo sem visualizacao.
7. Variedade suficiente para nao repetir somente os mesmos cards.

O score deve considerar o tempo desde `lastSeenAt`.

Modelo inicial sugerido:

```text
score =
  overdueScore
  + wrongCount * 3
  + consecutiveWrong * 5
  + difficulty * 2
  + daysSinceLastSeen * 1.5
  + neverSeenBonus
  - consecutiveCorrect * 2
  - recentlySeenPenalty
```

Regras:

- Cards nunca vistos devem receber bonus.
- Cards vistos ha menos de alguns minutos devem receber penalidade forte.
- Cards vistos no mesmo dia devem receber penalidade menor.
- O bonus por dias desde `lastSeenAt` deve ter teto configuravel.
- O score minimo usado como peso deve ser `1`.
- A proxima carta deve ser sorteada aleatoriamente usando o score como peso.
- A probabilidade de uma carta deve ser o score da carta dividido pela soma dos
  scores de todas as cartas candidatas.
- Uma carta com score menor deve continuar tendo chance de ser selecionada.
- O sorteio ponderado deve permitir uma fonte de aleatoriedade controlada nos
  testes.

## Metricas

Devem existir metricas do modulo.

Metricas sugeridas:

- total de decks.
- total de cards ativos.
- cards revisados no dia.
- cards revisados na semana.
- total de acertos.
- total de erros.
- taxa de acerto.
- cards vencidos para revisao.
- cards nunca vistos.
- tempo desde a ultima revisao.
- distribuicao por deck.
- distribuicao por tag.

## Endpoints Conceituais

Rotas sugeridas:

- `POST /api/v1/flashcards/decks/generate`
- `GET /api/v1/flashcards/generation-jobs/{jobId}`
- `GET /api/v1/flashcards/decks`
- `GET /api/v1/flashcards/decks/{deckId}`
- `PUT /api/v1/flashcards/decks/{deckId}`
- `DELETE /api/v1/flashcards/decks/{deckId}`
- `GET /api/v1/flashcards/decks/{deckId}/cards`
- `GET /api/v1/flashcards/cards/{cardId}`
- `PUT /api/v1/flashcards/cards/{cardId}`
- `DELETE /api/v1/flashcards/cards/{cardId}`
- `GET /api/v1/flashcards/tags`
- `GET /api/v1/flashcards/review-ratings`
- `POST /api/v1/flashcards/review/next-card`
- `POST /api/v1/flashcards/cards/{cardId}/answer`
- `GET /api/v1/flashcards/metrics`

## Validacoes

Validacoes sugeridas:

- `name` do deck e obrigatorio.
- `context` e obrigatorio na geracao por IA.
- `targetLanguage` e obrigatorio.
- `baseLanguage` e obrigatorio.
- `type` e obrigatorio.
- `type` deve ser valido.
- `cardCount` deve ser maior que zero.
- `cardCount` deve respeitar limite maximo configuravel.
- `rating` deve ser valido.
- A resposta da IA deve conter JSON valido.
- A resposta da IA deve conter `cards`.
- Cada card retornado pela IA deve respeitar os campos obrigatorios do tipo.
- Cada card retornado pela IA deve ter pelo menos 1 exemplo traduzido.

## Cenarios De Teste

Devem ser criados testes cobrindo:

- cria job de geracao e deck do usuario.
- divide `cardCount` em batches de no maximo 100.
- para `cardCount = 250`, cria batches 100, 100 e 50.
- processa batch de vocabulario com cards validos.
- processa batch de verbos irregulares com passado simples e participio.
- processa batch de expressoes.
- cria tags sugeridas pela IA.
- reutiliza tags ja existentes do usuario.
- envia `alreadyGeneratedTerms` completo para o DeepSeek em cada batch.
- system prompt contem a regra obrigatoria para nao repetir termos.
- descarta card duplicado retornado pela IA.
- retenta completar saldo restante quando houver duplicidade.
- marca job como `COMPLETED` quando cria todos os cards solicitados.
- marca job como `PARTIAL_COMPLETED` quando cria apenas parte dos cards.
- marca job como `FAILED` quando a IA falha sem criar cards.
- usuario nao acessa deck de outro usuario.
- usuario nao acessa job de outro usuario.
- usuario nao acessa card de outro usuario.
- retorna proximo card por sorteio ponderado pelo score.
- retorna proximo card filtrando por deck quando `deckId` for informado.
- retorna proximo card considerando todos os decks quando `deckId` for omitido.
- atualiza `lastSeenAt` ao entregar o proximo card.
- cards nunca vistos recebem score alto.
- lista ratings possiveis para resposta de revisao.
- score prioriza cards vencidos.
- score considera `wrongCount`, `difficulty`, `consecutiveWrong` e tempo desde
  `lastSeenAt`.
- score penaliza cards vistos recentemente.
- sorteio permite selecionar cards com score menor.
- apenas o card sorteado tem `lastSeenAt` atualizado.
- registra resposta `AGAIN`.
- registra resposta `HARD`.
- registra resposta `GOOD`.
- registra resposta `EASY`.
- atualiza `lastReviewedAt`, contadores, dificuldade e `nextReviewAt`.
- lista metricas do modulo.
