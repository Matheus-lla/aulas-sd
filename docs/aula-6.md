# Aula 6 — Validação e persistência de pedidos com Spring Data JPA

## 1. Objetivo da aula

Validar os dados recebidos em `POST /pedidos` com Bean Validation e salvar os
pedidos válidos em PostgreSQL usando Spring Data JPA. A aplicação responde
`201 Created` depois da gravação; pedidos inválidos recebem `400 Bad Request`
com os erros dos campos e não são enviados ao banco.

Na demonstração em nuvem, a aplicação roda em uma VM do Compute Engine e o banco
em uma instância do Cloud SQL for PostgreSQL. A criação dos recursos e toda a
configuração de rede e firewall são feitas pelo Google Cloud Console no browser.

## 2. Conceitos importantes

- `PedidoRequest` é o DTO que transporta `item` e `quantidade` do JSON.
- `@Valid` solicita ao Spring a validação do DTO antes de executar o controller.
- `@NotBlank` exige que `item` não seja nulo, vazio ou composto somente por espaços.
- `@NotNull` exige que `quantidade` não seja nula; `@Positive` exige valor maior que zero.
- `ValidacaoExceptionHandler` transforma `MethodArgumentNotValidException` em
  `400 Bad Request`, com uma mensagem e os erros por campo.
- JPA é a especificação de mapeamento entre objetos Java e tabelas. Hibernate é
  a implementação usada neste projeto; Spring Data JPA fornece o repository.
- `Pedido` é a única entidade: `@Entity` identifica a classe persistida e
  `@Table(name = "pedidos")` define a tabela. Cada objeto salvo corresponde a uma linha.
- `@Id` define a chave primária; `@GeneratedValue(strategy = GenerationType.UUID)`
  faz o Hibernate gerar o UUID. O ID da resposta é o mesmo salvo no banco.
- `@Column(nullable = false)` define colunas obrigatórias. O construtor sem
  argumentos permite ao JPA reconstruir a entidade ao consultar o banco.
- `PedidoRepository` estende `JpaRepository<Pedido, UUID>`. O Spring fornece a
  implementação de métodos como `save`, `findAll` e `count`.
- A injeção pelo construtor disponibiliza o repository ao controller. O método
  `save` executa a persistência em uma transação; uma falha na gravação impede
  o retorno de sucesso.
- O driver JDBC do PostgreSQL permite que a aplicação se comunique com o banco.

O exemplo tem somente **uma entidade e um repository**, sem camada de serviço,
relacionamentos ou endpoints adicionais. O DTO continua separado da entidade:
o cliente envia apenas item e quantidade, enquanto a aplicação define ID e status.

## 3. Arquitetura e configurações necessárias

```text
Postman / curl
  |
  | POST /pedidos + JSON (porta 8080)
  v
Spring MVC: @Valid em PedidoRequest
  |
  +-- inválido -> ValidacaoExceptionHandler -> 400 + erros
  |
  +-- válido -> PedidoController -> PedidoRepository.save(Pedido)
                                      |
                                      v
                                 JPA / Hibernate
                                      |
                                      | JDBC (porta 5432)
                                      v
                                  PostgreSQL
                                      |
                                      +-- gravação concluída -> 201 + ID salvo
```

Estrutura principal:

```text
src/main/java/aula/sd/ecommerce/
├── EcommerceApplication.java
├── PedidoRequest.java
├── PedidoController.java
├── Pedido.java
├── PedidoRepository.java
└── ValidacaoExceptionHandler.java

src/main/resources/
├── application.properties
└── application-local.properties
```

O `pom.xml` inclui `spring-boot-starter-data-jpa` e o driver `postgresql`, além
das dependências web e de validação já utilizadas. O driver H2 usa escopo `runtime`
para permitir executar a aplicação localmente e também os testes com banco em memória.

Configuração padrão para PostgreSQL em `application.properties`:

```properties
spring.application.name=ecommerce
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

| Variável      | Significado                              | Exemplo                                      |
|---------------|------------------------------------------|----------------------------------------------|
| `DB_URL`      | URL JDBC com host, porta e nome do banco | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USER`     | Usuário do PostgreSQL                    | `postgres`                                   |
| `DB_PASSWORD` | Senha desse usuário                      | Informada no terminal, sem gravar no código  |

Na execução com PostgreSQL, o banco `ecommerce` e o usuário precisam existir antes
da inicialização. Com
`ddl-auto=update`, o Hibernate cria a tabela `pedidos` se ela não existir e aplica
atualizações ao esquema conforme o mapeamento. O usuário precisa ter permissão de
criação de tabelas no esquema `public`. Reiniciar a aplicação preserva os pedidos.
Essa opção simplifica a aula; em produção, a evolução do esquema deve ser feita
com migrações controladas. Consulte a
[documentação de inicialização do Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

`open-in-view=false` encerra o uso do contexto de persistência na camada de acesso
a dados. O Spring Boot configura a conexão e encontra a entidade e o repository
no pacote da aplicação, sem classes extras de configuração.

A tabela contém `id` (UUID), `item` (texto), `quantidade` (inteiro) e `status`
(texto). O status inicial é `CRIADO`, definido no construtor de `Pedido`.
A API continua na porta 8080, padrão do Spring Boot.

## 4. Como compilar e executar os testes automatizados

Com Java 21 instalado, execute na raiz do projeto:

```bash
./mvnw clean verify
```

O comando compila, executa os testes e gera:

```text
target/ecommerce-0.0.1-SNAPSHOT.jar
```

O teste `contextLoads` sobe o contexto Spring com o perfil `local`, ativado por
`@ActiveProfiles("local")`, usando H2 em memória. Ele verifica a inicialização
da aplicação, incluindo JPA e repository, sem exigir PostgreSQL, GCP ou variáveis
de conexão. O fluxo HTTP e a gravação dos pedidos são conferidos nos testes
manuais das seções seguintes.

## 5. Como executar localmente

Com Java 21 instalado, na raiz do projeto, execute:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Esse comando inicia a API e o H2 em memória no mesmo processo. Não é necessário
instalar PostgreSQL, subir containers, acessar o GCP nem definir `DB_URL`,
`DB_USER` ou `DB_PASSWORD`. Na primeira execução, o Maven precisa baixar as
dependências; a aplicação e o banco funcionam inteiramente na máquina local.

O perfil `local` carrega `application-local.properties`, que substitui as
propriedades de conexão da configuração padrão:

```properties
spring.datasource.url=jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false
```

O Hibernate cria a tabela `pedidos` automaticamente. Cada pedido válido é salvo
no H2 e recebe um ID, usando a mesma entidade e o mesmo repository. **Os dados
locais são temporários: ao encerrar e iniciar novamente a aplicação, o banco
começa vazio.** O modo de compatibilidade do H2 não substitui a validação com
PostgreSQL na etapa em nuvem.

Para executar o JAR gerado na seção 4 com a mesma configuração local:

```bash
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Escolha uma das duas formas de execução para não ocupar a porta 8080 duas vezes.
No IntelliJ, também é possível definir `local` no campo **Active profiles** da
configuração de execução Spring Boot, ou passar `--spring.profiles.active=local`
como argumento da aplicação.

Sem o perfil `local`, a aplicação usa PostgreSQL e exige as variáveis de conexão,
conforme a seção 7. Na VM, execute sem ativar esse perfil para gravar no Cloud SQL.

Aguarde a aplicação iniciar na porta 8080. Em outro terminal, teste:

Pedido válido:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

Item vazio:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"","quantidade":2}'
```

Quantidade zero:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":0}'
```

### 5.1. Ver o banco H2 pelo navegador

Com a aplicação em execução com o perfil `local`, abra
[http://localhost:8080/h2-console](http://localhost:8080/h2-console).
Se ela já estava rodando antes de habilitar o console, reinicie-a.

Na tela de login, preencha:

| Campo        | Valor                                                   |
|--------------|---------------------------------------------------------|
| Driver Class | `org.h2.Driver`                                         |
| JDBC URL     | `jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1` |
| User Name    | `sa`                                                    |
| Password     | Deixe vazio                                             |

Clique em **Connect**. Use exatamente a URL acima para acessar o mesmo banco
em memória da aplicação; não use a URL de banco em arquivo sugerida pelo console.

No editor SQL, execute com **Run**:

```sql
SELECT *
FROM pedidos;
```

Envie um pedido válido pelo Postman ou curl e execute a consulta novamente para
ver a nova linha. Depois envie um pedido inválido e confira que a contagem não
mudou. A tabela também aparece no painel lateral do console.

O console está habilitado somente no perfil `local`, com acesso restrito à própria
máquina. Ele acompanha o ciclo de vida da aplicação: ao encerrá-la, o console
fica indisponível e os dados em memória são perdidos.

No Spring Boot 4, além de `spring.h2.console.enabled=true`, é necessária a
dependência `spring-boot-h2console`, já incluída no `pom.xml`. Referência:
[console web do H2 no Spring Boot](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.h2-web-console).

## 6. Resultados esperados

Pedido válido:

```text
HTTP/1.1 201
Content-Type: application/json

{"id":"UUID_GERADO","status":"CRIADO","mensagem":"Pedido criado com sucesso"}
```

Cada POST válido insere uma nova linha, mesmo que o corpo seja repetido. O log
mostra `Pedido salvo: id=UUID_GERADO, item=notebook, quantidade=2`.

Item vazio:

```text
HTTP/1.1 400
Content-Type: application/json

{"mensagem":"Dados do pedido inválidos","erros":{"item":"não deve estar vazio"}}
```

Quantidade zero:

```text
HTTP/1.1 400
Content-Type: application/json

{"mensagem":"Dados do pedido inválidos","erros":{"quantidade":"deve ser maior que zero"}}
```

A ordem das propriedades JSON pode variar. As duas requisições inválidas não
inserem linhas no banco.

## 7. PostgreSQL e aplicação no Google Cloud

Após criar a instância Cloud SQL com **IP público** e inicializar a VM, autorize
a conexão pelo Google Cloud Console:

1. Em **Compute Engine > Instâncias de VM**, copie o **IP externo da VM**.
2. Em **Cloud SQL > sua instância > Conexões > Rede**, localize **Redes autorizadas** e adicione `IP_PUBLICO_DA_VM/32`,
   com um nome como `vm-aula`.
3. Salve e aguarde a alteração terminar. Use o IP da VM, de onde sai a conexão
   da aplicação.

Usaremos o usuário existente `postgres` e a sua senha, sem criar outro usuário.
E usaremos também o banco padrão `postgres`.

### 7.1. Subindo a aplicação

Atualize o projeto e compile o novo codigo.

```bash
git pull
git checkout aula-6
./mvnw clean verify
```

Para subir o sistema e conectar ao banco, primeiro deve ser exportado as
variáveis de ambiente com as configurações do banco de dados.

```bash
export DB_URL='jdbc:postgresql://IP_PUBLICO_DO_BANCO:5432/postgres?sslmode=require'
export DB_USER='postgres'
export DB_PASSWORD='SENHA_DO_BANCO'
./mvnw spring-boot:run
```

Substitua `IP_PUBLICO_DO_BANCO` pelo IP publico da instância do Cloud SQL. A URL usa o **IP do
banco**, não o IP da VM nem o nome de conexão `projeto:região:instância`.

Substitua `SENHA_DO_BANCO` pela senha gerada na criação da instância do banco.

Na máquina local, teste usando o IP externo da **VM**:

```bash
curl -i -X POST http://IP_PUBLICO_DA_VM:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

### 7.2. Conferir os registros no banco

No **Cloud SQL Studio**, conecte ao banco e execute:

```sql
SELECT *
FROM pedidos;
```

Compare o `id` da resposta HTTP com a linha da tabela. Repita os testes de item
vazio e quantidade zero: a contagem deve permanecer igual. Reinicie a aplicação
e consulte novamente para mostrar que os registros continuam no PostgreSQL.

## 8. Como testar pelo Postman

Com a aplicação em execução na VM, abra o Postman na máquina local e crie uma
requisição HTTP, seguindo o mesmo procedimento da aula 5:

1. Selecione o método **POST**.
2. Informe `http://IP_PUBLICO_DA_VM:8080/pedidos`, substituindo o IP pelo endereço
   externo da VM exibido no Console.
3. Na aba **Body**, selecione **raw** e escolha o formato **JSON**.
4. Insira o corpo:

```json
{
  "item": "notebook",
  "quantidade": 2
}
```

`item` é uma string e `quantidade` é um inteiro, enviado sem aspas. Não envie ID
nem status: esses valores são definidos pela aplicação.

Na aba **Headers**, confira o cabeçalho que o Postman normalmente adiciona ao
selecionar JSON:

| Key            | Value              |
|----------------|--------------------|
| `Content-Type` | `application/json` |

Clique em **Send**. O resultado esperado é **201 Created**:

```json
{
  "id": "UUID_GERADO",
  "status": "CRIADO",
  "mensagem": "Pedido criado com sucesso"
}
```

Confira no Cloud SQL Studio se o ID retornado existe na tabela `pedidos`, usando
a consulta da seção 7.1. Depois altere o corpo e envie cada cenário:

| Cenário            | Body (raw / JSON)                    | Resultado esperado                                 | Efeito no banco  |
|--------------------|--------------------------------------|----------------------------------------------------|------------------|
| Item vazio         | `{"item":"","quantidade":2}`         | 400, `erros.item`: `não deve estar vazio`          | Nenhuma inserção |
| Quantidade zero    | `{"item":"notebook","quantidade":0}` | 400, `erros.quantidade`: `deve ser maior que zero` | Nenhuma inserção |
| Quantidade ausente | `{"item":"notebook"}`                | 400, `erros.quantidade`: `não deve ser nula`       | Nenhuma inserção |

O erro de item vazio tem este corpo:

```json
{
  "mensagem": "Dados do pedido inválidos",
  "erros": {
    "item": "não deve estar vazio"
  }
}
```

Confira novamente a contagem no banco após os casos inválidos. Para testar a
aplicação na própria máquina, iniciada com o perfil `local` da seção 5, use
`http://localhost:8080/pedidos`, mantendo método, cabeçalho e corpos.

## 9. Erros comuns e identificação

| Sintoma                                     | Como identificar                                       | Correção                                                                                          |
|---------------------------------------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Aplicação não inicia sem configuração       | Log indica URL JDBC inválida ou variável não resolvida | Exporte `DB_URL`, `DB_USER` e `DB_PASSWORD` no mesmo terminal da aplicação                        |
| Conexão com o banco expira                  | Log contém `Connect timed out`                         | Confira IP público do banco, IP de saída da VM nas redes autorizadas e saída TCP 5432 (seção 7)   |
| Execução local tenta conectar ao PostgreSQL | Log não mostra o perfil `local` ativo                  | Use o comando da seção 5 com `-Dspring-boot.run.profiles=local`                                   |
| `password authentication failed`            | PostgreSQL rejeita as credenciais                      | Confira o usuário no Console e informe novamente a senha na VM                                    |
| `database "ecommerce" does not exist`       | Instância existe, mas o banco não                      | Crie `ecommerce` em Bancos de dados no Console                                                    |
| `permission denied for schema public`       | Hibernate não consegue criar a tabela                  | Confira se conectou ao banco escolhido com o usuário `postgres` e se ele tem permissão no esquema |
| Falha de SSL ou certificado                 | Log acusa exigência TLS/certificado                    | Use `sslmode=require` e o modo TLS sem certificado de cliente da seção 7                          |
| Pedido inválido retorna 400                 | Mapa `erros` identifica o campo                        | Envie item com texto e quantidade maior que zero                                                  |
| Pedido válido retorna 500                   | Log mostra erro de persistência                        | Confira disponibilidade e permissões do banco; a API só retorna 201 após salvar                   |
| Porta 8080 já está em uso                   | Log contém `Port 8080 was already in use`              | Encerre a outra aplicação nessa porta                                                             |
| Postman não acessa a VM                     | Teste na VM funciona, mas o acesso externo expira      | Confira IP externo, tag da VM e regra de entrada 8080 para o IP do cliente no Console             |
| `java: command not found`                   | Java ausente na VM                                     | Instale Java 21 antes de executar o JAR                                                           |

O advice trata somente falhas das anotações de validação. Erros de banco não são
convertidos em erros de campo, e não há tratamento personalizado para JSON malformado.

## 10. Encerramento e limpeza dos recursos

Dentro da VM, pressione `Ctrl+C` para encerrar a aplicação e depois use `exit`
para fechar a sessão SSH.

Desligue a VM e o banco no Google console.
