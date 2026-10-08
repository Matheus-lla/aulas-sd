import logging
import os
import signal
from concurrent.futures import ThreadPoolExecutor
from threading import Event

import grpc
from psycopg.errors import ConnectionTimeout

from estoque.database import BancoEstoque
from estoque.generated import produto_estoque_pb2_grpc
from estoque.grpc_server import EstoqueGrpcService


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    host = os.getenv("GRPC_HOST", "127.0.0.1")
    porta = int(os.getenv("GRPC_PORT", "9091"))
    workers = int(os.getenv("GRPC_WORKERS", "4"))
    if not 1 <= porta <= 65535 or workers <= 0:
        raise ValueError("GRPC_PORT deve estar entre 1 e 65535 e GRPC_WORKERS deve ser positivo.")

    encerramento = Event()
    signal.signal(signal.SIGINT, lambda *_: encerramento.set())
    signal.signal(signal.SIGTERM, lambda *_: encerramento.set())
    banco = BancoEstoque()
    try:
        banco.inicializar()
    except ConnectionTimeout:
        # Esse erro ocorre antes das tabelas e do gRPC. Não mostramos senha nem DSN.
        logging.error(
            "Conexão com o banco não concluída em %ss: host=%s, porta=%s, banco=%s, perfil=%s. "
            "Verifique endereço, acesso TCP, regras de rede e TLS a partir desta máquina. "
            "Consulte a seção de diagnóstico de conexão no README.",
            banco.parametros["connect_timeout"],
            banco.parametros["host"],
            banco.parametros["port"],
            banco.parametros["dbname"],
            banco.perfil,
        )
        raise SystemExit(1)
    with ThreadPoolExecutor(max_workers=workers) as executor:
        # O servidor recebe Protobuf por HTTP/2 e despacha os métodos do .proto.
        # Registrar o servicer associa o contrato gerado à implementação Python.
        servidor = grpc.server(executor)
        produto_estoque_pb2_grpc.add_EstoqueServiceServicer_to_server(
            EstoqueGrpcService(banco), servidor
        )
        endereco = f"[{host}]:{porta}" if ":" in host else f"{host}:{porta}"
        if servidor.add_insecure_port(endereco) == 0:
            raise RuntimeError(f"Não foi possível abrir {endereco}")
        try:
            servidor.start()
            logging.info("Estoque Python iniciado: gRPC em %s, banco %s", endereco, banco.perfil)
            encerramento.wait()
        finally:
            # Aguarda chamadas em andamento antes de encerrar o processo.
            servidor.stop(grace=5).wait()


if __name__ == "__main__":
    main()
