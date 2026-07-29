package aula.sd.ecommerce.pedido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String item;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private String status;

    protected Pedido() {
    }

    public Pedido(String item, Integer quantidade, String status) {
        this.item = item;
        this.quantidade = quantidade;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public String getStatus() {
        return status;
    }
}
