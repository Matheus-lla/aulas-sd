package aula.sd.ecommerce.application;

import aula.sd.ecommerce.config.AuthProperties;
import aula.sd.ecommerce.dto.auth.LoginRequest;
import aula.sd.ecommerce.dto.auth.LoginResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthProperties properties;
    private final TokenService tokenService;

    public AuthService(AuthProperties properties, TokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        // Login de laboratório: preserva a credencial configurada, sem provedor externo.
        boolean credenciaisValidas = properties.username().equals(request.usuario()) && properties.password()
                                                                                                  .equals(request.senha());

        if (!credenciaisValidas) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }

        return tokenService.emitir(properties.username());
    }
}
