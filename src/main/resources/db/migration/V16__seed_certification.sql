INSERT INTO certification (
    supplier_id,
    certification,
    issuing_body,
    issued_at,
    expires_at,
    status
) VALUES
(1, 'Orgânico Brasil', 'MAPA', '2025-01-15', '2027-01-14', 'ACTIVE'),
(1, 'Fairtrade', 'Fairtrade International', '2024-06-01', '2026-05-31', 'EXPIRED'),
(2, 'Rainforest Alliance', 'Rainforest Alliance', '2025-03-10', '2027-03-09', 'ACTIVE'),
(3, 'ISO 14001', 'ABNT', '2025-08-20', '2028-08-19', 'ACTIVE'),
(4, 'CAR Regularizado', 'Secretaria Estadual do Meio Ambiente', '2024-02-12', '2026-02-11', 'EXPIRED'),
(5, 'Orgânico Brasil', 'MAPA', '2025-05-05', '2027-05-04', 'ACTIVE'),
(6, 'ISO 14001', 'ABNT', '2025-09-01', '2028-08-31', 'ACTIVE'),
(7, 'Programa de Logística Verde', 'Instituto Logística Sustentável', '2025-07-18', '2027-07-17', 'UNDER_REVIEW'),
(8, 'RenovaBio', 'ANP', '2025-04-22', '2026-04-21', 'EXPIRED'),
(9, 'FSC Cadeia de Custódia', 'FSC Brasil', '2025-11-11', '2028-11-10', 'ACTIVE'),
(10, 'ISO 19011 Auditoria', 'ABNT', '2025-10-03', '2027-10-02', 'ACTIVE'),
(11, 'ABVTEX', 'ABVTEX', '2025-06-14', '2027-06-13', 'SUSPENDED');