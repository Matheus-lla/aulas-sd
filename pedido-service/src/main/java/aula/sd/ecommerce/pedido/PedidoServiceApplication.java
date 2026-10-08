package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@SpringBootApplication
public class PedidoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidoServiceApplication.class, args);
    }

    @Bean
    EstoqueServiceGrpc.EstoqueServiceBlockingStub estoqueService(GrpcChannelFactory channels) {
        // O stub Java usa o mesmo .proto do servidor Python; só conhece mensagens e métodos.
        // O canal usa o endereço de application.properties e envia Protobuf pela rede.
        return EstoqueServiceGrpc.newBlockingStub(channels.createChannel("estoque"));
    }
}
