# Aula 5 — Primeira API REST com Spring

## 1. Objetivo da aula

Criar a primeira API REST do e-commerce com Spring Boot. A aplicação recebe um
pedido em JSON por meio de `POST /pedidos`, registra o item e a quantidade no log
e responde com status HTTP 200.

## 2. Conceitos importantes

- `EcommerceApplication` é a classe principal. A anotação
  `@SpringBootApplication` habilita a configuração automática, e
  `SpringApplication.run` inicia a aplicação.
- `PedidoController` usa `@RestController` para receber requisições HTTP.
- `@RequestBody` converte o JSON da requisição em um objeto Java.
- `PedidoRequest` é um DTO declarado como `record`. Ele transporta somente
  `item` e `quantidade`.
- JSON é o formato usado no corpo da requisição. O cabeçalho
  `Content-Type: application/json` informa esse formato ao servidor.
- `ResponseEntity.ok` devolve a mensagem com status HTTP `200 OK`.

Não há validação, tratamento de erros, banco de dados ou camada de serviço nesta
aula.

## 3. Arquitetura e configurações necessárias

```text
curl
  |
  | POST /pedidos + JSON
  v
PedidoController
  |
  +-- converte o JSON em PedidoRequest
  +-- registra item e quantidade no log
  +-- responde 200 OK
```

Estrutura principal:

```text
src/main/java/aula/sd/ecommerce/
├── EcommerceApplication.java
├── PedidoController.java
└── PedidoRequest.java
```

| Valor                 | Definição nesta aula          |
|-----------------------|-------------------------------|
| Método e rota         | `POST /pedidos`               |
| Porta                 | `8080`, padrão do Spring Boot |
| Host local            | `localhost` no comando `curl` |
| Item de exemplo       | `notebook`, enviado no JSON   |
| Quantidade de exemplo | `2`, enviada no JSON          |

A porta 8080 não está declarada no código nem em `application.properties`: é o
padrão do Spring Boot. O item e a quantidade também não estão fixados no código;
eles chegam no JSON. Portanto, outros valores podem ser enviados sem editar uma
constante ou recompilar. Nesta aula, não deve ser criada configuração para trocar
a porta.

## 4. Como compilar

Na máquina local, na raiz do projeto:

```bash
./mvnw clean verify
```

O Maven Wrapper baixa a versão necessária do Maven, compila o código, executa o
teste existente e cria o arquivo executável em:

```text
target/ecommerce-0.0.1-SNAPSHOT.jar
```

## 5. Como executar

Na máquina local, na raiz do projeto:

```bash
./mvnw spring-boot:run
```

Aguarde o log informar que a aplicação foi iniciada na porta 8080. Em outro
terminal local, execute:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

Para encerrar a aplicação, pressione `Ctrl+C` no terminal em que ela está sendo
executada.

## 6. Resultados esperados

O cliente deve receber uma resposta semelhante a:

```text
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8

Pedido recebido com sucesso
```

No terminal da aplicação, o log deve conter:

```text
Pedido recebido: item=notebook, quantidade=2
```

## 7. Execução no Google Cloud

Inicie a VM via Google console:

Para subir o servidor, compile e execute

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

Para testar substitua `IP_PUBLICO` pelo IP da maquina no comando a baixo:

```bash
curl -i -X POST http://IP_PUBLICO:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"item":"notebook","quantidade":2}'
```

## 8. Como testar pelo Postman

Com a aplicação ainda em execução na VM, abra o Postman na máquina local e crie
uma requisição HTTP:

1. Selecione o método **POST**.
2. Informe a URL `http://IP_PUBLICO:8080/pedidos`, substituindo `IP_PUBLICO`
   pelo endereço externo obtido na seção anterior.
3. Na aba **Body**, selecione **raw** e escolha o formato **JSON**.
4. Insira o corpo da requisição:

```json
{
  "item": "notebook",
  "quantidade": 2
}
```

Os campos correspondem ao `PedidoRequest`: `item` é uma string e `quantidade`
é um número inteiro, enviado sem aspas.

Na aba **Headers**, confira se o cabeçalho abaixo está presente. Ao escolher
JSON no corpo, o Postman normalmente o adiciona automaticamente:

| Key            | Value              |
|----------------|--------------------|
| `Content-Type` | `application/json` |

Clique em **Send**. A resposta esperada é o status **200 OK**, com o corpo:

```text
Pedido recebido com sucesso
```

No terminal em que a aplicação está rodando, confira também o registro do item
e da quantidade recebidos.

Para testar a aplicação executada na sua própria máquina, use a URL
`http://localhost:8080/pedidos`, mantendo o mesmo método, cabeçalho e corpo.
O caminho `/pedidos` vem de `@RequestMapping("/pedidos")`, e o método POST é
definido por `@PostMapping` no `PedidoController`.

## 9. Erros comuns e identificação

| Sintoma                             | Como identificar                                       | Correção                                                                      |
|-------------------------------------|--------------------------------------------------------|-------------------------------------------------------------------------------|
| `Connection refused` localmente     | A aplicação não mostra que iniciou na porta 8080       | Execute `./mvnw spring-boot:run` e aguarde a inicialização                    |
| Status `415 Unsupported Media Type` | O `curl` não enviou o tipo do conteúdo                 | Inclua `-H "Content-Type: application/json"`                                  |
| Status `400 Bad Request`            | O corpo não é um JSON válido                           | Confira aspas, chaves e vírgula do exemplo                                    |
| Porta 8080 já em uso                | O log contém `Port 8080 was already in use`            | Encerre o outro processo que está usando a porta                              |
| Acesso à VM expira                  | O acesso local funciona, mas o IP público não responde | Confira a regra com `gcloud compute firewall-rules describe aula-5-http-8080` |
| `java: command not found` na VM     | O Java ainda não foi instalado                         | Execute `sudo apt install -y openjdk-21-jdk` dentro da VM                     |

## 10. Encerramento e limpeza dos recursos

Dentro da VM, pressione `Ctrl+C` para encerrar a aplicação e depois use `exit`
para fechar a sessão SSH.

Desligue a máquina no Google console.