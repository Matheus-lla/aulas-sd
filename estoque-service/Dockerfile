# syntax=docker/dockerfile:1
# Python oficial 3.12 atende ao pyproject; a variante slim reduz o sistema base.
FROM python:3.12-slim-bookworm AS build
WORKDIR /workspace

# Dependências fixadas mudam menos que os fontes e aproveitam o cache de camadas.
COPY estoque-service/requirements*.txt estoque-service/pyproject.toml estoque-service/
# Wheels de execução ficam separados das ferramentas de geração Protobuf.
RUN pip wheel --no-cache-dir --wheel-dir /wheels -r estoque-service/requirements-runtime.txt \
    && pip install --no-cache-dir -r estoque-service/requirements.txt
COPY contratos-grpc/src/main/proto contratos-grpc/src/main/proto
COPY estoque-service/gerar-proto.sh estoque-service/
COPY estoque-service/src estoque-service/src
# Gera o mesmo contrato utilizado pelo Java; não depende de arquivos gerados locais.
RUN PYTHON_ESTOQUE=python bash estoque-service/gerar-proto.sh \
    && pip wheel --no-cache-dir --no-deps --wheel-dir /wheels ./estoque-service

# Mesma distribuição/versão mantém compatibilidade dos wheels binários (Psycopg).
FROM python:3.12-slim-bookworm AS runtime
WORKDIR /app
# O mount temporário lê os wheels do build sem guardá-los em camadas finais.
# Somente os pacotes de execução instalados permanecem na imagem.
RUN --mount=type=bind,from=build,source=/wheels,target=/wheels \
    pip install --no-cache-dir --no-index --find-links=/wheels estoque-service==1.0.0 \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
USER 10001:10001
# Evita buffer nos logs e bytecode em diretórios de pacotes sem permissão de escrita.
# O bind amplo é necessário para receber chamadas de outros contêineres.
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 GRPC_HOST=0.0.0.0
# Padrão local; o servidor lê PORT (prioritário) ou GRPC_PORT ao iniciar.
EXPOSE 9091
# Forma exec entrega SIGTERM ao tratamento de encerramento já existente no Python.
ENTRYPOINT ["python", "-m", "estoque.main"]
