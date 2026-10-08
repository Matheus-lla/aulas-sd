import os

import psycopg
from psycopg.rows import dict_row


class BancoEstoque:
    def __init__(self):
        self.perfil = os.getenv("DB_PROFILE", "local")
        if self.perfil not in {"local", "cloud"}:
            raise ValueError("DB_PROFILE deve ser local ou cloud.")
        if self.perfil == "cloud":
            obrigatorias = ("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD")
            if any(not os.getenv(nome) for nome in obrigatorias):
                raise ValueError("Na nuvem, configure DB_HOST, DB_NAME, DB_USER e DB_PASSWORD.")
        timeout = int(os.getenv("DB_CONNECT_TIMEOUT", "5"))
        if timeout < 2:
            raise ValueError("DB_CONNECT_TIMEOUT deve ser de pelo menos 2 segundos.")
        local = self.perfil == "local"
        self.parametros = {
            "host": os.getenv("DB_HOST", "127.0.0.1"),
            "port": int(os.getenv("DB_PORT", "5435" if local else "5432")),
            "dbname": os.getenv("DB_NAME", "estoque_demo"),
            "user": os.getenv("DB_USER", "sa" if local else "ecommerce"),
            "password": os.getenv("DB_PASSWORD", "sa" if local else ""),
            "sslmode": "disable" if local else os.getenv("DB_SSLMODE", "require"),
            "connect_timeout": timeout,
        }

    def conectar(self):
        # H2 local expõe o protocolo PG; na nuvem o mesmo driver acessa PostgreSQL.
        # ClientCursor parametriza SQL sem prepared statements do protocolo estendido,
        # que não é integralmente implementado pelo H2. Os valores nunca são concatenados.
        conexao = psycopg.connect(
            **self.parametros,
            cursor_factory=psycopg.ClientCursor,
            row_factory=dict_row,
            prepare_threshold=None,
        )
        try:
            if self.perfil == "local":
                conexao.execute("SET LOCK_TIMEOUT 2000")
            else:
                conexao.execute("SET LOCAL lock_timeout = '2s'")
                conexao.execute("SET LOCAL statement_timeout = '5s'")
            return conexao
        except Exception:
            conexao.close()
            raise

    def inicializar(self):
        # A mesma tabela pertence apenas ao estoque em ambos os ambientes.
        # Não há limpeza, população automática ou acesso ao banco de pedidos.
        with self.conectar() as conexao:
            conexao.execute("""
                CREATE TABLE IF NOT EXISTS public.produtos (
                    id VARCHAR(80) PRIMARY KEY,
                    nome VARCHAR(255) NOT NULL,
                    descricao VARCHAR(1000) NOT NULL,
                    preco NUMERIC(12, 2) NOT NULL CHECK (preco >= 0),
                    moeda VARCHAR(3) NOT NULL,
                    quantidade_disponivel INTEGER NOT NULL CHECK (quantidade_disponivel >= 0)
                )
            """)
