package aula.sd.ecommerce;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Faz o Spring carregar esta classe como fonte de configuração.
// Centraliza a proteção das rotas e a validação dos tokens no gateway.
@Configuration
public class SecurityConfig {

    // Deve ser igual ao segredo de GeradorTokenJwt para validar os tokens que ele assina.
    private static final String SEGREDO_JWT = "segredo-didatico-com-tamanho-suficiente-para-hs256";

    // Registra a cadeia de filtros que o Spring Security usará nas requisições.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Desativa o token CSRF neste exemplo de API que usa JWT no cabeçalho Authorization.
        http.csrf(AbstractHttpConfigurer::disable)
            // STATELESS evita criar ou usar sessão HTTP; cada chamada deve apresentar seu token.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Seleciona POST /pedidos como a requisição que exige autenticação.
            .authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.POST, "/pedidos")
                                                         // Exige uma autenticação válida antes de permitir o acesso à rota.
                                                         .authenticated()
                                                         // Seleciona todas as demais requisições, fora da regra anterior.
                                                         .anyRequest()
                                                         // Libera essas outras requisições sem exigir autenticação.
                                                         .permitAll())
            // Ativa a autenticação por Bearer JWT usando o JwtDecoder registrado abaixo.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // Constrói e devolve a cadeia que autentica antes do controller e da validação do DTO.
        return http.build();
    }

    // Cria o componente que verificará os tokens apresentados pelo cliente.
    @Bean
    JwtDecoder jwtDecoder() {
        // Recria a mesma chave HMAC-SHA256 usada para assinar no gerador.
        SecretKey chave = new SecretKeySpec(SEGREDO_JWT.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        // Aceita assinaturas HS256 verificadas com essa chave e usa a validação padrão de datas.
        // Ler o payload sozinho não autentica: a assinatura e a validade precisam ser verificadas.
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
