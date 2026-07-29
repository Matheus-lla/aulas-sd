package aula.sd.ecommerce.estoque;

import aula.sd.ecommerce.grpc.ConsultarEstoqueRequest;
import aula.sd.ecommerce.grpc.ConsultarEstoqueResponse;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Map;

@GrpcService
public class EstoqueServiceImpl extends EstoqueServiceGrpc.EstoqueServiceImplBase {

    private static final Map<String, Integer> ESTOQUE = Map.of(
            "notebook", 10,
            "teclado", 20,
            "mouse", 30
    );

    @Override
    public void consultarEstoque(ConsultarEstoqueRequest request,
                                 StreamObserver<ConsultarEstoqueResponse> responseObserver) {
        ConsultarEstoqueResponse response = ConsultarEstoqueResponse.newBuilder()
                                                                    .setQuantidadeDisponivel(ESTOQUE.getOrDefault(request.getItem(), 0))
                                                                    .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
