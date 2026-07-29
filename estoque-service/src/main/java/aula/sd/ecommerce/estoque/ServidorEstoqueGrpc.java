package aula.sd.ecommerce.estoque;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ServidorEstoqueGrpc {

    private static final int PORTA = 9091;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server servidor = ServerBuilder.forPort(PORTA).addService(new EstoqueServiceImpl()).build().start();

        System.out.println("Servidor de estoque gRPC ouvindo na porta " + PORTA);
        servidor.awaitTermination();
    }
}
