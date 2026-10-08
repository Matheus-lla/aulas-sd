#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
usage() { echo "Uso: DEMO_ENV=local $0 {popular|limpar} [h2|postgres]" >&2; exit 1; }
[[ $# -ge 1 && $# -le 2 ]] || usage
acao=$1
[[ "$acao" == popular || "$acao" == limpar ]] || usage
banco=${2:-h2}
[[ "$banco" == h2 || "$banco" == postgres ]] || usage
[[ "${DEMO_ENV:-}" == local ]] || { echo 'Defina DEMO_ENV=local; uso restrito ao laboratório.' >&2; exit 1; }
if [[ "$banco" == postgres ]]; then
    export PGHOST="${PGHOST:-localhost}" PGPORT="${PGPORT:-5432}" PGUSER="${PGUSER:-ecommerce}"
    # Endereços arbitrários e configurações indiretas não podem redirecionar a limpeza.
    case "$PGHOST" in localhost|127.0.0.1|::1) ;; *) echo 'Somente PostgreSQL no host local é permitido.' >&2; exit 1 ;; esac
    [[ "$PGPORT" =~ ^[0-9]+$ ]] || usage
    unset PGSERVICE PGSERVICEFILE PGOPTIONS PGHOSTADDR
    command -v psql >/dev/null || { echo 'Instale o cliente PostgreSQL (psql).' >&2; exit 1; }
    for destino in pedidos_demo estoque_demo; do
        atual=$(psql -X -w -d "$destino" -Atc 'SELECT current_database()')
        [[ "$atual" == "$destino" ]] || { echo 'Banco de demonstração inesperado.' >&2; exit 1; }
    done
else
    # Destinos locais fixos; não lemos URLs arbitrárias para uma operação destrutiva.
    [[ -f data/pedidos_demo.mv.db && -f data/estoque_python_demo.mv.db ]] || {
        echo 'Inicie pedidos e estoque com H2 uma vez para criar os bancos e depois pare-os.' >&2; exit 1;
    }
    source scripts/h2-driver.sh
    obter_driver_h2
    temporario=$(mktemp -d)
    trap 'rm -rf "$temporario"' EXIT
    cat > "$temporario/ExecutarSql.java" <<'JAVA'
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
public class ExecutarSql {
    public static void main(String[] args) throws Exception {
        try (var connection = DriverManager.getConnection(args[0], "sa", args[1])) {
            connection.setAutoCommit(false);
            if (args.length == 2) return; // Somente verifica arquivo/credencial antes de alterar dados.
            try (var reader = Files.newBufferedReader(Path.of(args[2]))) {
                org.h2.tools.RunScript.execute(connection, reader);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }
}
JAVA
    # Falha antes de modificar qualquer banco se um serviço ainda mantiver o arquivo aberto.
    java -cp "$H2_DRIVER" "$temporario/ExecutarSql.java" \
        'jdbc:h2:file:./data/pedidos_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE' ''
    java -cp "$H2_DRIVER" "$temporario/ExecutarSql.java" \
        'jdbc:h2:file:./data/estoque_python_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE' sa
fi
if [[ "$acao" == limpar ]]; then
    echo "Esta operação apagará somente pedidos, itens e produtos dos bancos de demonstração ($banco)."
    if [[ "$banco" == postgres ]]; then
        echo "Conexão: ${PGHOST}:${PGPORT}, usuário ${PGUSER}."
    else
        echo 'Arquivos: data/pedidos_demo.mv.db e data/estoque_python_demo.mv.db.'
    fi
    echo 'Pare os serviços antes de continuar. A estrutura das tabelas será preservada.'
    read -r -p 'Digite LIMPAR DEMO para confirmar: ' confirmacao
    [[ "$confirmacao" == 'LIMPAR DEMO' ]] || { echo 'Limpeza cancelada.'; exit 1; }
fi
for servico in pedidos estoque; do
    arquivo="scripts/sql/$servico.sql"
    [[ "$acao" == limpar ]] && arquivo="scripts/sql/limpar-$servico.sql"
    # Cada banco tem sua própria transação; nenhum serviço compartilha tabelas.
    if [[ "$banco" == postgres ]]; then
        psql -X -w -v ON_ERROR_STOP=1 --single-transaction -d "${servico}_demo" -f "$arquivo"
    else
        destino=pedidos_demo
        senha=''
        [[ "$servico" == estoque ]] && { destino=estoque_python_demo; senha=sa; }
        java -cp "$H2_DRIVER" "$temporario/ExecutarSql.java" \
            "jdbc:h2:file:./data/${destino};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE" "$senha" "$arquivo"
    fi
    echo "$servico: $acao concluído."
done
