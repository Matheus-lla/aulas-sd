package aula.sd.ecommerce.pedido;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ItemPedido {
    @Column(nullable = false, length = 80)
    private String produtoId;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false)
    private int quantidade;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected ItemPedido() {
    }

    public ItemPedido(aula.sd.ecommerce.grpc.ItemReservado item) {
        produtoId = item.getProdutoId();
        nome = item.getNome();
        quantidade = item.getQuantidade();
        precoUnitario = new BigDecimal(item.getPrecoUnitario());
        subtotal = new BigDecimal(item.getSubtotal());
    }

    public aula.sd.ecommerce.grpc.PedidoItem toGrpc() {
        return aula.sd.ecommerce.grpc.PedidoItem.newBuilder()
                                                .setProdutoId(produtoId)
                                                .setNome(nome)
                                                .setQuantidade(quantidade)
                                                .setPrecoUnitario(precoUnitario.toPlainString())
                                                .setSubtotal(subtotal.toPlainString())
                                                .build();
    }
}
