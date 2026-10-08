from dataclasses import dataclass
from decimal import Decimal


@dataclass
class Produto:
    # Uma linha SQL vira este objeto Python. Ele não é compartilhado com o Java:
    # o contrato de rede continua sendo exclusivamente o arquivo .proto.
    id: str
    nome: str
    descricao: str
    preco: Decimal
    moeda: str
    quantidade_disponivel: int
