package aula.sd.ecommerce.error;

import io.grpc.StatusRuntimeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    // A borda traduz os códigos gRPC para HTTP sem criar uma hierarquia de exceções.
    @ExceptionHandler(StatusRuntimeException.class)
    ResponseEntity<?> grpc(StatusRuntimeException e) {
        int status = switch (e.getStatus().getCode()) {
            case INVALID_ARGUMENT -> 400;
            case NOT_FOUND -> 404;
            case FAILED_PRECONDITION, ALREADY_EXISTS, ABORTED -> 409;
            case UNAVAILABLE -> 503;
            case DEADLINE_EXCEEDED -> 504;
            default -> 500;
        };
        String message = switch (status) {
            case 503 -> "Serviço temporariamente indisponível.";
            case 504 -> "O serviço excedeu o prazo de resposta. Consulte os pedidos antes de tentar novamente.";
            case 500 -> "Falha interna ao processar a operação.";
            default -> e.getStatus().getDescription();
        };
        return ResponseEntity.status(status).body(Map.of("message", message == null ? "Falha na operação." : message));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("message", message == null ? "Dados inválidos." : message));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> json(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Corpo JSON inválido."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> http(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Falha na operação." : e.getReason()));
    }
}
