package aula.sd.ecommerce.application;

import aula.sd.ecommerce.config.GrpcProperties;
import aula.sd.ecommerce.dto.pedido.CriarPedidoRequest;
import aula.sd.ecommerce.dto.pedido.PedidoResponse;
import aula.sd.ecommerce.grpc.ConsultarPedidoRequest;
import aula.sd.ecommerce.grpc.ItemPedidoRequest;
import aula.sd.ecommerce.grpc.ListarPedidosRequest;
import aula.sd.ecommerce.grpc.ListarPedidosResponse;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import aula.sd.ecommerce.mapper.PedidoRestMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PedidoGatewayService {

    private final PedidoServiceGrpc.PedidoServiceBlockingStub pedidoService;
    private final PedidoRestMapper mapper;
    private final GrpcProperties grpcProperties;

    public PedidoGatewayService(
            PedidoServiceGrpc.PedidoServiceBlockingStub pedidoService,
            PedidoRestMapper mapper,
            GrpcProperties grpcProperties
    ) {
        this.pedidoService = pedidoService;
        this.mapper = mapper;
        this.grpcProperties = grpcProperties;
    }

    public PedidoResponse criar(String usuario, CriarPedidoRequest pedido) {
        aula.sd.ecommerce.grpc.CriarPedidoRequest.Builder request =
                aula.sd.ecommerce.grpc.CriarPedidoRequest.newBuilder()
                                                         .setUsuario(usuario);
        pedido.itens().forEach(item -> request.addItens(
                ItemPedidoRequest.newBuilder()
                                 .setProdutoId(item.produtoId().strip())
                                 .setQuantidade(item.quantidade())
                                 .build()
        ));
        // O JSON recebido por REST vira uma mensagem Protobuf enviada pela rede.
        return mapper.toResponse(stubComDeadline().criarPedido(request.build()));
    }

    public PedidoResponse consultar(String usuario, String pedidoId) {
        ConsultarPedidoRequest request = ConsultarPedidoRequest.newBuilder()
                                                               .setPedidoId(pedidoId)
                                                               .setUsuario(usuario)
                                                               .build();
        return mapper.toResponse(stubComDeadline().consultarPedido(request));
    }

    public List<PedidoResponse> listar(String usuario) {
        ListarPedidosRequest request = ListarPedidosRequest.newBuilder()
                                                           .setUsuario(usuario)
                                                           .build();
        ListarPedidosResponse response = stubComDeadline().listarPedidos(request);
        return response.getPedidosList().stream().map(mapper::toResponse).toList();
    }

    private PedidoServiceGrpc.PedidoServiceBlockingStub stubComDeadline() {
        return pedidoService.withDeadlineAfter(
                grpcProperties.deadline().toNanos(),
                TimeUnit.NANOSECONDS
        );
    }
}
