# 001 - Users Module

## Objetivo

Criar o modulo de users para cadastro, autenticacao, ativacao de conta,
renovacao de sessao e gerenciamento dos dados do usuario autenticado.

## Entidade User

Cada usuario deve conter:

- `id`: UUID.
- `name`: string obrigatoria com minimo de 2 caracteres.
- `email`: email obrigatorio e unico.
- `password`: string obrigatoria com senha forte.
- `profileImage`: codigo da imagem armazenada no MinIO.
- `theme`: `"dark"` ou `"light"`, com padrao `"light"`.
- `isActive`: booleano com padrao `false`.

## Regras de Senha

A senha deve atender aos seguintes criterios:

- minimo de 8 caracteres;
- pelo menos 1 letra maiuscula;
- pelo menos 1 letra minuscula;
- pelo menos 1 numero;
- pelo menos 1 caractere especial.

## Regras de Imagem de Perfil

A imagem de perfil deve ser recebida como string base64.

Formatos aceitos:

- `png`;
- `jpg`;
- `jpeg`;
- `webp`;
- `heic`;
- `heif`.

O tamanho maximo permitido para a imagem e 10 MB.

A imagem deve ser privada no MinIO. O MinIO nao deve expor acesso publico direto
a imagem. A aplicacao deve ser a unica responsavel por carregar e exibir a
imagem para o usuario autenticado.

Quando o usuario nao possuir imagem cadastrada, a rota de exibicao deve retornar
uma imagem estatica padrao.

## Regras de Ativacao

Todo usuario criado deve iniciar com `isActive = false`.

O usuario nao deve enviar `isActive` na criacao. Esse campo deve ser alterado
somente pelo fluxo de confirmacao de email.

Apenas usuarios ativos podem realizar login.

O codigo de ativacao deve expirar em 15 minutos.

Deve ser possivel solicitar um novo codigo de ativacao apos a criacao do
usuario ou apos a expiracao de um codigo anterior.

Por enquanto, o envio de email nao deve ser integrado. O codigo deve ser exibido
no console da aplicacao.

## Cookies de Autenticacao

A autenticacao deve usar JWT por cookie.

Cookies esperados:

- `access_token`;
- `refresh_token`.

Duracao dos tokens:

- `access_token`: 15 minutos;
- `refresh_token`: 2 dias.

Configuracao dos cookies:

- `HttpOnly`;
- `Secure`;
- `SameSite=Lax`.

## Rotas Publicas

As seguintes rotas devem ser publicas:

- `POST /api/v1/users`;
- `POST /api/v1/users/login`;
- `GET /api/v1/users/refresh`, que deve ser acessivel sem `access_token`, mas deve
  exigir `refresh_token` valido;
- rota para solicitar envio do codigo de ativacao por email;
- rota para ativar o usuario com email e codigo.

Todas as demais rotas da aplicacao devem ser privadas e acessiveis somente com
autenticacao via JWT por cookie.

## Endpoints

Todas as rotas deste modulo devem ter requests correspondentes na collection
Bruno versionada em `bruno/`, incluindo metodo, caminho, cookies, payloads e
exemplos de resposta esperados para testes manuais da API.

### POST /api/v1/users

Cria um usuario.

Deve receber:

- `name`;
- `email`;
- `password`;
- `profileImage`;
- `theme`.

Nao deve receber:

- `id`;
- `isActive`.

Regras:

- `name` deve ser obrigatorio e ter minimo de 2 caracteres.
- `email` deve ser obrigatorio, valido e unico.
- `password` deve atender a regra de senha forte.
- `profileImage`, quando informada, deve ser base64 valida, ter no maximo 10 MB
  e usar um formato aceito.
- `theme`, quando ausente, deve assumir `"light"`.
- `isActive` deve ser salvo como `false`.

### POST /api/v1/users/login

Autentica um usuario.

Deve receber:

- `email`;
- `password`.

Regras:

- Apenas usuarios ativos podem realizar login.
- Se as credenciais forem validas e o usuario estiver ativo, deve retornar os
  cookies `access_token` e `refresh_token`.
- Se email e senha estiverem corretos, mas o usuario estiver inativo, deve gerar
  e enviar um novo codigo de ativacao para o email do usuario. Por enquanto, o
  codigo deve ser exibido no console da aplicacao.
- Quando o usuario estiver inativo e as credenciais estiverem corretas, deve
  retornar uma devolutiva informando que a conta precisa ser ativada e que um
  codigo de ativacao foi enviado.
- Se as credenciais forem invalidas, deve informar apenas que as credenciais sao
  invalidas.

### GET /api/v1/users/refresh

Renova a autenticacao.

Deve receber:

- cookie `refresh_token`.

Regras:

- Se o refresh token for valido, deve emitir novos cookies `access_token` e
  `refresh_token`.
- Se o refresh token for invalido, expirado ou ausente, deve negar a renovacao.

### GET /api/v1/users/me

Retorna os dados do usuario autenticado.

Regras:

- Deve identificar o usuario pelo cookie de autenticacao.
- Deve retornar os dados do usuario.
- No lugar do codigo da imagem de perfil, deve retornar a URL para visualizacao
  da imagem.

### GET /api/v1/users/image

Exibe a imagem de perfil do usuario autenticado.

Regras:

- Deve exigir autenticacao.
- Deve buscar a imagem privada no MinIO usando o codigo salvo no usuario.
- Deve retornar a imagem somente para o proprio usuario autenticado.
- Caso o usuario nao possua imagem cadastrada, deve retornar uma imagem estatica
  padrao.

### PUT /api/v1/users/image

Altera ou remove a imagem de perfil do usuario autenticado.

Deve receber:

- string base64 com a nova imagem; ou
- `null` para remover a imagem atual.

Regras:

- Deve exigir autenticacao.
- Quando receber uma imagem, deve validar formato e tamanho maximo de 10 MB.
- Quando receber uma nova imagem, deve salva-la no MinIO e atualizar o codigo
  salvo no usuario.
- Quando receber `null`, deve excluir a imagem atual do MinIO e limpar o codigo
  salvo no usuario.

### PUT /api/v1/users/me

Atualiza os dados do usuario autenticado.

Pode alterar:

- `name`;
- `password`;
- `theme`.

Nao pode alterar:

- `id`;
- `email`;
- `profileImage`;
- `isActive`.

Regras:

- Deve exigir autenticacao.
- `name`, quando informado, deve ter minimo de 2 caracteres.
- `password`, quando informada, deve atender a regra de senha forte.
- `theme`, quando informado, deve ser `"dark"` ou `"light"`.

### Rota de Solicitacao de Codigo de Ativacao

Solicita o envio de um codigo de ativacao.

Deve receber:

- `email`.

Regras:

- Deve gerar um novo codigo de ativacao.
- O codigo deve expirar em 15 minutos.
- O codigo deve ser exibido no console da aplicacao.
- Nao deve integrar envio real de email neste momento.

### Rota de Ativacao de Usuario

Ativa um usuario usando email e codigo de ativacao.

Deve receber:

- `email`;
- `code`.

Regras:

- Deve validar se o codigo pertence ao email informado.
- Deve validar se o codigo ainda nao expirou.
- Se valido, deve alterar `isActive` para `true`.
- Codigo invalido ou expirado nao deve ativar o usuario.

## Regras de Erro

Todas as mensagens de erro retornadas pela API devem ser em ingles.

Erros de autenticacao nao devem expor se o email existe, se a senha esta errada. Para esses casos, deve retornar mensagem generica
de credenciais invalidas.
