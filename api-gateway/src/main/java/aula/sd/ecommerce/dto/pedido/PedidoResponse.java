package aula.sd.ecommerce.dto.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        String id,
        String usuario,
        StatusPedidoResponse status,
        List<PedidoItemResponse> itens,
        BigDecimal valorTotal,
        String moeda,
        Instant criadoEm
) {

    public PedidoResponse {
        itens = List.copyOf(itens);
    }
}
