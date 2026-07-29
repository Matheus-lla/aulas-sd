package aula.sd.ecommerce;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody PedidoRequest pedido) {
        System.out.println("Pedido recebido: item = {" + pedido.item() + "}, quantidade={" + pedido.quantidade() + "}");
        return ResponseEntity.ok("Pedido recebido com sucesso");
    }
}
