package aula.sd.ecommerce.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "não deve estar vazio") String usuario,
        @NotBlank(message = "não deve estar vazia") String senha
) {
}
