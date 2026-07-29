package aula.sd.ecommerce;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidacaoExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratar(MethodArgumentNotValidException exception) {
        Map<String, String> erros = new HashMap<>();
        exception.getBindingResult()
                 .getFieldErrors()
                 .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        Map<String, Object> resposta = Map.of("mensagem", "Dados do pedido inválidos",
                                              "erros", erros);

        return ResponseEntity.badRequest().body(resposta);
    }
}
