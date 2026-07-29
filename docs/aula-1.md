# Aula 1 — Aplicação Java em uma VM

## 1. Objetivo da aula

Criar um servidor HTTP mínimo com APIs do JDK, executá-lo localmente e publicá-lo temporariamente em uma VM do Google
Compute Engine.

## 2. Conceitos importantes

- `HttpServer` pertence ao módulo `jdk.httpserver`.
- O servidor escuta em todas as interfaces de rede e possui somente o contexto `/`.
- A resposta usa status HTTP `200`, conteúdo HTML e codificação UTF-8.
- O processo permanece em execução até ser interrompido com `Ctrl+C`.

A porta está fixada diretamente em `ServidorHttpBasico`:

```java
private static final int PORTA = 8080;
```

Para alterá-la, seria necessário editar essa constante e executar novamente a compilação. Nesta aula, todos os comandos
usam a porta `8080`.

## 3. Arquitetura e configurações necessárias

```text
Navegador ou curl
        |
        | HTTP :8080
        v
ServidorHttpBasico
        |
        +-- GET / -> HTTP 200 + HTML em UTF-8
```

| Configuração     | Valor                                  |
|------------------|----------------------------------------|
| Classe principal | `aula.sd.ecommerce.ServidorHttpBasico` |
| Host de escuta   | todas as interfaces                    |
| Porta            | `8080`                                 |
| Rota             | `/`                                    |
| Content-Type     | `text/html; charset=UTF-8`             |
| Java             | `21`                                   |

O projeto não usa Spring nem dependências externas. O `pom.xml` configura Java 21, UTF-8 e o módulo `jdk.httpserver`.

## 4. Como compilar

No terminal local, na raiz do projeto:

```bash
./mvnw compile
```

Ao final, o Maven deve exibir `BUILD SUCCESS`. A classe compilada ficará em `target/classes`.

## 5. Como executar

No terminal local, na raiz do projeto:

```bash
java --add-modules jdk.httpserver \
  -cp target/classes \
  aula.sd.ecommerce.ServidorHttpBasico
```

Resultado esperado:

```text
Servidor iniciado em http://localhost:8080/
```

Mantenha esse terminal aberto. Em outro terminal local, teste a rota:

```bash
curl -i http://localhost:8080/
```

## 6. Resultados esperados

O `curl` deve mostrar o status `200`, o tipo de conteúdo com UTF-8 e a página HTML:

```text
HTTP/1.1 200 OK
Content-type: text/html; charset=UTF-8

<html><body><h1>Servidor HTTP em execução</h1></body></html>
```

O nome e a capitalização dos demais cabeçalhos podem variar. Interrompa o servidor local com `Ctrl+C` depois do teste.

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
chmod +x mvnw
./mvnw compile
```

Ainda dentro da VM, inicie o servidor:

```bash
java --add-modules jdk.httpserver \
  -cp target/classes \
  aula.sd.ecommerce.ServidorHttpBasico
```

Consulte o IP público da VM no console do GCP.

Copie o valor exibido e substitua `IP_PUBLICO_DA_VM` no teste:

```bash
curl -i http://IP_PUBLICO_DA_VM:8080/
```

O resultado esperado é o mesmo status `200` e o mesmo HTML obtidos no teste local.

## 8. Erros comuns e identificação

| Sintoma                                | Como identificar e corrigir                                                                                |
|----------------------------------------|------------------------------------------------------------------------------------------------------------|
| `release version 21 not supported`     | Execute `java -version` e `javac -version`; instale o JDK 21 e compile novamente.                          |
| `Permission denied` ao executar `mvnw` | Dentro da VM, execute `chmod +x mvnw`.                                                                     |
| `Address already in use`               | A porta `8080` já está ocupada. Use `ss -ltnp                                                              | grep ':8080'`, encerre o processo encontrado e reinicie o servidor. |
| `Connection refused`                   | Confirme no terminal da VM se a mensagem de inicialização apareceu e se o processo continua executando.    |

## 9. Encerramento e limpeza dos recursos

No terminal conectado à VM, interrompa o servidor com `Ctrl+C` e saia:

```bash
exit
```

Depois, pare a VM via o console do GCP.
