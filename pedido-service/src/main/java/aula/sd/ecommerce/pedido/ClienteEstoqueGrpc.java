package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.ConsultarEstoqueRequest;
import aula.sd.ecommerce.grpc.ConsultarEstoqueResponse;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClienteEstoqueGrpc {

    private final EstoqueServiceGrpc.EstoqueServiceBlockingStub stub;

    public ClienteEstoqueGrpc(String host, int porta) {
        ManagedChannel canal = ManagedChannelBuilder.forAddress(host, porta).usePlaintext().build();
        stub = EstoqueServiceGrpc.newBlockingStub(canal);
    }

    public ConsultarEstoqueResponse consultar(String item) {
        ConsultarEstoqueRequest request = ConsultarEstoqueRequest.newBuilder().setItem(item).build();
        return stub.consultarEstoque(request);
    }
}
