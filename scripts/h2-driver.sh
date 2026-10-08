#!/usr/bin/env bash
# Compartilhado pelos comandos de banco; usa o H2 já incluído no jar de pedidos.
obter_driver_h2() {
    local jar_pedidos=pedido-service/target/pedido-service-0.0.1-SNAPSHOT.jar
    [[ -f "$jar_pedidos" ]] || { echo 'Compile o projeto Maven antes de usar H2.' >&2; return 1; }
    local dependencia
    dependencia=$(jar tf "$jar_pedidos" | sed -n '/^BOOT-INF\/lib\/h2-[^/]*\.jar$/p')
    [[ -n "$dependencia" ]] || { echo 'Driver H2 não encontrado no jar de pedidos.' >&2; return 1; }
    local jar_absoluto="$PWD/$jar_pedidos"
    mkdir -p target/h2
    (cd target/h2 && jar xf "$jar_absoluto" "$dependencia")
    H2_DRIVER="$PWD/target/h2/$dependencia"
}
