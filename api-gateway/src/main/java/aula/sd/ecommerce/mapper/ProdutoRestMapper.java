package aula.sd.ecommerce.mapper;

import aula.sd.ecommerce.dto.produto.ProdutoResponse;
import aula.sd.ecommerce.grpc.Produto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProdutoRestMapper {

    public ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                new BigDecimal(produto.getPreco()),
                produto.getMoeda(),
                produto.getQuantidadeDisponivel()
        );
    }
}
