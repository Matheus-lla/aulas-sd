# Aula 3 — Primeiro serviço gRPC

## 1. Objetivo da aula

Criar um serviço de pedidos com gRPC Java puro, sem Spring. O exemplo define um
contrato Protobuf, gera as classes Java, inicia um servidor e faz uma chamada com
um cliente bloqueante.

## 2. Conceitos importantes

- `pedido.proto` é o contrato compartilhado. Ele define o método remoto, a
  requisição e a resposta.
- `CriarPedidoRequest` e `CriarPedidoResponse` são classes de mensagem geradas
  pelo Protobuf.
- `PedidoServiceGrpc.PedidoServiceImplBase` é a classe base gerada que o servidor
  estende para implementar `CriarPedido`.
- `PedidoServiceBlockingStub` é o stub usado pelo cliente para fazer a chamada e
  aguardar a resposta.
- `./mvnw generate-sources` executa o compilador Protobuf e o gerador de stubs
  gRPC.
- Os arquivos Java gerados ficam em `target/generated-sources/protobuf/`. Eles
  não devem ser copiados nem editados em `src/main/java`.

## 3. Arquitetura e configurações necessárias

```text
ClientePedidosGrpc
        |
        | gRPC + Protobuf
        v
ServidorPedidosGrpc:9090
        |
        v
PedidoServiceImpl
```

Valores fixados diretamente no código:

| Classe | Constante | Valor |
|---|---|---|
| `ServidorPedidosGrpc` | `PORTA` | `9090` |
| `ClientePedidosGrpc` | `HOST` | `localhost` |
| `ClientePedidosGrpc` | `PORTA` | `9090` |
| `ClientePedidosGrpc` | `ITEM` | `notebook` |
| `ClientePedidosGrpc` | `QUANTIDADE` | `2` |

Para alterar host, porta, item ou quantidade, edite a constante correspondente e
compile o projeto novamente. O programa não lê argumentos nem variáveis de
ambiente.

O Maven gera dois grupos de arquivos:

```text
target/generated-sources/protobuf/java/
target/generated-sources/protobuf/grpc-java/
```

O primeiro contém as mensagens Protobuf. O segundo contém a classe do serviço,
a classe base do servidor e os stubs do cliente.

## 4. Como compilar

Execute localmente, na raiz do projeto:

```bash
./mvnw generate-sources
./mvnw compile
```

O primeiro comando permite observar separadamente a geração. O segundo também
executa a geração como parte do ciclo Maven antes de compilar.

## 5. Como executar

Abra dois terminais na raiz do projeto.

No primeiro terminal, execute localmente o servidor:

```bash
./mvnw exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ServidorPedidosGrpc
```

Mantenha esse processo aberto. No segundo terminal, execute localmente o cliente:

```bash
./mvnw exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ClientePedidosGrpc
```

O cliente usa plaintext porque esta aula demonstra somente o fluxo básico de
gRPC e Protobuf.

## 6. Resultados esperados

Ao iniciar o servidor:

```text
Servidor gRPC ouvindo na porta 9090
```

Após a chamada do cliente, o servidor imprime:

```text
Pedido recebido: item=notebook, quantidade=2
```

O cliente recebe uma resposta semelhante a:

```text
Pedido ID: 08c2fe64-8ea1-40c7-a5d4-37701bfb5f0f
Status: RECEBIDO
Mensagem: Pedido criado com sucesso.
```

O UUID muda a cada chamada.

## 7. Execução no Google Cloud

Acesse a VM, dentro da VM, instale o JDK 21:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk
java -version
```

O comando `java -version` deve informar a versão 21.

No terminal clone o projeto da aula:

```bash
git clone <link do github>
```

Compile o projeto:

```bash
cd ~/ecommerce
git checkout aula-2
./mvnw compile
```

Ainda dentro da VM, compile e inicie o servidor:

```bash
cd ~/ecommerce
./mvnw compile
./mvnw exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ServidorPedidosGrpc
```

Edite localmente `ClientePedidosGrpc.java` e troque o valor de `HOST` pelo IP
mostrado no google console da VM rodando o servidor, por exemplo:

```java
private static final String HOST = "34.123.45.67";
```

O número acima é apenas um exemplo. Depois da alteração, execute localmente:

```bash
./mvnw compile
./mvnw exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ClientePedidosGrpc
```

## 8. Erros comuns e identificação

| Sintoma | Como identificar | Ação |
|---|---|---|
| `UNAVAILABLE: io exception` | Servidor parado, host incorreto ou porta bloqueada | Confirme o processo do servidor, `HOST`, porta 9090 e firewall |
| `Connection refused` | Nenhum processo escutando na porta informada | Inicie o servidor antes do cliente |
| Classe gerada não encontrada | `target/generated-sources/protobuf/` não existe | Execute `./mvnw generate-sources` e `./mvnw compile` |
| Erro de versão do Java | `java -version` não mostra 21 | Instale ou selecione o JDK 21 |
| Chamada local funciona, mas a VM não | `HOST` ainda é `localhost` ou o firewall não permite o IP do cliente | Altere `HOST`, recompile e confira a regra |

## 9. Encerramento e limpeza dos recursos

Interrompa servidor com `Ctrl+C`, e execute o comando para encerrar a conexão ssh.

```bash
exit
```

Depois, pare a VM via o console do GCP.
