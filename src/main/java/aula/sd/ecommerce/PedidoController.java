package aula.sd.ecommerce;

import jakarta.validation.Valid;
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

    private final PedidoRepository pedidoRepository;

    public PedidoController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@Valid @RequestBody PedidoRequest pedido) {
        Pedido salvo = pedidoRepository.save(new Pedido(pedido.item(), pedido.quantidade()));

        System.out.println("Pedido salvo: id=" + salvo.getId() + ", item=" + salvo.getItem() + ", quantidade=" + salvo.getQuantidade());

        Map<String, String> response = Map.of("id", salvo.getId().toString(),
                                              "status", salvo.getStatus(),
                                              "mensagem", "Pedido criado com sucesso");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
