package aula.sd.ecommerce.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank @Size(min = 32, message = "deve possuir pelo menos 32 caracteres") String secret,
                            @Positive long expirationSeconds) {}
