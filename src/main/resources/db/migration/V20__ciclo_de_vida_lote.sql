ALTER TABLE batch
    DROP CHECK chk_batch_active_produced_at,
    DROP CHECK chk_batch_rejection_reason;

ALTER TABLE batch
    MODIFY status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELED')
        COLLATE utf8mb4_0900_as_ci NOT NULL;

ALTER TABLE batch
    ADD CONSTRAINT chk_batch_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    ADD CONSTRAINT chk_batch_produced_at CHECK (status NOT IN ('ACTIVE', 'COMPLETED') OR produced_at IS NOT NULL);

UPDATE batch b
SET b.status = 'COMPLETED'
WHERE b.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM chain c WHERE c.batch_id = b.id AND c.stage_type = 'RETAIL');

UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN users u ON u.id = c.responsible_user_id
SET c.responsible_user_id = COALESCE(
        (SELECT MIN(s.id) FROM users s
         WHERE s.company_id = b.supplier_id
           AND s.is_active
           AND s.role IN ('ADMIN', 'SUPPLIER')),
        c.responsible_user_id)
WHERE c.stage_type <> 'RETAIL'
  AND NOT (u.company_id <=> b.supplier_id);

UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN users u ON u.id = c.responsible_user_id
SET c.responsible_user_id = COALESCE(
        (SELECT MIN(s.id) FROM users s
         WHERE s.company_id = b.market_id
           AND s.is_active
           AND s.role IN ('ADMIN', 'MANAGER')),
        c.responsible_user_id)
WHERE c.stage_type = 'RETAIL'
  AND NOT (u.company_id <=> b.market_id);
