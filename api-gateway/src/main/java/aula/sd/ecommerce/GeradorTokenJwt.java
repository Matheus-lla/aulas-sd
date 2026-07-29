package aula.sd.ecommerce;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

// Simula a emissão de um JWT pelo terminal; não implementa login com usuário e senha.
public class GeradorTokenJwt {

    // Segredo didático compartilhado com SecurityConfig; HS256 exige pelo menos 32 bytes.
    private static final String SEGREDO_JWT = "segredo-didatico-com-tamanho-suficiente-para-hs256";
    // Identifica o sujeito do token; em um login real, viria do usuário autenticado.
    private static final String SUBJECT = "aluno";
    // Define que a expiração será 30 minutos depois da emissão.
    private static final Duration DURACAO = Duration.ofMinutes(30);

    // Executa o gerador como programa independente, sem iniciar o servidor Spring.
    public static void main(String[] args) {
        // Converte o segredo em uma chave HMAC-SHA256, usado pelo algoritmo JWT HS256.
        SecretKey chave = new SecretKeySpec(SEGREDO_JWT.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        // Configura o assinador com a chave compartilhada e o algoritmo HS256.
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build();
        // Captura o instante atual para calcular as datas do mesmo token.
        Instant agora = Instant.now();
        // Inicia a construção do payload; essas informações são legíveis, não criptografadas.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                                          // Define a claim sub, que identifica o sujeito como aluno.
                                          .subject(SUBJECT)
                                          // Define a claim iat, indicando quando o token foi emitido.
                                          .issuedAt(agora)
                                          // Define a claim exp, indicando até quando o token será aceito.
                                          .expiresAt(agora.plus(DURACAO))
                                          // Finaliza o conjunto de claims que será assinado.
                                          .build();
        // Cria o cabeçalho com alg=HS256 para identificar o algoritmo da assinatura.
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        // Junta cabeçalho e claims, assina com a chave e obtém cabeçalho.payload.assinatura.
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

        // Imprime o JWT para enviá-lo no Postman ou no cabeçalho Authorization: Bearer TOKEN.
        System.out.println(token);
    }
}
