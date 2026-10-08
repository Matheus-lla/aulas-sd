package aula.sd.ecommerce;

import aula.sd.ecommerce.config.AuthProperties;
import aula.sd.ecommerce.config.CorsProperties;
import aula.sd.ecommerce.config.GrpcProperties;
import aula.sd.ecommerce.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AuthProperties.class,
        CorsProperties.class,
        GrpcProperties.class,
        JwtProperties.class
})
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
