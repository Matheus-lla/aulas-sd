package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Supplier;

@GrpcService
public class PedidoServiceImpl extends PedidoServiceGrpc.PedidoServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);
    private final PedidoService service;
    public PedidoServiceImpl(PedidoService service) { this.service = service; }

    @Override
    public void criarPedido(CriarPedidoRequest r, StreamObserver<aula.sd.ecommerce.grpc.Pedido> out) {
        responder(out, () -> service.criar(r));
    }
    @Override
    public void consultarPedido(ConsultarPedidoRequest r, StreamObserver<aula.sd.ecommerce.grpc.Pedido> out) {
        responder(out, () -> service.consultar(r));
    }
    @Override
    public void listarPedidos(ListarPedidosRequest r, StreamObserver<ListarPedidosResponse> out) {
        responder(out, () -> service.listar(r.getUsuario()));
    }
    private <T> void responder(StreamObserver<T> out, Supplier<T> operacao) {
        try {
            // O proxy transacional confirma a gravação antes de enviarmos sucesso pela rede.
            T resposta = operacao.get();
            out.onNext(resposta);
            out.onCompleted();
        } catch (StatusRuntimeException e) {
            out.onError(e);
        } catch (Exception e) {
            log.error("Falha nos pedidos", e);
            out.onError(Status.INTERNAL.withDescription("Falha interna nos pedidos.").asRuntimeException());
        }
    }
}
