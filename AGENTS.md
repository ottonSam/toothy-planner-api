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

Devem ser criados DTOs quando necessario para separar entrada/saida de dados das
entidades de dominio.

As validacoes dos campos devem ser feitas na camada de entidade, informando as
mensagens de cada erro para garantir o padrao de linguagem definido pelo
projeto.

Entidades devem usar Lombok para reduzir codigo boilerplate e Bean Validation
do Spring Boot/Jakarta Validation para declarar validacoes de campos.

Todos os controllers devem expor rotas iniciando com `/api/v1`.

## Antes de implementar funcionalidades

Antes de escrever uma funcionalidade, descreva as regras de negocio propostas e
aguarde a inclusao, remocao ou ajuste de regras.

Implemente somente depois que as regras estiverem confirmadas.

## Antes de criar testes

Todos os modulos e funcionalidades devem ter testes.

Antes de criar ou alterar testes, descreva os cenarios de teste e aguarde a
inclusao, remocao ou ajuste de cenarios.

Crie ou altere os testes somente depois que os cenarios estiverem confirmados.

## Specs

As specs devem ficar na pasta `specs/`, na raiz do projeto.

Ao ser solicitada a criacao de uma spec, solicite todo o descritivo da
funcionalidade antes de criar o arquivo. Caso alguma informacao esteja ausente,
ambigua ou pouco clara, pergunte antes de criar a spec.

As specs devem ser numeradas de forma sequencial.

Specs devem ser implementadas somente quando houver solicitacao explicita para
implementar a spec.
