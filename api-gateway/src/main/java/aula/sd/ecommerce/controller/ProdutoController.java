package aula.sd.ecommerce.controller;

import aula.sd.ecommerce.application.ProdutoGatewayService;
import aula.sd.ecommerce.dto.produto.ProdutoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {

    private final ProdutoGatewayService produtoService;

    public ProdutoController(ProdutoGatewayService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar() {
        return ResponseEntity.ok(produtoService.listar());
    }

    @GetMapping("/{produtoId}")
    public ResponseEntity<ProdutoResponse> consultar(@PathVariable String produtoId) {
        return ResponseEntity.ok(produtoService.consultar(produtoId));
    }
}
