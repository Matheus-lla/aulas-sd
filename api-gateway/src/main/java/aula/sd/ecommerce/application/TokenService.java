package aula.sd.ecommerce.application;

import aula.sd.ecommerce.config.JwtProperties;
import aula.sd.ecommerce.dto.auth.LoginResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TokenService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String PAPEL_ADMIN = "ADMIN";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public LoginResponse emitir(String usuario) {
        Instant agora = clock.instant();
        // O JWT identifica o usuário e expira; assinatura HS256 permite verificar sua origem.
        // O conteúdo é legível: a senha nunca entra nas claims.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                                          .subject(usuario)
                                          .issuedAt(agora)
                                          .expiresAt(agora.plusSeconds(properties.expirationSeconds()))
                                          .claim("papel", PAPEL_ADMIN)
                                          .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                                 .getTokenValue();

        return new LoginResponse(token, TOKEN_TYPE, properties.expirationSeconds());
    }
}
