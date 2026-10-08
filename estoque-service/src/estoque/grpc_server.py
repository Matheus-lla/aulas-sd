import logging
from decimal import Decimal

import grpc
from psycopg import OperationalError

from estoque.generated import produto_estoque_pb2 as mensagens
from estoque.generated import produto_estoque_pb2_grpc as contrato
from estoque.models import Produto

logger = logging.getLogger(__name__)


class ErroEstoque(Exception):
    """Uma única exceção para regras de negócio e seu código público gRPC."""

    def __init__(self, codigo, mensagem):
        super().__init__(mensagem)
        self.codigo = codigo


class EstoqueGrpcService(contrato.EstoqueServiceServicer):
    def __init__(self, banco):
        self.banco = banco

    # Estes nomes vêm exatamente do .proto. O Java usa o stub gerado do mesmo
    # contrato, sem importar esta classe Python ou os modelos de persistência.
    def ListarProdutos(self, request, context):
        return self._responder(context, self._listar)

    def ConsultarProduto(self, request, context):
        return self._responder(context, lambda: self._consultar(request.produto_id))

    def ReservarEstoque(self, request, context):
        return self._responder(context, lambda: self._reservar(request, context))

    def _listar(self):
        with self.banco.conectar() as conexao:
            produtos = conexao.execute("SELECT * FROM public.produtos ORDER BY id").fetchall()
            return mensagens.ListarProdutosResponse(
                produtos=[self._produto_proto(Produto(**produto)) for produto in produtos]
            )

    def _consultar(self, produto_id):
        produto_id = produto_id.strip()
        if not produto_id:
            raise ErroEstoque(grpc.StatusCode.INVALID_ARGUMENT, "Informe o produto.")
        with self.banco.conectar() as conexao:
            produto = conexao.execute(
                "SELECT * FROM public.produtos WHERE id = %s", (produto_id,)
            ).fetchone()
            if produto is None:
                raise ErroEstoque(grpc.StatusCode.NOT_FOUND, "Produto não encontrado.")
            return self._produto_proto(Produto(**produto))

    def _reservar(self, request, context):
        itens = sorted(request.itens, key=lambda item: item.produto_id.strip())
        ids = [item.produto_id.strip() for item in itens]
        if not itens or len(set(ids)) != len(ids) or any(
            not produto_id or item.quantidade <= 0
            for produto_id, item in zip(ids, itens)
        ):
            raise ErroEstoque(
                grpc.StatusCode.INVALID_ARGUMENT,
                "Informe produtos distintos com quantidade positiva.",
            )

        reservados = []
        total = Decimal("0.00")
        # O contexto confirma o commit ao sair sem erro ou desfaz tudo em caso de falha.
        # Cada chamada abre e fecha sua própria conexão; não há estado entre threads.
        with self.banco.conectar() as conexao:
            for produto_id, item in zip(ids, itens):
                # SELECT FOR UPDATE bloqueia a linha no H2 ou PostgreSQL: chamadas concorrentes
                # não podem ler o mesmo saldo e sobrescrever a baixa umas das outras.
                # A ordem dos IDs é igual em todas as reservas para evitar deadlocks.
                registro = conexao.execute(
                    "SELECT * FROM public.produtos WHERE id = %s FOR UPDATE",
                    (produto_id,),
                ).fetchone()
                produto = Produto(**registro) if registro else None
                if produto is None:
                    raise ErroEstoque(
                        grpc.StatusCode.NOT_FOUND, f"Produto não encontrado: {produto_id}"
                    )
                if produto.quantidade_disponivel < item.quantidade:
                    raise ErroEstoque(
                        grpc.StatusCode.FAILED_PRECONDITION,
                        f"Estoque insuficiente: {produto.nome}",
                    )
                if produto.moeda != "BRL":
                    raise ErroEstoque(
                        grpc.StatusCode.FAILED_PRECONDITION,
                        "O catálogo didático utiliza apenas BRL.",
                    )
                subtotal = produto.preco * item.quantidade
                conexao.execute(
                    "UPDATE public.produtos SET quantidade_disponivel = quantidade_disponivel - %s WHERE id = %s",
                    (item.quantidade, produto_id),
                )
                reservados.append(mensagens.ItemReservado(
                    produto_id=produto.id,
                    nome=produto.nome,
                    quantidade=item.quantidade,
                    preco_unitario=format(produto.preco, ".2f"),
                    subtotal=format(subtotal, ".2f"),
                ))
                total += subtotal
            if not context.is_active():
                # Evita confirmar uma operação cujo prazo já acabou antes do commit.
                # Ainda existe uma janela entre commit e resposta, descrita no README.
                raise ErroEstoque(grpc.StatusCode.DEADLINE_EXCEEDED, "Prazo da reserva excedido.")

        # O commit local já terminou. Decimal vira string Protobuf, sem perda por float.
        return mensagens.ReservarEstoqueResponse(
            itens=reservados, valor_total=format(total, ".2f"), moeda="BRL"
        )

    @staticmethod
    def _produto_proto(produto):
        return mensagens.Produto(
            id=produto.id,
            nome=produto.nome,
            descricao=produto.descricao,
            preco=format(produto.preco, ".2f"),
            moeda=produto.moeda,
            quantidade_disponivel=produto.quantidade_disponivel,
        )

    @staticmethod
    def _responder(context, operacao):
        try:
            return operacao()
        except ErroEstoque as erro:
            context.abort(erro.codigo, str(erro))
        except OperationalError:
            logger.exception("Falha operacional no banco de estoque")
            context.abort(grpc.StatusCode.UNAVAILABLE, "Banco de estoque temporariamente indisponível.")
        except Exception:
            logger.exception("Falha inesperada no estoque")
            context.abort(grpc.StatusCode.INTERNAL, "Falha interna no estoque.")
