package aula.sd.ecommerce;

import aula.sd.ecommerce.grpc.CriarPedidoRequest;
import aula.sd.ecommerce.grpc.CriarPedidoResponse;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoServiceGrpc.PedidoServiceBlockingStub pedidosService;

    public PedidoController(PedidoServiceGrpc.PedidoServiceBlockingStub pedidosService) {
        this.pedidosService = pedidosService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@Valid @RequestBody PedidoRequest pedido) {
        LOGGER.info("Pedido recebido: item={}, quantidade={}", pedido.item(), pedido.quantidade());

        CriarPedidoRequest request = CriarPedidoRequest.newBuilder()
                .setItem(pedido.item())
                .setQuantidade(pedido.quantidade())
                .build();
        CriarPedidoResponse resposta = pedidosService.criarPedido(request);

        Map<String, String> corpo = Map.of(
                "id", resposta.getPedidoId(),
                "status", resposta.getStatus(),
                "mensagem", resposta.getMensagem()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(corpo);
    }
}
