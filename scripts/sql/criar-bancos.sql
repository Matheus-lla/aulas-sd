-- Executar uma única vez como administrador do PostgreSQL local, fora de transação.
-- Bancos separados demonstram a propriedade dos dados de cada serviço.
CREATE ROLE ecommerce LOGIN PASSWORD 'ecommerce';
CREATE DATABASE estoque_demo OWNER ecommerce;
CREATE DATABASE pedidos_demo OWNER ecommerce;
