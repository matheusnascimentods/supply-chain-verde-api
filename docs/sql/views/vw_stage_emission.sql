-- Current definition from V9__drop_supplier.sql.
CREATE OR REPLACE VIEW vw_stage_emission AS
SELECT b.id AS batch_id,
       b.supplier_id,
       b.market_id,
       p.id AS product_id,
       p.name AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.id AS chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       DATE(ce.created_at) AS calculated_at
FROM batch b
JOIN product p ON p.id = b.product_id
JOIN chain c ON c.batch_id = b.id
JOIN carbon_emission ce ON ce.chain_id = c.id;
