package aula.sd.ecommerce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoRequest(@NotBlank(message = "não deve estar vazio") String item,
                            @NotNull(message = "não deve ser nula") @Positive(message = "deve ser maior que zero") Integer quantidade) {}
