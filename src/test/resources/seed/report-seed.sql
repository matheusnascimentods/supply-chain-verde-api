INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Rua da Sustentabilidade', '100', 'Centro', NULL, '01000-000', 'São Paulo', 'SP');

INSERT INTO supplier (name, cnpj, address_id, phone)
SELECT 'Fornecedor Seed', '11222333000181', address_id, '11999999999'
FROM address
WHERE city = 'São Paulo'
  AND street = 'Rua da Sustentabilidade';

INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, tracked_product_count)
SELECT supplier_id, DATE '2026-01-01', DATE '2026-03-31', 125.5000, 4
FROM supplier
WHERE cnpj = '11222333000181';
