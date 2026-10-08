package aula.sd.ecommerce.controller;

import aula.sd.ecommerce.application.PedidoGatewayService;
import aula.sd.ecommerce.dto.pedido.CriarPedidoRequest;
import aula.sd.ecommerce.dto.pedido.PedidoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoGatewayService pedidoService;

    public PedidoController(PedidoGatewayService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CriarPedidoRequest request) {
        // A identidade vem do JWT validado; o cliente não escolhe o dono do pedido.
        PedidoResponse pedido = pedidoService.criar(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/pedidos/" + pedido.id());
        return ResponseEntity.created(location).body(pedido);
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<PedidoResponse> consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable String pedidoId) {
        return ResponseEntity.ok(pedidoService.consultar(jwt.getSubject(), pedidoId));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pedidoService.listar(jwt.getSubject()));
    }
}
