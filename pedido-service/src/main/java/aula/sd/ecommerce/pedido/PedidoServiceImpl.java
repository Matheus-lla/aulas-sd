package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.ConsultarEstoqueResponse;
import aula.sd.ecommerce.grpc.CriarPedidoRequest;
import aula.sd.ecommerce.grpc.CriarPedidoResponse;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

// Implementa no servidor o contrato gerado a partir de PedidoService no .proto.
public class PedidoServiceImpl extends PedidoServiceGrpc.PedidoServiceImplBase {

    private static final String ESTOQUE_HOST = "localhost";
    private static final int ESTOQUE_PORTA = 9091;

    private final ClienteEstoqueGrpc clienteEstoque = new ClienteEstoqueGrpc(ESTOQUE_HOST, ESTOQUE_PORTA);

    @Override
    // request é a única entrada declarada no .proto. O gRPC adiciona responseObserver
    // à API Java do servidor para enviar a resposta; por isso o método retorna void.
    public void criarPedido(CriarPedidoRequest request, StreamObserver<CriarPedidoResponse> responseObserver) {
        ConsultarEstoqueResponse estoque = clienteEstoque.consultar(request.getItem());
        boolean quantidadeSuficiente = estoque.getQuantidadeDisponivel() >= request.getQuantidade();
        String pedidoId = UUID.randomUUID().toString();

        // A requisição já chega desserializada como uma mensagem Protobuf.
        System.out.printf("Pedido recebido: item=%s, quantidade=%d%n", request.getItem(), request.getQuantidade());

        // A resposta também é uma mensagem imutável gerada pelo Protobuf.
        CriarPedidoResponse response = CriarPedidoResponse.newBuilder()
                                                          .setPedidoId(pedidoId)
                                                          .setStatus(quantidadeSuficiente ? "ACEITO" : "REJEITADO")
                                                          .setMensagem(quantidadeSuficiente ? "Pedido aceito." : "Estoque insuficiente.")
                                                          .build();

        // Envia a resposta ao cliente e sinaliza que o RPC terminou com sucesso.
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
