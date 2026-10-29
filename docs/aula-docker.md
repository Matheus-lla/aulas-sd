# Aula — Docker e publicação no Google Artifact Registry

## 1. Objetivo da aula

Empacotar o e-commerce em imagens independentes, construir e executar os
microsserviços localmente e publicar as imagens no **Google Artifact Registry**.
Pedidos e Estoque são o foco da prática; o gateway também recebe uma imagem,
pois é o processo que entrega a interface e permite testar o sistema por REST.
O banco PostgreSQL continua no **Cloud SQL**, externo aos contêineres.

Esta aula usa o código atual descrito no [README](../README.md), com estoque
**Python**, pedidos Java e rotas `/api/v1`. Os documentos `aula-5.md`, `aula-6.md`
e `aula-7.md` registram etapas anteriores: seus comandos, estoque Java e rotas
`/pedidos` não representam esta versão. Não alteraremos regras de negócio nem
faremos deploy no Cloud Run.

## 2. Conceitos importantes

- **Imagem:** pacote com aplicação, bibliotecas e ambiente de execução. Uma tag
  identifica uma versão; o digest identifica seu conteúdo.
- **Contêiner:** processo iniciado a partir da imagem, com sua própria visão de
  rede e arquivos. Vários contêineres podem executar a mesma imagem.
- **Dockerfile:** receita que o Docker usa para construir uma imagem.
- **Registry:** serviço que armazena e distribui imagens. Usaremos o Artifact
  Registry para guardar o que será utilizado nas próximas aulas.

Uma máquina virtual executa um sistema operacional convidado com seu próprio
kernel. Contêineres Linux compartilham o kernel do host Docker e isolam processos
por recursos do sistema operacional. No Docker Desktop, esse host Linux roda em
uma VM. O isolamento não elimina dependências, não torna uma rede automaticamente
segura e não garante limites de CPU/memória sem configuração. O usuário sem root
reduz os privilégios do processo da aplicação.

O Dockerfile constrói **camadas**. Se uma instrução e suas entradas não mudam, o
Docker pode reutilizar o cache. Dependências ficam antes dos fontes quando isso
permite reaproveitamento. O **contexto de build** é o conjunto de arquivos que o
builder pode ler; o `.dockerignore` reduz esse conjunto e exclui dados locais e
segredos. Um arquivo ignorado não pode ser recuperado por `COPY`.

A imagem é configurada na execução por **variáveis de ambiente**. Trocar banco,
porta ou destino gRPC não exige recompilar. Uma rede Docker criada pelo usuário
permite resolver os contêineres por seus nomes, como `estoque` e `pedidos`.
`localhost` dentro de cada contêiner aponta para ele próprio.

Em `-p 127.0.0.1:8080:8080`, a primeira porta pertence ao computador do aluno e a
segunda ao contêiner; o endereço restringe a publicação ao próprio computador.
**`EXPOSE` documenta a porta padrão: não publica portas e não configura o servidor.**
É a aplicação que lê `PORT`, `GRPC_PORT` ou `SERVER_PORT` e abre o listener.

Referências: [build em múltiplos estágios](https://docs.docker.com/build/building/multi-stage/),
[contexto e dockerignore](https://docs.docker.com/build/concepts/context/#dockerignore-files)
e [redes bridge](https://docs.docker.com/engine/network/drivers/bridge/).

## 3. Arquitetura e configurações necessárias

```text
Navegador / curl :8080
        |
        v
  gateway (Java, REST + JWT)
        +---- gRPC :9090 ----> pedidos (Java) ---- JDBC ----> Cloud SQL / pedidos
        |                         |
        |                         +---- gRPC :9091 ----+
        +---- gRPC :9091 ------------------------------> estoque (Python)
                                                           |
                                                       Psycopg / PG
                                                           v
                                                    Cloud SQL / estoque
```

| Componente | Dockerfile | Porta padrão no contêiner | Dados persistidos |
|---|---|---|---|
| `pedido-service` | `pedido-service/Dockerfile` | 9090, gRPC HTTP/2 | `pedidos` e `pedido_itens`, banco próprio |
| `estoque-service` | `estoque-service/Dockerfile` | 9091, gRPC HTTP/2 | `produtos`, banco próprio |
| `api-gateway` | `api-gateway/Dockerfile` | 8080, HTTP | Não possui banco |
| `contratos-grpc` | Sem imagem | Sem listener | Biblioteca de contratos `.proto` |

Cada serviço tem um ponto de entrada e ciclo de vida próprios. A separação das
imagens permite iniciar, versionar e configurar cada um sem carregar os demais.
O agregador Maven e a biblioteca compartilhada não são servidores. Os arquivos
de `web` são incluídos no JAR do gateway pelo seu POM, sem processo adicional.

### 3.1. Cloud SQL: configuração, autenticação e rede

O código usa **conexão direta** ao PostgreSQL: JDBC em pedidos e Psycopg no
estoque. Não utiliza Cloud SQL Auth Proxy, Connector ou autenticação IAM de banco.
O login do `gcloud` para publicar imagens não autentica a aplicação no PostgreSQL.
Não é necessário copiar chave de conta de serviço para nenhuma imagem.

Antes da prática, o professor deve disponibilizar o ambiente de demonstração:

1. Instância Cloud SQL em funcionamento, com dois bancos e usuários/senhas já
   definidos e tabelas compatíveis com esta versão.
2. Host/IP e porta PostgreSQL alcançáveis a partir do host Docker. Com IP público,
   o IP de saída dessa máquina precisa estar nas redes autorizadas da instância.
   Com IP privado, é necessária uma rota para a VPC, por exemplo pelo ambiente
   de laboratório já preparado. Docker não cria essa rota.
3. Saída TCP 5432 liberada na rede utilizada e política TLS conhecida. Os exemplos
   usam `sslmode=require`; se o ambiente exigir verificação de identidade ou
   certificados de cliente, siga a configuração de certificados do professor e
   monte os arquivos necessários somente na execução, como leitura. Nunca os
   copie para a imagem. `require` cifra o transporte sem assegurar a identidade
   do servidor como `verify-full` com CA e nome de host verificados.

Configurar a URL escolhe o destino; usuário e senha autenticam no banco; rota,
firewall, redes autorizadas e TLS viabilizam a conexão. São etapas diferentes.
Use o host do banco, **não** o identificador `projeto:região:instância` como host.
Se um ambiente já utilizar proxy externo, seu endereço precisa ser acessível ao
contêiner: `localhost` do contêiner não alcança o proxy do computador. O proxy e
o banco nunca fazem parte da imagem da aplicação. Não introduziremos proxy nem
banco local como requisito desta aula.

Pedidos usa `ddl-auto=update` e estoque chama `CREATE TABLE IF NOT EXISTS` ao
iniciar. Isso foi preservado. Execute os serviços apenas nos bancos didáticos
preparados para esta versão; inicialização pode modificar o schema. As consultas
da seção 6 não alteram dados de negócio. Não execute população, limpeza, reserva
ou criação de pedidos no Cloud SQL real durante a validação desta tarefa.

Referências: [opções de conexão ao Cloud SQL](https://docs.cloud.google.com/sql/docs/postgres/connection-options)
e [diagnóstico de conectividade](https://docs.cloud.google.com/sql/docs/postgres/debugging-connectivity).

### 3.2. Variáveis de execução

As imagens não fixam banco, IP, projeto, credenciais nem URLs dos outros serviços.
Os arquivos de exemplo em `docker/` são fictícios e precisam ser copiados e
ajustados antes da execução. `docker run --env-file` lê esses arquivos; as
aplicações não carregam `.env` automaticamente.

| Variável | Serviço | Obrigatoriedade e padrão |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Pedidos | Defina `cloud` nesta prática; sem definição, usa `local`/H2 |
| `DB_URL` | Pedidos cloud | Obrigatória, URL JDBC PostgreSQL com host, banco e TLS |
| `DB_USER`, `DB_PASSWORD` | Pedidos/estoque cloud | Obrigatórias; sem credenciais de fallback em cloud |
| `DB_PROFILE` | Estoque | Defina `cloud`; padrão anterior `local`/H2 |
| `DB_HOST`, `DB_NAME` | Estoque cloud | Obrigatórias, host e banco PostgreSQL |
| `DB_PORT` | Estoque | 5432 em cloud; 5435 em local |
| `DB_SSLMODE` | Estoque cloud | `require`; adequar à política TLS do ambiente |
| `DB_CONNECT_TIMEOUT` | Estoque | 5 segundos, mínimo 2 |
| `ESTOQUE_GRPC_TARGET` | Pedidos/gateway | `static://localhost:9091`; nesta rede, `static://estoque:9091` |
| `PEDIDO_GRPC_TARGET` | Gateway | `static://localhost:9090`; nesta rede, `static://pedidos:9090` |
| `PORT` | Todos | Opcional localmente; fornecida pelo Cloud Run; em pedidos/estoque tem prioridade sobre `GRPC_PORT` |
| `GRPC_PORT` | Pedidos/estoque | 9090 / 9091, se `PORT` estiver ausente |
| `GRPC_HOST` | Estoque | Na imagem: `0.0.0.0`; fora dela: mantém `127.0.0.1` |
| `GRPC_WORKERS` | Estoque | 4 threads |
| `SERVER_PORT` | Gateway | Configuração nativa Spring; se definida, sobrepõe `server.port`, inclusive a resolução de `PORT`; não combine ambas |
| `APP_USERNAME`, `APP_PASSWORD` | Gateway | `admin` / `admin`, somente demonstração |
| `JWT_SECRET` | Gateway | Segredo didático do `application.properties`; substitua para uso real (HS256, ao menos 32 bytes) |
| `JWT_EXPIRATION_SECONDS` | Gateway | 3600 segundos |
| `GRPC_DEADLINE` | Gateway | 5s; pedidos aguarda até 2s pela reserva no estoque |
| `CORS_ALLOWED_ORIGINS` | Gateway | `http://localhost:3000,http://localhost:5173`; interface servida pelo gateway usa a mesma origem |
| `LOCAL_DB_URL` | Pedidos local | URL H2 em arquivo em `./data/pedidos_demo`; não usada em cloud |

Os listeners Java estão em `0.0.0.0`. Não defina `PORT` nos arquivos principais da
prática: os comandos usam as portas padrão. Para experimentar outra porta,
recrie, por exemplo, o gateway com `-e PORT=8088 -p 127.0.0.1:8080:8088`.
Alterar apenas o lado esquerdo de `-p` não exige alterar a aplicação.
No gateway, use `PORT` ou a variável anterior `SERVER_PORT`, nunca as duas.
No Cloud Run, deixe `SERVER_PORT` ausente e utilize a `PORT` fornecida pela plataforma.

## 4. Leitura dos Dockerfiles

Abra [Pedidos](../pedido-service/Dockerfile), [Estoque](../estoque-service/Dockerfile)
e [Gateway](../api-gateway/Dockerfile). Os comentários explicam as decisões de
cada estágio. As imagens base são oficiais, com versão de linguagem e distribuição
explícitas: Eclipse Temurin 21 em Ubuntu Noble e Python 3.12 slim Bookworm.
Essas tags recebem correções; não são digests imutáveis.

Nos dois serviços Java, o estágio `build` contém JDK 21 e baixa o Maven 3.9.16
especificado no Wrapper. Copia todos os POMs para ler o agregador, mas compila
somente o serviço selecionado e suas dependências com `-pl ... -am verify`.
O cache BuildKit de `/root/.m2` reaproveita dependências entre builds. O teste
existente de contexto roda no build do gateway; pedidos e contratos não possuem
testes próprios. Nenhum Dockerfile desabilita testes.

O estágio final Java contém JRE e **o JAR executável de nome exato** copiado do
build. O JAR de contratos e o `.jar.original` não são pontos de entrada. No gateway,
o build também copia `web`, necessário para os recursos empacotados pelo Maven.

No Python, o build prepara wheels de `requirements-runtime.txt`, instala a
ferramenta adicional de `requirements.txt`, gera o Protobuf pelo script existente
e empacota o serviço em wheel. A imagem final instala os wheels de execução;
`grpcio-tools`, contratos-fonte e script de geração ficam no estágio de build.
Não é necessário ter `.venv` ou arquivos `_pb2.py` gerados no computador do aluno.

Os estágios finais usam UID/GID `10001`, diretório `/app` e `ENTRYPOINT` em forma
exec: Java ou Python recebem os sinais de parada diretamente. Não há shell
expandindo variáveis ou selecionando JARs por curinga. Logs seguem para
stdout/stderr, consultados por `docker logs`.

**Durante o build** são baixadas dependências, gerados contratos e compilados os
artefatos. Nenhum serviço acessa o Cloud SQL. **Durante a execução**, o processo
lê o ambiente, conecta ao banco e inicia seu listener. Construir com sucesso não
prova conectividade de banco ou comunicação entre os serviços.

Os três builds usam **a raiz do repositório como contexto**, indicada pelo `.`
final nos comandos. `-f` escolhe o Dockerfile, sem mudar o contexto. Executar
`docker build pedido-service` perde o POM raiz e os contratos compartilhados.
O [.dockerignore](../.dockerignore) da raiz admite somente entradas do build,
preservando Wrapper, módulos e `web`, e excluindo artefatos, ambientes, bancos,
arquivos de credenciais e arquivos gerados localmente.

Referências: [boas práticas de build](https://docs.docker.com/build/building/best-practices/),
[imagem Temurin](https://hub.docker.com/_/eclipse-temurin) e
[imagem Python](https://hub.docker.com/_/python).

## 5. Como construir as imagens

### 5.1. Pré-requisitos

Na máquina local, abra Bash na **raiz deste repositório**, onde estão `pom.xml`,
`mvnw` e `.dockerignore`. Todos os comandos seguintes são executados nessa raiz,
no mesmo terminal, salvo indicação explícita. No Windows, use Bash no WSL com
integração Docker Desktop.

São necessários Docker Engine/Desktop em execução, BuildKit/Buildx, acesso à
internet para imagens/dependências e espaço em disco. Java, Maven e Python locais
não são necessários para construir as imagens. Para a prática completa, prepare
também o acesso ao Cloud SQL e instale Google Cloud CLI. `curl` e `python3` serão
usados para verificar a API e extrair o token no host.

```bash
pwd
test -f pom.xml && test -f .dockerignore
docker version
docker buildx version
docker info --format '{{.OSType}}/{{.Architecture}}'
gcloud version
curl --version
python3 --version
```

`docker version` deve mostrar Client e Server. Não use `sudo docker` para parte
da prática e Docker sem sudo em outra parte: a configuração de autenticação pode
ficar em usuários diferentes.

### 5.2. Build e conferência

Na raiz, defina a versão local e construa as três imagens:

```bash
export TAG='aula-docker-v1'
docker build --platform linux/amd64 -f pedido-service/Dockerfile -t "ecommerce/pedido-service:$TAG" .
docker build --platform linux/amd64 -f estoque-service/Dockerfile -t "ecommerce/estoque-service:$TAG" .
docker build --platform linux/amd64 -f api-gateway/Dockerfile -t "ecommerce/api-gateway:$TAG" .

docker image ls 'ecommerce/*'
docker image inspect "ecommerce/pedido-service:$TAG" "ecommerce/estoque-service:$TAG" "ecommerce/api-gateway:$TAG" \
  --format '{{.RepoTags}} | {{.Os}}/{{.Architecture}} | usuário={{.Config.User}} | entrada={{json .Config.Entrypoint}}'
```

O resultado deve conter três imagens `linux/amd64`, usuário `10001:10001` e o
comando do serviço. Os JARs gerados no host não são utilizados pelo Docker.

O Cloud Run exige imagem Linux com executável compatível com x86_64; por isso
especificamos `linux/amd64` desde o início. Em Apple Silicon ou outro host ARM,
Docker Desktop normalmente fornece emulação. O build e a execução podem ser mais
lentos. Se o builder não conseguir carregar a imagem localmente, substitua cada
`docker build` por `docker buildx build --load`, mantendo os demais argumentos.
Em Linux ARM sem emulação preparada, use um builder amd64 disponibilizado pelo
laboratório ou peça a preparação da emulação ao responsável. Uma imagem arm64
não se torna amd64 ao mudar sua tag. Não é necessário publicar uma imagem
multi-arquitetura para esta aula.

Referências: [build para múltiplas plataformas](https://docs.docker.com/build/building/multi-platform/)
e [contrato de execução do Cloud Run](https://docs.cloud.google.com/run/docs/container-contract).

## 6. Como executar e verificar localmente

### 6.1. Preparar o ambiente e a rede

Na raiz, copie os exemplos uma única vez:

```bash
cp docker/pedidos.env.example docker/pedidos.env
cp docker/estoque.env.example docker/estoque.env
cp docker/gateway.env.example docker/gateway.env
chmod 600 docker/pedidos.env docker/estoque.env docker/gateway.env
```

Edite os três arquivos com um editor local. Substitua `cloud-sql.example.invalid`,
os nomes dos bancos e as credenciais fictícias pelos valores **do ambiente
didático** fornecidos pelo professor. Cada serviço pode ter usuário/senha próprios.
Não coloque aspas ao redor dos valores em arquivos `--env-file`, nem use
`$OUTRA_VARIAVEL`: o Docker não faz interpolação de shell ali.
Não publique os arquivos preenchidos: `*.env` está ignorado pelo Git e pelo build.

```bash
docker network create ecommerce-aula
```

Os nomes `estoque`, `pedidos` e `gateway` usados a seguir são resolvidos pela rede
`ecommerce-aula`. Não use `localhost` como destino dos outros serviços. O banco
continua remoto, pelo host configurado, sem contêiner de PostgreSQL ou proxy.

### 6.2. Iniciar os três serviços

Na raiz e no terminal que contém `TAG`, execute um comando por vez:

```bash
docker run -d --name estoque --network ecommerce-aula --platform linux/amd64 \
  --env-file docker/estoque.env \
  -p 127.0.0.1:9091:9091 "ecommerce/estoque-service:$TAG"
docker logs -f estoque
```

Aguarde `Estoque Python iniciado: gRPC em 0.0.0.0:9091, banco cloud`.
Pressione Ctrl+C para sair do acompanhamento de logs; o contêiner continua ativo.
Se o banco não estiver acessível, o processo pode encerrar antes de abrir gRPC.

```bash
docker run -d --name pedidos --network ecommerce-aula --platform linux/amd64 \
  --env-file docker/pedidos.env \
  -p 127.0.0.1:9090:9090 "ecommerce/pedido-service:$TAG"
docker logs -f pedidos
```

Aguarde o perfil `cloud`, a conexão e a mensagem `Started PedidoServiceApplication`.
Saia dos logs com Ctrl+C e inicie o gateway:

```bash
docker run -d --name gateway --network ecommerce-aula --platform linux/amd64 \
  --env-file docker/gateway.env \
  -p 127.0.0.1:8080:8080 "ecommerce/api-gateway:$TAG"
docker logs -f gateway
```

Aguarde `Started EcommerceApplication` e saia dos logs com Ctrl+C.
As portas gRPC foram publicadas para inspeção no host, mas o tráfego entre os
contêineres usa diretamente a rede Docker e não depende dessas publicações.
Um serviço pode iniciar e ainda falhar ao chamar outro: o gateway não verifica
os servidores gRPC durante sua inicialização.

```bash
docker ps -a --filter network=ecommerce-aula
docker logs --tail 50 estoque
docker logs --tail 50 pedidos
docker logs --tail 50 gateway
```

### 6.3. Verificações funcionais sem alterar dados de negócio

Na raiz, com os serviços ativos, confira a interface e a autenticação:

```bash
curl -fsS -o /dev/null -w 'Interface: %{http_code}\n' http://localhost:8080/
curl -i http://localhost:8080/api/v1/produtos
```

Espere 200 para a interface e 401 para a API sem JWT. O primeiro resultado
confere que os arquivos de `web` foram incluídos na imagem. Nenhum deles prova
acesso ao Cloud SQL.

Para o login abaixo, o exemplo mantém `admin` / `admin` do arquivo fictício
do gateway. Se alterou essas credenciais, use os valores correspondentes.

```bash
LOGIN_JSON=$(curl -fsS -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usuario":"admin","senha":"admin"}')
TOKEN=$(printf '%s' "$LOGIN_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

curl -fsS -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/produtos \
  -H "Authorization: Bearer $TOKEN"
curl -fsS -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/pedidos \
  -H "Authorization: Bearer $TOKEN"
```

Espere 200 e listas JSON, inclusive `[]` se os bancos não têm registros para a
consulta. Produtos verifica gateway → estoque → banco de estoque; pedidos
verifica gateway → pedidos → banco de pedidos. Isso atesta interoperabilidade
entre Java e Python e acesso aos dois bancos sem reservar ou inserir dados.

As chamadas REST de leitura não passam pelo canal pedidos → estoque. Para
verificar a conectividade desse canal também sem baixar estoque, faça um teste
TCP a partir do contêiner de pedidos:

```bash
docker exec pedidos bash -c 'timeout 5 bash -c "</dev/tcp/estoque/9091"'
```

Saída 0 comprova resolução do nome e acesso TCP a partir de pedidos, sem executar
uma reserva. Esse teste não comprova o fluxo de criação de pedido. A única
operação atual que exercita pedidos → estoque por gRPC é `CriarPedido`, que
altera ambos os bancos. Reserve esse teste para um ambiente descartável
autorizado; não o execute no Cloud SQL real nesta validação.

Abra também [http://localhost:8080](http://localhost:8080), faça login e consulte
catálogo e pedidos existentes. Não confirme compras nesta prática de leitura.
Se o Cloud SQL estiver indisponível, registre o erro e a etapa alcançada: imagem
construída ou gateway acessível não significam sistema funcional completo.

### 6.4. Encerramento

Na raiz, encerre somente os recursos criados nesta prática:

```bash
docker stop --timeout 15 gateway pedidos estoque
docker rm gateway pedidos estoque
docker network rm ecommerce-aula
unset TOKEN LOGIN_JSON
```

As imagens permanecem para publicação. A remoção dos contêineres não remove dados
do Cloud SQL. Os perfis locais H2 anteriores continuam disponíveis para execução
sem Docker conforme o README; dados no sistema de arquivos de um contêiner são
descartáveis e não devem ser usados como persistência futura no Cloud Run.

## 7. Publicação no Google Artifact Registry

Esta seção é executada **pelos alunos durante a aula**, na raiz do repositório,
após revisar o projeto e as permissões. Os comandos não fazem deploy. A conta
do Google usada aqui é independente da credencial de login do e-commerce e dos
usuários PostgreSQL.

### 7.1. Autenticar e conferir o projeto

```bash
gcloud auth login
gcloud auth list --filter=status:ACTIVE
gcloud projects list --format='table(projectId,name)'
read -r -p 'ID do projeto didático exibido na lista: ' PROJECT_ID
export PROJECT_ID
export REGION='southamerica-east1'
export REPOSITORY='ecommerce-aula'
export TAG='aula-docker-v1'
gcloud config set project "$PROJECT_ID"
gcloud config get-value project
gcloud projects describe "$PROJECT_ID" --format='table(projectId,name,lifecycleState)'
```

Use o **ID**, não o nome de exibição ou número do projeto. Região e repositório
são valores completos sugeridos para a aula; ajuste-os aos fornecidos pelo
professor, especialmente se já houver um repositório. Mantenha as variáveis no
mesmo terminal. Os comandos seguintes incluem `--project` para explicitar o destino.

### 7.2. API, permissões e repositório

O projeto precisa ter faturamento e Artifact Registry API habilitados, conforme
as políticas do laboratório. A publicação requer
`roles/artifactregistry.writer` **no repositório**, que permite enviar, aplicar
tags e ler as imagens. Esse papel não cria repositórios. Somente leitura requer
`roles/artifactregistry.reader`. Não solicite Owner ou Editor.

A criação de repositórios deve ficar com o responsável pelo ambiente, que tenha
`artifactregistry.repositories.create` (por exemplo, o papel específico
`roles/artifactregistry.admin` no projeto). Habilitar a API exige
`serviceusage.services.enable`, como no papel `roles/serviceusage.serviceUsageAdmin`.
Esses privilégios de preparação não são necessários para cada aluno que apenas
publicará em um repositório existente. Não alteraremos IAM nesta prática.

Na raiz, confira a API e os repositórios visíveis para sua conta:

```bash
gcloud services list --enabled --project="$PROJECT_ID" \
  --filter='config.name:artifactregistry.googleapis.com'
gcloud artifacts repositories list --project="$PROJECT_ID" --location="$REGION"
```

Se a API estiver desabilitada, **o responsável autorizado pela preparação** pode
habilitá-la. Não execute esta preparação se o recurso já foi disponibilizado:

```bash
gcloud services enable artifactregistry.googleapis.com --project="$PROJECT_ID"
```

Se o repositório não existir, **somente o responsável com permissão de criação**
executa, na raiz:

```bash
gcloud artifacts repositories create "$REPOSITORY" \
  --repository-format=docker --location="$REGION" --project="$PROJECT_ID" \
  --description='Imagens do e-commerce para a aula de Sistemas Distribuidos'
```

Confira o destino antes de continuar; ele deve ser um repositório Docker padrão,
na região e projeto definidos:

```bash
gcloud artifacts repositories describe "$REPOSITORY" \
  --location="$REGION" --project="$PROJECT_ID"
```

Referências: [criar repositórios](https://docs.cloud.google.com/artifact-registry/docs/repositories/create-repos)
e [habilitar o serviço](https://docs.cloud.google.com/artifact-registry/docs/enable-service).

### 7.3. Autenticar Docker, aplicar tags e enviar

Na raiz, configure o credential helper para **esse host regional**:

```bash
gcloud auth configure-docker "${REGION}-docker.pkg.dev"
export REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}"
printf 'Destino: %s\nVersão: %s\n' "$REGISTRY" "$TAG"

docker tag "ecommerce/pedido-service:$TAG" "$REGISTRY/pedido-service:$TAG"
docker tag "ecommerce/estoque-service:$TAG" "$REGISTRY/estoque-service:$TAG"
docker tag "ecommerce/api-gateway:$TAG" "$REGISTRY/api-gateway:$TAG"

docker push "$REGISTRY/pedido-service:$TAG"
docker push "$REGISTRY/estoque-service:$TAG"
docker push "$REGISTRY/api-gateway:$TAG"
```

O formato de cada referência é
`REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/IMAGE:TAG`.
Por exemplo, com o projeto fictício `sd-ecommerce-demo`, Pedidos seria
`southamerica-east1-docker.pkg.dev/sd-ecommerce-demo/ecommerce-aula/pedido-service:aula-docker-v1`.
Os comandos usam o projeto real lido na seção 7.1, sem exigir substituições
adicionais. A tag local e a remota identificam a mesma imagem; aplicar tag não
recompila. Use uma nova versão, como `aula-docker-v2`, ao alterar os fontes e
reconstruir. Evite sobrescrever versões já usadas por outra turma.

### 7.4. Conferir a publicação

Na raiz, confira as três imagens e suas tags/digests:

```bash
gcloud artifacts docker images list "$REGISTRY" --include-tags --project="$PROJECT_ID"
gcloud artifacts docker tags list "$REGISTRY/pedido-service" --project="$PROJECT_ID"
gcloud artifacts docker tags list "$REGISTRY/estoque-service" --project="$PROJECT_ID"
gcloud artifacts docker tags list "$REGISTRY/api-gateway" --project="$PROJECT_ID"
```

No Console, abra **Artifact Registry > Repositórios > ecommerce-aula** na região
escolhida e confirme Pedidos, Estoque e Gateway com a versão publicada. Guarde os
digests apresentados para identificar exatamente as imagens da próxima aula.
Publicar no registry não inicia contêineres nem torna a API acessível na nuvem.

Referências: [autenticação Docker](https://docs.cloud.google.com/artifact-registry/docs/docker/authentication)
e [tags, push e conferência](https://docs.cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling).

## 8. Preparação para as próximas aulas: Cloud Run

As três imagens possuem processo em primeiro plano, logs padrão e usuário sem
root. Gateway atende HTTP; Pedidos e Estoque usam **gRPC sobre HTTP/2**, não RMI
ou sockets de protocolo próprio. A entrada gRPC é aceita pelo Cloud Run.
Os listeners respeitam `PORT` e escutam em `0.0.0.0` na execução das imagens.
Esses pontos atendem requisitos de empacotamento, mas não comprovam implantação.

O serviço gerenciado termina TLS e encaminha tráfego sem TLS ao contêiner.
Para gRPC, deve-se planejar HTTP/2 até o contêiner (h2c). Os clientes Java atuais
usam alvos `static://...:9090/9091` e a negociação padrão plaintext do Spring gRPC
do projeto. Endereços Docker só existem na rede local. Nas próximas aulas será
necessário definir os destinos gerenciados, normalmente domínio e porta 443,
negociação TLS dos canais e autenticação entre serviços privados, incluindo ID
token e autorização de invocação quando aplicável. O JWT HS256 do usuário no
gateway não substitui a identidade de serviço exigida pelo Cloud Run.

Também será necessário preparar conectividade/identidade e segredos de acesso ao
Cloud SQL, considerar capacidade de conexões e a inicialização do schema. O perfil
`cloud` mantém os dados fora do contêiner. O perfil H2 em arquivo não fornece
persistência compartilhada ou durável no Cloud Run. Esta tarefa preserva os
clientes e contratos atuais e não implementa esses passos de deploy.

Referências: [contrato de execução](https://docs.cloud.google.com/run/docs/container-contract)
e [gRPC no Cloud Run](https://docs.cloud.google.com/run/docs/triggering/grpc).

## 9. Erros comuns e identificação

| Sintoma | Como identificar | Correção |
|---|---|---|
| `COPY ... not found` | POM raiz, contratos ou `web` ausentes | Execute o build na raiz, com `-f .../Dockerfile` e `.` final |
| JAR ausente ou sem main | Nome de artefato ou versão foi alterado | Confira o POM e atualize o caminho explícito no Dockerfile; não copie `.jar.original` |
| Build Maven falha em teste | Log do estágio `build` contém falha de `verify` | Leia o erro e reproduza `./mvnw clean verify` com JDK 21; não omita testes para esconder a falha |
| `ModuleNotFoundError` de Protobuf | Geração não ocorreu ou usou contexto incorreto | Reconstrua com o Dockerfile Python; ele executa `gerar-proto.sh` com o contrato da raiz |
| `permission denied ... docker.sock` | Client aparece, Server não | Use um ambiente Docker autorizado pelo laboratório; não altere permissões do socket para acesso irrestrito |
| Porta ocupada | `port is already allocated` | Encerre o processo antigo ou altere apenas a porta do host, por exemplo `-p 127.0.0.1:8082:8080` |
| Nome de contêiner já existe | `name ... is already in use` | Confira `docker ps -a`; remova apenas o contêiner da prática antes de recriá-lo |
| `UNAVAILABLE` / HTTP 503 | Serviço parado, nome ou destino incorreto | Confira logs, rede comum e os alvos `static://estoque:9091` / `static://pedidos:9090` |
| `localhost` funciona fora, falha dentro | Destino aponta para o próprio contêiner | Use nome Docker para outro serviço e endereço alcançável para o banco externo |
| Banco obrigatório ausente | Variável não resolvida ou mensagem de configuração cloud | Preencha todos os campos do arquivo correto e recrie o contêiner; editar o arquivo não modifica um processo ativo |
| Conexão Cloud SQL expira | Timeout antes de abrir gRPC | Confira host, porta, rota/VPC, IP de saída autorizado e firewall; aumentar timeout não libera rede |
| PostgreSQL rejeita senha/TLS | `authentication failed` ou erro de certificado | Confira credencial de banco e política TLS, separadamente do login `gcloud`; não desative TLS remoto |
| `contains null values` / coluna ausente | Banco contém tabelas de outra aula | Use o banco didático preparado para esta versão e consulte o README; não apague dados nem use `ddl-auto=create` |
| Login funciona, catálogo não | 200 no login e 503/504 na consulta | JWT não prova acesso gRPC/SQL; examine estoque e conectividade do banco |
| `Unauthenticated` no push | Host regional ausente no credential helper | Confira conta ativa e repita `gcloud auth configure-docker "${REGION}-docker.pkg.dev"` no mesmo usuário do Docker |
| `Permission denied` no push/criação | Conta autenticada sem autorização suficiente | Writer permite publicar no repositório; criação/API exigem preparação pelo responsável |
| Repositório não encontrado | Região, projeto ou nome incorretos | Execute `repositories describe` e confira `REGISTRY` antes de repetir push |
| `exec format error` / imagem arm64 | `docker image inspect` mostra arquitetura diferente | Reconstrua para `linux/amd64` com builder/emulação adequados, e teste com `--platform linux/amd64` |

## 10. Critérios de conclusão

- Três imagens construídas a partir dos fontes, com tags explícitas, arquitetura
  `linux/amd64`, entrada correta e usuário sem root.
- Três contêineres executados com a configuração do ambiente didático, na rede
  `ecommerce-aula`; portas e logs conferidos.
- Interface e autenticação verificadas; leituras de produtos e pedidos passaram
  pelas chamadas gRPC até os bancos disponíveis. Resultado e dependências
  indisponíveis registrados sem declarar como validada uma etapa não executada.
- Canal de rede pedidos → estoque verificado sem criar pedidos ou reservar dados
  reais. A execução do fluxo de compra permanece dependente de ambiente descartável.
- Pedidos, Estoque e Gateway publicados no Artifact Registry e versões/digests
  conferidos. O professor pode encerrar a etapa local e realizar a publicação
  posteriormente se a conta ou a rede de nuvem estiver indisponível.
- Contêineres e rede da prática removidos, dados remotos preservados e nenhuma
  implantação no Cloud Run realizada.

As verificações efetivamente executadas durante a preparação deste repositório
estão em [validacao-docker.md](validacao-docker.md). Os resultados históricos da
versão anterior estão em [validacao-versao-final.md](validacao-versao-final.md).
