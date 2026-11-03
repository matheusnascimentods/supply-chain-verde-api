INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Rua da Sustentabilidade', '100', 'Centro', NULL, '01000-000', 'São Paulo', 'SP');

INSERT INTO company (name, cnpj, address_id, phone, email, type)
SELECT 'Fornecedor Seed', '11222333000181', id, '11999999999', 'fornecedor.seed@supplychainverde.local', 'SUPPLIER'
FROM address
WHERE city = 'São Paulo'
  AND street = 'Rua da Sustentabilidade';

INSERT INTO report (supplier_id, market_id, period_start_at, period_end_at, total_co2_kg, tracked_product_count)
SELECT s.id, (SELECT MIN(m.id) FROM company m WHERE m.type = 'MARKET'), DATE '2026-01-01', DATE '2026-03-31', 125.5000, 4
FROM company s
WHERE s.cnpj = '11222333000181';
