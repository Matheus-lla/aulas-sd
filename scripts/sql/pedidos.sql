-- Banco pedidos_demo. Produtos são referências externas, sem FK entre bancos.
-- Pode ser repetido: insere exemplos ausentes e preserva os registros existentes.
INSERT INTO public.pedidos (id, usuario, status, valor_total, moeda, criado_em)
SELECT CAST('11111111-1111-4111-8111-111111111111' AS UUID),
       'admin', 'CONFIRMADO', 3750.00, 'BRL',
       CAST('2026-10-01T13:00:00Z' AS TIMESTAMP WITH TIME ZONE)
WHERE NOT EXISTS (
    SELECT 1 FROM public.pedidos
    WHERE id = CAST('11111111-1111-4111-8111-111111111111' AS UUID)
);

INSERT INTO public.pedido_itens (pedido_id, posicao, produto_id, nome, quantidade, preco_unitario, subtotal)
SELECT CAST('11111111-1111-4111-8111-111111111111' AS UUID),
       0, 'notebook', 'Notebook', 1, 3500.00, 3500.00
WHERE NOT EXISTS (
    SELECT 1 FROM public.pedido_itens
    WHERE pedido_id = CAST('11111111-1111-4111-8111-111111111111' AS UUID) AND posicao = 0
);

INSERT INTO public.pedido_itens (pedido_id, posicao, produto_id, nome, quantidade, preco_unitario, subtotal)
SELECT CAST('11111111-1111-4111-8111-111111111111' AS UUID),
       1, 'teclado', 'Teclado', 1, 250.00, 250.00
WHERE NOT EXISTS (
    SELECT 1 FROM public.pedido_itens
    WHERE pedido_id = CAST('11111111-1111-4111-8111-111111111111' AS UUID) AND posicao = 1
);
