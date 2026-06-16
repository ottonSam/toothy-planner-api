# Toothy Planner API - Endpoints para integracao

Documento criado para ser anexado a uma inteligencia artificial ou usado por
clientes HTTP que precisem integrar com a API.

## Base

- Base local: `http://localhost:8080`
- Prefixo de todas as rotas: `/api/v1`
- Formato principal: `application/json`
- Datas simples: `YYYY-MM-DD`, exemplo `2026-06-09`
- Datas com horario: ISO-8601 com offset, exemplo `2026-06-09T14:30:00-03:00`
- IDs: UUID em string, exemplo `70356d1a-d81b-4251-992b-c2897db69652`

## Autenticacao

A API usa cookies HTTP-only:

- `access_token`: usado nas rotas autenticadas.
- `refresh_token`: usado em `GET /api/v1/users/refresh`.

Rotas publicas:

- `POST /api/v1/users`
- `POST /api/v1/users/login`
- `GET /api/v1/users/refresh`
- `POST /api/v1/users/activation-code`
- `POST /api/v1/users/activate`

Todas as demais rotas exigem cookie `access_token` valido.

Ao autenticar ou renovar sessao, a API retorna cookies `Set-Cookie`:

- `access_token`, validade de 15 minutos.
- `refresh_token`, validade de 2 dias.

## Formato de erro

Erros retornam JSON com o campo `message`.

```json
{
  "message": "Mensagem do erro"
}
```

Exemplos comuns:

- `400 Bad Request`: payload invalido, parametro invalido ou regra de negocio violada.
- `401 Unauthorized`: usuario nao autenticado, token invalido ou credenciais invalidas.
- `404 Not Found`: recurso nao encontrado ou nao pertence ao usuario autenticado.
- `409 Conflict`: recurso duplicado ou progresso ja registrado.
- `502 Bad Gateway`: falha na geracao de relatorio pela integracao DeepSeek.

## Enums

### UserTheme

```json
["DARK", "LIGHT"]
```

### GoalType

```json
["LONG_TERM", "MEDIUM_TERM", "CALENDAR"]
```

### ActivityType

```json
["DAYS", "COUNT", "TIME"]
```

### WeekDay

```json
[
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY"
]
```

## Schemas de resposta reutilizaveis

### MessageResponse

```json
{
  "message": "Authenticated"
}
```

### UserResponse

```json
{
  "id": "70356d1a-d81b-4251-992b-c2897db69652",
  "name": "John Doe",
  "email": "john@example.com",
  "profileImage": "/api/v1/users/image",
  "theme": "LIGHT",
  "isActive": true
}
```

### OptionResponse

```json
{
  "label": "Dias",
  "value": "DAYS"
}
```

### GoalResponse

```json
{
  "id": "70356d1a-d81b-4251-992b-c2897db69652",
  "name": "Aumentar faturamento",
  "type": "LONG_TERM",
  "isComplete": false
}
```

### CalendarResponse

```json
{
  "id": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Calendario Q3",
  "weeks": 12,
  "starts": "2026-06-09",
  "weekStartsOn": "TUESDAY",
  "weekEndsOn": "MONDAY",
  "goalIds": ["70356d1a-d81b-4251-992b-c2897db69652"]
}
```

### ActivityResponse

```json
{
  "id": "70356d1a-d81b-4251-992b-c2897db69652",
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Study sessions",
  "week": 2,
  "type": "DAYS",
  "goal": 3,
  "weekStartsAt": "2026-06-16",
  "weekEndsAt": "2026-06-22",
  "progress": 1,
  "progressDays": ["MONDAY"]
}
```

Para atividades `TIME`, `goal` e `progress` sao retornados em minutos.

### WeeklyPerformanceReportResponse

```json
{
  "id": "70356d1a-d81b-4251-992b-c2897db69652",
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "week": 1,
  "weekStartsAt": "2026-06-09",
  "weekEndsAt": "2026-06-15",
  "userFeedback": "Foi uma semana produtiva.",
  "metrics": {
    "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
    "calendarDescription": "Calendario Q3",
    "week": 1,
    "weekStartsAt": "2026-06-09",
    "weekEndsAt": "2026-06-15",
    "weekStartsOn": "TUESDAY",
    "weekEndsOn": "MONDAY",
    "totalActivities": 1,
    "expectedTotal": 3,
    "deliveredTotal": 1,
    "deliveryPercentage": 33.333333333333336,
    "generatedAt": "2026-06-16T10:00:00-03:00",
    "activities": [
      {
        "activityId": "70356d1a-d81b-4251-992b-c2897db69652",
        "description": "Study sessions",
        "type": "DAYS",
        "goal": 3,
        "delivered": 1,
        "deliveryPercentage": 33.333333333333336,
        "weekStartsAt": "2026-06-09",
        "weekEndsAt": "2026-06-15",
        "progressRecords": [
          {
            "registeredAt": "2026-06-12T10:00:00-03:00",
            "progressDate": "2026-06-12",
            "value": 1,
            "daysRemainingToWeekEnd": 3
          }
        ]
      }
    ]
  },
  "markdownReport": "# Relatorio Semanal de Desempenho\n\n...",
  "createdAt": "2026-06-16T10:00:00-03:00",
  "updatedAt": "2026-06-16T10:00:00-03:00"
}
```

Percentuais de relatorio sao limitados a `100.0`, mesmo quando o progresso real
ultrapassa a meta.

## Endpoints de usuarios

### POST /api/v1/users

Cria usuario inativo.

Autenticacao: publica.

Request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Strong1!",
  "profileImage": null,
  "theme": "LIGHT"
}
```

Regras principais:

- `name`: minimo 2 caracteres.
- `email`: formato valido; e normalizado para minusculo.
- `password`: minimo 8 caracteres, uma maiuscula, uma minuscula, um numero e um caractere especial.
- `profileImage`: opcional; quando informado, deve ser imagem base64 valida, ate 10 MB, em formato suportado.
- `theme`: opcional na criacao; se ausente, o backend usa `LIGHT`.

Resposta `201 Created`: `UserResponse`.

Erros comuns:

- `400`: dados invalidos.
- `409`: `Email already exists`.

### POST /api/v1/users/login

Autentica usuario ativo.

Autenticacao: publica.

Request:

```json
{
  "email": "john@example.com",
  "password": "Strong1!"
}
```

Resposta para usuario ativo `200 OK`, com cookies `access_token` e
`refresh_token`:

```json
{
  "message": "Authenticated"
}
```

Resposta para usuario inativo com credenciais corretas `200 OK`, sem cookies:

```json
{
  "message": "Account activation required. A new activation code was sent."
}
```

Erros comuns:

- `401`: `Invalid credentials`.

### POST /api/v1/users/logout

Remove cookies de autenticacao.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`, com cookies `access_token` e `refresh_token` expirados:

```json
{
  "message": "Logout successful"
}
```

### GET /api/v1/users/refresh

Renova os tokens usando o cookie `refresh_token`.

Autenticacao: publica, mas exige cookie `refresh_token` valido.

Request: sem body.

Resposta `200 OK`, com novos cookies `access_token` e `refresh_token`:

```json
{
  "message": "Authenticated"
}
```

Erros comuns:

- `401`: `Invalid refresh token`.

### GET /api/v1/users/me

Retorna o usuario autenticado.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`: `UserResponse`.

### PUT /api/v1/users/me

Atualiza parcialmente o perfil do usuario autenticado.

Autenticacao: exige `access_token`.

Request:

```json
{
  "name": "John Updated",
  "password": "NewStrong1!",
  "theme": "DARK"
}
```

Campos podem ser omitidos ou enviados como `null` para nao alterar. Quando
enviados, seguem as mesmas validacoes de criacao.

Resposta `200 OK`: `UserResponse`.

### GET /api/v1/users/image

Retorna os bytes da imagem de perfil do usuario autenticado.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

- Body: bytes da imagem.
- `Content-Type`: tipo da imagem armazenada, por exemplo `image/png`.
- Cache desabilitado por headers.

### PUT /api/v1/users/image

Atualiza a imagem de perfil.

Autenticacao: exige `access_token`.

Request:

```json
{
  "image": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

Resposta `200 OK`:

```json
{
  "message": "Profile image updated"
}
```

Erros comuns:

- `400`: imagem base64 invalida, maior que 10 MB ou formato nao suportado.

### POST /api/v1/users/activation-code

Solicita um codigo de ativacao para usuario existente.

Autenticacao: publica.

Request:

```json
{
  "email": "john@example.com"
}
```

Resposta `200 OK`:

```json
{
  "message": "Activation code sent"
}
```

Erros comuns:

- `400`: email invalido.
- `404`: `User not found`.

### POST /api/v1/users/activate

Ativa um usuario usando codigo de ativacao.

Autenticacao: publica.

Request:

```json
{
  "email": "john@example.com",
  "code": "123456"
}
```

Resposta `200 OK`:

```json
{
  "message": "User activated"
}
```

Erros comuns:

- `400`: `Invalid activation code`.

## Endpoints de metas

### POST /api/v1/goals

Cria meta do usuario autenticado.

Autenticacao: exige `access_token`.

Request:

```json
{
  "name": "Aumentar faturamento",
  "type": "LONG_TERM",
  "isComplete": false
}
```

Regras principais:

- `name`: minimo 3 caracteres.
- `type`: obrigatorio; um dos valores de `GoalType`.
- `isComplete`: opcional na criacao; se ausente, o backend usa `false`.

Resposta `201 Created`: `GoalResponse`.

### GET /api/v1/goals

Lista metas do usuario autenticado.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

```json
[
  {
    "id": "70356d1a-d81b-4251-992b-c2897db69652",
    "name": "Aumentar faturamento",
    "type": "LONG_TERM",
    "isComplete": false
  }
]
```

### GET /api/v1/goals/{id}

Busca uma meta por ID.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da meta.

Resposta `200 OK`: `GoalResponse`.

Erros comuns:

- `404`: `Goal not found`.

### PUT /api/v1/goals/{id}

Atualiza uma meta.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da meta.

Request:

```json
{
  "name": "Aumentar faturamento recorrente",
  "type": "LONG_TERM",
  "isComplete": true
}
```

No update, `isComplete` e obrigatorio.

Resposta `200 OK`: `GoalResponse`.

Erros comuns:

- `400`: `Goal completion status is required`.
- `404`: `Goal not found`.

### DELETE /api/v1/goals/{id}

Remove uma meta.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da meta.

Resposta `204 No Content`: sem body.

### GET /api/v1/goals/types

Lista tipos de meta.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

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

## Endpoints de calendarios

### POST /api/v1/calendars

Cria calendario do usuario autenticado.

Autenticacao: exige `access_token`.

Request:

```json
{
  "description": "Calendario Q3",
  "weeks": 12,
  "starts": "2026-06-09",
  "goalIds": ["70356d1a-d81b-4251-992b-c2897db69652"]
}
```

Regras principais:

- `description`: minimo 3 caracteres.
- `weeks`: obrigatorio; entre 1 e 52.
- `starts`: obrigatorio; data no formato `YYYY-MM-DD`.
- `goalIds`: opcional; todas as metas devem pertencer ao usuario autenticado.
- `weekStartsOn` e `weekEndsOn` sao calculados pelo backend a partir de `starts`.

Resposta `201 Created`: `CalendarResponse`.

Erros comuns:

- `400`: `Calendar weeks is required`.
- `400`: `All goals must belong to the authenticated user`.

### GET /api/v1/calendars

Lista calendarios do usuario autenticado.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

```json
[
  {
    "id": "70356d1a-d81b-4251-992b-c2897db69652",
    "description": "Calendario Q3",
    "weeks": 12,
    "starts": "2026-06-09",
    "weekStartsOn": "TUESDAY",
    "weekEndsOn": "MONDAY",
    "goalIds": ["70356d1a-d81b-4251-992b-c2897db69652"]
  }
]
```

### GET /api/v1/calendars/{id}

Busca calendario por ID.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID do calendario.

Resposta `200 OK`: `CalendarResponse`.

Erros comuns:

- `404`: `Calendar not found`.

### PUT /api/v1/calendars/{id}

Atualiza calendario.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID do calendario.

Request:

```json
{
  "description": "Calendario Q3 atualizado",
  "weeks": 12,
  "starts": "2026-06-09",
  "goalIds": ["70356d1a-d81b-4251-992b-c2897db69652"]
}
```

Resposta `200 OK`: `CalendarResponse`.

### DELETE /api/v1/calendars/{id}

Remove calendario.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID do calendario.

Resposta `204 No Content`: sem body.

### GET /api/v1/calendars/{calendarId}/weeks/{week}/activities

Lista atividades de um calendario em uma semana.

Autenticacao: exige `access_token`.

Path params:

- `calendarId`: UUID do calendario.
- `week`: numero inteiro da semana, entre `1` e `calendar.weeks`.

Resposta `200 OK`:

```json
[
  {
    "id": "70356d1a-d81b-4251-992b-c2897db69652",
    "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
    "description": "Study sessions",
    "week": 2,
    "type": "DAYS",
    "goal": 3,
    "weekStartsAt": "2026-06-16",
    "weekEndsAt": "2026-06-22",
    "progress": 1,
    "progressDays": ["MONDAY"]
  }
]
```

Erros comuns:

- `400`: `Activity week must be greater than 0`.
- `400`: `Activity week must be less than or equal to calendar weeks`.
- `404`: `Calendar not found`.

## Endpoints de atividades

### POST /api/v1/activities

Cria atividade vinculada a um calendario do usuario autenticado.

Autenticacao: exige `access_token`.

Request para atividade por dias:

```json
{
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Study sessions",
  "week": 2,
  "type": "DAYS",
  "goal": "3"
}
```

Request para atividade por contagem:

```json
{
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Publicar artigos",
  "week": 2,
  "type": "COUNT",
  "goal": "5"
}
```

Request para atividade por tempo:

```json
{
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Estudar ingles",
  "week": 2,
  "type": "TIME",
  "goal": "3h 20m"
}
```

Regras principais:

- `calendarId`: obrigatorio; calendario deve pertencer ao usuario autenticado.
- `description`: minimo 3 caracteres.
- `week`: obrigatorio; entre `1` e `calendar.weeks`.
- `type`: obrigatorio; um dos valores de `ActivityType`.
- `goal`: string obrigatoria.
- Para `DAYS` e `COUNT`, `goal` deve ser inteiro positivo em string.
- Para `TIME`, `goal` deve usar horas/minutos, como `3h 20m`, `45m` ou `2h`.

Resposta `201 Created`: `ActivityResponse`.

Erros comuns:

- `400`: `Calendar id is required`.
- `400`: `Activity week is required`.
- `400`: `Activity goal must be an integer`.
- `400`: `Time must use hours and minutes, for example 3h 20m`.
- `404`: `Calendar not found`.

### GET /api/v1/activities

Lista atividades do usuario autenticado.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

```json
[
  {
    "id": "70356d1a-d81b-4251-992b-c2897db69652",
    "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
    "description": "Study sessions",
    "week": 2,
    "type": "DAYS",
    "goal": 3,
    "weekStartsAt": "2026-06-16",
    "weekEndsAt": "2026-06-22",
    "progress": 0,
    "progressDays": []
  }
]
```

### GET /api/v1/activities/{id}

Busca atividade por ID.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da atividade.

Resposta `200 OK`: `ActivityResponse`.

Erros comuns:

- `404`: `Activity not found`.

### PUT /api/v1/activities/{id}

Atualiza atividade.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da atividade.

Request:

```json
{
  "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
  "description": "Study sessions updated",
  "week": 2,
  "type": "DAYS",
  "goal": "4"
}
```

Resposta `200 OK`: `ActivityResponse`.

### DELETE /api/v1/activities/{id}

Remove atividade.

Autenticacao: exige `access_token`.

Path params:

- `id`: UUID da atividade.

Resposta `204 No Content`: sem body.

### GET /api/v1/activities/types

Lista tipos de atividade.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

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

Lista dias da semana.

Autenticacao: exige `access_token`.

Request: sem body.

Resposta `200 OK`:

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

Registra progresso de uma atividade `DAYS`.

Autenticacao: exige `access_token`.

Request:

```json
{
  "activityId": "70356d1a-d81b-4251-992b-c2897db69652",
  "day": "MONDAY"
}
```

Resposta `200 OK`: `ActivityResponse`.

Erros comuns:

- `400`: `Week day is required`.
- `404`: `Activity not found`.
- `409`: `Week day already registered for this activity`.

### POST /api/v1/activities/progress/count

Registra progresso numerico.

Autenticacao: exige `access_token`.

Request:

```json
{
  "activityId": "70356d1a-d81b-4251-992b-c2897db69652",
  "value": 2
}
```

Regras principais:

- `value`: obrigatorio; inteiro positivo.

Resposta `200 OK`: `ActivityResponse`.

Erros comuns:

- `400`: `Progress value is required`.
- `400`: `Progress value must be positive`.
- `404`: `Activity not found`.

### POST /api/v1/activities/progress/time

Registra progresso de tempo.

Autenticacao: exige `access_token`.

Request:

```json
{
  "activityId": "70356d1a-d81b-4251-992b-c2897db69652",
  "time": "1h 30m"
}
```

Regras principais:

- `time`: obrigatorio; deve usar horas/minutos, como `3h 20m`, `45m` ou `2h`.
- O backend converte o valor para minutos e soma em `progress`.

Resposta `200 OK`: `ActivityResponse`.

Erros comuns:

- `400`: `Time value is required`.
- `400`: `Time must use hours and minutes, for example 3h 20m`.
- `404`: `Activity not found`.

## Endpoints de relatorios semanais

### POST /api/v1/calendars/{calendarId}/weeks/{week}/reports

Gera relatorio semanal de desempenho com IA e salva metricas + Markdown.

Autenticacao: exige `access_token`.

Path params:

- `calendarId`: UUID do calendario.
- `week`: numero inteiro da semana, entre `1` e `calendar.weeks`.

Request:

```json
{
  "userFeedback": "Foi uma semana produtiva, mas concentrei parte das entregas no fim."
}
```

Regras principais:

- `userFeedback`: obrigatorio e nao pode ser vazio.
- O calendario deve pertencer ao usuario autenticado.
- O relatorio so pode ser gerado em ou apos a data final da semana.
- So pode existir um relatorio por calendario/semana.
- A resposta inclui `metrics` em JSON e `markdownReport` em Markdown.
- O relatorio em Markdown e gerado em portugues do Brasil.

Resposta `201 Created`: `WeeklyPerformanceReportResponse`.

Erros comuns:

- `400`: `User feedback is required`.
- `400`: `Week must be greater than 0`.
- `400`: `Week must be less than or equal to calendar weeks`.
- `400`: `Weekly performance report can only be generated on or after the week end date`.
- `404`: `Calendar not found`.
- `409`: `Weekly performance report already exists for this week`.
- `502`: falha de configuracao ou resposta da DeepSeek.

### GET /api/v1/calendars/{calendarId}/weeks/{week}/reports

Busca relatorio de um calendario em uma semana.

Autenticacao: exige `access_token`.

Path params:

- `calendarId`: UUID do calendario.
- `week`: numero inteiro da semana.

Request: sem body.

Resposta `200 OK`: `WeeklyPerformanceReportResponse`.

Erros comuns:

- `404`: `Weekly performance report not found`.

### GET /api/v1/calendars/{calendarId}/reports

Lista relatorios de um calendario em ordem crescente de semana.

Autenticacao: exige `access_token`.

Path params:

- `calendarId`: UUID do calendario.

Request: sem body.

Resposta `200 OK`:

```json
[
  {
    "id": "70356d1a-d81b-4251-992b-c2897db69652",
    "calendarId": "70356d1a-d81b-4251-992b-c2897db69652",
    "week": 1,
    "weekStartsAt": "2026-06-09",
    "weekEndsAt": "2026-06-15",
    "userFeedback": "Foi uma semana produtiva.",
    "metrics": {},
    "markdownReport": "# Relatorio Semanal de Desempenho\n\n...",
    "createdAt": "2026-06-16T10:00:00-03:00",
    "updatedAt": "2026-06-16T10:00:00-03:00"
  }
]
```

Erros comuns:

- `404`: `Calendar not found`.

## Inventario resumido de rotas

| Metodo | Rota | Autenticacao | Resposta principal |
| --- | --- | --- | --- |
| POST | `/api/v1/users` | Publica | `201 UserResponse` |
| POST | `/api/v1/users/login` | Publica | `200 MessageResponse` + cookies |
| POST | `/api/v1/users/logout` | Cookie `access_token` | `200 MessageResponse` + cookies expirados |
| GET | `/api/v1/users/refresh` | Cookie `refresh_token` | `200 MessageResponse` + cookies |
| GET | `/api/v1/users/me` | Cookie `access_token` | `200 UserResponse` |
| PUT | `/api/v1/users/me` | Cookie `access_token` | `200 UserResponse` |
| GET | `/api/v1/users/image` | Cookie `access_token` | `200 bytes da imagem` |
| PUT | `/api/v1/users/image` | Cookie `access_token` | `200 MessageResponse` |
| POST | `/api/v1/users/activation-code` | Publica | `200 MessageResponse` |
| POST | `/api/v1/users/activate` | Publica | `200 MessageResponse` |
| POST | `/api/v1/goals` | Cookie `access_token` | `201 GoalResponse` |
| GET | `/api/v1/goals` | Cookie `access_token` | `200 GoalResponse[]` |
| GET | `/api/v1/goals/{id}` | Cookie `access_token` | `200 GoalResponse` |
| PUT | `/api/v1/goals/{id}` | Cookie `access_token` | `200 GoalResponse` |
| DELETE | `/api/v1/goals/{id}` | Cookie `access_token` | `204 sem body` |
| GET | `/api/v1/goals/types` | Cookie `access_token` | `200 OptionResponse[]` |
| POST | `/api/v1/calendars` | Cookie `access_token` | `201 CalendarResponse` |
| GET | `/api/v1/calendars` | Cookie `access_token` | `200 CalendarResponse[]` |
| GET | `/api/v1/calendars/{id}` | Cookie `access_token` | `200 CalendarResponse` |
| PUT | `/api/v1/calendars/{id}` | Cookie `access_token` | `200 CalendarResponse` |
| DELETE | `/api/v1/calendars/{id}` | Cookie `access_token` | `204 sem body` |
| GET | `/api/v1/calendars/{calendarId}/weeks/{week}/activities` | Cookie `access_token` | `200 ActivityResponse[]` |
| POST | `/api/v1/activities` | Cookie `access_token` | `201 ActivityResponse` |
| GET | `/api/v1/activities` | Cookie `access_token` | `200 ActivityResponse[]` |
| GET | `/api/v1/activities/{id}` | Cookie `access_token` | `200 ActivityResponse` |
| PUT | `/api/v1/activities/{id}` | Cookie `access_token` | `200 ActivityResponse` |
| DELETE | `/api/v1/activities/{id}` | Cookie `access_token` | `204 sem body` |
| GET | `/api/v1/activities/types` | Cookie `access_token` | `200 OptionResponse[]` |
| GET | `/api/v1/activities/days` | Cookie `access_token` | `200 OptionResponse[]` |
| POST | `/api/v1/activities/progress/days` | Cookie `access_token` | `200 ActivityResponse` |
| POST | `/api/v1/activities/progress/count` | Cookie `access_token` | `200 ActivityResponse` |
| POST | `/api/v1/activities/progress/time` | Cookie `access_token` | `200 ActivityResponse` |
| POST | `/api/v1/calendars/{calendarId}/weeks/{week}/reports` | Cookie `access_token` | `201 WeeklyPerformanceReportResponse` |
| GET | `/api/v1/calendars/{calendarId}/weeks/{week}/reports` | Cookie `access_token` | `200 WeeklyPerformanceReportResponse` |
| GET | `/api/v1/calendars/{calendarId}/reports` | Cookie `access_token` | `200 WeeklyPerformanceReportResponse[]` |
