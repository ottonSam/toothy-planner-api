# Instrucoes do projeto

## Arquitetura

Siga as regras descritas em `toothy-planner-api/ARCHITECTURE.md`.

Todo modulo de negocio deve conter:

- `entities`
- `repositories`
- `controllers`
- `usecases`

Controllers e repositories nao devem conter regra de negocio. A regra de
negocio deve ficar nos use cases.

## Antes de implementar funcionalidades

Antes de escrever uma funcionalidade, descreva as regras de negocio propostas e
aguarde a inclusao, remocao ou ajuste de regras.

Implemente somente depois que as regras estiverem confirmadas.

## Antes de criar testes

Todos os modulos e funcionalidades devem ter testes.

Antes de criar ou alterar testes, descreva os cenarios de teste e aguarde a
inclusao, remocao ou ajuste de cenarios.

Crie ou altere os testes somente depois que os cenarios estiverem confirmados.
