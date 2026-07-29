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
        return EstoqueServiceGrpc.newBlockingStub(channels.createChannel("estoque"));
    }
}
