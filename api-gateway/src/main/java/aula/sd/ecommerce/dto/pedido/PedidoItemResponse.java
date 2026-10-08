package aula.sd.ecommerce.dto.pedido;

import java.math.BigDecimal;

public record PedidoItemResponse(
        String produtoId,
        String nome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}
