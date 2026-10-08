package aula.sd.ecommerce.dto.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(
        @NotBlank(message = "não deve estar vazio") String produtoId,
        @NotNull(message = "não deve ser nula")
        @Positive(message = "deve ser maior que zero")
        Integer quantidade
) {
}
