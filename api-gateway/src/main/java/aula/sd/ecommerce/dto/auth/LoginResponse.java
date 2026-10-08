package aula.sd.ecommerce.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
