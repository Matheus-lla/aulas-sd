package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.ConsultarPedidoRequest;
import aula.sd.ecommerce.grpc.CriarPedidoRequest;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import aula.sd.ecommerce.grpc.ItemReservaRequest;
import aula.sd.ecommerce.grpc.ListarPedidosResponse;
import aula.sd.ecommerce.grpc.ReservarEstoqueRequest;
import io.grpc.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PedidoService {
    private final PedidoRepository repository;
    private final EstoqueServiceGrpc.EstoqueServiceBlockingStub estoque;

    public PedidoService(PedidoRepository repository, EstoqueServiceGrpc.EstoqueServiceBlockingStub estoque) {
        this.repository = repository;
        this.estoque = estoque;
    }

    @Transactional
    public aula.sd.ecommerce.grpc.Pedido criar(CriarPedidoRequest request) {
        validarUsuario(request.getUsuario());
        if (request.getItensCount() == 0)
            throw Status.INVALID_ARGUMENT.withDescription("Informe pelo menos um item.").asRuntimeException();
        var ids = new HashSet<String>();
        var reservaRequest = ReservarEstoqueRequest.newBuilder();
        for (var item : request.getItensList()) {
            if (item.getProdutoId().isBlank() || item.getQuantidade() <= 0 || !ids.add(item.getProdutoId())) {
                throw Status.INVALID_ARGUMENT.withDescription("Informe produtos distintos com quantidade positiva.")
                                             .asRuntimeException();
            }
            reservaRequest.addItens(ItemReservaRequest.newBuilder()
                                                      .setProdutoId(item.getProdutoId())
                                                      .setQuantidade(item.getQuantidade()));
        }
        // Chamada síncrona real: preços e disponibilidade vêm exclusivamente do estoque.
        // O prazo limita a espera; não repetimos automaticamente uma operação que baixa estoque.
        var reserva = estoque.withDeadlineAfter(2, TimeUnit.SECONDS).reservarEstoque(reservaRequest.build());
        // Esta transação é apenas do banco de pedidos. Se falhar após a reserva, o estoque
        // pode ter sido baixado sem pedido. A limitação distribuída é explicada no README.
        Pedido pedido = repository.save(new Pedido(request.getUsuario(), reserva));
        return pedido.toGrpc();
    }

    @Transactional(readOnly = true)
    public aula.sd.ecommerce.grpc.Pedido consultar(ConsultarPedidoRequest request) {
        validarUsuario(request.getUsuario());
        UUID id;
        try {
            id = UUID.fromString(request.getPedidoId());
        } catch (IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT.withDescription("Identificador de pedido inválido.").asRuntimeException();
        }
        return repository.findByIdAndUsuario(id, request.getUsuario())
                         .orElseThrow(() -> Status.NOT_FOUND.withDescription("Pedido não encontrado.")
                                                            .asRuntimeException())
                         .toGrpc();
    }

    @Transactional(readOnly = true)
    public ListarPedidosResponse listar(String usuario) {
        validarUsuario(usuario);
        var resposta = ListarPedidosResponse.newBuilder();
        repository.findByUsuarioOrderByCriadoEmDesc(usuario).forEach(p -> resposta.addPedidos(p.toGrpc()));
        return resposta.build();
    }

    private void validarUsuario(String usuario) {
        if (usuario.isBlank() || usuario.length() > 255)
            throw Status.INVALID_ARGUMENT.withDescription("Usuário inválido.").asRuntimeException();
    }
}
