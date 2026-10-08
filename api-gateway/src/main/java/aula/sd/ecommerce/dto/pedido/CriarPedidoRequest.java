package aula.sd.ecommerce.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPedidoRequest(
        @NotNull(message = "não deve ser nula")
        @Size(min = 1, message = "deve conter pelo menos um item")
        List<@NotNull(message = "não deve ser nulo") @Valid ItemPedidoRequest> itens
) {
}
