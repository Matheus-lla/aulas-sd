# Aula 2 — Sockets TCP

## 1. Objetivo da aula

Criar um cliente e um servidor TCP usando somente as APIs do JDK. Uma única
conexão aceita várias mensagens, e o cliente mede o tempo de ida e volta (RTT)
de cada uma.

## 2. Conceitos importantes

- `ServerSocket` abre a porta do servidor e aguarda conexões.
- `Socket` representa a conexão TCP entre cliente e servidor.
- O protocolo é textual: cada requisição e cada resposta ocupam uma linha em
  UTF-8.
- O servidor atende um cliente por vez. Não há múltiplas threads nesta aula.
- RTT é o intervalo entre o envio da mensagem e o recebimento da resposta,
  medido com `System.nanoTime()`.
- A mensagem `SAIR` recebe uma resposta e encerra somente a conexão atual. O
  servidor volta a aguardar o próximo cliente.

## 3. Arquitetura e configurações necessárias

```text
+----------------+       TCP, texto UTF-8       +----------------+
| ClienteTcp     | ----------------------------> | ServidorTcp    |
| mede o RTT     | <---------------------------- | inclui horário |
+----------------+       uma linha por mensagem  +----------------+
```

| Valor | Onde está definido | Valor inicial |
|---|---|---|
| Host do servidor | `ClienteTcp.HOST` | `localhost` |
| Porta do cliente | `ClienteTcp.PORTA` | `5000` |
| Porta do servidor | `ServidorTcp.PORTA` | `5000` |

Host e porta estão fixados diretamente no código. Para alterá-los, edite a
constante correspondente e execute `./mvnw compile` novamente.

Para cada linha enviada, o servidor devolve uma linha no formato:

```text
Mensagem recebida: <mensagem> | Horário do servidor: <horário>
```

## 4. Como compilar

No terminal local, na raiz do projeto, use um JDK 21:

```bash
./mvnw compile
```

As classes compiladas serão criadas em `target/classes`.

## 5. Como executar

Primeiro, no terminal local 1, inicie o servidor:

```bash
java -cp target/classes aula.sd.ecommerce.ServidorTcp
```

Em seguida, no terminal local 2, inicie o cliente:

```bash
java -cp target/classes aula.sd.ecommerce.ClienteTcp
```

Digite várias mensagens no mesmo cliente, por exemplo:

```text
olá
consulta de produto
SAIR
```

O cliente exibe uma resposta e um RTT para cada linha. Depois de `SAIR`, execute
o cliente novamente para confirmar que o servidor continua disponível:

```bash
java -cp target/classes aula.sd.ecommerce.ClienteTcp
```

Para encerrar o servidor, pressione `Ctrl+C` no terminal 1.

## 6. Resultados esperados

Ao iniciar, o servidor mostra:

```text
Servidor TCP ouvindo na porta 5000
```

Para uma mensagem `olá`, a saída do cliente será semelhante a:

```text
Mensagem recebida: olá | Horário do servidor: 14:32:10.123456789
RTT: 1.234 ms
```

O horário e o RTT variam a cada execução. Após `SAIR`, somente aquele cliente
termina. O processo do servidor permanece ativo e aceita uma nova conexão.

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

Ainda dentro da VM, inicie o servidor:

```bash
java -cp target/classes aula.sd.ecommerce.ServidorTcp
```

Consulte o IP público da VM no console do GCP.

Copie o valor exibido e substitua no codigo do cliente `HOST = "localhost" -> HOST = "<IP da VM>"`.

execute o codigo do cliente.

```bash
java -cp target/classes aula.sd.ecommerce.ClienteTcp
```

O resultado esperado é a mesma conexão com o servidor podendo enviar mensagens e recebendo respostas das mesagens enviadas.

## 8. Erros comuns e identificação

| Sintoma | Como identificar | Correção                                           |
|---|---|----------------------------------------------------|
| Erro de versão na compilação | `java -version` ou `javac -version` não mostra a versão 21 | Instale ou selecione o JDK 21                      |
| `Connection refused` | O servidor não está em execução ou o host está incorreto | Inicie o servidor e confira `ClienteTcp.HOST`      |
| Cliente ainda usa `localhost` | A constante foi alterada, mas as classes antigas continuam em `target/classes` | Execute `./mvnw compile` novamente                 |
| `Address already in use` | Outro processo já ocupa a porta 5000 | Encerre o servidor anterior antes de iniciar outro |
| Conexão externa não responde | A porta 5000 ou a tag da VM não corresponde à regra | Confira a regra de rede autorizada                 |

## 9. Encerramento e limpeza dos recursos

No terminal conectado à VM, interrompa o servidor com `Ctrl+C` e saia:

```bash
exit
```

Depois, pare a VM via o console do GCP.