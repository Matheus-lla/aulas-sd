package aula.sd.ecommerce.estoque;

import aula.sd.ecommerce.grpc.ConsultarEstoqueRequest;
import aula.sd.ecommerce.grpc.ConsultarEstoqueResponse;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class EstoqueServiceImpl extends EstoqueServiceGrpc.EstoqueServiceImplBase {

    private static final Map<String, Integer> ESTOQUE = Map.of(
            "notebook", 10,
            "teclado", 20,
            "mouse", 30
    );

    @Override
    public void consultarEstoque(ConsultarEstoqueRequest request,
                                 StreamObserver<ConsultarEstoqueResponse> responseObserver) {
        int quantidadeDisponivel = ESTOQUE.get(request.getItem());
        boolean disponivel = quantidadeDisponivel > 0;

        ConsultarEstoqueResponse response = ConsultarEstoqueResponse.newBuilder()
                                                                    .setItem(request.getItem())
                                                                    .setQuantidadeDisponivel(quantidadeDisponivel)
                                                                    .setDisponivel(disponivel)
                                                                    .setMensagem(disponivel
                                                                                 ? "Item disponível."
                                                                                 : "Item indisponível.")
                                                                    .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
