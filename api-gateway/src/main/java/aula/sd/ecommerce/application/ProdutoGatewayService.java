package aula.sd.ecommerce.application;

import aula.sd.ecommerce.config.GrpcProperties;
import aula.sd.ecommerce.dto.produto.ProdutoResponse;
import aula.sd.ecommerce.grpc.ConsultarProdutoRequest;
import aula.sd.ecommerce.grpc.EstoqueServiceGrpc;
import aula.sd.ecommerce.grpc.ListarProdutosRequest;
import aula.sd.ecommerce.grpc.ListarProdutosResponse;
import aula.sd.ecommerce.mapper.ProdutoRestMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProdutoGatewayService {

    private final EstoqueServiceGrpc.EstoqueServiceBlockingStub estoqueService;
    private final ProdutoRestMapper mapper;
    private final GrpcProperties grpcProperties;

    public ProdutoGatewayService(
            EstoqueServiceGrpc.EstoqueServiceBlockingStub estoqueService,
            ProdutoRestMapper mapper,
            GrpcProperties grpcProperties
    ) {
        this.estoqueService = estoqueService;
        this.mapper = mapper;
        this.grpcProperties = grpcProperties;
    }

    public List<ProdutoResponse> listar() {
        ListarProdutosResponse response = stubComDeadline()
                .listarProdutos(ListarProdutosRequest.getDefaultInstance());
        return response.getProdutosList().stream().map(mapper::toResponse).toList();
    }

    public ProdutoResponse consultar(String produtoId) {
        ConsultarProdutoRequest request = ConsultarProdutoRequest.newBuilder()
                                                                 .setProdutoId(produtoId)
                                                                 .build();
        return mapper.toResponse(stubComDeadline().consultarProduto(request));
    }

    private EstoqueServiceGrpc.EstoqueServiceBlockingStub stubComDeadline() {
        return estoqueService.withDeadlineAfter(
                grpcProperties.deadline().toNanos(),
                TimeUnit.NANOSECONDS
        );
    }
}
