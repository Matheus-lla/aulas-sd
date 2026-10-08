package aula.sd.ecommerce.pedido;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String usuario;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;
    @Column(nullable = false, length = 3)
    private String moeda;
    @Column(nullable = false)
    private Instant criadoEm;
    @ElementCollection
    @CollectionTable(name = "pedido_itens", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicao")
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(String usuario, aula.sd.ecommerce.grpc.ReservarEstoqueResponse reserva) {
        this.usuario = usuario;
        status = "CONFIRMADO";
        criadoEm = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        moeda = reserva.getMoeda();
        valorTotal = new BigDecimal(reserva.getValorTotal());
        reserva.getItensList().forEach(item -> itens.add(new ItemPedido(item)));
    }

    public aula.sd.ecommerce.grpc.Pedido toGrpc() {
        var resposta = aula.sd.ecommerce.grpc.Pedido.newBuilder()
                                                    .setId(id.toString())
                                                    .setUsuario(usuario)
                                                    .setStatus(status)
                                                    .setValorTotal(valorTotal.toPlainString())
                                                    .setMoeda(moeda)
                                                    .setCriadoEm(criadoEm.toString());
        itens.forEach(item -> resposta.addItens(item.toGrpc()));
        return resposta.build();
    }
}
