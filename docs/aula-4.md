# Aula 4 — Pedidos e estoque via gRPC

## 1. Objetivo da aula

Separar o exemplo de e-commerce em módulos Maven e fazer o serviço de pedidos
consultar um serviço de estoque por gRPC antes de aceitar ou rejeitar um pedido.
Os dois servidores usam Java puro, sem Spring.

## 2. Conceitos importantes

- `contratos-grpc` contém os arquivos `.proto` compartilhados e gera as classes
  Java usadas pelos outros módulos.
- `pedido-service` recebe o pedido, consulta o estoque e compara a quantidade
  solicitada com a quantidade disponível.
- `estoque-service` consulta um mapa fixo em memória e informa a quantidade do
  item.
- O cliente de pedidos chama o servidor na porta 9090. O servidor de pedidos
  atua como cliente do estoque na porta 9091.
- As classes geradas ficam somente em
  `contratos-grpc/target/generated-sources/protobuf/`. Não copie nem edite essas
  classes em `src/main/java`.
- O Build Helper Maven Plugin registra os diretórios gerados como fontes do
  módulo `contratos-grpc`, permitindo que IDEs resolvam os imports dessas
  classes.
- Se o estoque estiver desligado, a chamada falha com o erro gRPC padrão. Esta
  aula não adiciona fallback, retry ou tratamento especial.

## 3. Arquitetura e configurações necessárias

```text
ClientePedidosGrpc
        |
        | gRPC :9090
        v
ServidorPedidosGrpc
        |
        | ClienteEstoqueGrpc
        | gRPC :9091
        v
ServidorEstoqueGrpc
        |
        v
Map.of("notebook", 10, "teclado", 20, "mouse", 30)
```

Módulos Maven:

| Módulo            | Responsabilidade                                           |
|-------------------|------------------------------------------------------------|
| `contratos-grpc`  | Contratos `pedido.proto` e `estoque.proto` e código gerado |
| `pedido-service`  | Servidor e cliente de pedidos e cliente do estoque         |
| `estoque-service` | Servidor e implementação da consulta de estoque            |

Valores fixados diretamente no código:

| Classe                | Constante       | Valor local |
|-----------------------|-----------------|-------------|
| `ServidorPedidosGrpc` | `PORTA`         | `9090`      |
| `ClientePedidosGrpc`  | `HOST`          | `localhost` |
| `ClientePedidosGrpc`  | `PORTA`         | `9090`      |
| `ClientePedidosGrpc`  | `ITEM`          | `notebook`  |
| `ClientePedidosGrpc`  | `QUANTIDADE`    | `2`         |
| `PedidoServiceImpl`   | `ESTOQUE_HOST`  | `localhost` |
| `PedidoServiceImpl`   | `ESTOQUE_PORTA` | `9091`      |
| `ServidorEstoqueGrpc` | `PORTA`         | `9091`      |

Host, portas, item e quantidade não são recebidos por argumentos nem por
variáveis de ambiente. Para alterar um desses valores, edite a constante na
classe indicada e recompile o projeto.

## 4. Como compilar

Execute localmente, na raiz do projeto:

```bash
./mvnw clean install
```

O Maven compila os módulos nesta ordem:

```text
ecommerce
contratos-grpc
pedido-service
estoque-service
```

O resultado esperado termina com:

```text
BUILD SUCCESS
```

## 5. Como executar

Abra três terminais na raiz do projeto e respeite a ordem abaixo.

No primeiro terminal, inicie localmente o estoque:

```bash
./mvnw -pl estoque-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.estoque.ServidorEstoqueGrpc
```

No segundo terminal, inicie localmente os pedidos:

```bash
./mvnw -pl pedido-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ServidorPedidosGrpc
```

No terceiro terminal, execute localmente o cliente:

```bash
./mvnw -pl pedido-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ClientePedidosGrpc
```

Com `ITEM = "notebook"` e `QUANTIDADE = 2`, o estoque disponível é 10 e o
pedido deve ser aceito.

Para demonstrar a rejeição, interrompa o cliente, edite
`pedido-service/src/main/java/aula/sd/ecommerce/pedido/ClientePedidosGrpc.java`
e altere:

```java
private static final int QUANTIDADE = 11;
```

Recompile localmente:

```bash
./mvnw -pl pedido-service compile
```

Execute novamente o cliente:

```bash
./mvnw -pl pedido-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ClientePedidosGrpc
```

O pedido deve ser rejeitado porque 11 é maior que o estoque de 10 notebooks.
Depois da demonstração, restaure `QUANTIDADE = 2` e recompile.

## 6. Resultados esperados

O servidor de estoque inicia com:

```text
Servidor de estoque gRPC ouvindo na porta 9091
```

O servidor de pedidos inicia com:

```text
Servidor gRPC ouvindo na porta 9090
```

Para duas unidades de notebook, o cliente mostra uma resposta semelhante a:

```text
Pedido ID: 08c2fe64-8ea1-40c7-a5d4-37701bfb5f0f
Status: ACEITO
Mensagem: Pedido aceito.
```

Para 11 unidades, o resultado esperado é:

```text
Pedido ID: 1b54f9ba-40a1-4b09-ae7f-10d73d92aeb6
Status: REJEITADO
Mensagem: Estoque insuficiente.
```

O UUID muda a cada chamada.

## 7. Execução no Google Cloud

O exemplo usa duas VMs na mesma rede.

Configure as dependencias da maquina.

```bash
sudo apt update
sudo apt install -y git
sudo apt install -y openjdk-21-jdk
git clone https://github.com/Matheus-lla/aulas-sd.git
cd aulas-sd
```

Descubra o IP interno da VM servindo o modulo de estoque.

Edite no código dentro da vm que ira fazer a requisição para o modulo de estoque
`pedido-service/src/main/java/aula/sd/ecommerce/pedido/PedidoServiceImpl.java` e
substitua `localhost` pelo IP interno:

```java
private static final String ESTOQUE_HOST = "IP_INTERNO_DO_ESTOQUE";
```

```bash
sed -i 's/ESTOQUE_HOST = "localhost"/ESTOQUE_HOST = "IP_INTERNO_A_SER_SUBSTITUIDO"/' \
pedido-service/src/main/java/aula/sd/ecommerce/pedido/PedidoServiceImpl.java
```

Valide se a alteração foi aplicada com:

```bash
git diff
```

Se digitar o IP errado na hora de executar o comando, apenas reverta com o seguinte comando:

```bash
git checkout .
```

Descubra o IP público da VM de pedidos:

Edite localmente
`pedido-service/src/main/java/aula/sd/ecommerce/pedido/ClientePedidosGrpc.java`
e use o IP público:

```java
private static final String HOST = "IP_PUBLICO_DOS_PEDIDOS";
```

Como os hosts são constantes, recompile depois das alterações.

Dentro da VM de estoque, instale o Java 21, compile e inicie o serviço:

```bash
./mvnw clean install
./mvnw -pl estoque-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.estoque.ServidorEstoqueGrpc
```

Dentro da VM de pedidos, instale o Java 21, compile e inicie o serviço:

```bash
./mvnw clean install
./mvnw -pl pedido-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ServidorPedidosGrpc
```

Com os dois servidores ativos, execute o cliente na máquina local:

```bash
./mvnw -pl pedido-service exec:java \
  -Dexec.mainClass=aula.sd.ecommerce.pedido.ClientePedidosGrpc
```

## 8. Erros comuns e identificação

| Sintoma                                                | Como identificar                                                     | Ação                                                              |
|--------------------------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------|
| `UNAVAILABLE: io exception` ao chamar pedidos          | Servidor de pedidos parado, `HOST` incorreto ou porta 9090 bloqueada | Inicie o servidor, confira a constante e a regra de firewall      |
| `UNAVAILABLE` no servidor de pedidos                   | Estoque parado, `ESTOQUE_HOST` incorreto ou porta 9091 bloqueada     | Inicie o estoque e confira IP interno e firewall                  |
| `Connection refused`                                   | Nenhum processo está ouvindo na porta informada                      | Respeite a ordem estoque → pedidos → cliente                      |
| Dependência `contratos-grpc` não encontrada            | Os módulos não foram instalados no repositório Maven local           | Execute `./mvnw clean install` na raiz                            |
| Classe gRPC gerada não encontrada                      | A geração Protobuf não foi executada no módulo de contratos          | Execute `./mvnw clean install` e confira `contratos-grpc/target/` |
| Imports `aula.sd.ecommerce.grpc` vermelhos no IntelliJ | O modelo Maven ainda não foi recarregado                             | Na janela Maven, clique em `Reload All Maven Projects`            |
| Erro de versão do Java                                 | `java -version` não mostra 21                                        | Instale ou selecione o JDK 21                                     |
| Funciona localmente, mas não entre VMs                 | `ESTOQUE_HOST` ainda está como `localhost`                           | Use o IP interno da VM de estoque e recompile                     |

## 9. Encerramento e limpeza dos recursos

Interrompa servidor com `Ctrl+C`, e execute o comando para encerrar a conexão ssh.

```bash
exit
```

Depois, pare a VM via o console do GCP.
