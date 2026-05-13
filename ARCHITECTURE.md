# Regras de arquitetura

Este projeto organiza funcionalidades por modulo de negocio. Cada modulo deve
ficar dentro do pacote base da aplicacao e deve explicitar suas camadas.

## Estrutura obrigatoria por modulo

Todo modulo deve seguir esta estrutura:

```text
br.com.ottonsam.toothy_planner_api.<modulo>
├── controllers
├── entities
├── repositories
└── usecases
```

Responsabilidades:

- `entities`: modelos de dominio e entidades persistidas do modulo.
- `repositories`: contratos e adaptadores de persistencia do modulo.
- `controllers`: endpoints HTTP e entrada/saida da API.
- `usecases`: regras de negocio, orquestracao e fluxos da funcionalidade.

Controllers nao devem conter regra de negocio. Repositories nao devem conter
regra de negocio. A regra de negocio deve ficar nos use cases.

## Fluxo para novas funcionalidades

Antes de implementar uma funcionalidade, as regras de negocio devem ser
descritas em texto e revisadas.

Fluxo obrigatorio:

1. Descrever as regras de negocio da funcionalidade.
2. Aguardar a inclusao, remocao ou ajuste de regras.
3. Implementar somente depois que as regras estiverem confirmadas.

## Fluxo para testes

Todos os modulos e funcionalidades devem ser cobertos por testes.

Antes de criar ou alterar testes, os cenarios devem ser descritos em texto e
revisados.

Fluxo obrigatorio:

1. Descrever os cenarios de teste da funcionalidade ou modulo.
2. Aguardar a inclusao, remocao ou ajuste de cenarios.
3. Criar ou alterar os testes somente depois que os cenarios estiverem
   confirmados.

## Cobertura minima esperada

Cada funcionalidade deve ter testes cobrindo:

- caminho de sucesso;
- validacoes de entrada;
- regras de negocio;
- erros esperados;
- integracao entre controller, use case e repository quando aplicavel.

Cada modulo deve ter cobertura para suas entidades, repositories, controllers e
use cases, respeitando o nivel de teste adequado para cada camada.
