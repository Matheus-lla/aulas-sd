# Estoque em Python — H2 local e PostgreSQL na nuvem

O servidor gRPC é inteiramente Python. O banco local é H2, iniciado como processo
independente; na nuvem, a configuração aponta para PostgreSQL. Não há Java ou Spring
na implementação do microsserviço.

A partir da raiz do repositório, prepare os componentes:

```bash
./mvnw -Dmaven.test.skip=true clean package
python3 -m venv estoque-service/.venv
estoque-service/.venv/bin/python -m pip install -e ./estoque-service
estoque-service/gerar-proto.sh
```

Em terminais separados, execute:

```bash
scripts/h2.sh
estoque-service/.venv/bin/python -m estoque.main
```

O primeiro comando inicia o H2 na porta 5435, usando o driver já empacotado em pedidos.
O segundo inicia o serviço Python na porta 9091. Os defaults são `DB_PROFILE=local`,
`DB_HOST=127.0.0.1`, `DB_PORT=5435`, `DB_NAME=estoque_demo`, `DB_USER=sa`, `DB_PASSWORD=sa`.
O alias do banco aponta para `data/estoque_python_demo.mv.db`.

H2 implementa um subconjunto do protocolo PG. Por isso o Python utiliza Psycopg
`ClientCursor`, parâmetros SQL e transações simples, sem um dialect SQLAlchemy ou
ponte JDBC. Cada chamada abre sua conexão; o contexto faz commit/rollback e fecha-a.
`SELECT FOR UPDATE` protege as reservas concorrentes nos dois bancos.

## Organização

- `main.py`: inicialização gRPC, variáveis de ambiente e encerramento.
- `database.py`: perfis local/cloud, conexões e criação da tabela ausente.
- `models.py`: `Produto` como dataclass Python correspondente à linha SQL.
- `grpc_server.py`: implementa os métodos do contrato e as regras de estoque.
- `generated/`: mensagens/servicer gerados, sem edição manual.

O contrato é `contratos-grpc/src/main/proto/produto_estoque.proto`. O script
`gerar-proto.sh` gera o Python desse mesmo arquivo; Maven gera os clientes Java.
Não existe uma cópia de contrato exclusiva do Python.

Para nuvem, selecione `DB_PROFILE=cloud` e configure `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` e `DB_SSLMODE`. TLS PostgreSQL é habilitado por padrão.
Consulte a execução completa, população, limpeza e variáveis no [README principal](../README.md).

Referências: [servidor e geração gRPC Python](https://grpc.io/docs/languages/python/basics/),
[protocolo PG no H2](https://h2database.github.io/html/advanced.html) e
[ClientCursor Psycopg](https://www.psycopg.org/psycopg3/docs/advanced/cursors.html#client-side-cursors).
