package aula.sd.ecommerce.pedido;

import aula.sd.ecommerce.grpc.CriarPedidoRequest;
import aula.sd.ecommerce.grpc.CriarPedidoResponse;
import aula.sd.ecommerce.grpc.PedidoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Cliente que abre um canal, chama o método remoto e exibe a resposta.
public class ClientePedidosGrpc {

    private static final String HOST = "localhost";
    private static final int PORTA = 9090;

    private static final String ITEM = "notebook";
    private static final int QUANTIDADE = 2;

    public static void main(String[] args) {
        // O canal mantém a conexão usada pelo cliente para se comunicar com o servidor.
        ManagedChannel canal = ManagedChannelBuilder.forAddress(HOST, PORTA).usePlaintext().build();

        // O stub (ponta/canal/cliente) bloqueante aguarda a resposta antes de continuar a execução.
        PedidoServiceGrpc.PedidoServiceBlockingStub stub = PedidoServiceGrpc.newBlockingStub(canal);

        // A classe e seu builder foram gerados a partir de CriarPedidoRequest no .proto.
        CriarPedidoRequest request = CriarPedidoRequest.newBuilder().setItem(ITEM).setQuantidade(QUANTIDADE).build();

        // O stub serializa a requisição, realiza o RPC e desserializa a resposta.
        CriarPedidoResponse response = stub.criarPedido(request);

        System.out.println("Pedido ID: " + response.getPedidoId());
        System.out.println("Status: " + response.getStatus());
        System.out.println("Mensagem: " + response.getMensagem());

        // Libera os recursos de rede mantidos pelo canal.
        canal.shutdown();
    }
}
