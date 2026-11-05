-- Seed de demonstração: mais usuários com os perfis que não são ADMIN, e lotes pendentes, ativos e recusados.
-- Senha de todos os usuários novos: a mesma do V2 (rolocompressor06).

-- Seeds não geram eventos (RN-07 da V5). Os eventos desta migration são apagados no fim.
SET @audit_before = (SELECT COALESCE(MAX(id), 0) FROM audit_log);
SET @password = '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm';
SET @market_1 = (SELECT id FROM company WHERE cnpj = '20202020000152');
SET @market_2 = (SELECT id FROM company WHERE cnpj = '30303030000138');
SET @market_3 = (SELECT id FROM company WHERE cnpj = '40404040000113');

-- 1. Usuários criados na V10 ficam sem o .example no email.
UPDATE users
SET email = REPLACE(email, '.example.com', '.com')
WHERE email LIKE '%@liverpool.example.com'
   OR email LIKE '%@arsenal.example.com'
   OR email LIKE '%@chelsea.example.com'
   OR email LIKE '%@manutd.example.com';

-- 2. Jogadores novos. market preenchido: vão para aquele mercado com o perfil indicado, que completa os 10 usuários
-- do plano PRO. Nulo: viram SUPPLIER de um fornecedor, na ordem de seq (o BASIC aceita 3 usuários e cada um tem 1).
CREATE TEMPORARY TABLE tmp_player (
    seq    INT PRIMARY KEY,
    name   VARCHAR(180) NOT NULL,
    email  VARCHAR(180) NOT NULL,
    role   VARCHAR(20)  NOT NULL,
    market TINYINT      NULL
);

INSERT INTO tmp_player (seq, name, email, role, market) VALUES
    (1, 'M. Maignan', 'm.maignan@milan.com', 'MANAGER', 1),
    (2, 'L. Modrić', 'l.modric@milan.com', 'AUDITOR', 1),
    (3, 'N. Barella', 'n.barella@inter.com', 'MANAGER', 2),
    (4, 'H. Çalhanoğlu', 'h.calhanoglu@inter.com', 'AUDITOR', 2),
    (5, 'G. Di Lorenzo', 'g.dilorenzo@napoli.com', 'MANAGER', 3),
    (6, 'K. De Bruyne', 'k.debruyne@napoli.com', 'AUDITOR', 3),
    (7, 'M. Gila', 'm.gila@milan.com', 'SUPPLIER', NULL),
    (8, 'K. De Winter', 'k.dewinter@milan.com', 'SUPPLIER', NULL),
    (9, 'S. Pavlović', 's.pavlovic@milan.com', 'SUPPLIER', NULL),
    (10, 'D. Moreira', 'd.moreira@milan.com', 'SUPPLIER', NULL),
    (11, 'A. Rabiot', 'a.rabiot@milan.com', 'SUPPLIER', NULL),
    (12, 'Y. Musah', 'y.musah@milan.com', 'SUPPLIER', NULL),
    (13, 'C. Pulišić', 'c.pulisic@milan.com', 'SUPPLIER', NULL),
    (14, 'S. Diawara', 's.diawara@milan.com', 'SUPPLIER', NULL),
    (15, 'G. Ramos', 'g.ramos@milan.com', 'SUPPLIER', NULL),
    (16, 'J. Martínez', 'j.martinez@inter.com', 'SUPPLIER', NULL),
    (17, 'A. Bastoni', 'a.bastoni@inter.com', 'SUPPLIER', NULL),
    (18, 'M. Akanji', 'm.akanji@inter.com', 'SUPPLIER', NULL),
    (19, 'Y. Bisseck', 'y.bisseck@inter.com', 'SUPPLIER', NULL),
    (20, 'C. Augusto', 'c.augusto@inter.com', 'SUPPLIER', NULL),
    (21, 'D. Spence', 'd.spence@inter.com', 'SUPPLIER', NULL),
    (22, 'P. Sučić', 'p.sucic@inter.com', 'SUPPLIER', NULL),
    (23, 'A. Diouf', 'a.diouf@inter.com', 'SUPPLIER', NULL),
    (24, 'M. Thuram', 'm.thuram@inter.com', 'SUPPLIER', NULL),
    (25, 'G. Vicario', 'g.vicario@juventus.com', 'SUPPLIER', NULL),
    (26, 'P. Kalulu', 'p.kalulu@juventus.com', 'SUPPLIER', NULL),
    (27, 'Bremer', 'bremer@juventus.com', 'SUPPLIER', NULL),
    (28, 'J. Cabal', 'j.cabal@juventus.com', 'SUPPLIER', NULL),
    (29, 'J. Lucumí', 'j.lucumi@juventus.com', 'SUPPLIER', NULL),
    (30, 'D. Luiz', 'd.luiz@juventus.com', 'SUPPLIER', NULL),
    (31, 'T. Koopmeiners', 't.koopmeiners@juventus.com', 'SUPPLIER', NULL),
    (32, 'W. McKennie', 'w.mckennie@juventus.com', 'SUPPLIER', NULL),
    (33, 'K. Alajbegović', 'k.alajbegovic@juventus.com', 'SUPPLIER', NULL),
    (34, 'N. Woltemade', 'n.woltemade@juventus.com', 'SUPPLIER', NULL),
    (35, 'R. Kolo Muani', 'r.kolomuani@juventus.com', 'SUPPLIER', NULL),
    (36, 'A. Meret', 'a.meret@napoli.com', 'SUPPLIER', NULL),
    (37, 'M. Olivera', 'm.olivera@napoli.com', 'SUPPLIER', NULL),
    (38, 'R. Marín', 'r.marin@napoli.com', 'SUPPLIER', NULL),
    (39, 'L. Marianucci', 'l.marianucci@napoli.com', 'SUPPLIER', NULL),
    (40, 'S. McTominay', 's.mctominay@napoli.com', 'SUPPLIER', NULL),
    (41, 'F. Anguissa', 'f.anguissa@napoli.com', 'SUPPLIER', NULL),
    (42, 'N. Lang', 'n.lang@napoli.com', 'SUPPLIER', NULL),
    (43, 'L. Lucca', 'l.lucca@napoli.com', 'SUPPLIER', NULL),
    (44, 'M. Politano', 'm.politano@napoli.com', 'SUPPLIER', NULL);

-- 2a. Seis jogadores nos mercados: cada mercado fica com 10 usuários.
INSERT INTO users (name, email, password, role, company_id)
SELECT name, email, @password, role, CASE market WHEN 1 THEN @market_1 WHEN 2 THEN @market_2 ELSE @market_3 END
FROM tmp_player
WHERE market IS NOT NULL;

-- 2b. Os outros 38 jogadores, um por fornecedor, na ordem do id.
CREATE TEMPORARY TABLE tmp_supplier_player AS
SELECT name, email, ROW_NUMBER() OVER (ORDER BY seq) AS position
FROM tmp_player
WHERE market IS NULL;

CREATE TEMPORARY TABLE tmp_supplier AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS position
FROM company
WHERE type = 'SUPPLIER';

INSERT INTO users (name, email, password, role, company_id)
SELECT p.name, p.email, @password, 'SUPPLIER', s.id
FROM tmp_supplier_player p
JOIN tmp_supplier s ON s.position = p.position;

DROP TEMPORARY TABLE tmp_supplier;
DROP TEMPORARY TABLE tmp_supplier_player;
DROP TEMPORARY TABLE tmp_player;

-- 3. Três lotes por fornecedor, com o produto de menor id dele, cada um num mercado: um PENDING (sem data de
-- produção), um ACTIVE e um REJECTED. Os lotes não têm etapas, então os relatórios existentes não mudam (RN-23).
CREATE TEMPORARY TABLE tmp_supplier_product AS
SELECT supplier_id, MIN(id) AS product_id
FROM product
GROUP BY supplier_id;

CREATE TEMPORARY TABLE tmp_market (slot TINYINT PRIMARY KEY, id BIGINT NOT NULL);
INSERT INTO tmp_market VALUES (0, @market_1), (1, @market_2), (2, @market_3);

CREATE TEMPORARY TABLE tmp_rejection (slot TINYINT PRIMARY KEY, reason VARCHAR(500) NOT NULL);
INSERT INTO tmp_rejection VALUES
    (0, 'Sem capacidade de produção no período solicitado.'),
    (1, 'Quantidade acima do volume da safra atual.'),
    (2, 'Produto em entressafra; nova previsão no próximo trimestre.'),
    (3, 'Certificação em revisão; o lote não pode ser emitido agora.');

INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, created_at)
SELECT sp.product_id, sp.supplier_id, m.id, 'PENDING', 100 + sp.supplier_id * 15,
       TIMESTAMP('2026-10-20') + INTERVAL sp.supplier_id HOUR
FROM tmp_supplier_product sp
JOIN tmp_market m ON m.slot = sp.supplier_id MOD 3;

INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, produced_at, created_at)
SELECT sp.product_id, sp.supplier_id, m.id, 'ACTIVE', 250 + sp.supplier_id * 20,
       DATE('2026-08-01') + INTERVAL sp.supplier_id DAY, DATE('2026-08-01') + INTERVAL sp.supplier_id DAY
FROM tmp_supplier_product sp
JOIN tmp_market m ON m.slot = (sp.supplier_id + 1) MOD 3;

INSERT INTO batch (product_id, supplier_id, market_id, status, rejection_reason, quantity, created_at)
SELECT sp.product_id, sp.supplier_id, m.id, 'REJECTED', r.reason, 500 + sp.supplier_id * 30,
       DATE('2026-09-01') + INTERVAL sp.supplier_id DAY
FROM tmp_supplier_product sp
JOIN tmp_market m ON m.slot = (sp.supplier_id + 2) MOD 3
JOIN tmp_rejection r ON r.slot = sp.supplier_id MOD 4;

DROP TEMPORARY TABLE tmp_rejection;
DROP TEMPORARY TABLE tmp_market;
DROP TEMPORARY TABLE tmp_supplier_product;

DELETE FROM audit_log WHERE id > @audit_before;
