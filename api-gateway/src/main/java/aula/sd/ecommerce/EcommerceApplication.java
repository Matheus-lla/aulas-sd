package aula.sd.ecommerce;

import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    @Bean
    PedidoServiceGrpc.PedidoServiceBlockingStub pedidosService(GrpcChannelFactory channels) {
        return PedidoServiceGrpc.newBlockingStub(channels.createChannel("pedidos"));
    }
}
