package aula.sd.ecommerce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClienteTcp {

    private static final String HOST = "localhost";
    private static final int PORTA = 5000;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(HOST, PORTA);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            System.out.print("Conexão iniciada com servidor, digite uma mensagem: ");

            String mensagem;
            while ((mensagem = teclado.readLine()) != null) {
                long inicio = System.nanoTime();
                saida.println(mensagem);
                String resposta = entrada.readLine();
                long fim = System.nanoTime();

                System.out.println(resposta);
                System.out.printf("RTT: %.3f ms%n", (fim - inicio) / 1_000_000.0);

                if ("SAIR".equals(mensagem)) {
                    break;
                }
            }
        }
    }
}
