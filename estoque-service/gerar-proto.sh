#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
python_estoque="${PYTHON_ESTOQUE:-estoque-service/.venv/bin/python}"
# O include virtual gera imports no pacote estoque.generated, sem editar código gerado.
# Java e Python leem o mesmo arquivo físico; a rota gRPC continua ecommerce.EstoqueService.
"$python_estoque" -m grpc_tools.protoc \
    -Iestoque/generated=contratos-grpc/src/main/proto \
    --python_out=estoque-service/src \
    --grpc_python_out=estoque-service/src \
    estoque/generated/produto_estoque.proto
