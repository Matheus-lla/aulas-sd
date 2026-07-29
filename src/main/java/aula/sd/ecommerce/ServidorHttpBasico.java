package aula.sd.ecommerce;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ServidorHttpBasico {

    private static final int PORTA = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);

        servidor.createContext("/", exchange -> {
            byte[] resposta = "<html><body><h1>Servidor HTTP em execução</h1></body></html>".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, resposta.length);

            try (OutputStream corpo = exchange.getResponseBody()) {
                corpo.write(resposta);
            }
        });

        servidor.start();
        System.out.println("Servidor iniciado em http://localhost:8080/");
    }
}
