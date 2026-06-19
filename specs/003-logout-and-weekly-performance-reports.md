# 003 - Logout And Weekly Performance Reports

## Objetivo

Adicionar uma rota de logout para limpar os cookies de autenticacao do usuario e
criar a funcionalidade de relatorios semanais de desempenho dos calendars,
incluindo metricas calculadas, armazenamento do relatorio gerado e integracao
com DeepSeek para gerar uma analise em Markdown em portugues do Brasil baseada
nos principios do 12 Week Year.

## Regras Gerais

Todas as rotas devem iniciar com `/api/v1`.

Todas as mensagens de erro retornadas pela API devem ser em ingles.

Todas as rotas privadas devem ser acessiveis somente com autenticacao via JWT por
cookie.

Todas as entidades devem pertencer ao usuario autenticado direta ou
indiretamente.

O usuario dono nao deve ser enviado no payload de criacao ou edicao. O usuario
deve sempre ser obtido a partir da autenticacao.

Todas as rotas desta spec devem ter requests correspondentes na collection Bruno
versionada em `bruno/`, incluindo metodo, caminho, cookies, payloads e exemplos
de resposta esperados para testes manuais da API.

## Frente de Autenticacao

### Logout

Adicionar uma rota de logout ao modulo de users.

Endpoint:

- `POST /api/v1/users/logout`

Regras:

- A rota deve limpar os cookies `access_token` e `refresh_token`.
- A limpeza deve ser feita retornando os cookies com valor vazio e expiracao
  imediata.
- Os cookies limpos devem manter a mesma configuracao usada nos cookies de
  autenticacao:
  - `HttpOnly`;
  - `Secure`;
  - `SameSite=Lax`;
  - `Path=/`.
- O logout nao deve invalidar tokens no servidor.
- O logout nao deve exigir persistencia de refresh token em banco.
- A resposta deve retornar uma mensagem simples de sucesso.

Resposta esperada:

```json
{
  "message": "Logout successful"
}
```

## Frente de Goal Calendar

## Entidades

### Calendar

Adicionar campos calculados e persistidos ao calendar para representar a regra
de inicio e fim da semana.

Novos campos:

- `weekStartsOn`: enum obrigatorio de dia da semana.
- `weekEndsOn`: enum obrigatorio de dia da semana.

Regras:

- `weekStartsOn` deve ser calculado a partir do campo `starts`.
- `weekEndsOn` deve ser calculado automaticamente como o dia anterior ao
  `weekStartsOn`.
- O cliente nao deve enviar `weekStartsOn` ou `weekEndsOn` na criacao ou edicao
  do calendar.
- Quando `starts` for alterado, `weekStartsOn` e `weekEndsOn` devem ser
  recalculados.
- Exemplo: se `starts` for uma segunda-feira, `weekStartsOn` deve ser `MONDAY` e
  `weekEndsOn` deve ser `SUNDAY`.

### Activity

Adicionar campos calculados e persistidos por activity para facilitar a geracao
de relatorios semanais.

Novos campos:

- `weekStartsAt`: date obrigatorio.
- `weekEndsAt`: date obrigatorio.

Regras:

- `weekStartsAt` deve ser calculado a partir de `calendar.starts` e
  `activity.week`.
- `weekEndsAt` deve ser calculado como 6 dias apos `weekStartsAt`.
- O cliente nao deve enviar `weekStartsAt` ou `weekEndsAt` na criacao ou edicao
  da activity.
- Quando a activity for criada, `weekStartsAt` e `weekEndsAt` devem ser
  calculados automaticamente.
- Quando `calendarId` ou `week` forem alterados na edicao da activity,
  `weekStartsAt` e `weekEndsAt` devem ser recalculados.
- A semana 1 deve iniciar em `calendar.starts`.
- A semana 2 deve iniciar 7 dias apos `calendar.starts`, e assim
  sucessivamente.

### WeeklyPerformanceReport

Criar uma entidade para armazenar relatorios semanais de desempenho.

Campos:

- `id`: UUID.
- `calendar`: obrigatorio. Um calendar pode ter diversos relatorios semanais.
- `week`: inteiro obrigatorio.
- `weekStartsAt`: date obrigatorio.
- `weekEndsAt`: date obrigatorio.
- `userFeedback`: texto obrigatorio.
- `metrics`: JSON obrigatorio contendo os dados calculados usados para gerar o
  relatorio.
- `markdownReport`: texto obrigatorio contendo o relatorio final gerado pelo
  DeepSeek em Markdown.
- `createdAt`: data e hora obrigatoria.
- `updatedAt`: data e hora obrigatoria.

Regras:

- Deve existir apenas um relatorio por `calendar` e `week`.
- O relatorio deve pertencer ao usuario autenticado indiretamente pelo calendar.
- Um usuario so pode gerar, listar ou visualizar relatorios de calendars que
  pertencem a ele.
- `week` deve ser maior que 0 e menor ou igual ao numero de semanas do calendar.
- `userFeedback` e obrigatorio e nao pode ser vazio.
- O relatorio so pode ser gerado no ultimo dia da semana em questao ou depois.
- O ultimo dia da semana deve ser `weekEndsAt`.
- Se a data atual for anterior a `weekEndsAt`, a geracao deve ser negada.
- Se ja existir relatorio para o mesmo `calendar` e `week`, a geracao deve ser
  negada.
- O Markdown salvo deve estar inteiramente em portugues do Brasil.

## Metricas do Relatorio Semanal

O relatorio deve calcular metricas a partir das activities da semana selecionada
e dos seus registros de progresso.

Dados gerais da semana:

- `calendarId`;
- `calendarDescription`;
- `week`;
- `weekStartsAt`;
- `weekEndsAt`;
- `weekStartsOn`;
- `weekEndsOn`;
- `totalActivities`;
- `expectedTotal`;
- `deliveredTotal`;
- `deliveryPercentage`;
- `generatedAt`.

Dados por activity:

- `activityId`;
- `description`;
- `type`;
- `goal`;
- `delivered`;
- `deliveryPercentage`;
- `weekStartsAt`;
- `weekEndsAt`;
- `progressRecords`.

Dados por registro de progresso:

- `registeredAt`;
- `progressDate`;
- `value`;
- `daysRemainingToWeekEnd`.

Regras:

- Apenas activities da semana selecionada devem compor as metricas.
- Para activities do tipo `DAYS`, `goal` representa a quantidade de dias
  esperada e cada registro de dia deve valer `1`.
- Para activities do tipo `COUNT`, `goal` representa a quantidade numerica
  esperada e cada registro deve contribuir com o valor informado.
- Para activities do tipo `TIME`, `goal` representa o total de minutos esperado e
  cada registro deve contribuir com os minutos convertidos.
- O progresso real pode ultrapassar o `goal`.
- As metricas de percentual devem ser limitadas a `100%`, mesmo quando o
  progresso real ultrapassar o `goal`.
- `deliveredTotal` deve manter o total real entregue, mesmo quando ultrapassar o
  esperado.
- `deliveryPercentage` geral deve ser a media aritmetica dos percentuais das
  activities, depois de cada percentual individual ser limitado a `100%`.
- Todas as activities devem ter o mesmo peso no percentual geral,
  independentemente do tipo ou do valor de `goal`.
- Se a semana nao possuir activities, `deliveryPercentage` deve ser `0%`.
- `expectedTotal` e `deliveredTotal` devem continuar representando as somas
  absolutas.
- `daysRemainingToWeekEnd` deve representar quantos dias faltavam entre a data
  do registro de progresso e `weekEndsAt`.
- Registros realizados depois de `weekEndsAt` devem aparecer no relatorio, mas
  `daysRemainingToWeekEnd` deve ser `0`.
- Registros realizados antes de `weekStartsAt` nao devem ser considerados para a
  semana.

## Relatorios Anteriores

Ao gerar um relatorio semanal, a aplicacao deve buscar ate 3 relatorios
anteriores do mesmo calendar.

Regras:

- Devem ser considerados apenas relatorios do mesmo calendar.
- Devem ser considerados apenas relatorios com `week` menor que a semana atual.
- Os relatorios devem ser enviados ao DeepSeek do mais recente para o mais
  antigo.
- Se houver menos de 3 relatorios anteriores, devem ser enviados apenas os
  existentes.
- Se nao houver relatorios anteriores, a geracao deve continuar normalmente.

## Integracao Com DeepSeek

Adicionar uma integracao com DeepSeek para gerar o relatorio textual em
Markdown.

Regras:

- A chamada ao DeepSeek deve acontecer no use case de geracao do relatorio.
- A chave de API e configuracoes da DeepSeek devem ser lidas de variaveis de
  ambiente ou propriedades da aplicacao.
- Controllers nao devem conhecer detalhes da integracao com DeepSeek.
- Repositories nao devem conter regra de negocio de geracao de relatorio.
- Se a chamada ao DeepSeek falhar, o relatorio nao deve ser salvo.
- A resposta da API deve indicar erro de geracao quando a integracao falhar.
- O conteudo gerado pelo DeepSeek deve ser salvo em `markdownReport`.
- O relatorio gerado deve estar inteiramente em portugues do Brasil.
- A aplicacao deve enviar ao DeepSeek:
  - contexto do calendar;
  - metricas da semana atual;
  - metricas por activity;
  - registros de progresso;
  - ate 3 relatorios anteriores;
  - feedback textual obrigatorio do usuario.

### Prompt Base

O prompt enviado ao DeepSeek deve seguir esta estrutura base:

```text
Voce e um avaliador de desempenho semanal da metodologia 12 Week Year.

Escreva em portugues do Brasil, em Markdown, com no maximo 200 palavras.
Use tom direto, pratico e respeitoso. Nao use motivacao generica, nao invente
dados e nao crie novas metricas.

Metricas da semana atual:
{{current_week_metrics}}

Relatorios das semanas anteriores, do mais recente para o mais antigo:
{{previous_reports}}

Feedback do usuario sobre a semana:
{{user_feedback}}

Regras da avaliacao:
- Relacione o que foi entregue na semana atual com ate 3 semanas anteriores e
  com o feedback do usuario.
- Se nao houver historico, avalie somente a semana atual e o feedback.
- Considere sucesso semanal somente quando deliveryPercentage for maior que
  85%. O valor 85% exato nao deve ser classificado automaticamente como sucesso.
- Nao liste todas as metricas nem detalhe cada atividade.
- Produza exatamente estas duas secoes:

# Avaliacao da Semana

## Recomendacao para a Proxima Semana
```

## Endpoints

### POST /api/v1/calendars/{calendarId}/weeks/{week}/reports

Gera o relatorio semanal de desempenho de uma semana do calendar.

Payload:

- `userFeedback`.

Regras:

- Deve exigir autenticacao.
- `calendarId` deve pertencer ao usuario autenticado.
- `week` deve ser maior que 0 e menor ou igual ao numero de semanas do calendar.
- `userFeedback` e obrigatorio.
- Deve validar se a semana ja terminou ou se a data atual e o ultimo dia da
  semana.
- Deve negar a geracao se ja existir relatorio para o mesmo calendar e semana.
- Deve calcular e salvar as metricas da semana.
- Deve buscar ate 3 relatorios anteriores do mesmo calendar.
- Deve chamar DeepSeek para gerar o relatorio em Markdown.
- Deve salvar `metrics` e `markdownReport`.
- Deve retornar o relatorio criado.

### GET /api/v1/calendars/{calendarId}/weeks/{week}/reports

Busca o relatorio semanal de desempenho de uma semana do calendar.

Regras:

- Deve exigir autenticacao.
- `calendarId` deve pertencer ao usuario autenticado.
- Deve retornar apenas relatorio do calendar informado e da semana informada.
- Se o relatorio nao existir, deve retornar erro de nao encontrado.

### GET /api/v1/calendars/{calendarId}/reports

Lista relatorios semanais de um calendar.

Regras:

- Deve exigir autenticacao.
- `calendarId` deve pertencer ao usuario autenticado.
- Deve retornar apenas relatorios do calendar informado.
- Os relatorios devem ser ordenados por `week` em ordem crescente.

## DTOs

### WeeklyPerformanceReportRequest

Campos:

- `userFeedback`: texto obrigatorio.

### WeeklyPerformanceReportResponse

Campos:

- `id`;
- `calendarId`;
- `week`;
- `weekStartsAt`;
- `weekEndsAt`;
- `userFeedback`;
- `metrics`;
- `markdownReport`;
- `createdAt`;
- `updatedAt`.

## Regras de Testes

Devem existir testes cobrindo:

- Logout limpando os cookies `access_token` e `refresh_token`.
- Logout retornando mensagem de sucesso.
- Criacao de calendar calculando `weekStartsOn` e `weekEndsOn` a partir de
  `starts`.
- Edicao de calendar recalculando `weekStartsOn` e `weekEndsOn` quando `starts`
  for alterado.
- Criacao de activity calculando `weekStartsAt` e `weekEndsAt`.
- Edicao de activity recalculando `weekStartsAt` e `weekEndsAt` quando
  `calendarId` ou `week` forem alterados.
- Geracao de relatorio semanal com sucesso no ultimo dia da semana.
- Geracao de relatorio semanal com sucesso depois do ultimo dia da semana.
- Bloqueio de geracao antes do ultimo dia da semana.
- Bloqueio de relatorio duplicado para o mesmo calendar e semana.
- Bloqueio de acesso entre usuarios diferentes.
- Validacao de `week` maior que 0.
- Validacao de `week` menor ou igual ao numero de semanas do calendar.
- Validacao de `userFeedback` obrigatorio.
- Calculo de metricas para activities do tipo `DAYS`.
- Calculo de metricas para activities do tipo `COUNT`.
- Calculo de metricas para activities do tipo `TIME`.
- Percentual limitado a `100%` quando o progresso ultrapassar a meta.
- `deliveredTotal` mantendo o valor real entregue quando ultrapassar a meta.
- Calculo de `daysRemainingToWeekEnd` nos registros de progresso.
- Busca de ate 3 relatorios anteriores do mesmo calendar.
- Geracao sem relatorios anteriores.
- Falha da DeepSeek impedindo que o relatorio seja salvo.
- Busca de relatorio semanal por calendar e week.
- Listagem de relatorios de um calendar ordenada por week.
