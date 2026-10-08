-- Banco estoque_demo. Inicie o estoque Python uma vez para criar as tabelas.
-- Saldos iniciais já descontam o pedido fictício em pedidos.sql.
-- Repetir a população não repõe estoque consumido nem altera produtos existentes.
INSERT INTO public.produtos (id, nome, descricao, preco, moeda, quantidade_disponivel)
SELECT 'notebook', 'Notebook', 'Notebook para dev', 3500.00, 'BRL', 9
WHERE NOT EXISTS (SELECT 1 FROM public.produtos WHERE id = 'notebook');

INSERT INTO public.produtos (id, nome, descricao, preco, moeda, quantidade_disponivel)
SELECT 'teclado', 'Teclado', 'Teclado mecânico USB', 250.00, 'BRL', 19
WHERE NOT EXISTS (SELECT 1 FROM public.produtos WHERE id = 'teclado');

INSERT INTO public.produtos (id, nome, descricao, preco, moeda, quantidade_disponivel)
SELECT 'mouse', 'Mouse', 'Mouse óptico sem fio', 120.00, 'BRL', 30
WHERE NOT EXISTS (SELECT 1 FROM public.produtos WHERE id = 'mouse');
