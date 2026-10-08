package aula.sd.ecommerce.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.grpc")
public record GrpcProperties(@NotNull Duration deadline) {

    public GrpcProperties {
        if (deadline != null && (deadline.isZero() || deadline.isNegative())) {
            throw new IllegalArgumentException("app.grpc.deadline deve ser positiva");
        }
    }
}
