#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/h2-driver.sh
obter_driver_h2
porta="${H2_PG_PORT:-5435}"
[[ "$porta" =~ ^[0-9]+$ && "$porta" -ge 1 && "$porta" -le 65535 ]] || { echo 'H2_PG_PORT inválida.' >&2; exit 1; }
mkdir -p data
# Um arquivo novo para o estoque Python preserva o H2 antigo do estoque Java.
# A senha fictícia não pode ser vazia no protocolo PG acessado por libpq/Psycopg.
java -cp "$H2_DRIVER" org.h2.tools.RunScript \
    -url 'jdbc:h2:file:./data/estoque_python_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' \
    -user sa -password sa -script /dev/null
# H2 é um processo de banco separado, não código Java dentro do microsserviço Python.
# A chave restringe a abertura ao banco previsto; não habilitamos conexões remotas.
exec java -cp "$H2_DRIVER" org.h2.tools.Server -pg -pgPort "$porta" \
    -baseDir "$PWD/data" -ifExists \
    -key estoque_demo 'estoque_python_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE'
