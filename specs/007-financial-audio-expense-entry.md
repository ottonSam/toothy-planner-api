# 007 - Financial Audio Expense Entry

## Objetivo

Adicionar uma rota propria para cadastro de entrada financeira via audio.

O fluxo deve ser:

1. API recebe audio codificado em Base64.
2. API envia o audio para um servico local de transcricao rodando via Docker.
3. Servico de transcricao retorna texto.
4. API envia o texto transcrito para o fluxo de classificacao ja existente com
   DeepSeek.
5. O gasto e criado diretamente como pontual, parcelado ou recorrente.
6. O audio e descartado apos o processamento.

Esta spec complementa a spec `006 - Financial Manager Refactor`.

## Regras Gerais

Todas as rotas devem iniciar com `/api/v1`.

Todas as mensagens de erro retornadas pela API devem ser em ingles.

A rota deve ser privada e acessivel somente com autenticacao via JWT por cookie.

O usuario dono da carteira deve ser obtido a partir da autenticacao.

Controllers e repositories nao devem conter regra de negocio. A regra de negocio
deve ficar nos use cases.

O audio nao deve ser persistido em banco, arquivo local, storage externo ou logs.

O texto transcrito pode ser usado apenas para criar o gasto e retornar a resposta
da rota.

Nao deve existir etapa de preview ou confirmacao antes da criacao do gasto.

Gastos criados via audio devem continuar editaveis posteriormente pelas rotas
manuais existentes, permitindo alterar categoria, valor, descricao e data quando
aplicavel.

## Formato De Audio

Para facilitar a integracao web, o formato preferencial deve ser:

- `audio/webm`
- codec Opus quando disponivel no navegador

A API deve receber o audio em Base64 dentro de JSON, nao por multipart.

O payload deve informar o `contentType` para que o servico de transcricao consiga
criar o arquivo temporario com a extensao correta.

Formatos aceitos inicialmente:

- `audio/webm`
- `audio/ogg`
- `audio/wav`
- `audio/mpeg`
- `audio/mp4`

Caso o formato nao seja suportado, a API deve retornar `400 Bad Request`.

## Endpoint

### POST /api/v1/financial-manager/wallets/{walletId}/expenses/audio

Cria um gasto a partir de audio.

Request:

```json
{
  "audioBase64": "AAAA...",
  "contentType": "audio/webm",
  "referenceDate": "2026-07-13"
}
```

Campos:

- `audioBase64`: obrigatorio. Conteudo do audio em Base64.
- `contentType`: obrigatorio. MIME type do audio.
- `referenceDate`: opcional. Data de referencia para o texto quando a fala nao
  informar data. Quando omitida, deve usar a data atual do backend.

Response:

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
    "category": {
      "key": "COMPRAS",
      "name": "Compras",
      "color": "#CA8A04",
      "icon": "shopping-bag"
    },
    "description": "Notebook",
    "totalAmount": null,
    "installmentAmount": 199.00,
    "installments": 12,
    "firstExpenseDate": "2026-07-13",
    "createdAt": "2026-07-13T09:00:00-03:00",
    "updatedAt": "2026-07-13T09:00:00-03:00"
  },
  "recurringExpense": null,
  "generatedExpenses": []
}
```

A resposta deve seguir o mesmo formato conceitual da criacao por texto, mas
incluindo `transcribedText`.

`classification.type` deve indicar o tipo identificado:

- `ONE_TIME`
- `INSTALLMENT`
- `RECURRING`

## Integracao Com Fluxo De Texto

Apos obter `transcribedText`, a API deve reutilizar a regra existente de criacao
por texto.

O texto transcrito deve ser enviado para a classificacao DeepSeek com a mesma
semantica de:

- identificar categoria fixa;
- identificar tipo de gasto;
- criar gasto pontual, parcelado ou recorrente;
- usar `referenceDate` quando a fala nao informar data;
- criar parcelas nos ciclos correspondentes;
- criar recorrencias conforme regras vigentes;
- marcar os gastos gerados com origem de IA.

Deve ser adicionada uma origem especifica para gastos criados por audio, caso a
implementacao opte por distinguir origem:

- `AI_AUDIO`

Caso a implementacao prefira manter apenas a origem atual, os gastos criados via
audio podem usar `AI_TEXT`, desde que a resposta da rota informe
`transcribedText`.

## Infraestrutura Docker

Deve existir um servico Docker para transcricao de audio.

Servico obrigatorio:

- container baseado em `faster-whisper`;
- expor endpoint HTTP interno para a API;
- aceitar audio em Base64 e `contentType`;
- retornar texto transcrito em JSON.

O servico deve usar o modelo `faster-whisper` para executar a transcricao dentro
da infraestrutura Docker da aplicacao, sem depender de chamadas externas para
transcricao.

O modelo usado pelo `faster-whisper` deve ser configuravel por variavel de
ambiente do container de transcricao.

Variaveis esperadas no servico de transcricao:

- `FASTER_WHISPER_MODEL`: modelo utilizado na transcricao.
- `FASTER_WHISPER_DEVICE`: dispositivo de execucao, por exemplo `cpu` ou `cuda`.
- `FASTER_WHISPER_COMPUTE_TYPE`: tipo de computacao, por exemplo `int8` para
  CPU.

Valores padrao sugeridos:

- `FASTER_WHISPER_MODEL=small`
- `FASTER_WHISPER_DEVICE=cpu`
- `FASTER_WHISPER_COMPUTE_TYPE=int8`

O servico deve ficar no `docker-compose.yml` junto com a API e o Postgres para
facilitar deploy na VPS.

O servico de transcricao nao deve ser exposto publicamente na VPS. A API deve se
comunicar com ele pela rede interna do Docker Compose.

Variaveis de ambiente esperadas na API:

- `TRANSCRIPTION_BASE_URL`: URL interna do servico de transcricao.
- `TRANSCRIPTION_TIMEOUT_SECONDS`: timeout da chamada HTTP.
- `TRANSCRIPTION_MAX_AUDIO_BYTES`: tamanho maximo permitido apos decodificar o
  Base64.

Valores padrao sugeridos:

- `TRANSCRIPTION_BASE_URL=http://audio-transcriber:8000`
- `TRANSCRIPTION_TIMEOUT_SECONDS=120`
- `TRANSCRIPTION_MAX_AUDIO_BYTES=10485760`

## Contrato Do Servico De Transcricao

### POST /transcribe

Request enviado pela API ao container de transcricao:

```json
{
  "audioBase64": "AAAA...",
  "contentType": "audio/webm"
}
```

Response esperada:

```json
{
  "text": "fui ao mercado e gastei 32 reais"
}
```

Se a transcricao falhar, o servico deve retornar erro HTTP e a API deve traduzir
isso para uma mensagem clara.

## Validacoes

Se `audioBase64` estiver ausente ou vazio, retornar:

- status: `400 Bad Request`
- message: `Audio content is required`

Se `audioBase64` nao for Base64 valido, retornar:

- status: `400 Bad Request`
- message: `Audio content must be valid Base64`

Se `contentType` estiver ausente ou vazio, retornar:

- status: `400 Bad Request`
- message: `Audio content type is required`

Se `contentType` nao for suportado, retornar:

- status: `400 Bad Request`
- message: `Audio content type is not supported`

Se o audio decodificado exceder o limite configurado, retornar:

- status: `400 Bad Request`
- message: `Audio content exceeds maximum size`

Se a transcricao retornar texto vazio, retornar:

- status: `502 Bad Gateway`
- message: `Audio transcription returned empty text`

Se o servico de transcricao nao responder dentro do timeout, retornar:

- status: `502 Bad Gateway`
- message: `Audio transcription timed out`

Se o servico de transcricao estiver indisponivel, retornar:

- status: `502 Bad Gateway`
- message: `Audio transcription service is unavailable`

Se a classificacao DeepSeek falhar apos a transcricao, deve retornar as mesmas
mensagens detalhadas ja existentes no fluxo de texto.

## Segurança E Privacidade

O audio nao deve ser salvo.

O Base64 do audio nao deve ser logado.

O texto transcrito nao deve ser logado em nivel `INFO` ou superior.

Erros podem logar apenas metadados seguros:

- usuario autenticado;
- wallet id;
- content type;
- tamanho do audio decodificado;
- causa tecnica sem conteudo do audio.

## Collection Bruno

Deve ser adicionada request na collection Bruno:

- `Financial Manager/Create Expense From Audio.bru`

O exemplo deve usar `audioBase64` pequeno ou placeholder documentado.

## Documentacao

O documento `docs/financial-manager-integration.md` deve ser atualizado com:

- nova rota de audio;
- payload Base64;
- formatos aceitos;
- resposta com `transcribedText`;
- erros esperados;
- observacao de que o audio e descartado.

## Cenarios De Teste

Devem ser criados testes cobrindo:

- cria gasto pontual por audio transcrito;
- cria compra parcelada por audio transcrito;
- cria gasto recorrente por audio transcrito;
- usa `referenceDate` quando a fala nao informa data;
- retorna erro quando o audio esta ausente;
- retorna erro quando o Base64 e invalido;
- retorna erro quando o `contentType` nao e suportado;
- retorna erro quando o audio excede o tamanho maximo;
- retorna erro quando a transcricao retorna texto vazio;
- retorna erro quando o servico de transcricao esta indisponivel;
- nao cria gasto quando a transcricao falha;
- nao cria gasto quando a classificacao DeepSeek falha;
- usuario nao consegue criar gasto em carteira de outro usuario;
- gastos criados por audio podem ser editados pelas rotas manuais existentes.
