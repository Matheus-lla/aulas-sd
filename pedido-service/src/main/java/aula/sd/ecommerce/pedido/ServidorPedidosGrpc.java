package aula.sd.ecommerce.pedido;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// Inicializa o servidor gRPC e publica a implementação do serviço de pedidos.
public class ServidorPedidosGrpc {

    private static final int PORTA = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Registra o serviço que atenderá os métodos definidos no arquivo .proto.
        Server servidor = ServerBuilder.forPort(PORTA)
                                        .addService(new PedidoServiceImpl())
                                        .build()
                                        .start();

        System.out.println("Servidor gRPC ouvindo na porta " + PORTA);

        // Mantém o processo ativo enquanto o servidor espera novas chamadas.
        servidor.awaitTermination();
    }
}
