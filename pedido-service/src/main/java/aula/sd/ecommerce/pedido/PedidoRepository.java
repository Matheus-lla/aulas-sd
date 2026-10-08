package aula.sd.ecommerce.pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByUsuarioOrderByCriadoEmDesc(String usuario);

    Optional<Pedido> findByIdAndUsuario(UUID id, String usuario);
}
