package aula.sd.ecommerce;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, String>> tratar(StatusRuntimeException exception) {
        if (exception.getStatus().getCode() == Status.Code.UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body(Map.of("mensagem", "Serviço de pedidos indisponível"));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(Map.of("mensagem", "Erro na comunicação gRPC"));
    }
}
