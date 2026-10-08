package aula.sd.ecommerce.config;

import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    PedidoServiceGrpc.PedidoServiceBlockingStub pedidoServiceStub(GrpcChannelFactory channels) {
        return PedidoServiceGrpc.newBlockingStub(channels.createChannel("pedidos"));
    }

    @Bean
    EstoqueServiceGrpc.EstoqueServiceBlockingStub estoqueServiceStub(GrpcChannelFactory channels) {
        return EstoqueServiceGrpc.newBlockingStub(channels.createChannel("estoque"));
    }
}
