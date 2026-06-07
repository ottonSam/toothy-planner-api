# 002 - Goal Calendar Management

## Objetivo

Criar o modulo de gerenciamento de goals, calendars e activities para organizar
objetivos do usuario autenticado, planejar calendarios por semanas e registrar
progresso de atividades.

## Regras Gerais

Todas as rotas devem iniciar com `/api/v1`.

Todas as rotas deste modulo devem ser privadas e acessiveis somente com
autenticacao via JWT por cookie.

Todas as entidades devem pertencer ao usuario autenticado direta ou
indiretamente.

O usuario dono nao deve ser enviado no payload de criacao ou edicao. O usuario
deve sempre ser obtido a partir da autenticacao.

Um usuario so pode listar, criar, visualizar, editar, deletar ou registrar
progresso em recursos que pertencem a ele.

Todas as mensagens de erro retornadas pela API devem ser em ingles.

Todas as rotas deste modulo devem ter requests correspondentes na collection
Bruno versionada em `bruno/`, incluindo metodo, caminho, cookies, payloads e
exemplos de resposta esperados para testes manuais da API.

## Entidades

### Goal

Campos:

- `id`: UUID.
- `description`: string obrigatoria com minimo de 3 caracteres.
- `type`: enum obrigatorio.
- `isComplete`: booleano com padrao `false`.
- `user`: obrigatorio. Um usuario pode ter diversos goals.

Tipos de goal:

- `LONG_TERM`;
- `MEDIUM_TERM`;
- `CALENDAR`.

Regras:

- `user` nao deve ser recebido na criacao ou edicao.
- `user` deve ser definido pelo usuario autenticado.
- Um usuario so pode acessar goals em que `goal.user` seja igual ao usuario
  autenticado.

### Calendar

Campos:

- `id`: UUID.
- `description`: string obrigatoria com minimo de 3 caracteres.
- `weeks`: inteiro obrigatorio maior que 0 e menor que 53.
- `starts`: date obrigatorio.
- `goals`: relacao N:N opcional com goals.
- `user`: obrigatorio. Um usuario pode ter diversos calendars.

Regras:

- `user` nao deve ser recebido na criacao ou edicao.
- `user` deve ser definido pelo usuario autenticado.
- Calendar pode ser criado sem goals.
- Na criacao ou edicao, o cliente pode enviar `goalIds`.
- Todos os `goalIds` informados devem pertencer ao usuario autenticado.
- Um usuario so pode acessar calendars em que `calendar.user` seja igual ao
  usuario autenticado.

### Activity

Campos:

- `id`: UUID.
- `description`: string obrigatoria com minimo de 3 caracteres.
- `week`: inteiro obrigatorio.
- `calendar`: obrigatorio. Um calendar pode ter diversas activities.
- `type`: enum obrigatorio.
- `progressDays`: lista de enums de dias da semana.
- `progressCount`: inteiro.
- `progressTime`: inteiro em minutos.
- `progress`: valor calculado e nao salvo diretamente na tabela `activities`.
- `goal`: inteiro obrigatorio.

Tipos de activity:

- `DAYS`;
- `COUNT`;
- `TIME`.

Dias da semana:

- `SUNDAY`;
- `MONDAY`;
- `TUESDAY`;
- `WEDNESDAY`;
- `THURSDAY`;
- `FRIDAY`;
- `SATURDAY`.

Regras:

- `calendar` deve pertencer ao usuario autenticado.
- Um usuario so pode acessar activities cujo calendar pertence ao usuario
  autenticado.
- `week` deve ser menor ou igual ao numero de semanas do calendar.
- `goal` e obrigatorio para todos os tipos.
- Para `DAYS`, `goal` representa a quantidade de dias.
- Para `COUNT`, `goal` representa a quantidade numerica esperada.
- Para `TIME`, `goal` representa o total de minutos esperado.
- Em criacao e edicao de activity do tipo `TIME`, o payload deve receber `goal`
  como texto de tempo e converter para minutos.
- O texto de tempo deve aceitar horas e minutos em qualquer ordem, exemplos:
  `3h 20m`, `2m 4h`, `45m`, `2h`.
- Criacao e edicao de activity nao devem atualizar progresso.
- Registros de progresso devem ser salvos em estrutura propria.
- O campo `progress` retornado pela API deve ser calculado a partir dos registros
  de progresso e nao deve ser salvo diretamente na tabela `activities`.

## Endpoints de Goals

### POST /api/v1/goals

Cria um goal para o usuario autenticado.

Payload:

- `name`;
- `type`;
- `isComplete` opcional.

Regras:

- `name` deve ter minimo de 3 caracteres.
- `type` deve ser um dos tipos validos.
- `user` deve ser definido pelo usuario autenticado.
- `isComplete`, quando ausente, deve assumir `false`.

### GET /api/v1/goals

Lista os goals do usuario autenticado.

Regras:

- Deve retornar apenas goals em que `goal.user` seja igual ao usuario
  autenticado.

### GET /api/v1/goals/{id}

Busca um goal por id.

Regras:

- Deve retornar apenas se o goal pertencer ao usuario autenticado.

### PUT /api/v1/goals/{id}

Atualiza um goal.

Payload:

- `name`;
- `type`;
- `isComplete`.

Regras:

- Deve permitir editar apenas goals do usuario autenticado.
- `user` nao pode ser alterado.

### DELETE /api/v1/goals/{id}

Remove um goal.

Regras:

- Deve permitir deletar apenas goals do usuario autenticado.

### GET /api/v1/goals/types

Lista os tipos de goal.

Resposta esperada:

```json
[
  {
    "label": "Longo prazo",
    "value": "LONG_TERM"
  },
  {
    "label": "Medio prazo",
    "value": "MEDIUM_TERM"
  },
  {
    "label": "Calendário",
    "value": "CALENDAR"
  }
]
```

## Endpoints de Calendars

### POST /api/v1/calendars

Cria um calendar para o usuario autenticado.

Payload:

- `description`;
- `weeks`;
- `starts`;
- `goalIds` opcional.

Regras:

- `description` deve ter minimo de 3 caracteres.
- `weeks` deve ser maior que 0 e menor que 53.
- `starts` deve ser uma data valida.
- `goalIds`, quando informado, deve conter apenas goals do usuario autenticado.
- `user` deve ser definido pelo usuario autenticado.

### GET /api/v1/calendars

Lista os calendars do usuario autenticado.

Regras:

- Deve retornar apenas calendars em que `calendar.user` seja igual ao usuario
  autenticado.

### GET /api/v1/calendars/{id}

Busca um calendar por id.

Regras:

- Deve retornar apenas se o calendar pertencer ao usuario autenticado.

### PUT /api/v1/calendars/{id}

Atualiza um calendar.

Payload:

- `description`;
- `weeks`;
- `starts`;
- `goalIds` opcional.

Regras:

- Deve permitir editar apenas calendars do usuario autenticado.
- `goalIds`, quando informado, deve conter apenas goals do usuario autenticado.
- `user` nao pode ser alterado.

### DELETE /api/v1/calendars/{id}

Remove um calendar.

Regras:

- Deve permitir deletar apenas calendars do usuario autenticado.

## Endpoints de Activities

### POST /api/v1/activities

Cria uma activity.

Payload:

- `calendarId`;
- `description`;
- `week`;
- `type`;
- `goal`.

Regras:

- `calendarId` deve pertencer ao usuario autenticado.
- `description` deve ter minimo de 3 caracteres.
- `week` deve ser menor ou igual ao numero de semanas do calendar.
- `type` deve ser um dos tipos validos.
- `goal` e obrigatorio.
- Para `TIME`, `goal` deve ser recebido como texto e convertido para minutos.
- Criacao nao deve atualizar progresso.

### GET /api/v1/activities

Lista activities do usuario autenticado.

Regras:

- Deve retornar apenas activities cujo calendar pertence ao usuario autenticado.

### GET /api/v1/activities/{id}

Busca uma activity por id.

Regras:

- Deve retornar apenas se a activity pertencer a um calendar do usuario
  autenticado.

### PUT /api/v1/activities/{id}

Atualiza uma activity.

Payload:

- `calendarId`;
- `description`;
- `week`;
- `type`;
- `goal`.

Regras:

- Deve permitir editar apenas activities cujo calendar pertence ao usuario
  autenticado.
- O novo `calendarId`, quando informado, tambem deve pertencer ao usuario
  autenticado.
- `week` deve ser menor ou igual ao numero de semanas do calendar.
- Para `TIME`, `goal` deve ser recebido como texto e convertido para minutos.
- Edicao nao deve atualizar progresso.

### DELETE /api/v1/activities/{id}

Remove uma activity.

Regras:

- Deve permitir deletar apenas activities cujo calendar pertence ao usuario
  autenticado.

### GET /api/v1/activities/types

Lista os tipos de activity.

Resposta esperada:

```json
[
  {
    "label": "Dias",
    "value": "DAYS"
  },
  {
    "label": "Contagem",
    "value": "COUNT"
  },
  {
    "label": "Tempo",
    "value": "TIME"
  }
]
```

### GET /api/v1/activities/days

Lista os dias da semana.

Resposta esperada:

```json
[
  {
    "label": "Domingo",
    "value": "SUNDAY"
  },
  {
    "label": "Segunda-feira",
    "value": "MONDAY"
  },
  {
    "label": "Terça-feira",
    "value": "TUESDAY"
  },
  {
    "label": "Quarta-feira",
    "value": "WEDNESDAY"
  },
  {
    "label": "Quinta-feira",
    "value": "THURSDAY"
  },
  {
    "label": "Sexta-feira",
    "value": "FRIDAY"
  },
  {
    "label": "Sábado",
    "value": "SATURDAY"
  }
]
```

### POST /api/v1/activities/progress/days

Registra progresso de dias.

Payload:

- `activityId`;
- `day`.

Regras:

- A activity deve pertencer a um calendar do usuario autenticado.
- `day` deve ser um dia da semana valido.
- O mesmo dia da semana so pode ser registrado uma vez dentro da mesma activity.
- Outra activity, mesmo que seja da mesma semana, pode registrar o mesmo dia.
- Se o dia ja tiver sido registrado na activity, deve retornar erro.

### POST /api/v1/activities/progress/count

Registra progresso de contagem.

Payload:

- `activityId`;
- `value`.

Regras:

- A activity deve pertencer a um calendar do usuario autenticado.
- `value` deve ser inteiro positivo.
- O valor deve ser somado ao progresso de count.

### POST /api/v1/activities/progress/time

Registra progresso de tempo.

Payload:

- `activityId`;
- `time`.

Regras:

- A activity deve pertencer a um calendar do usuario autenticado.
- `time` deve ser texto contendo horas e/ou minutos.
- O texto de tempo deve aceitar horas e minutos em qualquer ordem, exemplos:
  `3h 20m`, `2m 4h`, `45m`, `2h`.
- O tempo deve ser convertido para minutos.
- Os minutos devem ser somados ao progresso de `TIME`.

## Regras de Testes

Devem existir testes cobrindo:

- CRUD de goals limitado ao usuario autenticado.
- Listagem de tipos de goal.
- CRUD de calendars limitado ao usuario autenticado.
- Validacao de `goalIds` pertencentes ao usuario autenticado.
- CRUD de activities limitado ao usuario autenticado.
- Validacao de `week <= calendar.weeks`.
- Conversao de texto de tempo para minutos.
- Listagem de tipos de activity.
- Listagem de dias da semana.
- Registro de progresso de days sem duplicidade por activity.
- Registro de progresso de count somando valores positivos.
- Registro de progresso de time somando minutos convertidos.
- Bloqueio de acesso entre usuarios diferentes.
