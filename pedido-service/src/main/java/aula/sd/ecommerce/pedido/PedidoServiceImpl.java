package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.ConsultarEstoqueRequest;
import aula.sd.ecommerce.grpc.ConsultarEstoqueResponse;
import aula.sd.ecommerce.grpc.CriarPedidoRequest;
import aula.sd.ecommerce.grpc.CriarPedidoResponse;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class PedidoServiceImpl extends PedidoServiceGrpc.PedidoServiceImplBase {

    private final EstoqueServiceGrpc.EstoqueServiceBlockingStub estoque;
    private final PedidoRepository repository;

    public PedidoServiceImpl(EstoqueServiceGrpc.EstoqueServiceBlockingStub estoque, PedidoRepository repository) {
        this.estoque = estoque;
        this.repository = repository;
    }

    @Override
    public void criarPedido(CriarPedidoRequest request, StreamObserver<CriarPedidoResponse> responseObserver) {
        ConsultarEstoqueRequest consulta = ConsultarEstoqueRequest.newBuilder().setItem(request.getItem()).build();
        ConsultarEstoqueResponse respostaEstoque = estoque.consultarEstoque(consulta);
        boolean quantidadeSuficiente = respostaEstoque.getQuantidadeDisponivel() >= request.getQuantidade();
        Pedido pedido = repository.save(new Pedido(request.getItem(), request.getQuantidade(),
                quantidadeSuficiente ? "ACEITO" : "REJEITADO"));

        CriarPedidoResponse response = CriarPedidoResponse.newBuilder()
                                                          .setPedidoId(pedido.getId().toString())
                                                          .setStatus(pedido.getStatus())
                                                          .setMensagem(quantidadeSuficiente ? "Pedido aceito." : "Estoque insuficiente.")
                                                          .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
