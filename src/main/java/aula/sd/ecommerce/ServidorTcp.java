package aula.sd.ecommerce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

public class ServidorTcp {

    private static final int PORTA = 5000;

    public static void main(String[] args) throws IOException {
        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("Servidor TCP ouvindo na porta " + PORTA);

            while (true) {
                try (Socket cliente = servidor.accept();
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter saida = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8), true)
                ) {
                    String mensagem;
                    while ((mensagem = entrada.readLine()) != null) {
                        System.out.println("Mensagem recebida: " + mensagem);
                        saida.println("Mensagem recebida: " + mensagem + " | Horário do servidor: " + LocalTime.now());

                        if ("SAIR".equals(mensagem)) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
