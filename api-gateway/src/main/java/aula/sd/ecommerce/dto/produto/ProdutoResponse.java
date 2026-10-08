package aula.sd.ecommerce.dto.produto;

import java.math.BigDecimal;

public record ProdutoResponse(
        String id,
        String nome,
        String descricao,
        BigDecimal preco,
        String moeda,
        int quantidadeDisponivel
) {
}
