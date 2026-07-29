# Aula 7 — API Gateway, microsserviços e autenticação JWT

## 1. Objetivo da aula

Separar o e-commerce em microsserviços e usar um **API Gateway** como ponto de
entrada do cliente. A API continua recebendo `POST /pedidos` por REST, enquanto
os serviços internos se comunicam por gRPC e pedidos mantém a persistência da
aula 6.

O foco da aula é proteger essa entrada com **autenticação JWT**: gerar um token,
enviá-lo como Bearer, validar sua assinatura e expiração e observar a diferença
entre uma requisição sem autenticação e uma requisição com dados inválidos.

## 2. Conceitos importantes

### 2.1. API Gateway e separação em microsserviços

O API Gateway é a entrada do sistema para o cliente. Ele recebe HTTP/JSON,
aplica autenticação e validação e encaminha o pedido ao serviço responsável.
O cliente usa uma única URL, mesmo que o processamento envolva várias aplicações.

Nesta arquitetura, cada aplicação tem uma responsabilidade:

| Módulo | Responsabilidade |
|---|---|
| `api-gateway` | API REST, autenticação JWT, validação do DTO e chamada ao serviço de pedidos |
| `pedido-service` | Consulta de estoque, decisão de aceite ou rejeição e persistência do pedido |
| `estoque-service` | Consulta do estoque fixo em memória |
| `contratos-grpc` | Contratos `.proto` compartilhados e código gerado; não é um processo servidor |

As três aplicações são processos Java separados. Podem rodar na mesma máquina
ou em VMs diferentes. A entidade `Pedido` e o `PedidoRepository` passam para
`pedido-service`, que devolve o UUID realmente salvo no banco.

Reutilizamos o gRPC já estudado: gateway chama pedidos, e pedidos chama estoque.
`@GrpcService` registra as implementações, e `GrpcChannelFactory` cria os canais
para os stubs dos clientes. O Maven gera as classes a partir dos `.proto` em
`contratos-grpc/target/generated-sources/protobuf/`.

### 2.2. O que é JWT e como ele autentica uma requisição

JWT é um formato de token que transporta informações chamadas **claims**. Neste
laboratório, o token representa o sujeito `aluno` e tem prazo de validade.
O cliente apresenta esse token ao gateway a cada requisição protegida.

Um JWT possui três partes separadas por pontos:

```text
cabeçalho.payload.assinatura
```

- **Cabeçalho:** informa o algoritmo de assinatura, aqui `HS256`.
- **Payload:** contém as claims, como o sujeito e as datas de emissão e expiração.
- **Assinatura:** permite verificar se o token foi assinado com a chave esperada
  e se o conteúdo foi alterado.

O cabeçalho e o payload são codificados em Base64URL e podem ser lidos.
**JWT assinado não é conteúdo criptografado.** Alterar uma claim sem refazer a
assinatura com a chave correta torna o token inválido.

HS256 usa um segredo compartilhado para assinar e validar. `GeradorTokenJwt`
emite o token, e `SecurityConfig` configura o gateway para validar com o mesmo
segredo didático:

```text
segredo-didatico-com-tamanho-suficiente-para-hs256
```

Esse valor fixo serve para o laboratório; em produção, a chave precisa ser
protegida e o tráfego do cliente deve usar HTTPS.

| Claim | Significado | Valor definido pelo gerador |
|---|---|---|
| `sub` | Sujeito identificado pelo token | `aluno` |
| `iat` | Instante de emissão | Horário atual |
| `exp` | Instante de expiração | 30 minutos após a emissão |

O gerador simula a emissão de um token e é executado pelo terminal. O exemplo
não implementa uma tela ou endpoint de login com usuário e senha. A autenticação
ensinada aqui é a validação do token apresentado à API.

### 2.3. Bearer Token e Spring Security

O cliente envia o JWT no cabeçalho HTTP:

```http
Authorization: Bearer TOKEN
```

Bearer significa que quem apresenta um token válido pode usá-lo para acessar a
rota. O gateway verifica a assinatura e a expiração antes de executar o controller.
Sem token, ou com token inválido ou expirado, responde **401 Unauthorized**.

Autenticação identifica o sujeito; autorização define o que ele pode acessar.
Nesta aula, a regra para `POST /pedidos` exige apenas estar autenticado.
Não há regras por papéis ou permissões.

| Elemento do código | Papel na autenticação |
|---|---|
| `SecurityFilterChain` | Define a proteção de `POST /pedidos` e ativa o Resource Server JWT |
| `JwtDecoder` | Valida a assinatura HS256 e as datas do token recebido |
| `GeradorTokenJwt` | Assina um token com as claims de `aluno` |
| `SessionCreationPolicy.STATELESS` | Faz a API trabalhar sem sessão HTTP; cada requisição apresenta seu token |
| `ValidacaoExceptionHandler` | Trata os erros do DTO depois que a autenticação foi aceita |

CSRF fica desabilitado para este exemplo de API com Bearer token. O
`PedidoController` continua recebendo o DTO e chamando pedidos; a autenticação
é feita pelos filtros do Spring Security antes dele.

Com token válido, um item vazio retorna **400 Bad Request** pela validação da
aula 6. O mesmo corpo sem token retorna **401**, pois a autenticação vem primeiro.
JWT é validado no gateway; a comunicação interna continua por gRPC.

Referência: [JWT no Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## 3. Arquitetura e configurações necessárias

```text
Postman / curl
  |
  | REST + JSON + Authorization: Bearer TOKEN :8080
  v
api-gateway
  |
  +-- Spring Security: token ausente ou inválido -> 401
  +-- Bean Validation: dados inválidos -> 400
  |
  | gRPC :9090
  v
pedido-service ---- JPA ----> H2 local / PostgreSQL no servidor
  |
  | gRPC :9091
  v
estoque-service (estoque fixo em memória)
```

O estoque tem 10 notebooks, 20 teclados e 30 mouses. Um item desconhecido tem
quantidade disponível zero. A consulta não faz baixa nem reserva de estoque.
Pedidos aceitos e rejeitados por estoque são salvos e retornam HTTP 201; cada
POST válido cria um novo registro.

| Aplicação / recurso | Endereço local |
|---|---|
| Gateway REST | `localhost:8080` |
| Pedidos gRPC | `localhost:9090` |
| Estoque gRPC | `localhost:9091` |
| Console H2 de pedidos | `localhost:8081/h2-console`, somente no perfil `local` |

As dependências de Spring Security, OAuth2 Resource Server e JOSE ficam no
`api-gateway`. JOSE fornece o suporte à assinatura e validação do JWT. O segredo
no gerador e no gateway deve ser o mesmo; o sujeito e a duração ficam definidos
em `GeradorTokenJwt`.

O banco fica em `pedido-service`. As configurações estão em
`pedido-service/src/main/resources/`: H2 em memória no perfil `local` e PostgreSQL
na execução padrão, usando as variáveis da aula 6:

| Variável | Uso |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do usuário |

O H2 usa `create-drop` e perde os dados ao encerrar pedidos. O PostgreSQL usa
`ddl-auto=update` e preserva os registros existentes na tabela `pedidos`.

## 4. Como compilar e executar os testes automatizados

Na máquina local, na raiz do projeto:

```bash
./mvnw clean verify
```

O Maven compila os módulos com Java 21, gera as classes Protobuf, executa o
teste existente de inicialização do gateway e cria os três JARs executáveis:

```text
estoque-service/target/estoque-service-0.0.1-SNAPSHOT.jar
pedido-service/target/pedido-service-0.0.1-SNAPSHOT.jar
api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

## 5. Como executar localmente

Depois de compilar, abra três terminais na raiz do projeto.

No primeiro terminal local, inicie o estoque:

```bash
java -jar estoque-service/target/estoque-service-0.0.1-SNAPSHOT.jar
```

No segundo terminal local, inicie pedidos com o banco H2:

```bash
java -jar pedido-service/target/pedido-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

No terceiro terminal local, inicie o gateway:

```bash
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

Em outro terminal local, gere um token. A classe não recebe argumentos e
imprime somente o JWT:

```bash
java -Dloader.main=aula.sd.ecommerce.GeradorTokenJwt \
  -cp api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher
```

Para usar o token nos comandos curl, também é possível gerá-lo diretamente em
uma variável do shell:

```bash
TOKEN=$(java -Dloader.main=aula.sd.ecommerce.GeradorTokenJwt \
  -cp api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher)
```

Teste sem token:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

Teste com token inválido:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Authorization: Bearer token-invalido" \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

Teste a validação do pedido com token válido, mas pedido invalido:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"item":"","quantidade":2}'
```

Teste o pedido completo com token válido:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

Para encerrar, pressione `Ctrl+C` nos três terminais das aplicações.

### 5.1. Consultar o banco local

O perfil `local` usa H2 em memória e não exige PostgreSQL nem variáveis de banco.
Abra [http://localhost:8081/h2-console](http://localhost:8081/h2-console) e
preencha:

| Campo | Valor |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1` |
| User Name | `sa` |
| Password | Deixe vazio |

Clique em **Connect** e execute:

```sql
SELECT * FROM pedidos;
```

Compare o ID da resposta com a linha salva. Sem token, com token inválido ou
com dados rejeitados pela validação, não deve haver nova linha. Com token válido,
pedidos aceitos e rejeitados por falta de estoque são gravados.

## 6. Resultados esperados

Sem o cabeçalho `Authorization`, o filtro responde antes do controller:

```text
HTTP/1.1 401
WWW-Authenticate: Bearer
```

O token inválido também retorna:

```text
HTTP/1.1 401
WWW-Authenticate: Bearer
```

Com token válido e item vazio, o JWT é aceito e Bean Validation retorna:

```text
HTTP/1.1 400
Content-Type: application/json

{"mensagem":"Dados do pedido inválidos","erros":{"item":"não deve estar vazio"}}
```

Com token válido e pedido processado pelo fluxo gRPC:

```text
HTTP/1.1 201
Content-Type: application/json

{"id":"UUID_GERADO","status":"ACEITO","mensagem":"Pedido aceito."}
```

O ID retornado é o UUID salvo na tabela `pedidos`. Com token válido e 11 unidades
de notebook, a resposta também é HTTP 201, mas com status `REJEITADO` e mensagem
`Estoque insuficiente.`; esse pedido também é persistido.

A ordem das propriedades JSON pode variar. O cabeçalho `WWW-Authenticate` pode
conter detalhes adicionais do erro. Um token expirado ou um token cujo
conteúdo ou assinatura tenha sido alterado retorna HTTP 401.

## 7. PostgreSQL e aplicações no Google Cloud

Inicie as três VMs pelo Google Cloud Console, mantendo a organização da aula:
uma para o gateway, uma para pedidos e uma para estoque, na mesma rede. O banco
PostgreSQL fica no Cloud SQL, como na aula 6.

Pelo Console, confira as regras de entrada:

- TCP 8080 na VM do gateway, para o IP público da máquina do cliente.
- TCP 9090 na VM de pedidos, somente para a VM do gateway.
- TCP 9091 na VM de estoque, somente para a VM de pedidos.

Em **Cloud SQL > sua instância > Conexões > Rede**, adicione o IP externo da
**VM de pedidos** como `IP_PUBLICO_DA_VM_DE_PEDIDOS/32` em Redes autorizadas.
Somente o serviço de pedidos acessa o banco. Usaremos o usuário e o banco
existentes `postgres`, como na aula 6.

### 7.1. Subindo as aplicações

Com Java 21 e o projeto disponíveis nas VMs, abra uma sessão SSH pelo Console
em cada uma. Atualize e compile o projeto na raiz:

```bash
git pull
git checkout aula-7
./mvnw clean verify
```

Na VM de estoque:

```bash
java -jar estoque-service/target/estoque-service-0.0.1-SNAPSHOT.jar
```

Na VM de pedidos, exporte as configurações do banco e indique o IP interno do
estoque na inicialização:

```bash
export DB_URL='jdbc:postgresql://IP_PUBLICO_DO_BANCO:5432/postgres?sslmode=require'
export DB_USER='postgres'
export DB_PASSWORD='SENHA_DO_BANCO'
java -jar pedido-service/target/pedido-service-0.0.1-SNAPSHOT.jar \
  --spring.grpc.client.channel.estoque.target=static://IP_INTERNO_DO_ESTOQUE:9091
```

Substitua `IP_PUBLICO_DO_BANCO` pelo IP do Cloud SQL e informe a senha do usuário
`postgres`. Execute sem o perfil `local` para usar PostgreSQL. A instância deve
aceitar TLS sem exigir certificado de cliente, conforme a configuração da aula 6.

Na VM do gateway:

```bash
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar \
  --spring.grpc.client.channel.pedidos.target=static://IP_INTERNO_DOS_PEDIDOS:9090
```

Os argumentos substituem os destinos locais dos canais, sem editar os arquivos
nem recompilar. Copie os IPs internos das VMs pelo Console. As portas 9090 e
9091 são usadas somente entre os serviços; o cliente acessa a porta 8080.

### 7.2. Conferir os registros no banco

No **Cloud SQL Studio**, conecte ao banco `postgres` e execute:

```sql
SELECT * FROM pedidos;
```

Envie os pedidos com o token gerado conforme a seção 5. O segredo no gerador
e no gateway precisa ser o mesmo. Compare o ID da resposta REST com a linha
gravada. Envie um pedido aceito e um rejeitado e confira ambos no banco. Depois
envie requisições sem token, com token inválido e com item vazio ou quantidade
zero: a contagem deve permanecer igual. Reiniciar pedidos preserva os registros
no PostgreSQL.

## 8. Como testar pelo Postman

Com as aplicações em execução, gere um token pelo comando da seção 5 e copie
a linha impressa no terminal. Abra o Postman na máquina local:

1. Selecione o método **POST**.
2. Informe `http://IP_PUBLICO_DO_GATEWAY:8080/pedidos`, usando o IP externo da VM
   do gateway. Para testar localmente, use `http://localhost:8080/pedidos`.
3. Na aba **Body**, selecione **raw** e escolha **JSON**.
4. Insira o corpo:

```json
{
  "item": "notebook",
  "quantidade": 2
}
```

Confira em **Headers** o cabeçalho `Content-Type: application/json`.
Na aba **Authorization**, selecione **No Auth** e clique em **Send**: espere
**401 Unauthorized**. O pedido não chega ao controller nem é gravado no banco.

Depois selecione **Bearer Token** e preencha **Token** com `token-invalido`.
Envie novamente: espere HTTP 401.

Agora substitua o campo **Token** pelo JWT gerado no terminal. Cole somente o
JWT, sem aspas e sem escrever `Bearer`: o Postman adiciona o prefixo ao cabeçalho
`Authorization`. Remova qualquer cabeçalho Authorization preenchido manualmente
para evitar duplicidade.

Clique em **Send**. Espere **201 Created**, com resposta semelhante a:

```json
{
  "id": "UUID_SALVO",
  "status": "ACEITO",
  "mensagem": "Pedido aceito."
}
```

Confira o ID no console H2 ou no Cloud SQL Studio. Mantendo o token válido,
envie os cenários de validação e estoque:

| Cenário | Body (raw / JSON) | Resultado esperado |
|---|---|---|
| Estoque insuficiente | `{"item":"notebook","quantidade":11}` | 201, `REJEITADO`, salvo no banco |
| Item vazio | `{"item":"","quantidade":2}` | 400, `erros.item`, sem inserção |
| Quantidade zero | `{"item":"notebook","quantidade":0}` | 400, `erros.quantidade`, sem inserção |
| Quantidade ausente | `{"item":"notebook"}` | 400, `erros.quantidade`, sem inserção |

Para explicar a ordem dos filtros, envie item vazio com **No Auth**: a resposta
é 401. Com um token válido, o mesmo corpo retorna 400 pelo
`ValidacaoExceptionHandler`. Autenticação ocorre antes da validação do pedido.

Se o token expirar, execute novamente o gerador e substitua o campo Token.
O cliente continua usando REST; gRPC permanece apenas entre os serviços.

## 9. Erros comuns e identificação

| Sintoma | Como identificar | Correção |
|---|---|---|
| HTTP 401 sem token | Não existe cabeçalho `Authorization` | Gere o token e envie `Authorization: Bearer $TOKEN` |
| HTTP 401 com token | Token foi alterado, expirou ou não usa o mesmo segredo | Gere um token novo com `GeradorTokenJwt` |
| HTTP 400 com token válido | O mapa `erros` indica item vazio ou quantidade inválida | Corrija o JSON sem remover o Bearer token |
| HTTP 503 com token válido | `pedido-service` está parado ou inacessível | Inicie pedidos e confira host, porta 9090 e firewall |
| Erro `UNAVAILABLE` em pedidos | `estoque-service` está parado ou inacessível | Inicie estoque e confira host, porta 9091 e firewall |
| Porta em uso | O log informa falha ao vincular 8080, 9090 ou 9091 | Encerre a instância anterior |
| Funciona localmente, mas não nas VMs | Os canais gRPC ainda apontam para `localhost` | Informe os IPs internos nos argumentos de inicialização |
| Token aparece com logs adicionais | O gateway foi iniciado em vez do gerador autônomo | Use o comando com `PropertiesLauncher` e `GeradorTokenJwt` |
| `java: command not found` na VM | Java 21 não está instalado | Instale `openjdk-21-jdk` dentro da VM |

Se pedidos não iniciar por erro de banco, confira `DB_URL`, `DB_USER` e
`DB_PASSWORD` no mesmo terminal da aplicação. Localmente, use o perfil `local`.
No Cloud SQL, confira credenciais e a autorização do IP de saída da VM de pedidos.
O console H2 local continua em `http://localhost:8081/h2-console`.

## 10. Encerramento e limpeza dos recursos

Dentro das VMs, pressione `Ctrl+C` para encerrar cada aplicação e depois use
`exit` para fechar as sessões SSH.

Desligue as VMs e o banco no Google Console. Na execução local, encerre também
os três processos com `Ctrl+C`.
