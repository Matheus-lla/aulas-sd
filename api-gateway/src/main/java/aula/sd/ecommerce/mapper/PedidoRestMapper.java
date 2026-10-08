package aula.sd.ecommerce.mapper;

import aula.sd.ecommerce.dto.pedido.PedidoItemResponse;
import aula.sd.ecommerce.dto.pedido.PedidoResponse;
import aula.sd.ecommerce.dto.pedido.StatusPedidoResponse;
import aula.sd.ecommerce.grpc.Pedido;
import aula.sd.ecommerce.grpc.PedidoItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class PedidoRestMapper {

    public PedidoResponse toResponse(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuario(),
                StatusPedidoResponse.valueOf(pedido.getStatus()),
                pedido.getItensList().stream().map(this::toResponse).toList(),
                new BigDecimal(pedido.getValorTotal()),
                pedido.getMoeda(),
                Instant.parse(pedido.getCriadoEm())
        );
    }

    private PedidoItemResponse toResponse(PedidoItem item) {
        return new PedidoItemResponse(
                item.getProdutoId(),
                item.getNome(),
                item.getQuantidade(),
                new BigDecimal(item.getPrecoUnitario()),
                new BigDecimal(item.getSubtotal())
        );
    }
}
